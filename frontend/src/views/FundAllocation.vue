<template>
  <div class="page-container">
    <el-card class="summary-card">
      <div class="summary-head">
        <div>
          <div class="summary-kicker">这个周期打算怎么分</div>
          <div class="summary-total">已分配 {{ formatPercent(total) }}%</div>
          <div class="summary-remain" :class="{ done: complete }">
            {{ complete ? '这个周期已经分完' : `还剩 ${formatPercent(remaining)}%` }}
          </div>
        </div>
        <div class="filters">
          <el-radio-group v-model="period" @change="onPeriodChange">
            <el-radio-button value="MONTH">月度</el-radio-button>
            <el-radio-button value="YEAR">年度</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="period === 'MONTH'"
            v-model="yearMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            @change="load"
          />
          <el-date-picker
            v-else
            v-model="yearText"
            type="year"
            value-format="YYYY"
            placeholder="选择年份"
            @change="load"
          />
        </div>
      </div>

      <div class="split" role="img" :aria-label="`已分配 ${formatPercent(total)}%`">
        <div
          v-for="slice in slices"
          :key="slice.key"
          class="slice"
          :style="{ width: slice.width, background: slice.color }"
          :title="`${slice.label} ${slice.percent}%`"
        ></div>
        <div v-if="restWidth > 0" class="slice rest" :style="{ width: restWidth + '%' }"></div>
      </div>
      <div class="legend" v-if="slices.length">
        <span v-for="slice in slices" :key="slice.key" class="legend-item">
          <i :style="{ background: slice.color }"></i>
          {{ slice.label }} {{ slice.percent }}%
        </span>
        <span v-if="remaining > 0" class="legend-item">
          <i class="rest-dot"></i>
          未分配 {{ formatPercent(remaining) }}%
        </span>
      </div>
      <el-empty v-else description="还没分配。选一个分类，写下它占多少。" :image-size="72" />
    </el-card>

    <el-card>
      <template #header>
        <div class="header-bar">
          <span class="card-title">分类比例</span>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增分配</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column label="分类" min-width="140">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="比例" width="160">
          <template #default="{ row }">
            <span class="percent">{{ formatPercent(row.percentage) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="占这一份" min-width="220">
          <template #default="{ row, $index }">
            <div class="row-bar">
              <div class="row-fill" :style="{ width: Number(row.percentage) + '%', background: colorAt($index) }"></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分配' : '新增分配'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="比例">
          <el-input-number
            v-model="form.percentage"
            :min="0.01"
            :max="100"
            :precision="2"
            :step="5"
            style="width: 100%"
          />
        </el-form-item>
        <p class="hint" :class="{ warn: projected > 100 }">
          保存后这个周期合计 {{ formatPercent(projected) }}%
          <template v-if="projected > 100">，已经超过 100%</template>
        </p>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="projected > 100" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFundAllocation,
  addFundAllocation,
  updateFundAllocation,
  deleteFundAllocation,
  type FundAllocation
} from '@/api/fundAllocation'
import { useMemberStore } from '@/stores/member'
import { EXPENSE_CATEGORIES as categories, categoryLabel } from '@/constants/categories'

const SLICE_COLORS = ['#E85D4C', '#2A9D8F', '#f7b733', '#3f7a56', '#8b6be0', '#35a7d7', '#c46a45', '#6d9a78']

const memberStore = useMemberStore()
const loading = ref(false)
const tableData = ref<FundAllocation[]>([])
const total = ref(0)
const remaining = ref(100)
const complete = ref(false)

const now = new Date()
const period = ref<'MONTH' | 'YEAR'>('MONTH')
const yearMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const yearText = ref(String(now.getFullYear()))

const dialogVisible = ref(false)
const form = reactive<FundAllocation>({
  memberId: 0,
  category: '',
  percentage: 10,
  period: 'MONTH',
  year: now.getFullYear(),
  month: now.getMonth() + 1
})

const colorAt = (index: number) => SLICE_COLORS[index % SLICE_COLORS.length]

const formatPercent = (value?: number | null) => {
  const num = Number(value || 0)
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 0, maximumFractionDigits: 2 })
}

const scale = computed(() => (total.value > 100 ? 100 / total.value : 1))

