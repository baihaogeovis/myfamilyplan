<template>
  <div class="dashboard-container">
    <!-- 统计卡片 -->
    <el-row :gutter="20" class="stats-row" type="flex">
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card income">
          <div class="stat-icon"><Wallet /></div>
          <div class="stat-info">
            <div class="stat-label">本月收入</div>
            <div class="stat-value">¥{{ formatMoney(overview.monthIncome) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card expense">
          <div class="stat-icon"><ShoppingCart /></div>
          <div class="stat-info">
            <div class="stat-label">本月支出</div>
            <div class="stat-value">¥{{ formatMoney(overview.monthExpense) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card budget">
          <div class="stat-icon"><TrendingUp /></div>
          <div class="stat-info">
            <div class="stat-label">预算剩余</div>
            <div class="stat-value">¥{{ formatMoney(overview.budgetRemaining) }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6" :lg="6">
        <el-card class="stat-card pending" @click="goApproval">
          <div class="stat-icon"><Bell /></div>
          <div class="stat-info">
            <div class="stat-label">待办事项</div>
            <div class="stat-value">{{ overview.pendingCount }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 + 最近流水 -->
    <el-row :gutter="20" style="margin-top: 20px" type="flex">
      <el-col :xs="24" :sm="24" :md="16" :lg="16">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">{{ currentMonth }} 收支趋势</span>
            </div>
          </template>
          <div ref="trendChart" class="chart-container"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="8" :lg="8">
        <el-card class="recent-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">最近收支</span>
            </div>
          </template>
          <el-empty v-if="recent.length === 0" description="暂无记录" :image-size="60" />
          <div v-else class="recent-list">
            <div
              v-for="item in recent"
              :key="item.id"
              class="recent-item"
              :class="item.type === 'INCOME' ? 'income-item' : 'expense-item'"
            >
              <div class="item-info">
                <div class="item-title">{{ item.category }}</div>
                <div class="item-time">{{ item.date }}</div>
              </div>
              <div class="item-amount" :class="item.type === 'INCOME' ? 'positive' : 'negative'">
                {{ item.type === 'INCOME' ? '+' : '-' }}¥{{ formatMoney(item.amount) }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待我审批 -->
    <el-card style="margin-top: 20px">
      <template #header>
        <div class="card-header">
          <span class="card-title">待我审批</span>
          <el-button link type="primary" @click="goApproval">去处理</el-button>
        </div>
      </template>
      <el-empty v-if="pendingList.length === 0" description="暂无待审批事项" :image-size="60" />
      <el-table v-else :data="pendingList" stripe>
        <el-table-column prop="purpose" label="用途" />
        <el-table-column label="金额" width="140">
          <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="180" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { Wallet, ShoppingCart, TrendingUp, Bell } from 'lucide-vue-next'
import { useMemberStore } from '@/stores/member'
import {
  getDashboardOverview,
  getDashboardTrend,
  getDashboardRecent,
  type DashboardOverview,
  type TrendPoint,
  type RecentTransaction
} from '@/api/dashboard'
import { getApprovalPage, type ApprovalRecord } from '@/api/approval'

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
const currentMonth = ref(new Date().toISOString().slice(0, 7))

const trendChart = ref<HTMLElement>()
let chart: echarts.ECharts | null = null

const formatMoney = (v: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

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
    legend: { data: ['收入', '支出'] },
    grid: { left: 60, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: trendPoints.value.map((p) => p.date.slice(5)) },
    yAxis: { type: 'value' },
    series: [
      { name: '收入', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.income), itemStyle: { color: '#67C23A' }, areaStyle: { opacity: 0.1 } },
      { name: '支出', type: 'line', smooth: true, data: trendPoints.value.map((p) => p.expense), itemStyle: { color: '#F56C6C' }, areaStyle: { opacity: 0.1 } }
    ]
  })
}

const loadRecent = async () => {
  recent.value = await getDashboardRecent(8)
}

const loadPending = async () => {
  if (!memberStore.currentMemberId) return
  try {
    const res = await getApprovalPage({
      approverId: memberStore.currentMemberId,
      status: 'PENDING',
      page: 1,
      size: 10
    })
    pendingList.value = res.records
  } catch {
    // ignore
  }
}

const goApproval = () => router.push('/approval')

const handleResize = () => chart?.resize()

onMounted(async () => {
  await memberStore.ensureLoaded()
  await Promise.all([loadOverview(), loadTrend(), loadRecent(), loadPending()])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  chart?.dispose()
})
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}
.stats-row {
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}
.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
}
.stat-icon {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  color: white;
}
.income .stat-icon {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.expense .stat-icon {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}
.budget .stat-icon {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}
.pending .stat-icon {
  background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
}
.stat-info {
  flex: 1;
}
.stat-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}
.chart-card,
.recent-card {
  border-radius: 12px;
}
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.chart-container {
  height: 350px;
}
.recent-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 8px;
  transition: background 0.3s;
}
.recent-item:hover {
  background: #f5f7fa;
}
.item-info {
  flex: 1;
}
.item-title {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}
.item-time {
  font-size: 12px;
  color: #909399;
}
.item-amount {
  font-size: 16px;
  font-weight: 600;
}
.positive {
  color: #67c23a;
}
.negative {
  color: #f56c6c;
}
</style>
