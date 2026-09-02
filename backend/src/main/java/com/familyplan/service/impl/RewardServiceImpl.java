package com.familyplan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.familyplan.common.BusinessException;
import com.familyplan.dto.RewardCategoryItem;
import com.familyplan.dto.RewardEarnRequest;
import com.familyplan.dto.RewardSpendRequest;
import com.familyplan.dto.RewardSummary;
import com.familyplan.entity.FamilyMember;
import com.familyplan.entity.RewardAccount;
import com.familyplan.entity.RewardItem;
import com.familyplan.entity.RewardTxn;
import com.familyplan.mapper.RewardAccountMapper;
import com.familyplan.mapper.RewardTxnMapper;
import com.familyplan.service.IFamilyMemberService;
import com.familyplan.service.IRewardItemService;
import com.familyplan.service.IRewardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 奖励额度服务实现
 *
 * 账务不变量: 余额只由 status=CONFIRMED 的流水决定, 因此消费一律先落 PENDING,
 * 确认时用条件更新扣减(影响行数 0 即额度不足); 发放/赚取/手动调整由父母操作即生效。
 */
@Slf4j
@Service
public class RewardServiceImpl implements IRewardService {

    private static final String ITEM_TYPE_EARN = "EARN";
    private static final String ITEM_TYPE_SPEND = "SPEND";
    private static final String MODE_DURATION = "DURATION";

    private static final String DIR_GRANT = "GRANT";
    private static final String DIR_EARN = "EARN";
    private static final String DIR_SPEND = "SPEND";
    private static final String DIR_EXPIRE = "EXPIRE";
    private static final String DIR_ADJUST = "ADJUST";

    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String REJECTED = "REJECTED";

    private static final String PERIOD_MONTH = "MONTH";

    @Autowired
    private RewardAccountMapper rewardAccountMapper;
    @Autowired
    private RewardTxnMapper rewardTxnMapper;
    @Autowired
    private IRewardItemService rewardItemService;
    @Autowired
    private IFamilyMemberService familyMemberService;

    @Override
    public RewardAccount getOrCreateAccount(Long familyId, Long memberId) {
        familyMemberService.checkMemberBelongsToFamily(familyId, memberId);
        RewardAccount account = selectAccount(memberId);
        if (account != null) {
            return account;
        }
        account = new RewardAccount();
        account.setFamilyId(familyId);
        account.setMemberId(memberId);
        account.setBalance(0);
        account.setWeeklyQuota(0);
        account.setPeriodType("WEEK");
        account.setCarryOver(1);
        account.setStatus(1);
        rewardAccountMapper.insert(account);
        log.info("已为孩子 {} 创建奖励额度账户", memberId);
        return account;
    }

