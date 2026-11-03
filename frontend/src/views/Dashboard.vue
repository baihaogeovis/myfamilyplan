<template>
  <div class="home">
    <section class="hero">
      <div class="hero-copy">
        <p class="eyebrow">{{ dateLine }}</p>
        <h1>{{ greeting }}，{{ currentMemberName || '家里人' }}</h1>
        <p class="lede">
          {{ lede }}
        </p>
        <div class="hero-actions">
          <el-button type="primary" round @click="go('/workbench')">看今日安排</el-button>
          <el-button round @click="go('/expense')">记一笔支出</el-button>
        </div>
      </div>
      <div class="family-strip" v-if="memberStore.members.length">
        <p class="strip-label">这个家</p>
        <button
          v-for="m in memberStore.members"
          :key="m.id"
          type="button"
          class="person"
          :class="{ on: m.id === memberStore.currentMemberId }"
          @click="focusMember(m.id as number)"
        >
          <span class="person-avatar" :style="toneStyle(m.id)">{{ (m.memberName || '?').charAt(0) }}</span>
          <span class="person-meta">
            <strong>{{ m.memberName }}</strong>
            <em>{{ householdRole(m.relation) }} · {{ relationLabel(m.relation) }}</em>
          </span>
        </button>
      </div>
    </section>

    <section class="week" aria-label="本周">
      <button
        v-for="day in week"
        :key="day.iso"
        type="button"
        class="week-day"
        :class="{ today: day.isToday }"
        @click="openDay(day.iso)"
      >
        <span>周{{ day.label }}</span>
        <strong>{{ day.dateNum }}</strong>
      </button>
    </section>

    <div class="home-grid">
      <section class="panel today-panel">
        <header class="panel-head">
          <div>
            <h2>今日安排</h2>
            <p>{{ doneCount }}/{{ tasks.length }} 已完成</p>
          </div>
          <el-button link type="primary" @click="go('/workbench')">全部</el-button>
        </header>
        <el-empty v-if="tasks.length === 0" description="今天还是空白的。先记一件小事。" :image-size="72">
          <el-button type="primary" round @click="go('/workbench')">去安排</el-button>
        </el-empty>
        <ul v-else class="task-list">
          <li v-for="task in visibleTasks" :key="task.id" :class="{ done: task.completed === 1 }">
            <el-checkbox :model-value="task.completed === 1" @change="toggleTask(task, $event)" />
            <div class="task-copy">
              <strong>{{ task.taskName }}</strong>
              <span>{{ task.taskType === 'WORK' ? '工作' : '家里' }}<template v-if="task.priority === 'HIGH'"> · 优先</template></span>
            </div>
          </li>
        </ul>
      </section>

      <aside class="side">
        <section class="panel money">
          <header class="panel-head">
            <div>
              <h2>这个月</h2>
              <p>账本放在今天后面</p>
            </div>
            <el-button link type="primary" @click="go('/statistics')">月报</el-button>
          </header>
          <div class="money-row">
            <div>
              <span>收入</span>
              <strong>¥{{ formatMoney(overview.monthIncome) }}</strong>
            </div>
            <div>
              <span>支出</span>
              <strong class="out">¥{{ formatMoney(overview.monthExpense) }}</strong>
            </div>
            <div>
              <span>预算还剩</span>
              <strong>¥{{ formatMoney(overview.budgetRemaining) }}</strong>
            </div>
          </div>
        </section>

        <section class="quick">
          <button type="button" @click="go('/growth-plan')"><Baby :size="18" />成长计划</button>
          <button type="button" @click="go('/reward')"><Gift :size="18" />奖励小店</button>
          <button type="button" @click="go('/life-plan')"><Sun :size="18" />想做的事</button>
          <button type="button" @click="go('/approval')">
            <BadgeCheck :size="18" />
            审批
            <em v-if="overview.pendingCount">{{ overview.pendingCount }}</em>
          </button>
        </section>
      </aside>
    </div>

    <div class="home-grid lower">
      <el-card class="chart-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <h2>{{ currentMonth }} 收支</h2>
          </div>
        </template>
        <div ref="trendChart" class="chart-container"></div>
      </el-card>

      <el-card class="recent-card" shadow="never">
        <template #header>
          <div class="panel-head">
            <h2>最近进出</h2>
            <el-button link type="primary" @click="go('/expense')">去记账</el-button>
          </div>
        </template>
        <el-empty v-if="recent.length === 0" description="还没有流水" :image-size="60" />
        <div v-else class="recent-list">
          <div v-for="item in recent" :key="item.id" class="recent-item">
            <div>
              <div class="item-title">{{ categoryLabel(item.category) }}</div>
              <div class="item-time">{{ item.date }}</div>
            </div>
            <div class="item-amount" :class="item.type === 'INCOME' ? 'positive' : 'negative'">
              {{ item.type === 'INCOME' ? '+' : '-' }}¥{{ formatMoney(item.amount) }}
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <section class="panel approvals" v-if="pendingList.length">
      <header class="panel-head">
        <div>
          <h2>等你点头</h2>
          <p>大额支出先过一眼</p>
        </div>
        <el-button link type="primary" @click="go('/approval')">去处理</el-button>
      </header>
      <div class="approval-list">
        <article v-for="row in pendingList" :key="row.id" class="approval">
          <div>
            <strong>{{ row.purpose }}</strong>
            <span>{{ row.createTime }}</span>
          </div>
          <em>¥{{ formatMoney(row.amount) }}</em>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Baby, BadgeCheck, Gift, Sun } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { echarts, type ECharts } from '@/utils/echarts'
