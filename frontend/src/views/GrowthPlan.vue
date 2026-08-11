<template>
  <div class="page-container">
    <!-- 顶部：孩子选择 + 统计 -->
    <el-card shadow="never" class="toolbar-card">
      <div class="toolbar">
        <div class="child-picker">
          <span class="picker-label">孩子</span>
          <el-select v-model="childId" placeholder="选择孩子" style="width: 180px" @change="switchChild">
            <el-option v-for="m in memberOptions" :key="m.id" :label="m.label" :value="m.id" />
          </el-select>
        </div>
        <div class="stat-items">
          <div class="stat-item">
            <div class="stat-label">进行中</div>
            <div class="stat-value planning">{{ inProgressCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">已达成</div>
            <div class="stat-value achieved">{{ achievedCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">平均进度</div>
            <div class="stat-value avg">{{ avgProgress }}%</div>
          </div>
        </div>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增目标</el-button>
      </div>
    </el-card>

    <!-- 短期/长期目标 -->
    <el-card shadow="never" class="plan-card">
      <template #header>
        <div class="card-header">
          <el-radio-group v-model="goalType" size="large" @change="load">
            <el-radio-button value="SHORT">短期目标</el-radio-button>
            <el-radio-button value="LONG">长期目标</el-radio-button>
          </el-radio-group>
          <span class="goal-tip">
            {{ goalType === 'SHORT' ? '短期目标：近期1-3个月可实现的小目标' : '长期目标：贯穿童年的大目标，慢慢培养' }}
          </span>
        </div>
      </template>

      <div class="goal-grid" v-loading="loading">
        <el-empty v-if="list.length === 0" description="暂无目标，点击右上角为孩子添加成长目标" />
        <div v-for="goal in list" :key="goal.id" class="goal-card" :class="goal.status.toLowerCase()">
          <div class="goal-top">
            <div class="goal-info">
              <div class="goal-title">{{ goal.title }}</div>
              <div class="goal-meta">
                <el-tag size="small" type="primary">{{ categoryLabel(goal.category) }}</el-tag>
                <span class="goal-date">
                  <el-icon><Calendar /></el-icon>
                  {{ goal.targetDate || '无期限' }}
                </span>
              </div>
            </div>
            <el-tag :type="statusTag(goal.status)" class="status-tag">{{ statusLabel(goal.status) }}</el-tag>
          </div>

          <div v-if="goal.description" class="goal-desc">{{ goal.description }}</div>

          <div class="goal-progress">
            <el-progress
              :percentage="goal.progress || 0"
              :stroke-width="10"
              :color="progressColor"
              :status="goal.status === 'ACHIEVED' ? 'success' : undefined"
            />
            <span class="progress-num">{{ goal.progress || 0 }}%</span>
          </div>

          <div class="goal-ops">
            <el-button v-if="goal.status === 'IN_PROGRESS'" link type="success" size="small" @click="markAchieved(goal)">达成</el-button>
            <el-button link type="primary" size="small" @click="openEdit(goal)">编辑</el-button>
            <el-button link type="danger" size="small" @click="remove(goal)">删除</el-button>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑目标' : '新增目标'" width="500px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="目标标题" required>
          <el-input
            v-model="form.title"
            placeholder="例如：背完乘法口诀 / 学会游泳 / 每天阅读30分钟"
            maxlength="100"
          />
        </el-form-item>
        <el-form-item label="目标类型">
          <el-radio-group v-model="form.goalType">
            <el-radio-button value="SHORT">短期</el-radio-button>
            <el-radio-button value="LONG">长期</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择" style="width: 100%">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="目标的具体内容和达成标准" maxlength="500" />
        </el-form-item>
        <el-form-item label="达成日期">
          <el-date-picker v-model="form.targetDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="当前进度">
          <el-slider v-model="form.progress" :step="5" show-input />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Calendar } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getGrowthPlanList,
  addGrowthPlan,
  updateGrowthPlan,
  updateGrowthProgress,
  changeGrowthStatus,
  deleteGrowthPlan,
  type GrowthPlan
} from '@/api/growthPlan'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()

const goalType = ref('SHORT')
const childId = ref<number | null>(null)
const list = ref<GrowthPlan[]>([])
const loading = ref(false)

const categories = ['学习', '运动', '艺术', '习惯', '社交', '自理', '其他']

// 孩子选项：优先展示子女，兜底显示全部成员
const memberOptions = computed(() => {
  const kids = memberStore.members.filter(m => /子|女|孩/.test(m.relation || ''))
  const pool = kids.length > 0 ? kids : memberStore.members
  return pool.map(m => ({
    id: m.id as number,
    label: m.relation ? `${m.memberName}（${m.relation}）` : m.memberName
  }))
})

const inProgressCount = computed(() => list.value.filter(g => g.status === 'IN_PROGRESS').length)
const achievedCount = computed(() => list.value.filter(g => g.status === 'ACHIEVED').length)
const avgProgress = computed(() => {
  if (list.value.length === 0) return 0
  return Math.round(list.value.reduce((s, g) => s + (g.progress || 0), 0) / list.value.length)
})

const categoryMap: Record<string, string> = {
  STUDY: '学习', SPORTS: '运动', ART: '艺术', HABIT: '习惯', SOCIAL: '社交', SELF: '自理', OTHER: '其他'
}
const categoryLabel = (c?: string) => (c && categoryMap[c]) || c || '其他'

const statusMap: Record<string, string> = { IN_PROGRESS: '进行中', ACHIEVED: '已达成', GIVEN_UP: '已放弃' }
const statusLabel = (v?: string) => statusMap[v || 'IN_PROGRESS'] || v || ''
const statusTag = (v?: string) => (v === 'ACHIEVED' ? 'success' : v === 'GIVEN_UP' ? 'info' : 'warning')
const progressColor = (p: number) => (p < 40 ? '#e6a23c' : p < 80 ? '#409eff' : '#67c23a')

const dialogVisible = ref(false)
const form = reactive<GrowthPlan>({
  memberId: 0,
  title: '',
  goalType: 'SHORT',
  category: '学习',
  description: '',
  targetDate: '',
  status: 'IN_PROGRESS',
  progress: 0,
  remark: ''
})

const switchChild = async () => {
  if (childId.value) memberStore.setCurrentMember(childId.value)
  await load()
}

const load = async () => {
  if (!childId.value) return
  loading.value = true
  try {
    list.value = await getGrowthPlanList(childId.value, goalType.value)
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  if (!childId.value) {
    ElMessage.warning('请先在「成员管理」中添加孩子')
    return
  }
  Object.assign(form, {
    id: undefined,
    memberId: childId.value,
    title: '',
    goalType: goalType.value,
    category: '学习',
    description: '',
    targetDate: '',
    status: 'IN_PROGRESS',
    progress: 0,
    remark: ''
  })
  dialogVisible.value = true
}

const openEdit = (row: GrowthPlan) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入目标标题')
    return
  }
  try {
    if (form.id) {
      await updateGrowthPlan(form)
      ElMessage.success('更新成功')
    } else {
      await addGrowthPlan(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  }
}

const markAchieved = async (goal: GrowthPlan) => {
  try {
    await ElMessageBox.confirm(`确定「${goal.title}」已经达成了吗？`, '达成确认', { type: 'success' })
    await changeGrowthStatus(goal.id as number, 'ACHIEVED')
    await updateGrowthProgress(goal.id as number, 100)
    ElMessage.success('目标达成，太棒了！')
    load()
  } catch {
    // 取消
  }
}

const remove = async (goal: GrowthPlan) => {
  try {
    await ElMessageBox.confirm(`确定删除「${goal.title}」?`, '提示', { type: 'warning' })
    await deleteGrowthPlan(goal.id as number)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 取消
  }
}

onMounted(async () => {
  if (memberStore.members.length === 0 && memberStore.currentMemberId) {
    // 兜底：使用当前成员作为默认孩子
    childId.value = memberStore.currentMemberId
  } else {
    // 优先选中第一个孩子
    childId.value = memberOptions.value[0]?.id ?? memberStore.currentMemberId
  }
  await load()
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
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.child-picker {
  display: flex;
  align-items: center;
  gap: 8px;
}

.picker-label {
  font-size: 14px;
  color: #606266;
}

.stat-items {
  display: flex;
  gap: 40px;
  flex: 1;
  justify-content: center;
}

.stat-item {
  text-align: center;
}

.stat-label {
  font-size: 13px;
  color: #909399;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  margin-top: 2px;
}

.stat-value.planning { color: #e6a23c; }
.stat-value.achieved { color: #67c23a; }
.stat-value.avg { color: #409eff; }

.plan-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.goal-tip {
  font-size: 13px;
  color: #909399;
}

.goal-grid {
  min-height: 120px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 16px;
}

.goal-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
  background: #fff;
  transition: all 0.2s;
}

.goal-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.goal-card.achieved {
  opacity: 0.7;
  background: #f8fdf9;
}

.goal-card.given_up {
  opacity: 0.55;
}

.goal-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.goal-info {
  flex: 1;
  min-width: 0;
}

.goal-title {
  font-weight: 600;
  font-size: 15px;
}

.goal-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 6px;
  flex-wrap: wrap;
}

.goal-date {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #909399;
}

.goal-date :deep(svg) {
  width: 13px;
  height: 13px;
}

.goal-desc {
  margin-top: 10px;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  border-radius: 6px;
  padding: 8px 10px;
  line-height: 1.6;
}

.goal-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.goal-progress .el-progress {
  flex: 1;
}

.progress-num {
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  min-width: 40px;
  text-align: right;
}

.goal-ops {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
  gap: 4px;
}
</style>