    @Override
    public void saveAccount(Long familyId, RewardAccount req) {
        if (req.getMemberId() == null) {
            throw new BusinessException("请先选择孩子");
        }
        RewardAccount account = getOrCreateAccount(familyId, req.getMemberId());
        if (req.getPeriodType() != null && !req.getPeriodType().isEmpty()
                && !"WEEK".equals(req.getPeriodType()) && !PERIOD_MONTH.equals(req.getPeriodType())) {
            throw new BusinessException("发放周期只能是 WEEK(周) 或 MONTH(月)");
        }
        if (req.getWeeklyQuota() != null && req.getWeeklyQuota() < 0) {
            throw new BusinessException("每周发放额度不能为负");
        }
        // 只按更新对象上的非空字段落库, balance 不在其中, 无法被前端篡改
        RewardAccount update = new RewardAccount();
        update.setId(account.getId());
        update.setWeeklyQuota(req.getWeeklyQuota());
        update.setPeriodType(req.getPeriodType());
        update.setCarryOver(req.getCarryOver() == null ? null : (req.getCarryOver() == 0 ? 0 : 1));
        update.setRemark(req.getRemark());
        rewardAccountMapper.updateById(update);
        log.info("保存奖励额度设置成功: 成员={}, 每周额度={}", account.getMemberId(), req.getWeeklyQuota());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardTxn grantPeriod(Long familyId, Long memberId, Integer points, Long operatorMemberId) {
        getOrCreateAccount(familyId, memberId);
        // 锁住账户行, 让同一孩子的发放/扣减串行, 保证"本周期只发一次"成立
        RewardAccount account = rewardAccountMapper.selectForUpdate(memberId);
        String periodKey = currentPeriodKey(account.getPeriodType());
        int amount = points != null ? points : account.getWeeklyQuota();
        if (amount <= 0) {
            throw new BusinessException("请先在额度设置里填写每周发放额度");
        }
        if (existsGrant(memberId, periodKey)) {
            throw new BusinessException("本周期(" + periodKey + ")的额度已经发放过了");
        }

        // 结转关闭时, 新周期发放前先把结余作废, 让额度只在本周期内有效
        if (account.getCarryOver() == 0 && account.getBalance() > 0) {
            int leftover = account.getBalance();
            rewardAccountMapper.deductBalance(memberId, leftover);
            RewardTxn expire = newTxn(familyId, memberId, periodKey, operatorMemberId);
            expire.setItemName("周期额度清零");
            expire.setCategory("OTHER");
            expire.setDirection(DIR_EXPIRE);
            expire.setStatus(CONFIRMED);
            expire.setPoints(-leftover);
            expire.setRemark("结转已关闭，上周期结余 " + leftover + " 额度作废");
            persist(expire);
        }

        rewardAccountMapper.increaseBalance(memberId, amount);
        RewardTxn grant = newTxn(familyId, memberId, periodKey, operatorMemberId);
        grant.setItemName("周期额度发放");
        grant.setCategory("OTHER");
        grant.setDirection(DIR_GRANT);
        grant.setStatus(CONFIRMED);
        grant.setPoints(amount);
        grant.setRemark("发放 " + periodKey + " 额度 " + amount + " 个");
        return persist(grant);
    }

    @Override
    public RewardSummary summary(Long familyId, Long memberId, String periodKey) {
        RewardAccount account = getOrCreateAccount(familyId, memberId);
        String key = (periodKey == null || periodKey.isEmpty())
                ? currentPeriodKey(account.getPeriodType()) : periodKey;

        RewardSummary summary = new RewardSummary();
        summary.setMemberId(memberId);
        FamilyMember member = familyMemberService.getById(memberId);
        summary.setMemberName(member == null ? "" : member.getMemberName());
        summary.setBalance(account.getBalance());
        summary.setWeeklyQuota(account.getWeeklyQuota());
        summary.setPeriodType(account.getPeriodType());
        summary.setCarryOver(account.getCarryOver());
        summary.setPeriodKey(key);
        summary.setGranted(0);
        summary.setEarned(0);
        summary.setSpent(0);
        summary.setExpired(0);
        summary.setAdjusted(0);

        for (Map<String, Object> row : rewardTxnMapper.sumByDirection(memberId, key)) {
            String direction = (String) row.get("direction");
            int total = ((Number) row.get("total")).intValue();
            switch (direction) {
                case DIR_GRANT -> summary.setGranted(total);
                case DIR_EARN -> summary.setEarned(total);
                // 消费与清零在流水里存负值, 概览按正数展示消耗量
                case DIR_SPEND -> summary.setSpent(Math.abs(total));
                case DIR_EXPIRE -> summary.setExpired(Math.abs(total));
                case DIR_ADJUST -> summary.setAdjusted(total);
                default -> log.warn("未知的额度流水方向: {}", direction);
            }
        }

        List<RewardCategoryItem> byCategory = new ArrayList<>();
        for (Map<String, Object> row : rewardTxnMapper.sumSpendByCategory(memberId, key)) {
            byCategory.add(new RewardCategoryItem((String) row.get("category"),
                    ((Number) row.get("total")).intValue()));
        }
        summary.setSpendByCategory(byCategory);

        LambdaQueryWrapper<RewardTxn> pending = new LambdaQueryWrapper<>();
        pending.eq(RewardTxn::getMemberId, memberId).eq(RewardTxn::getStatus, PENDING);
        summary.setPendingCount(rewardTxnMapper.selectCount(pending));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardTxn spend(Long familyId, RewardSpendRequest request, Long operatorMemberId) {
        RewardItem item = rewardItemService.getUsableItem(familyId, request.getItemId(), ITEM_TYPE_SPEND);
        RewardAccount account = getOrCreateAccount(familyId, request.getMemberId());
        int cost = calcPoints(item, request.getQuantity(), request.getMinutes());
        if (cost > account.getBalance()) {
            throw new BusinessException("额度不足，当前可用 " + account.getBalance() + " 个，本次需要 " + cost + " 个");
        }
        RewardTxn txn = newTxn(familyId, account.getMemberId(),
                currentPeriodKey(account.getPeriodType()), operatorMemberId);
        fillFromItem(txn, item);
        txn.setDirection(DIR_SPEND);
        txn.setStatus(PENDING);
        txn.setPoints(-cost);
        txn.setMinutes(MODE_DURATION.equals(item.getPricingMode()) ? request.getMinutes() : null);
        txn.setRemark(request.getRemark());
        return persist(txn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardTxn earn(Long familyId, RewardEarnRequest request, Long operatorMemberId) {
        RewardItem item = rewardItemService.getUsableItem(familyId, request.getItemId(), ITEM_TYPE_EARN);
        RewardAccount account = getOrCreateAccount(familyId, request.getMemberId());
        int points = calcPoints(item, request.getQuantity(), null);
        rewardAccountMapper.increaseBalance(account.getMemberId(), points);

        RewardTxn txn = newTxn(familyId, account.getMemberId(),
                currentPeriodKey(account.getPeriodType()), operatorMemberId);
        fillFromItem(txn, item);
        txn.setDirection(DIR_EARN);
        txn.setStatus(CONFIRMED);
        txn.setPoints(points);
        txn.setRemark(request.getRemark());
        return persist(txn);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RewardTxn adjust(Long familyId, Long memberId, Integer points, String remark, Long operatorMemberId) {
        if (points == null || points == 0) {
            throw new BusinessException("调整额度不能为 0");
        }
        RewardAccount account = getOrCreateAccount(familyId, memberId);
        if (points > 0) {
            rewardAccountMapper.increaseBalance(memberId, points);
        } else if (rewardAccountMapper.deductBalance(memberId, -points) == 0) {
            throw new BusinessException("额度不足，当前可用 " + account.getBalance() + " 个");
        }
        RewardTxn txn = newTxn(familyId, memberId, currentPeriodKey(account.getPeriodType()), operatorMemberId);
        txn.setItemName("手动调整");
        txn.setCategory("OTHER");
        txn.setDirection(DIR_ADJUST);
        txn.setStatus(CONFIRMED);
        txn.setPoints(points);
        txn.setRemark(remark);
        return persist(txn);
    }

    @Override
    public IPage<RewardTxn> pageTxn(Long familyId, Long memberId, String direction, String status,
                                    LocalDate startDate, LocalDate endDate, Integer page, Integer size) {
        LambdaQueryWrapper<RewardTxn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardTxn::getFamilyId, familyId);
        if (memberId != null) {
            wrapper.eq(RewardTxn::getMemberId, memberId);
        }
        if (direction != null && !direction.isEmpty()) {
            wrapper.eq(RewardTxn::getDirection, direction);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(RewardTxn::getStatus, status);
        }
        if (startDate != null) {
            wrapper.ge(RewardTxn::getCreateTime, startDate.atStartOfDay());
        }
        if (endDate != null) {
            wrapper.lt(RewardTxn::getCreateTime, endDate.plusDays(1).atStartOfDay());
        }
        wrapper.orderByDesc(RewardTxn::getCreateTime).orderByDesc(RewardTxn::getId);
        return rewardTxnMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long familyId, Long txnId, Long operatorMemberId) {
        RewardTxn txn = getTxn(familyId, txnId);
        if (!PENDING.equals(txn.getStatus())) {
            throw new BusinessException("该记录已处理，无法重复确认");
        }
        if (!DIR_SPEND.equals(txn.getDirection())) {
            throw new BusinessException("只有消费申请需要确认");
        }
        // 条件更新兜住并发: 余额不够时影响行数为 0
        if (rewardAccountMapper.deductBalance(txn.getMemberId(), -txn.getPoints()) == 0) {
            throw new BusinessException("额度不足，无法通过该笔消费");
        }
        txn.setStatus(CONFIRMED);
        txn.setConfirmTime(LocalDateTime.now());
        txn.setOperatorMemberId(operatorMemberId);
        rewardTxnMapper.updateById(txn);
        log.info("消费确认成功: ID={}, 扣减额度={}", txnId, -txn.getPoints());
    }

    @Override
    public void reject(Long familyId, Long txnId, String remark, Long operatorMemberId) {
        RewardTxn txn = getTxn(familyId, txnId);
        if (!PENDING.equals(txn.getStatus())) {
            throw new BusinessException("该记录已处理，无法重复驳回");
        }
        txn.setStatus(REJECTED);
        txn.setConfirmTime(LocalDateTime.now());
        txn.setOperatorMemberId(operatorMemberId);
        if (remark != null && !remark.isEmpty()) {
            txn.setRemark(remark);
        }
        rewardTxnMapper.updateById(txn);
        log.info("消费驳回成功: ID={}", txnId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTxn(Long familyId, Long txnId) {
        RewardTxn txn = getTxn(familyId, txnId);
        if (CONFIRMED.equals(txn.getStatus())) {
            if (!DIR_SPEND.equals(txn.getDirection())) {
                throw new BusinessException("已生效的发放/赚取记录请用「手动调整」冲减，不支持删除");
            }
            // 撤销已生效的消费: 回补额度后逻辑删除, 流水不再出现在统计中
            rewardAccountMapper.increaseBalance(txn.getMemberId(), -txn.getPoints());
        }
        rewardTxnMapper.deleteById(txnId);
        log.info("删除额度流水成功: ID={}", txnId);
    }

    @Override
    public String currentPeriodKey(String periodType) {
        LocalDate today = LocalDate.now();
        if (PERIOD_MONTH.equals(periodType)) {
            return String.format("%d-%02d", today.getYear(), today.getMonthValue());
        }
        WeekFields iso = WeekFields.ISO;
        return today.get(iso.weekBasedYear()) + "-W" + today.get(iso.weekOfWeekBasedYear());
    }

    /**
     * 折算本次消耗/赚取额度: 按时长方案按"整档向上取 × 每档额度", 按次方案按次数 × 每次额度
     */
    private int calcPoints(RewardItem item, Integer quantity, Integer minutes) {
        int unit = item.getPoints() == null ? 0 : item.getPoints();
        if (unit <= 0) {
            throw new BusinessException("该活动方案的额度设置有误，请到「活动方案」中修正");
        }
        if (MODE_DURATION.equals(item.getPricingMode())) {
            if (minutes == null || minutes <= 0) {
                throw new BusinessException("请填写时长(分钟)");
            }
            int step = item.getUnitMinutes() == null || item.getUnitMinutes() <= 0 ? 30 : item.getUnitMinutes();
            return (int) Math.ceil(minutes / (double) step) * unit;
        }
        int times = quantity == null || quantity <= 0 ? 1 : quantity;
        return times * unit;
    }

    private RewardAccount selectAccount(Long memberId) {
        LambdaQueryWrapper<RewardAccount> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardAccount::getMemberId, memberId).last("LIMIT 1");
        return rewardAccountMapper.selectOne(wrapper);
    }

    private boolean existsGrant(Long memberId, String periodKey) {
        LambdaQueryWrapper<RewardTxn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RewardTxn::getMemberId, memberId)
                .eq(RewardTxn::getDirection, DIR_GRANT)
                .eq(RewardTxn::getPeriodKey, periodKey)
                .ne(RewardTxn::getStatus, REJECTED);
        return rewardTxnMapper.selectCount(wrapper) > 0;
    }

    private RewardTxn getTxn(Long familyId, Long txnId) {
        RewardTxn txn = txnId == null ? null : rewardTxnMapper.selectById(txnId);
        if (txn == null || !familyId.equals(txn.getFamilyId())) {
            throw new BusinessException(403, "额度记录不存在或不属于当前家庭");
        }
        return txn;
    }

    private RewardTxn newTxn(Long familyId, Long memberId, String periodKey, Long operatorMemberId) {
        RewardTxn txn = new RewardTxn();
        txn.setFamilyId(familyId);
        txn.setMemberId(memberId);
        txn.setPeriodKey(periodKey);
        txn.setOperatorMemberId(operatorMemberId);
        txn.setQuantity(1);
        return txn;
    }

    /**
     * 方案信息以快照形式落到流水上, 后续改名或停用不影响历史账目
     */
    private void fillFromItem(RewardTxn txn, RewardItem item) {
        txn.setItemId(item.getId());
        txn.setItemName(item.getItemName());
        txn.setCategory(item.getCategory());
    }

    private RewardTxn persist(RewardTxn txn) {
        if (CONFIRMED.equals(txn.getStatus())) {
            txn.setConfirmTime(LocalDateTime.now());
        }
        rewardTxnMapper.insert(txn);
        return txn;
    }
}
