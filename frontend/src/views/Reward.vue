<template>
  <div class="page-container">
    <!-- 顶部：孩子选择 + 额度指标 + 发放 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar-top">
        <div class="child-picker">
          <span class="picker-label">孩子</span>
          <el-select v-model="childId" placeholder="选择孩子" style="width: 170px" @change="switchChild">
            <el-option v-for="m in childOptions" :key="m.id" :label="m.label" :value="m.id" />
          </el-select>
          <el-tag v-if="summary" size="small" type="info">{{ summary.periodKey }}</el-tag>
        </div>

        <div class="toolbar-ops">
          <el-button type="primary" :icon="Gift" @click="grant">发放本周期额度</el-button>
          <el-button :icon="Minus" @click="openAdjust">手动调整</el-button>
        </div>
      </div>

      <div class="stat-items">
        <div class="stat-item balance-chip">
          <div class="stat-label">当前余额</div>
          <div class="stat-value balance">{{ summary?.balance ?? 0 }}</div>
        </div>
        <div class="stat-divider" />
        <div class="stat-item">
          <div class="stat-label">已发放</div>
          <div class="stat-value granted">{{ summary?.granted ?? 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">已赚取</div>
          <div class="stat-value earned">{{ summary?.earned ?? 0 }}</div>
        </div>
        <div class="stat-item">
          <div class="stat-label">已花掉</div>
          <div class="stat-value spent">{{ summary?.spent ?? 0 }}</div>
        </div>
        <div class="stat-item clickable" @click="activeTab = 'pending'">
          <div class="stat-label">待确认</div>
          <div class="stat-value pending">{{ summary?.pendingCount ?? 0 }}</div>
        </div>
      </div>

      <div v-if="summary" class="usage-row">
        <span class="usage-label">本周期额度使用</span>
        <el-progress :percentage="usagePercent" :color="usageColor" :stroke-width="12" class="usage-bar" />
        <span class="usage-text">{{ summary.spent }} / {{ totalAvailable }} 额度</span>
      </div>
    </el-card>

    <el-card shadow="never">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <!-- 花额度 -->
        <el-tab-pane label="花额度" name="spend">
          <div class="item-grid" v-loading="loading">
            <el-empty v-if="spendItems.length === 0" description="还没有消费项目，去「活动方案」添加" />
            <div v-for="item in spendItems" :key="item.id" class="item-card">
              <div class="item-head">
                <div class="item-icon" :style="iconStyle(item)">{{ item.icon || item.itemName.charAt(0) }}</div>
                <div class="item-title-box">
                  <div class="item-title">{{ item.itemName }}</div>
                  <div class="item-meta">
                    <el-tag size="small">{{ rewardCategoryLabel(item.category) }}</el-tag>
                    <span class="item-price">{{ priceText(item) }}</span>
                  </div>
                </div>
              </div>

              <div class="item-body">
                <template v-if="item.pricingMode === 'DURATION'">
                  <span class="field-label">时长(分钟)</span>
                  <el-input-number
                    v-model="draftOf(item).minutes"
                    :min="item.unitMinutes || 30"
                    :step="item.unitMinutes || 30"
                    size="small"
                    controls-position="right"
                    style="width: 120px"
                  />
                </template>
                <template v-else>
                  <span class="field-label">次数</span>
                  <el-input-number
                    v-model="draftOf(item).quantity"
                    :min="1"
                    :max="99"
                    size="small"
                    controls-position="right"
                    style="width: 120px"
                  />
                </template>
                <div class="cost-line">
                  需消耗
                  <span class="cost-num">{{ spendCost(item) }}</span>
                  额度<span v-if="item.pricingMode === 'DURATION'" class="cost-hint">（{{ stepsOf(item) }} 档 × {{ item.points }}）</span>
                </div>
              </div>

              <div class="item-foot">
                <span v-if="notEnough(item)" class="lack-tip">额度不足</span>
                <span v-else class="ok-tip">余额 {{ summary?.balance ?? 0 }}</span>
                <el-button
                  type="primary"
                  size="small"
                  :disabled="notEnough(item)"
                  @click="submitSpend(item)"
                >
                  申请消费
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 赚额度 -->
        <el-tab-pane label="赚额度" name="earn">
          <div class="tab-tip">孩子有好表现，父母点一下就记上，额度立即到账。</div>
          <div class="item-grid" v-loading="loading">
            <el-empty v-if="earnItems.length === 0" description="还没有赚取项目，去「活动方案」添加" />
            <div v-for="item in earnItems" :key="item.id" class="item-card">
              <div class="item-head">
                <div class="item-icon" :style="iconStyle(item)">{{ item.icon || item.itemName.charAt(0) }}</div>
                <div class="item-title-box">
                  <div class="item-title">{{ item.itemName }}</div>
                  <div class="item-meta">
                    <el-tag size="small" type="success">{{ rewardCategoryLabel(item.category) }}</el-tag>
                    <span class="item-price">每次 {{ item.points }} 额度</span>
                  </div>
                </div>
              </div>
              <div class="item-body">
                <span class="field-label">次数</span>
                <el-input-number
                  v-model="draftOf(item).quantity"
                  :min="1"
                  :max="99"
                  size="small"
                  controls-position="right"
                  style="width: 120px"
                />
                <div class="cost-line">
                  可积攒 <span class="cost-num earn">{{ earnGain(item) }}</span> 额度
                </div>
              </div>
              <div class="item-foot">
                <span class="ok-tip">当前余额 {{ summary?.balance ?? 0 }}</span>
                <el-button type="success" size="small" @click="submitEarn(item)">记一笔</el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- 待确认 -->
        <el-tab-pane name="pending">
          <template #label>
            <span>待确认</span>
            <el-badge v-if="(summary?.pendingCount ?? 0) > 0" :value="summary?.pendingCount" class="tab-badge" />
          </template>
          <div class="tab-tip">消费需父母确认后才真正扣减额度，驳回不会消耗孩子的额度。</div>
          <el-table :data="pendingList" v-loading="loading" empty-text="没有待确认的消费申请">
            <el-table-column prop="itemName" label="项目" min-width="120" />
            <el-table-column label="分类" width="90">
              <template #default="{ row }">{{ rewardCategoryLabel(row.category) }}</template>
            </el-table-column>
            <el-table-column label="明细" min-width="130">
              <template #default="{ row }">
                <span v-if="row.minutes">{{ row.minutes }} 分钟</span>
                <span v-else>{{ row.quantity }} 次</span>
              </template>
            </el-table-column>
            <el-table-column label="额度" width="90">
              <template #default="{ row }">
                <span class="points-spend">{{ row.points }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="提交时间" width="170" />
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" size="small" @click="handleConfirm(row)">通过</el-button>
                <el-button link type="warning" size="small" @click="handleReject(row)">驳回</el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 流水 -->
        <el-tab-pane label="额度流水" name="txn">
          <div class="txn-filter">
            <el-select v-model="txnQuery.direction" placeholder="全部方向" clearable style="width: 130px" @change="loadTxn">
              <el-option v-for="d in REWARD_DIRECTIONS" :key="d.value" :label="d.label" :value="d.value" />
            </el-select>
            <el-select v-model="txnQuery.status" placeholder="全部状态" clearable style="width: 130px" @change="loadTxn">
              <el-option v-for="s in REWARD_TXN_STATUS" :key="s.value" :label="s.label" :value="s.value" />
            </el-select>
            <el-button link type="primary" @click="resetTxnQuery">重置</el-button>
          </div>
          <el-table :data="txnList" v-loading="loading" empty-text="暂无额度流水">
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column label="方向" width="100">
              <template #default="{ row }">
                <el-tag size="small" :type="directionTag(row.direction)">{{ rewardDirectionLabel(row.direction) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="itemName" label="项目" min-width="120" />
            <el-table-column label="分类" width="90">
              <template #default="{ row }">{{ rewardCategoryLabel(row.category) }}</template>
            </el-table-column>
            <el-table-column label="明细" width="110">
              <template #default="{ row }">
                <span v-if="row.minutes">{{ row.minutes }} 分钟</span>
                <span v-else>{{ row.quantity }} 次</span>
              </template>
            </el-table-column>
            <el-table-column label="额度" width="90">
              <template #default="{ row }">
                <span :class="row.points < 0 ? 'points-spend' : 'points-earn'">
                  {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusTag(row.status)">{{ rewardStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" size="small" @click="handleDelete(row)">
                  {{ row.status === 'CONFIRMED' && row.direction === 'SPEND' ? '撤销' : '删除' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            class="pager"
            v-model:current-page="txnQuery.page"
            v-model:page-size="txnQuery.size"
            :total="txnTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            @current-change="loadTxn"
            @size-change="loadTxn"
          />
        </el-tab-pane>

        <!-- 额度设置 -->
        <el-tab-pane label="额度设置" name="settings">
          <el-form :model="setting" label-width="140px" class="setting-form">
            <el-form-item label="每周期发放额度">
              <el-input-number v-model="setting.weeklyQuota" :min="0" :max="100000" :step="10" />
              <span class="form-tip">点「发放本周期额度」时按这个数量到账，设 0 表示不固定发放</span>
            </el-form-item>
            <el-form-item label="发放周期">
              <el-radio-group v-model="setting.periodType">
                <el-radio-button v-for="p in REWARD_PERIOD_TYPES" :key="p.value" :value="p.value">
                  {{ p.label }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="余额跨周期结转">
              <el-switch v-model="setting.carryOver" :active-value="1" :inactive-value="0" />
              <span class="form-tip">
                开启后没花完的额度会带到下一周；关闭则每个新周期把结余清零后重新发放
              </span>
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="setting.remark" type="textarea" :rows="2" maxlength="200" placeholder="如：期末考试周额度翻倍" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveSetting">保存设置</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 手动调整 -->
    <el-dialog v-model="adjustVisible" title="手动调整额度" width="420px">
      <el-form label-width="90px">
        <el-form-item label="调整额度">
          <el-input-number v-model="adjustForm.points" :step="5" style="width: 100%" />
          <span class="form-tip">正数为奖励，负数为扣减</span>
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="adjustForm.remark" type="textarea" :rows="2" maxlength="200" placeholder="如：帮忙照看弟弟奖励 20 额度" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdjust">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Gift, Minus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  adjustReward,
  calcSpendPoints,
  confirmRewardTxn,
  deleteRewardTxn,
  earnReward,
  getRewardAccount,
  getRewardItemList,
  getRewardSummary,
  getRewardTxnPage,
  grantRewardQuota,
  rejectRewardTxn,
  saveRewardAccount,
  spendReward,
  type RewardItem,
  type RewardSummary,
  type RewardTxn
} from '@/api/reward'
import {
  REWARD_DIRECTIONS,
  REWARD_PERIOD_TYPES,
  REWARD_TXN_STATUS,
  rewardCategoryLabel,
  rewardDirectionLabel,
  rewardStatusLabel
} from '@/constants/categories'
import { useMemberStore } from '@/stores/member'
import { PALETTE } from '@/constants/palette'

const memberStore = useMemberStore()

const childId = ref<number | null>(null)
const activeTab = ref('spend')
const loading = ref(false)

const summary = ref<RewardSummary | null>(null)
const spendItems = ref<RewardItem[]>([])
const earnItems = ref<RewardItem[]>([])
const pendingList = ref<RewardTxn[]>([])
const txnList = ref<RewardTxn[]>([])
const txnTotal = ref(0)
const txnQuery = reactive({ direction: '', status: '', page: 1, size: 10 })

/** 每个方案的输入态(次数/时长) */
const drafts = reactive<Record<number, { quantity: number; minutes: number }>>({})

const relationText = (r?: string) => {
  const map: Record<string, string> = { OWNER: '户主', SPOUSE: '配偶', CHILD: '子女', PARENT: '父母', OTHER: '其他' }
  return r ? map[r] || r : ''
}

const childOptions = computed(() => {
  const kids = memberStore.members.filter((m) => m.relation === 'CHILD')
  const pool = kids.length > 0 ? kids : memberStore.members
  return pool.map((m) => ({ id: m.id as number, label: `${m.memberName}（${relationText(m.relation)}）` }))
})

const totalAvailable = computed(() => {
  const s = summary.value
  if (!s) return 0
  return Math.max(s.granted + s.earned + Math.max(s.adjusted, 0), 0)
})

const usagePercent = computed(() => {
  const s = summary.value
  if (!s || totalAvailable.value === 0) return 0
  return Math.min(100, Math.round((s.spent / totalAvailable.value) * 100))
})

const usageColor = computed(() => {
  const p = usagePercent.value
  return p < 50 ? PALETTE.grass500 : p < 80 ? PALETTE.sun : PALETTE.berry
})

const setting = reactive({ weeklyQuota: 0, periodType: 'WEEK', carryOver: 1, remark: '' })

const adjustVisible = ref(false)
const adjustForm = reactive({ points: 10, remark: '' })

const itemId = (item: RewardItem) => item.id as number

const draftOf = (item: RewardItem) => {
  const id = itemId(item)
  if (!drafts[id]) {
    drafts[id] = { quantity: 1, minutes: item.unitMinutes && item.unitMinutes > 0 ? item.unitMinutes : 30 }
  }
  return drafts[id]
}

const spendCost = (item: RewardItem) => {
  if (item.pricingMode === 'DURATION') {
    return calcSpendPoints(item, draftOf(item).minutes)
  }
  return (draftOf(item).quantity || 1) * (item.points || 0)
}

const earnGain = (item: RewardItem) => (draftOf(item).quantity || 1) * (item.points || 0)

const stepsOf = (item: RewardItem) => {
  const step = item.unitMinutes && item.unitMinutes > 0 ? item.unitMinutes : 30
  return Math.ceil((draftOf(item).minutes || 0) / step)
}

const priceText = (item: RewardItem) =>
  item.pricingMode === 'DURATION'
    ? `${item.points} 额度 / ${item.unitMinutes || 30} 分钟`
    : `${item.points} 额度 / 次`

const notEnough = (item: RewardItem) => spendCost(item) > (summary.value?.balance ?? 0)

const iconStyle = (item: RewardItem) => ({
  background: `${item.color || PALETTE.grass500}20`,
  color: item.color || PALETTE.grass500
})

const directionTag = (v?: string) => {
  const map: Record<string, string> = { GRANT: 'primary', EARN: 'success', SPEND: 'danger', EXPIRE: 'info', ADJUST: 'warning' }
  return (map[v || ''] || 'info') as 'primary' | 'success' | 'danger' | 'info' | 'warning'
}

const statusTag = (v?: string) => {
  const map: Record<string, string> = { PENDING: 'warning', CONFIRMED: 'success', REJECTED: 'info' }
  return (map[v || ''] || 'info') as 'success' | 'info' | 'warning'
}

const loadItems = async () => {
  const list = await getRewardItemList()
  spendItems.value = list.filter((i) => i.itemType === 'SPEND' && i.active !== 0)
  earnItems.value = list.filter((i) => i.itemType === 'EARN' && i.active !== 0)
  ;[...spendItems.value, ...earnItems.value].forEach(draftOf)
}

const loadSummary = async () => {
  if (!childId.value) return
  summary.value = await getRewardSummary(childId.value)
}

/** 设置表单以账户接口为准回填, 概览接口不带规则字段 */
const loadSetting = async () => {
  if (!childId.value) return
  const account = await getRewardAccount(childId.value)
  Object.assign(setting, {
    weeklyQuota: account.weeklyQuota ?? 0,
    periodType: account.periodType || 'WEEK',
    carryOver: account.carryOver ?? 1,
    remark: account.remark || ''
  })
}

const loadPending = async () => {
  if (!childId.value) return
  const res = await getRewardTxnPage({ memberId: childId.value, status: 'PENDING', page: 1, size: 50 })
  pendingList.value = res.records
}

const loadTxn = async () => {
  if (!childId.value) return
  const res = await getRewardTxnPage({
    memberId: childId.value,
    direction: txnQuery.direction || undefined,
    status: txnQuery.status || undefined,
    page: txnQuery.page,
    size: txnQuery.size
  })
  txnList.value = res.records
  txnTotal.value = res.total
}

const resetTxnQuery = () => {
  txnQuery.direction = ''
  txnQuery.status = ''
  txnQuery.page = 1
  loadTxn()
}

const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadSetting(), loadPending(), loadTxn()])
  } finally {
    loading.value = false
  }
}

const onTabChange = async (name: string | number) => {
  if (name === 'txn') await loadTxn()
  if (name === 'pending') await loadPending()
}

const switchChild = async () => {
  if (childId.value) memberStore.setCurrentMember(childId.value)
  txnQuery.page = 1
  await refreshAll()
}

const requireChild = () => {
  if (!childId.value) {
    ElMessage.warning('请先在「成员管理」中添加孩子')
    return false
  }
  return true
}

const grant = async () => {
  if (!requireChild()) return
  try {
    await grantRewardQuota(childId.value as number)
    ElMessage.success('额度发放成功')
    await refreshAll()
  } catch {
    // 拦截器已提示
  }
}

const submitSpend = async (item: RewardItem) => {
  if (!requireChild()) return
  try {
    await spendReward({
      memberId: childId.value as number,
      itemId: itemId(item),
      quantity: draftOf(item).quantity,
      minutes: item.pricingMode === 'DURATION' ? draftOf(item).minutes : undefined
    })
    ElMessage.success('已提交消费申请，请在「待确认」中通过')
    activeTab.value = 'pending'
    await refreshAll()
  } catch {
    // 拦截器已提示
  }
}

const submitEarn = async (item: RewardItem) => {
  if (!requireChild()) return
  try {
    await earnReward({
      memberId: childId.value as number,
      itemId: itemId(item),
      quantity: draftOf(item).quantity
    })
    ElMessage.success(`已奖励 ${earnGain(item)} 额度`)
    await refreshAll()
  } catch {
    // 拦截器已提示
  }
}

const handleConfirm = async (row: RewardTxn) => {
  try {
    await ElMessageBox.confirm(
      `确定通过「${row.itemName}」吗？将扣减 ${Math.abs(row.points || 0)} 个额度。`,
      '确认消费',
      { type: 'warning' }
    )
    await confirmRewardTxn(row.id as number)
    ElMessage.success('已通过，额度已扣减')
    await refreshAll()
  } catch {
    // 取消或拦截器已提示
  }
}

const handleReject = async (row: RewardTxn) => {
  try {
    const { value } = await ElMessageBox.prompt('驳回原因（选填）', `驳回「${row.itemName}」`, {
      confirmButtonText: '驳回',
      cancelButtonText: '取消'
    })
    await rejectRewardTxn(row.id as number, value || undefined)
    ElMessage.success('已驳回，未扣减额度')
    await refreshAll()
  } catch {
    // 取消或拦截器已提示
  }
}

const handleDelete = async (row: RewardTxn) => {
  const revoke = row.status === 'CONFIRMED' && row.direction === 'SPEND'
  try {
    await ElMessageBox.confirm(
      revoke
        ? `撤销后「${row.itemName}」消耗的 ${Math.abs(row.points || 0)} 个额度将退回孩子账户。`
        : '确定删除这条额度记录？',
      revoke ? '撤销消费' : '提示',
      { type: 'warning' }
    )
    await deleteRewardTxn(row.id as number)
    ElMessage.success(revoke ? '已撤销，额度已退回' : '删除成功')
    await refreshAll()
  } catch {
    // 取消或拦截器已提示
  }
}

const openAdjust = () => {
  if (!requireChild()) return
  Object.assign(adjustForm, { points: 10, remark: '' })
  adjustVisible.value = true
}

const submitAdjust = async () => {
  if (!adjustForm.points) {
    ElMessage.warning('调整额度不能为 0')
    return
  }
  try {
    await adjustReward(childId.value as number, adjustForm.points, adjustForm.remark)
    ElMessage.success('调整成功')
    adjustVisible.value = false
    await refreshAll()
  } catch {
    // 拦截器已提示
  }
}

const saveSetting = async () => {
  if (!requireChild()) return
  try {
    await saveRewardAccount({
      memberId: childId.value as number,
      weeklyQuota: setting.weeklyQuota,
      periodType: setting.periodType,
      carryOver: setting.carryOver,
      remark: setting.remark
    })
    ElMessage.success('设置已保存')
    await loadSummary()
  } catch {
    // 拦截器已提示
  }
}

onMounted(async () => {
  await memberStore.ensureLoaded()
  childId.value = childOptions.value[0]?.id ?? memberStore.currentMemberId
  loading.value = true
  try {
    await loadItems()
    await Promise.all([loadSummary(), loadSetting(), loadPending(), loadTxn()])
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.page-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

.toolbar-card {
  margin-bottom: 16px;
  border-radius: var(--r-lg);
}

.toolbar-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.child-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.picker-label {
  font-size: 14px;
  color: var(--text-body);
}

.stat-items {
  display: flex;
  align-items: center;
  gap: 22px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.stat-item {
  text-align: center;
  min-width: 56px;
}

.balance-chip {
  padding: 6px 20px;
  border-radius: var(--r-md);
  background: linear-gradient(135deg, var(--grass-50), var(--sprout-soft));
  border: 1px solid var(--grass-200);
}

.stat-divider {
  width: 1px;
  align-self: stretch;
  background: var(--border-soft);
}

.stat-item.clickable {
  cursor: pointer;
  padding: 2px 10px;
  border-radius: 10px;
  transition: background-color var(--dur-1) linear, transform var(--dur-1) var(--ease-out);
}

.stat-item.clickable:hover {
  background: var(--grass-50);
  transform: translateY(-2px);
}

.stat-item.clickable:active {
  transform: scale(0.97);
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 2px;
}

.stat-value.balance { color: var(--grass-600); font-size: 30px; letter-spacing: -1px; }
.stat-value.granted { color: var(--text-muted); }
.stat-value.earned { color: var(--grass-600); }
.stat-value.spent { color: var(--sun-ink); }
.stat-value.pending { color: var(--berry-ink); }

.toolbar-ops {
  display: flex;
  gap: 8px;
}

.usage-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 14px;
}

.usage-label {
  font-size: 13px;
  color: var(--text-body);
  white-space: nowrap;
}

.usage-bar {
  flex: 1;
  max-width: 520px;
}

.usage-text {
  font-size: 13px;
  color: var(--text-muted);
}

.tab-tip {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 12px;
}

.tab-badge {
  margin-left: 6px;
}

.item-grid {
  min-height: 120px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.item-card {
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  padding: 14px 16px;
  background: #fff;
  transition: all 0.2s;
}

.item-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.item-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
}

.item-title-box {
  min-width: 0;
}

.item-title {
  font-weight: 600;
  font-size: 15px;
}

.item-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.item-price {
  font-size: 12px;
  color: var(--text-muted);
}

.item-body {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.field-label {
  font-size: 13px;
  color: var(--text-body);
}

.cost-line {
  width: 100%;
  font-size: 13px;
  color: var(--text-body);
}

.cost-num {
  font-size: 17px;
  font-weight: 700;
  color: var(--berry-ink);
  margin: 0 2px;
}

.cost-num.earn {
  color: var(--grass-600);
}

.cost-hint {
  color: var(--text-muted);
}

.item-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed var(--border-soft);
}

.ok-tip {
  font-size: 12px;
  color: var(--text-muted);
}

.lack-tip {
  font-size: 12px;
  color: var(--berry-ink);
}

.txn-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.points-earn {
  color: var(--grass-600);
  font-weight: 600;
}

.points-spend {
  color: var(--berry-ink);
  font-weight: 600;
}

.pager {
  margin-top: 14px;
  justify-content: flex-end;
}

.setting-form {
  max-width: 640px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
