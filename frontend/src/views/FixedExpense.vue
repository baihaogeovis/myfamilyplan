<template>
  <div class="page-container">
    <!-- 月份选择 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="month-picker">
          <el-date-picker
            v-model="month"
            type="month"
            value-format="YYYY-MM"
            format="YYYY年MM月"
            placeholder="选择月份"
            @change="load"
          />
        </div>
        <div class="toolbar-tip">
          <el-icon><Info /></el-icon>
          <span>固定支出按月设置，每月按时缴纳；资金统计基于「收入管理」「支出管理」的真实流水</span>
        </div>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增固定支出</el-button>
      </div>
    </el-card>

    <!-- 资金统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card income">
        <div class="stat-icon"><el-icon><TrendCharts /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">本月收入</div>
          <div class="stat-value">+¥{{ fmt(overview.monthIncome) }}</div>
          <div class="stat-sub">累计收入 ¥{{ fmt(overview.totalIncome) }}</div>
        </div>
      </div>
      <div class="stat-card expense">
        <div class="stat-icon"><el-icon><Coin /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">本月支出</div>
          <div class="stat-value">-¥{{ fmt(overview.monthExpense) }}</div>
          <div class="stat-sub">累计支出 ¥{{ fmt(overview.totalExpense) }}</div>
        </div>
      </div>
      <div class="stat-card balance">
        <div class="stat-icon"><el-icon><Wallet /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">本月结余</div>
          <div class="stat-value" :class="{ negative: overview.monthBalance < 0 }">
            {{ overview.monthBalance >= 0 ? '+' : '' }}¥{{ fmt(overview.monthBalance) }}
          </div>
          <div class="stat-sub">收入 − 支出</div>
        </div>
      </div>
      <div class="stat-card saving">
        <div class="stat-icon"><el-icon><PiggyBank /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">累计存款</div>
          <div class="stat-value" :class="{ negative: overview.totalBalance < 0 }">
            ¥{{ fmt(overview.totalBalance) }}
          </div>
          <div class="stat-sub">存了多少钱</div>
        </div>
      </div>
    </div>

    <!-- 固定支出 -->
    <el-card shadow="never" class="fixed-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">每月固定支出</span>
          <el-tag type="warning" size="large">
            月固定支出合计：¥{{ fmt(overview.monthFixedTotal) }}
          </el-tag>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="expenseName" label="支出名称" min-width="140" />
        <el-table-column label="分类" width="130">
          <template #default="{ row }">
            <el-tag :type="categoryTag(row.category)" size="small">{{ categoryLabel(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="月均金额" width="140">
          <template #default="{ row }">
            <span class="money">¥{{ fmt(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="缴费日" width="100">
          <template #default="{ row }">
            <span v-if="row.dueDay">每月{{ row.dueDay }}日</span>
            <span v-else class="gray">未设置</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.active === 1"
              @change="(v) => toggleActive(row, v)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" show-overflow-tooltip min-width="120" />
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="tableData.length === 0" description="还没有配置固定支出，点击右上角新增" />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑固定支出' : '新增固定支出'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="支出名称" required>
          <el-input v-model="form.expenseName" placeholder="例如：物业费、车位费、车位管理费、话费、网费" maxlength="50" />
        </el-form-item>
        <el-form-item label="分类" required>
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="月均金额" required>
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="50" style="width: 100%" />
        </el-form-item>
        <el-form-item label="每月缴费日">
          <el-input-number v-model="form.dueDay" :min="1" :max="28" :step="1" style="width: 100%" placeholder="1-28" />
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
import { ref, reactive, onMounted } from 'vue'
import { Plus, Info, TrendCharts, Coin, Wallet, PiggyBank } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getFinanceOverview,
  addFixedExpense,
  updateFixedExpense,
  deleteFixedExpense,
  type FixedExpense,
  type FinanceOverview
} from '@/api/fixedExpense'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()
const loading = ref(false)
const month = ref(new Date().toISOString().slice(0, 7))
const tableData = ref<FixedExpense[]>([])
const overview = reactive<FinanceOverview>({
  totalIncome: 0,
  totalExpense: 0,
  totalBalance: 0,
  monthIncome: 0,
  monthExpense: 0,
  monthBalance: 0,
  monthFixedTotal: 0,
  fixedExpenses: []
})

const categories = [
  { value: 'PROPERTY', label: '物业费' },
  { value: 'PARKING', label: '车位费' },
  { value: 'PARKING_MANAGE', label: '车位管理费' },
  { value: 'PHONE', label: '话费' },
  { value: 'INTERNET', label: '网费' },
  { value: 'UTILITY', label: '水电气' },
  { value: 'OTHER', label: '其他' }
]

const categoryMap = Object.fromEntries(categories.map(c => [c.value, c.label]))
const categoryLabel = (v: string) => categoryMap[v] || v
const categoryTag = (v: string) => {
  const map: Record<string, string> = {
    PROPERTY: 'primary',
    PARKING: 'success',
    PARKING_MANAGE: 'success',
    PHONE: 'warning',
    INTERNET: 'warning',
    UTILITY: 'danger',
    OTHER: 'info'
  }
  return map[v] || 'info'
}

const dialogVisible = ref(false)
const form = reactive<FixedExpense>({
  memberId: 0,
  expenseName: '',
  category: '',
  amount: 0,
  dueDay: 15,
  active: 1,
  autoRecord: 0,
  remark: ''
})

const fmt = (v: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const load = async () => {
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  loading.value = true
  try {
    const [y, m] = month.value.split('-').map(Number)
    const data = await getFinanceOverview(memberStore.currentMemberId, y, m)
    Object.assign(overview, data)
    tableData.value = data.fixedExpenses || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  Object.assign(form, {
    id: undefined,
    memberId: memberStore.currentMemberId,
    expenseName: '',
    category: 'OTHER',
    amount: 0,
    dueDay: 15,
    active: 1,
    autoRecord: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const openEdit = (row: FixedExpense) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.expenseName.trim()) {
    ElMessage.warning('请输入支出名称')
    return
  }
  try {
    if (form.id) {
      await updateFixedExpense(form)
      ElMessage.success('更新成功')
    } else {
      await addFixedExpense(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  }
}

const toggleActive = async (row: FixedExpense, val: boolean | string | number) => {
  try {
    const active = val === true || val === 1 ? 1 : 0
    await updateFixedExpense({ ...row, active })
    row.active = active
    load()
  } catch {
    // 拦截器已提示
  }
}

const remove = async (row: FixedExpense) => {
  try {
    await ElMessageBox.confirm(`确定删除「${row.expenseName}」?`, '提示', { type: 'warning' })
    await deleteFixedExpense(row.id as number)
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

.toolbar-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 13px;
  flex: 1;
  justify-content: center;
}

.stat-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 1100px) {
  .stat-cards {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.stat-icon :deep(svg) {
  width: 26px;
  height: 26px;
}

.stat-card.income .stat-icon { background: #67c23a; }
.stat-card.expense .stat-icon { background: #f56c6c; }
.stat-card.balance .stat-icon { background: #409eff; }
.stat-card.saving .stat-icon { background: #e6a23c; }

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  margin: 2px 0;
}

.stat-value.negative { color: #f56c6c; }

.stat-sub {
  font-size: 12px;
  color: #c0c4cc;
}

.fixed-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.money {
  color: #f56c6c;
  font-weight: 600;
}

.gray { color: #c0c4cc; }
</style>
