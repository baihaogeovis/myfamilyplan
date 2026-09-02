<template>
  <div class="page-container">
    <!-- 顶部统计 -->
    <div class="stat-cards">
      <div class="stat-card planning">
        <div class="stat-icon"><el-icon><Calendar /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">计划中</div>
          <div class="stat-value">{{ planningCount }}</div>
          <div class="stat-sub">待实现的美好愿望</div>
        </div>
      </div>
      <div class="stat-card completed">
        <div class="stat-icon"><el-icon><CheckCircle /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">已完成</div>
          <div class="stat-value">{{ completedCount }}</div>
          <div class="stat-sub">已经实现的目标</div>
        </div>
      </div>
      <div class="stat-card budget">
        <div class="stat-icon"><el-icon><Wallet /></el-icon></div>
        <div class="stat-body">
          <div class="stat-label">计划中预算</div>
          <div class="stat-value">¥{{ fmt(totalBudget) }}</div>
          <div class="stat-sub">所需总预算</div>
        </div>
      </div>
    </div>

    <!-- 计划列表 -->
    <el-card shadow="never" class="plan-card">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="planType" size="large" @change="load">
            <el-radio-button value="APPLIANCE">家电计划</el-radio-button>
            <el-radio-button value="TRAVEL">旅游计划</el-radio-button>
          </el-radio-group>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增计划</el-button>
        </div>
      </template>

      <div class="plan-grid" v-loading="loading">
        <el-empty v-if="list.length === 0" description="暂无计划，点击右上角添加美好愿望" />
        <div v-for="plan in list" :key="plan.id" class="plan-card-item" :class="plan.status.toLowerCase()">
          <div class="plan-top">
            <div class="plan-icon" :class="planType === 'APPLIANCE' ? 'appliance' : 'travel'">
              <el-icon>
                <component :is="planType === 'APPLIANCE' ? Tv : Plane" />
              </el-icon>
            </div>
            <div class="plan-info">
              <div class="plan-name">{{ plan.planName }}</div>
              <div class="plan-meta">
                <el-tag size="small" :type="planType === 'APPLIANCE' ? 'primary' : 'success'">
                  {{ plan.category || (planType === 'APPLIANCE' ? '家电' : '旅游') }}
                </el-tag>
                <el-tag size="small" :type="priorityTag(plan.priority)">{{ priorityLabel(plan.priority) }}</el-tag>
              </div>
            </div>
            <el-tag :type="statusTag(plan.status)" class="status-tag">{{ statusLabel(plan.status) }}</el-tag>
          </div>
          <div class="plan-bottom">
            <div class="plan-budget">
              <span class="budget-label">预算</span>
              <span class="budget-value">¥{{ fmt(plan.budget) }}</span>
            </div>
            <div class="plan-date">
              <el-icon><Calendar /></el-icon>
              {{ plan.planDate || '待定' }}
            </div>
            <div class="plan-ops">
              <el-button v-if="plan.status === 'PLANNED'" link type="success" size="small" @click="markComplete(plan)">完成</el-button>
              <el-button link type="primary" size="small" @click="openEdit(plan)">编辑</el-button>
              <el-button link type="danger" size="small" @click="remove(plan)">删除</el-button>
            </div>
          </div>
          <div v-if="plan.remark" class="plan-remark">{{ plan.remark }}</div>
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑计划' : '新增计划'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="计划名称" required>
          <el-input
            v-model="form.planName"
            :placeholder="planType === 'APPLIANCE' ? '例如：双开门冰箱、洗碗机' : '例如：三亚、云南、日本'"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算金额">
          <el-input-number v-model="form.budget" :min="0" :precision="2" :step="500" style="width: 100%" />
        </el-form-item>
        <el-form-item :label="planType === 'APPLIANCE' ? '计划时间' : '出行日期'">
          <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio-button value="HIGH">高</el-radio-button>
            <el-radio-button value="MEDIUM">中</el-radio-button>
            <el-radio-button value="LOW">低</el-radio-button>
          </el-radio-group>
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
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { Plus, Calendar, CheckCircle, Wallet, Tv, Plane } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getLifePlanList,
  addLifePlan,
  updateLifePlan,
  changeLifePlanStatus,
  deleteLifePlan,
  type LifePlan
} from '@/api/lifePlan'
import { useMemberStore } from '@/stores/member'
import { formatMoney } from '@/utils/format'

const memberStore = useMemberStore()

const planType = ref('APPLIANCE')
const list = ref<LifePlan[]>([])
const loading = ref(false)

const applianceCategories = ['电视', '冰箱', '空调', '洗衣机', '电脑', '手机', '厨房电器', '清洁电器', '卫浴电器', '其他']
const travelCategories = ['国内', '国外', '亲子游', '自驾游', '海岛度假', '文化之旅', '其他']
const categories = computed(() => (planType.value === 'APPLIANCE' ? applianceCategories : travelCategories))