import { useMemberStore } from '@/stores/member'
import { formatMoney } from '@/utils/format'
import { categoryLabel } from '@/constants/categories'
import { CHART_BASE, MONEY_COLORS } from '@/constants/palette'
import {
  formatFriendlyDate,
  formatLocalDate,
  greetingForHour,
  householdRole,
  memberTone,
  relationLabel,
  weekOf
} from '@/utils/family'
import {
  getDashboardOverview,
  getDashboardTrend,
  getDashboardRecent,
  type DashboardOverview,
  type TrendPoint,
  type RecentTransaction
} from '@/api/dashboard'
import { getApprovalPage, type ApprovalRecord } from '@/api/approval'
import { completeDailyTask, getDailyTaskList, type DailyTask } from '@/api/dailyTask'

const router = useRouter()
const memberStore = useMemberStore()

const overview = ref<DashboardOverview>({
  monthIncome: 0,
  monthExpense: 0,
  budgetRemaining: 0,
  pendingCount: 0,
  memberCount: 0
})
const recent = ref<RecentTransaction[]>([])
const pendingList = ref<ApprovalRecord[]>([])
const trendPoints = ref<TrendPoint[]>([])
const tasks = ref<DailyTask[]>([])
const currentMonth = ref(formatLocalDate().slice(0, 7))
const dateLine = formatFriendlyDate()
const greeting = greetingForHour()
const week = weekOf(formatLocalDate())

const trendChart = ref<HTMLElement>()
let chart: ECharts | null = null

const currentMemberName = computed(() => {
  const m = memberStore.members.find((x) => x.id === memberStore.currentMemberId)
  return m?.memberName || ''
})

const doneCount = computed(() => tasks.value.filter((t) => t.completed === 1).length)
const visibleTasks = computed(() => tasks.value.slice(0, 6))

const lede = computed(() => {
  if (tasks.value.length === 0) return '今天还没有安排。先把一件家里的小事写下来。'
  const left = tasks.value.length - doneCount.value
  if (left === 0) return '今天的安排都勾完了。可以去看看奖励，或者记一笔账。'
  if (overview.value.pendingCount > 0) {
    return `还有 ${left} 件事没勾，另外有 ${overview.value.pendingCount} 笔支出等你点头。`
  }
  return `还有 ${left} 件事没勾。账本在下面，不着急。`
})

const toneStyle = (id?: number) => {
  const tone = memberTone(id)
  return { background: tone.bg, color: tone.ink }
}

const go = (path: string) => router.push(path)

const openDay = (iso: string) => {
  router.push({ path: '/workbench', query: { date: iso } })
}

const focusMember = (id: number) => {
  if (id === memberStore.currentMemberId) return
  memberStore.setCurrentMember(id)
  ElMessage.success('已换成这位家人来看今天')
  window.location.reload()
}

const loadOverview = async () => {
  overview.value = await getDashboardOverview()
}

const loadTrend = async () => {
  trendPoints.value = await getDashboardTrend(currentMonth.value)
  await nextTick()
  renderTrend()
}

const renderTrend = () => {
  if (!trendChart.value) return
  if (!chart) chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `¥${(v || 0).toLocaleString()}` },
    legend: { data: ['收入', '支出'], icon: 'roundRect', itemWidth: 14, itemHeight: 8, textStyle: { color: CHART_BASE.axisLabelColor } },
    grid: { left: 52, right: 16, top: 36, bottom: 28 },
    animationDuration: 700,
    animationEasing: 'quarticOut',
    xAxis: {
      type: 'category',
      data: trendPoints.value.map((p) => p.date.slice(5)),
      axisLine: { lineStyle: { color: CHART_BASE.axisLineColor } },
      axisTick: { show: false },
      axisLabel: { color: CHART_BASE.axisLabelColor }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: CHART_BASE.axisLabelColor },
      splitLine: { lineStyle: { color: CHART_BASE.splitLineColor, type: 'dashed' } }
    },
    series: [
      { name: '收入', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, data: trendPoints.value.map((p) => p.income), itemStyle: { color: MONEY_COLORS.income }, lineStyle: { width: 3, color: MONEY_COLORS.income }, areaStyle: { color: MONEY_COLORS.income, opacity: 0.08 } },
      { name: '支出', type: 'line', smooth: true, symbol: 'circle', symbolSize: 7, data: trendPoints.value.map((p) => p.expense), itemStyle: { color: MONEY_COLORS.expense }, lineStyle: { width: 3, color: MONEY_COLORS.expense }, areaStyle: { color: MONEY_COLORS.expense, opacity: 0.06 } }
    ]
  })
}