const slices = computed(() =>
  tableData.value.map((row, index) => ({
    key: row.id ?? `${row.category}-${index}`,
    label: categoryLabel(row.category),
    percent: formatPercent(row.percentage),
    color: colorAt(index),
    width: `${Math.max(0, Number(row.percentage || 0) * scale.value)}%`
  }))
)

const restWidth = computed(() => {
  if (total.value <= 0 || total.value >= 100) return 0
  return Math.max(0, 100 - total.value)
})

const projected = computed(() => {
  const others = tableData.value
    .filter((row) => row.id !== form.id)
    .reduce((sum, row) => sum + Number(row.percentage || 0), 0)
  return Math.round((others + Number(form.percentage || 0)) * 100) / 100
})

const currentYear = () => (period.value === 'MONTH' ? Number(yearMonth.value.slice(0, 4)) : Number(yearText.value))
const currentMonth = () => (period.value === 'MONTH' ? Number(yearMonth.value.slice(5, 7)) : undefined)

const load = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  loading.value = true
  try {
    const res = await getFundAllocation({
      memberId: memberStore.currentMemberId,
      period: period.value,
      year: currentYear(),
      month: currentMonth()
    })
    tableData.value = res.items || []
    total.value = Number(res.totalPercentage || 0)
    remaining.value = Number(res.remainingPercentage || 0)
    complete.value = !!res.complete
  } finally {
    loading.value = false
  }
}

const onPeriodChange = () => {
  load()
}

const openAdd = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  Object.assign(form, {
    id: undefined,
    memberId: memberStore.currentMemberId,
    category: 'FOOD',
    percentage: Math.min(10, Math.max(remaining.value, 0.01)),
    period: period.value,
    year: currentYear(),
    month: currentMonth() ?? null
  })
  dialogVisible.value = true
}

const openEdit = (row: FundAllocation) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.category) {
    ElMessage.warning('请选择分类')
    return
  }
  if (!form.percentage || form.percentage <= 0) {
    ElMessage.warning('请填写大于 0 的比例')
    return
  }
  if (projected.value > 100) {
    ElMessage.warning('这个周期的比例合计不能超过 100%')
    return
  }
  const memberId = memberStore.currentMemberId
  if (!memberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  const payload: FundAllocation = {
    ...form,
    memberId,
    period: period.value,
    year: currentYear(),
    month: period.value === 'MONTH' ? Number(yearMonth.value.slice(5, 7)) : null
  }
  try {
    if (form.id) {
      await updateFundAllocation(payload)
      ElMessage.success('更新成功')
    } else {
      await addFundAllocation(payload)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 错误已在拦截器提示
  }
}

const remove = async (row: FundAllocation) => {
  try {
    await ElMessageBox.confirm('确定删除这条分配?', '提示', { type: 'warning' })
    await deleteFundAllocation(row.id as number)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 取消
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
.summary-card {
  margin-bottom: 16px;
}
.summary-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 18px;
}
.summary-kicker {
  color: var(--text-muted);
  font-size: 13px;
}
.summary-total {
  margin-top: 4px;
  font-family: var(--font-display);
  font-size: 32px;
  color: var(--text-strong);
  line-height: 1.2;
}
.summary-remain {
  margin-top: 4px;
  color: var(--coral);
  font-weight: 600;
}
.summary-remain.done {
  color: var(--teal);
}
.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
}
.split {
  display: flex;
  height: 18px;
  border-radius: 999px;
  overflow: hidden;
  background: #f3eee4;
}
.slice {
  height: 100%;
  min-width: 0;
}
.slice.rest {
  background: repeating-linear-gradient(
    -45deg,
    #f7f3eb,
    #f7f3eb 6px,
    #e6dfd2 6px,
    #e6dfd2 12px
  );
}
.legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-top: 14px;
  color: var(--text-body);
  font-size: 13px;
}
.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.legend-item i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;
}
.rest-dot {
  background: #e6dfd2;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}
.card-title {
  font-weight: 600;
  color: var(--text-strong);
}
.percent {
  color: var(--teal-deep, #1f7a70);
  font-weight: 700;
}
.row-bar {
  height: 8px;
  border-radius: 999px;
  background: #f3eee4;
  overflow: hidden;
}
.row-fill {
  height: 100%;
  border-radius: 999px;
}
.hint {
  margin: 0 0 0 80px;
  color: var(--text-muted);
  font-size: 13px;
}
.hint.warn {
  color: var(--coral);
}
</style>
