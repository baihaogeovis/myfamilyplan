<template>
  <div class="page-container">
    <!-- 顶部：日期 + 完成率统计 -->
    <el-card class="summary-card" shadow="never">
      <div class="summary-bar">
        <div class="date-area">
          <el-date-picker
            v-model="currentDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY年MM月DD日"
            placeholder="选择日期"
            @change="load"
          />
          <el-button text type="primary" @click="jumpToday">回到今天</el-button>
        </div>

        <div class="stat-items">
          <div class="stat-item">
            <div class="stat-label">今日完成率</div>
            <div class="stat-value total">{{ totalRate }}%</div>
            <el-progress :percentage="totalRate" :stroke-width="8" :show-text="false" />
          </div>
          <div class="stat-item">
            <div class="stat-label">家庭任务</div>
            <div class="stat-value family">{{ familyDone }} / {{ familyTotal }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">工作任务</div>
            <div class="stat-value work">{{ workDone }} / {{ workTotal }}</div>
          </div>
        </div>

        <div class="actions">
          <el-button :icon="History" @click="historyVisible = true">历史任务</el-button>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增任务</el-button>
        </div>
      </div>
    </el-card>

    <!-- 家庭/工作双栏任务列表 -->
    <div class="task-columns">
      <el-card shadow="never" class="task-card family-card">
        <template #header>
          <div class="card-header">
            <span class="card-title"><Home class="icon" /> 家庭任务</span>
            <el-tag :type="familyTotal > 0 && familyDone === familyTotal ? 'success' : 'info'" size="small">
              {{ familyDone }}/{{ familyTotal }}
            </el-tag>
          </div>
        </template>
        <el-empty v-if="familyTasks.length === 0" description="暂无家庭任务，点击右上角新增" :image-size="80" />
        <div v-else class="task-list">
          <div v-for="task in familyTasks" :key="task.id" class="task-item" :class="{ done: task.completed === 1 }">
            <el-checkbox :model-value="task.completed === 1" @change="(v) => toggleTask(task, v)" />
            <span class="task-name">{{ task.taskName }}</span>
            <el-tag v-if="task.priority === 'HIGH'" type="danger" size="small">高</el-tag>
            <el-tag v-else-if="task.priority === 'LOW'" type="info" size="small">低</el-tag>
            <span v-if="task.remark" class="task-remark">{{ task.remark }}</span>
            <div class="task-ops">
              <el-button link type="danger" size="small" @click="remove(task)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <el-card shadow="never" class="task-card work-card">
        <template #header>
          <div class="card-header">
            <span class="card-title"><Briefcase class="icon" /> 工作任务</span>
            <el-tag :type="workTotal > 0 && workDone === workTotal ? 'success' : 'info'" size="small">
              {{ workDone }}/{{ workTotal }}
            </el-tag>
          </div>
        </template>
        <el-empty v-if="workTasks.length === 0" description="暂无工作任务，点击右上角新增" :image-size="80" />
        <div v-else class="task-list">
          <div v-for="task in workTasks" :key="task.id" class="task-item" :class="{ done: task.completed === 1 }">
            <el-checkbox :model-value="task.completed === 1" @change="(v) => toggleTask(task, v)" />
            <span class="task-name">{{ task.taskName }}</span>
            <el-tag v-if="task.priority === 'HIGH'" type="danger" size="small">高</el-tag>
            <el-tag v-else-if="task.priority === 'LOW'" type="info" size="small">低</el-tag>
            <span v-if="task.remark" class="task-remark">{{ task.remark }}</span>
            <div class="task-ops">
              <el-button link type="danger" size="small" @click="remove(task)">删除</el-button>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 新增任务对话框 -->
    <el-dialog v-model="dialogVisible" title="新增任务" width="440px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="任务名称" required>
          <el-input v-model="form.taskName" placeholder="例如：打扫卫生 / 完成周报" maxlength="100" />
        </el-form-item>
        <el-form-item label="任务类型">
          <el-radio-group v-model="form.taskType">
            <el-radio-button value="FAMILY">家庭</el-radio-button>
            <el-radio-button value="WORK">工作</el-radio-button>
          </el-radio-group>
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

    <!-- 历史任务对话框 -->
    <el-dialog v-model="historyVisible" title="历史任务" width="780px">
      <div class="history-filters">
        <el-select v-model="hisFilters.taskType" placeholder="类型" clearable style="width: 120px" @change="loadHistory">
          <el-option label="家庭" value="FAMILY" />
          <el-option label="工作" value="WORK" />
        </el-select>
        <el-select v-model="hisFilters.completed" placeholder="状态" clearable style="width: 120px" @change="loadHistory">
          <el-option label="已完成" :value="1" />
          <el-option label="未完成" :value="0" />
        </el-select>
        <el-date-picker
          v-model="hisFilters.dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          @change="loadHistory"
        />
      </div>
      <el-table :data="historyData" v-loading="historyLoading" stripe>
        <el-table-column prop="taskName" label="任务" show-overflow-tooltip />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">
            <el-tag :type="row.taskType === 'FAMILY' ? 'warning' : 'primary'" size="small">
              {{ row.taskType === 'FAMILY' ? '家庭' : '工作' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成" width="80">
          <template #default="{ row }">
            <el-tag :type="row.completed === 1 ? 'success' : 'info'" size="small">
              {{ row.completed === 1 ? '已完成' : '未完成' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="taskDate" label="日期" width="120" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pager"
        v-model:current-page="hisPage"
        v-model:page-size="hisSize"
        :total="hisTotal"
        layout="total, prev, pager, next"
        @current-change="loadHistory"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, History, Home, Briefcase } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDailyTaskList,
  getDailyTaskPage,
  addDailyTask,
  completeDailyTask,
  deleteDailyTask,
  type DailyTask
} from '@/api/dailyTask'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()

const currentDate = ref(new Date().toISOString().slice(0, 10))
const tasks = ref<DailyTask[]>([])

const dialogVisible = ref(false)
const historyVisible = ref(false)

const form = reactive<DailyTask>({
  memberId: 0,
  taskName: '',
  taskType: 'FAMILY',
  priority: 'MEDIUM',
  remark: ''
})

// 历史
const historyLoading = ref(false)
const historyData = ref<DailyTask[]>([])
const hisTotal = ref(0)
const hisPage = ref(1)
const hisSize = ref(10)
const hisFilters = reactive({
  taskType: '',
  completed: undefined as number | undefined,
  dateRange: null as null | [string, string]
})

const familyTasks = computed(() => tasks.value.filter(t => t.taskType !== 'WORK'))
const workTasks = computed(() => tasks.value.filter(t => t.taskType === 'WORK'))

const familyTotal = computed(() => familyTasks.value.length)
const familyDone = computed(() => familyTasks.value.filter(t => t.completed === 1).length)
const workTotal = computed(() => workTasks.value.length)
const workDone = computed(() => workTasks.value.filter(t => t.completed === 1).length)

const totalRate = computed(() => {
  const total = tasks.value.length
  if (total === 0) return 0
  const done = tasks.value.filter(t => t.completed === 1).length
  return Math.round((done / total) * 100)
})

const load = async () => {
  await memberStore.ensureLoaded()
  if (!memberStore.currentMemberId) {
    ElMessage.warning('请先在「成员管理」中添加家庭成员')
    return
  }
  tasks.value = await getDailyTaskList(memberStore.currentMemberId, currentDate.value)
}

const jumpToday = () => {
  currentDate.value = new Date().toISOString().slice(0, 10)
  load()
}

const toggleTask = async (task: DailyTask, val: string | boolean | number) => {
  const completed = val === true || val === 1 ? 1 : 0
  try {
    await completeDailyTask(task.id as number, completed)
    task.completed = completed
    if (completed === 1) {
      ElMessage.success('任务完成，太棒了！')
    }
  } catch {
    // 拦截器已提示
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
    taskName: '',
    taskType: 'FAMILY',
    priority: 'MEDIUM',
    remark: ''
  })
  dialogVisible.value = true
}

const submit = async () => {
  if (!form.taskName.trim()) {
    ElMessage.warning('请输入任务名称')
    return
  }
  try {
    await addDailyTask({ ...form, taskDate: currentDate.value })
    ElMessage.success('添加成功')
    dialogVisible.value = false
    load()
  } catch {
    // 拦截器已提示
  }
}

const remove = async (task: DailyTask) => {
  try {
    await ElMessageBox.confirm('确定删除该任务?', '提示', { type: 'warning' })
    await deleteDailyTask(task.id as number)
    ElMessage.success('删除成功')
    load()
    if (historyVisible.value) loadHistory()
  } catch {
    // 取消
  }
}

const loadHistory = async () => {
  if (!memberStore.currentMemberId) return
  historyLoading.value = true
  try {
    const res = await getDailyTaskPage({
      memberId: memberStore.currentMemberId,
      page: hisPage.value,
      size: hisSize.value,
      taskType: hisFilters.taskType || undefined,
      completed: hisFilters.completed,
      startDate: hisFilters.dateRange?.[0],
      endDate: hisFilters.dateRange?.[1]
    })
    historyData.value = res.records
    hisTotal.value = res.total
  } finally {
    historyLoading.value = false
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
  border-radius: 12px;
}

.summary-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
}

.date-area {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-items {
  display: flex;
  align-items: center;
  gap: 40px;
  flex: 1;
  justify-content: center;
}

.stat-item {
  min-width: 120px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 6px;
}

.stat-value.total { color: #409eff; }
.stat-value.family { color: #e6a23c; }
.stat-value.work { color: #67c23a; }

.actions {
  display: flex;
  gap: 8px;
}

.task-columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .task-columns {
    grid-template-columns: 1fr;
  }
  .stat-items {
    gap: 20px;
  }
}

.task-card {
  border-radius: 12px;
}

.task-card .card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
  color: #1f2937;
}

.card-title .icon {
  width: 18px;
  height: 18px;
}

.family-card :deep(.el-card__header) { border-bottom-color: #f3d19e; background: linear-gradient(90deg, #fff9e6, #fff); }
.work-card :deep(.el-card__header) { border-bottom-color: #b3e19d; background: linear-gradient(90deg, #f0fff0, #fff); }

.task-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 8px;
  transition: all 0.2s;
  background: #fafafa;
}

.task-item:hover {
  background: #f0f5ff;
}

.task-item.done .task-name {
  text-decoration: line-through;
  color: #c0c4cc;
}

.task-name {
  flex: 1;
  font-size: 14px;
}

.task-remark {
  color: #909399;
  font-size: 12px;
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-ops {
  opacity: 0;
  transition: opacity 0.2s;
}

.task-item:hover .task-ops {
  opacity: 1;
}

.history-filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
