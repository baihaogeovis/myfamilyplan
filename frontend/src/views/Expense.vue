<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="filters">
            <el-select v-model="filters.category" placeholder="分类" clearable style="width: 140px" @change="load">
              <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <el-select v-model="filters.type" placeholder="类型" clearable style="width: 120px" @change="load">
              <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
            </el-select>
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              @change="load"
            />
            <el-button @click="resetFilters">重置</el-button>
          </div>
          <div class="actions">
            <el-button :icon="Download" :loading="exporting" @click="doExport">导出 Excel</el-button>
            <el-button type="primary" :icon="Plus" @click="openAdd">新增支出</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">{{ typeMap[row.type] || row.type }}</template>
        </el-table-column>
        <el-table-column label="金额" width="140">
          <template #default="{ row }">
            <span class="negative">-¥{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="expenseDate" label="日期" width="140" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑支出' : '新增支出'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
            <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { Download, Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getExpensePage,
  addExpense,
  updateExpense,
  deleteExpense,
  type ExpenseRecord
} from '@/api/expense'
import { useMemberStore } from '@/stores/member'
import { formatMoney } from '@/utils/format'
import { EXPENSE_CATEGORIES as categories, EXPENSE_TYPES as types } from '@/constants/categories'
import { exportExpense } from '@/api/export'
const categoryMap = Object.fromEntries(categories.map((c) => [c.value, c.label]))
const typeMap = Object.fromEntries(types.map((t) => [t.value, t.label]))

const memberStore = useMemberStore()
const loading = ref(false)
const tableData = ref<ExpenseRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)

const filters = reactive({
  category: '',
  type: '',
  dateRange: null as null | [string, string]
})

const exporting = ref(false)
const dialogVisible = ref(false)
const form = reactive<ExpenseRecord>({
  memberId: 0,
  amount: 0,
  category: '',
  type: '',
  expenseDate: '',
  remark: ''
})


const load = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  loading.value = true
  try {
    const res = await getExpensePage({
      memberId: memberStore.currentMemberId,
      page: page.value,
      size: size.value,
      category: filters.category || undefined,
      type: filters.type || undefined,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1]
    })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.category = ''
  filters.type = ''
  filters.dateRange = null
  page.value = 1
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
    amount: 0,
    category: '',
    type: '',
    expenseDate: new Date().toISOString().slice(0, 10),
    remark: ''
  })
  dialogVisible.value = true
}

const openEdit = (row: ExpenseRecord) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  try {
    if (form.id) {
      await updateExpense(form)
      ElMessage.success('更新成功')
    } else {
      await addExpense(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 错误已在拦截器提示
  }
}

const doExport = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  exporting.value = true
  try {
    await exportExpense({
      memberId: memberStore.currentMemberId,
      category: filters.category || undefined,
      type: filters.type || undefined,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1]
    })
  } finally {
    exporting.value = false
  }
}

const remove = async (row: ExpenseRecord) => {
  try {
    await ElMessageBox.confirm('确定删除该支出记录?', '提示', { type: 'warning' })
    await deleteExpense(row.id as number)
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
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
.filters,
.actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.negative {
  color: var(--berry-ink);
  font-weight: 600;
}
</style>
