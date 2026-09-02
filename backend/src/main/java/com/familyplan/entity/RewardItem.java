package com.familyplan.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 奖励活动方案实体(家庭维度价目表: 花额度项目与赚额度项目)
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("reward_item")
public class RewardItem extends BaseEntity {

    /** 所属家庭ID */
    private Long familyId;

    /** 方案名称 */
    private String itemName;

    /** 类型(EARN-赚额度,SPEND-花额度) */
    private String itemType;

    /** 分类(STUDY/SPORTS/HABIT/HOUSEWORK/SCREEN/TOY/SNACK/ACTIVITY/OTHER) */
    private String category;

    /** 计价方式(TIMES-按次,DURATION-按时长) */
    private String pricingMode;

    /** 单价额度(TIMES-每次额度, DURATION-每档额度) */
    private Integer points;

    /** 按时长计价的一档分钟数 */
    private Integer unitMinutes;

    /** 图标文字 */
    private String icon;

    /** 展示颜色 */
    private String color;

    /** 排序(小在前) */
    private Integer sortOrder;

    /** 是否启用(0-停用,1-启用) */
    private Integer active;

    /** 备注 */
    private String remark;
}