const loadRecent = async () => {
  recent.value = await getDashboardRecent(6)
}

const loadPending = async () => {
  if (!memberStore.currentMemberId) return
  const res = await getApprovalPage({
    approverId: memberStore.currentMemberId,
    status: 'PENDING',
    page: 1,
    size: 4
  })
  pendingList.value = res.records || []
}

const loadTasks = async () => {
  if (!memberStore.currentMemberId) return
  const list = await getDailyTaskList(memberStore.currentMemberId, formatLocalDate())
  tasks.value = list || []
}

const toggleTask = async (task: DailyTask, val: string | number | boolean) => {
  const completed = val === true || val === 1 ? 1 : 0
  try {
    await completeDailyTask(task.id as number, completed)
    task.completed = completed
  } catch {
    // 拦截器已提示
  }
}

const safe = async (job: () => Promise<void>) => {
  try {
    await job()
  } catch {
    // 单块失败不影响整页
  }
}

const handleResize = () => chart?.resize()

onMounted(async () => {
  await memberStore.ensureLoaded().catch(() => {})
  await Promise.all([
    safe(loadOverview),
    safe(loadTrend),
    safe(loadRecent),
    safe(loadPending),
    safe(loadTasks)
  ])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.home {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(260px, 0.9fr);
  gap: 28px;
  padding: 8px 8px 12px;
}

.eyebrow {
  margin: 0 0 8px;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: var(--clay);
}

.hero h1 {
  margin: 0;
  font-size: 44px;
  line-height: 1.15;
  font-weight: 600;
}

.lede {
  max-width: 460px;
  margin: 14px 0 0;
  font-size: 16px;
  line-height: 1.65;
  color: var(--text-body);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.family-strip {
  align-self: end;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px;
  border-radius: 24px;
  background: var(--surface);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-1);
}

.strip-label {
  margin: 0 4px 4px;
  font-size: 12px;
  letter-spacing: 0.12em;
  color: var(--text-muted);
}

.person {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 8px;
  border: 0;
  border-radius: 16px;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.person.on,
.person:hover {
  background: var(--surface-2);
}

.person-avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  font-weight: 700;
  flex-shrink: 0;
}

.person-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.person-meta strong {
  font-size: 14px;
  color: var(--text-strong);
}

.person-meta em {
  font-style: normal;
  font-size: 12px;
  color: var(--text-muted);
}

.week {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.week-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 0 14px;
  border: 1px solid transparent;
  border-radius: 18px;
  background: transparent;
  color: var(--text-muted);
  cursor: pointer;
}

.week-day strong {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
  color: var(--text-strong);
  font-weight: 600;
}

.week-day:hover {
  background: var(--surface);
}

.week-day.today {
  background: var(--grass-700);
  color: #f4f7f2;
}

.week-day.today strong {
  color: #fffdf8;
}

.home-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(280px, 0.85fr);
  gap: 16px;
}

.panel,
.chart-card,
.recent-card {
  background: var(--surface);
  border: 1px solid var(--border-soft);
  border-radius: 24px;
  box-shadow: var(--shadow-1);
}

.panel {
  padding: 20px 22px 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.panel-head h2,
.chart-card :deep(.el-card__header) h2,
.recent-card :deep(.el-card__header) h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 22px;
  font-weight: 600;
}

.panel-head p {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.task-list {
  list-style: none;
  margin: 14px 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.task-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: 16px;
  background: var(--surface-2);
}

.task-list li.done strong {
  color: var(--text-muted);
  text-decoration: line-through;
}

.task-copy {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.task-copy strong {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-strong);
}

.task-copy span {
  font-size: 12px;
  color: var(--text-muted);
}

.side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.money-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 16px;
}

.money-row div {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.money-row span {
  font-size: 12px;
  color: var(--text-muted);
}

.money-row strong {
  font-size: 18px;
  font-variant-numeric: tabular-nums;
  letter-spacing: -0.03em;
  color: var(--text-strong);
}

.money-row .out {
  color: var(--berry-ink);
}

.quick {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.quick button {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 64px;
  padding: 12px 14px;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: var(--surface);
  color: var(--text-strong);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-1);
}

.quick button em {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--clay);
  color: white;
  font-style: normal;
  font-size: 12px;
  line-height: 20px;
  text-align: center;
}

.quick button:hover {
  border-color: var(--border-strong);
}

.chart-container {
  height: 280px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 4px;
}

.item-title {
  font-size: 14px;
  color: var(--text-strong);
  font-weight: 600;
}

.item-time,
.approval span {
  font-size: 12px;
  color: var(--text-muted);
}

.item-amount {
  font-size: 15px;
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}

.positive { color: var(--grass-700); }
.negative { color: var(--berry-ink); }

.approval-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
}

.approval {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: var(--clay-soft);
}

.approval strong,
.approval em {
  display: block;
}

.approval strong {
  font-size: 14px;
  color: var(--text-strong);
}

.approval em {
  font-style: normal;
  font-weight: 700;
  color: var(--clay);
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .hero,
  .home-grid,
  .approval-list,
  .money-row {
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 34px;
  }

  .week {
    overflow-x: auto;
  }
}
</style>
