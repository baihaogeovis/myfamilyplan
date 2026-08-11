<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <el-tabs v-model="activeTab" @tab-change="load" class="tabs">
            <el-tab-pane label="我的申请" name="mine" />
            <el-tab-pane label="待我审批" name="pending" />
          </el-tabs>
          <el-button type="primary" :icon="Plus" @click="openAdd">发起申请</el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="purpose" label="用途" />
        <el-table-column label="金额" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="100">
          <template #default="{ row }">{{ nameOf(row.applicantId) }}</template>
        </el-table-column>
        <el-table-column label="审批人" width="100">
          <template #default="{ row }">{{ nameOf(row.approverId) || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <template v-if="activeTab === 'pending' && row.status === 'PENDING'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button v-else link type="danger" @click="remove(row)">删除</el-button>
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

    <el-dialog v-model="dialogVisible" title="发起资金申请" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用途">
          <el-input v-model="form.purpose" placeholder="如：购买家电" />
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="审批人">
          <el-select v-model="form.approverId" placeholder="请选择成员" style="width: 100%">
            <el-option
              v-for="m in approverOptions"
              :key="m.id"
              :label="m.memberName"
              :value="m.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApprovalPage,
  applyApproval,
  approveApproval,
  rejectApproval,
  deleteApproval,
  type ApprovalRecord
} from '@/api/approval'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()
const loading = ref(false)
const tableData = ref<ApprovalRecord[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const activeTab = ref<'mine' | 'pending'>('mine')

const dialogVisible = ref(false)
const form = reactive<ApprovalRecord>({
  applicantId: 0,
  approverId: undefined,
  amount: 0,
  purpose: '',
  remark: ''
})

const approverOptions = computed(() =>
  memberStore.members.filter((m) => m.id !== memberStore.currentMemberId)
)
const formatMoney = (v: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const nameOf = (id?: number) => {
  if (!id) return ''
  const m = memberStore.members.find((x) => x.id === id)
  return m ? m.memberName : ''
}
const statusType = (s?: string) =>
  s === 'APPROVED' ? 'success' : s === 'REJECTED' ? 'danger' : 'warning'
const statusText = (s?: string) =>
  s === 'APPROVED' ? '已通过' : s === 'REJECTED' ? '已驳回' : '待审批'

const load = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (activeTab.value === 'mine') {
      params.applicantId = memberStore.currentMemberId
    } else {
      params.approverId = memberStore.currentMemberId
      params.status = 'PENDING'
    }
    const res = await getApprovalPage(params)
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

const openAdd = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  Object.assign(form, {
    id: undefined,
    applicantId: memberStore.currentMemberId,
    approverId: undefined,
    amount: 0,
    purpose: '',
    remark: ''
  })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.approverId) {
    ElMessage.warning('请选择审批人')
    return
  }
  try {
    await applyApproval(form)
    ElMessage.success('申请已提交')
    dialogVisible.value = false
    load()
  } catch {
    // 错误已在拦截器提示
  }
}

const approve = async (row: ApprovalRecord) => {
  try {
    await ElMessageBox.confirm('确定通过该申请?', '审批', { type: 'success' })
    await approveApproval(row.id as number, memberStore.currentMemberId as number)
    ElMessage.success('已通过')
    load()
  } catch {
    // 取消
  }
}

const reject = async (row: ApprovalRecord) => {
  try {
    await ElMessageBox.confirm('确定驳回该申请?', '审批', { type: 'warning' })
    await rejectApproval(row.id as number, memberStore.currentMemberId as number)
    ElMessage.success('已驳回')
    load()
  } catch {
    // 取消
  }
}

const remove = async (row: ApprovalRecord) => {
  try {
    await ElMessageBox.confirm('确定删除该申请记录?', '提示', { type: 'warning' })
    await deleteApproval(row.id as number)
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
.tabs {
  flex: 1;
}
.tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