const planningCount = computed(() => list.value.filter(p => p.status === 'PLANNED').length)
const completedCount = computed(() => list.value.filter(p => p.status === 'COMPLETED').length)
const totalBudget = computed(() =>
  list.value.filter(p => p.status === 'PLANNED').reduce((s, p) => s + (p.budget || 0), 0)
)

const priorityMap: Record<string, string> = { HIGH: '高', MEDIUM: '中', LOW: '低' }
const priorityLabel = (v?: string) => priorityMap[v || 'MEDIUM'] || '中'
const priorityTag = (v?: string) => (v === 'HIGH' ? 'danger' : v === 'LOW' ? 'info' : 'warning')

const statusMap: Record<string, string> = { PLANNED: '计划中', COMPLETED: '已完成', CANCELLED: '已取消' }
const statusLabel = (v?: string) => statusMap[v || 'PLANNED'] || v || ''
const statusTag = (v?: string) => (v === 'COMPLETED' ? 'success' : v === 'CANCELLED' ? 'info' : 'warning')

const fmt = formatMoney

const dialogVisible = ref(false)
const form = reactive<LifePlan>({
  memberId: 0,
  planType: 'APPLIANCE',
  planName: '',
  category: '',
  budget: 0,
  planDate: '',
  status: 'PLANNED',
  priority: 'MEDIUM',
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
    list.value = await getLifePlanList(memberStore.currentMemberId, planType.value)
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
    memberId: memberStore.currentMemberId,
    planType: planType.value,
    planName: '',
    category: planType.value === 'APPLIANCE' ? '电视' : '国内',
    budget: 0,
    planDate: '',
    status: 'PLANNED',
    priority: 'MEDIUM',
    remark: ''
  })
  dialogVisible.value = true
}

const openEdit = (row: LifePlan) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.planName.trim()) {
    ElMessage.warning('请输入计划名称')
    return
  }
  try {
    if (form.id) {
      await updateLifePlan(form)
      ElMessage.success('更新成功')
    } else {
      await addLifePlan(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  }
}

const markComplete = async (plan: LifePlan) => {
  try {
    await ElMessageBox.confirm(`确定「${plan.planName}」已经实现了吗？`, '完成确认', { type: 'success' })
    await changeLifePlanStatus(plan.id as number, 'COMPLETED')
    ElMessage.success('恭喜，愿望实现！')
    load()
  } catch {
    // 取消
  }
}

const remove = async (plan: LifePlan) => {
  try {
    await ElMessageBox.confirm(`确定删除「${plan.planName}」?`, '提示', { type: 'warning' })
    await deleteLifePlan(plan.id as number)
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

.stat-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

@media (max-width: 900px) {
  .stat-cards {
    grid-template-columns: 1fr;
  }
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: white;
  border-radius: var(--r-lg);
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

.stat-card.planning .stat-icon { background: var(--sun); }
.stat-card.completed .stat-icon { background: var(--grass-500); }
.stat-card.budget .stat-icon { background: var(--sky); }

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-strong);
  margin: 2px 0;
}

.stat-sub {
  font-size: 12px;
  color: var(--text-muted);
}

.plan-card {
  border-radius: var(--r-lg);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
}

.plan-grid {
  min-height: 120px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 16px;
}

.plan-card-item {
  border: 1px solid var(--border-soft);
  border-radius: var(--r-lg);
  padding: 16px;
  transition: all 0.2s;
  background: #fff;
}

.plan-card-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.plan-card-item.completed {
  opacity: 0.65;
  background: #f8f9fb;
}

.plan-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.plan-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.plan-icon.appliance { background: linear-gradient(135deg, var(--sky), #6fd0f0); }
.plan-icon.travel { background: linear-gradient(135deg, var(--grass-500), var(--sprout)); }

.plan-icon :deep(svg) {
  width: 22px;
  height: 22px;
}

.plan-info {
  flex: 1;
  min-width: 0;
}

.plan-name {
  font-weight: 600;
  font-size: 15px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-meta {
  display: flex;
  gap: 6px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.plan-bottom {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px dashed var(--border-soft);
  flex-wrap: wrap;
}

.plan-budget {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.budget-label {
  font-size: 12px;
  color: var(--text-muted);
}

.budget-value {
  font-weight: 700;
  color: var(--sun-ink);
  font-size: 16px;
}

.plan-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--text-muted);
}

.plan-date :deep(svg) {
  width: 14px;
  height: 14px;
}

.plan-ops {
  margin-left: auto;
  display: flex;
}

.plan-remark {
  margin-top: 10px;
  font-size: 12px;
  color: var(--text-muted);
  background: var(--grass-50);
  border-radius: 6px;
  padding: 6px 10px;
}
</style>
