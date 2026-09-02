<template>
  <div class="page-container">
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="picker-group">
          <span class="picker-label">类型</span>
          <el-radio-group v-model="itemType" @change="load">
            <el-radio-button value="">全部</el-radio-button>
            <el-radio-button v-for="t in REWARD_ITEM_TYPES" :key="t.value" :value="t.value">
              {{ t.label }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="stat-items">
          <div class="stat-item">
            <div class="stat-label">消费项目</div>
            <div class="stat-value spend">{{ spendCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">赚取项目</div>
            <div class="stat-value earn">{{ earnCount }}</div>
          </div>
        </div>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增方案</el-button>
      </div>
      <div class="tip-line">
        这里是孩子可用的「活动方案」清单。按时长的项目按
        <b>整档向上取整</b>
        计费，例如看电视设为每 30 分钟 10 额度，看 45 分钟就按 2 档扣 20 额度。
      </div>
    </el-card>

    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" empty-text="暂无活动方案">
        <el-table-column label="方案名称" min-width="140">
          <template #default="{ row }">
            <span class="dot" :style="{ background: row.color || PALETTE.grass500 }" />
            {{ row.itemName }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.itemType === 'EARN' ? 'success' : 'danger'">
              {{ rewardItemTypeLabel(row.itemType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="90">
          <template #default="{ row }">{{ rewardCategoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="计价方式" width="100">
          <template #default="{ row }">{{ rewardPricingLabel(row.pricingMode) }}</template>
        </el-table-column>
        <el-table-column label="额度" min-width="150">
          <template #default="{ row }">
            <span v-if="row.pricingMode === 'DURATION'">{{ row.points }} 额度 / {{ row.unitMinutes }} 分钟</span>
            <span v-else>{{ row.points }} 额度 / 次</span>
          </template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="启用" width="80">
          <template #default="{ row }">
            <el-tag size="small" :type="row.active === 0 ? 'info' : 'success'">
              {{ row.active === 0 ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑活动方案' : '新增活动方案'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="方案名称" required>
          <el-input v-model="form.itemName" placeholder="如：看电视 / 买玩具 / 认真学习" maxlength="50" />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.itemType">
            <el-radio-button v-for="t in REWARD_ITEM_TYPES" :key="t.value" :value="t.value">{{ t.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" style="width: 100%">
            <el-option v-for="c in REWARD_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计价方式">
          <el-radio-group v-model="form.pricingMode">
            <el-radio-button v-for="p in REWARD_PRICING_MODES" :key="p.value" :value="p.value">{{ p.label }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="form.pricingMode === 'DURATION' ? '每档额度' : '每次额度'">
          <el-input-number v-model="form.points" :min="1" :max="100000" :step="5" />
          <span class="form-tip">{{ form.pricingMode === 'DURATION' ? '看完一档所需时长扣这些额度' : '做一件事扣/赚这些额度' }}</span>
        </el-form-item>
        <el-form-item v-if="form.pricingMode === 'DURATION'" label="一档时长">
          <el-input-number v-model="form.unitMinutes" :min="5" :max="600" :step="5" />
          <span class="form-tip">分钟；不足一档按一档计</span>
        </el-form-item>
        <el-form-item label="图标文字">
          <el-input v-model="form.icon" maxlength="4" placeholder="如：影" style="width: 120px" />
        </el-form-item>
        <el-form-item label="展示颜色">
          <el-color-picker v-model="form.color" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" :step="10" />
          <span class="form-tip">数字小的排在前面</span>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="form.active" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="200" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PALETTE } from '@/constants/palette'
import {
  addRewardItem,
  deleteRewardItem,
  getRewardItemList,
  updateRewardItem,
  type RewardItem
} from '@/api/reward'
import {
  REWARD_CATEGORIES,
  REWARD_ITEM_TYPES,
  REWARD_PRICING_MODES,
  rewardCategoryLabel,
  rewardItemTypeLabel,
  rewardPricingLabel
} from '@/constants/categories'

const itemType = ref('')
const list = ref<RewardItem[]>([])
const loading = ref(false)

const spendCount = computed(() => list.value.filter((i) => i.itemType === 'SPEND').length)
const earnCount = computed(() => list.value.filter((i) => i.itemType === 'EARN').length)

const dialogVisible = ref(false)
const emptyForm = (): RewardItem => ({
  id: undefined,
  itemName: '',
  itemType: 'SPEND',
  category: 'SCREEN',
  pricingMode: 'TIMES',
  points: 10,
  unitMinutes: 30,
  icon: '',
  color: PALETTE.grass500,
  sortOrder: 0,
  active: 1,
  remark: ''
})
const form = reactive<RewardItem>(emptyForm())

const load = async () => {
  loading.value = true
  try {
    list.value = await getRewardItemList(itemType.value || undefined)
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.assign(form, emptyForm())
  dialogVisible.value = true
}

const openEdit = (row: RewardItem) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.itemName.trim()) {
    ElMessage.warning('请输入方案名称')
    return
  }
  try {
    if (form.id) {
      await updateRewardItem(form)
      ElMessage.success('更新成功')
    } else {
      await addRewardItem(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 拦截器已提示
  }
}

const remove = async (row: RewardItem) => {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.itemName}」？历史额度流水不受影响。`,
      '提示',
      { type: 'warning' }
    )
    await deleteRewardItem(row.id as number)
    ElMessage.success('删除成功')
    await load()
  } catch {
    // 取消或拦截器已提示
  }
}

onMounted(load)
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.picker-group {
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
  gap: 32px;
  flex: 1;
  justify-content: center;
}

.stat-item {
  text-align: center;
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

.stat-value.spend { color: var(--berry-ink); }
.stat-value.earn { color: var(--grass-600); }

.tip-line {
  margin-top: 12px;
  font-size: 13px;
  color: var(--text-muted);
  line-height: 1.6;
}

.dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 8px;
  vertical-align: middle;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
