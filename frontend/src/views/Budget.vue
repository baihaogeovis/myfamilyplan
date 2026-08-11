<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="filters">
            <el-select v-model="filters.periodType" placeholder="周期" clearable style="width: 120px" @change="load">
              <el-option label="月度" value="MONTH" />
              <el-option label="年度" value="YEAR" />
            </el-select>
            <el-date-picker
              v-model="filters.yearMonth"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              @change="load"
            />
            <el-button @click="resetFilters">重置</el-button>
          </div>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增预算</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="周期" width="100">
          <template #default="{ row }">
            {{ row.periodType === 'YEAR' ? '年度' : '月度' }}
          </template>
        </el-table-column>
        <el-table-column label="期间" width="140">
          <template #default="{ row }">
            {{ row.year }}{{ row.periodType === 'MONTH' ? '-' + pad(row.month) : '' }}
          </template>
        </el-table-column>
        <el-table-column label="预算金额" width="160">
          <template #default="{ row }">
            <span class="positive">¥{{ formatMoney(row.amount) }}</span>
          </template>
        </el-table-column>
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

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑预算' : '新增预算'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期">
          <el-radio-group v-model="form.periodType">
            <el-radio value="MONTH">月度</el-radio>
            <el-radio value="YEAR">年度</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="年份">
          <el-date-picker
            v-model="yearModel"
            type="year"
            value-format="YYYY"
            placeholder="选择年份"
            style="width: 100%"
            @change="onYearChange"
          />
        </el-form-item>
        <el-form-item label="月份" v-if="form.periodType === 'MONTH'">
          <el-select v-model="form.month" placeholder="请选择" style="width: 100%">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="500" style="width: 100%" />
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
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBudgetPage, addBudget, updateBudget, deleteBudget, type BudgetPlan } from '@/api/budget'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()
const loading = ref(false)
const tableData = ref<BudgetPlan[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const categories = ['餐饮', '交通', '购物', '居住', '教育', '医疗', '娱乐', '其他', '总预算']
const yearModel = ref<string>(String(new Date().getFullYear()))

const filters = reactive({
  periodType: '',
  yearMonth: null as null | string
})

const dialogVisible = ref(false)
const form = reactive<BudgetPlan>({
  memberId: 0,
  amount: 0,
  category: '',
  periodType: 'MONTH',
  year: new Date().getFullYear(),
  month: new Date().getMonth() + 1,
  remark: ''
})

const pad = (v?: number) => String(v ?? 0).padStart(2, '0')
const formatMoney = (v: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const onYearChange = () => {
  form.year = Number(yearModel.value)
}

const load = async () => {
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  loading.value = true
  try {
    const params: any = {
      memberId: memberStore.currentMemberId,
      page: page.value,
      size: size.value,
      periodType: filters.periodType || undefined
    }
    if (filters.yearMonth) {
      const [y, m] = filters.yearMonth.split('-')
      params.year = Number(y)
      params.month = Number(m)
    }
    const res = await getBudgetPage(params)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  filters.periodType = ''
  filters.yearMonth = null
  load()
}

const openAdd = () => {
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  Object.assign(form, {
    id: undefined,
    memberId: memberStore.currentMemberId,
    amount: 0,
    category: '',
    periodType: 'MONTH',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    remark: ''
  })
  yearModel.value = String(form.year)
  dialogVisible.value = true
}

const openEdit = (row: BudgetPlan) => {
  Object.assign(form, { ...row })
  yearModel.value = String(row.year)
  dialogVisible.value = true
}

const submit = async () => {
  try {
    if (form.periodType === 'YEAR') {
      form.month = undefined
    }
    if (form.id) {
      await updateBudget(form)
      ElMessage.success('更新成功')
    } else {
      await addBudget(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 错误已在拦截器提示
  }
}

const remove = async (row: BudgetPlan) => {
  try {
    await ElMessageBox.confirm('确定删除该预算?', '提示', { type: 'warning' })
    await deleteBudget(row.id as number)
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
.filters {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
.positive {
  color: #409eff;
  font-weight: 600;
}
</style>
