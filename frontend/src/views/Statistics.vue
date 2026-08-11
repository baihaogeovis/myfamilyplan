<template>
  <div class="page-container">
    <!-- 筛选栏 -->
    <el-card class="filter-card">
      <div class="filter-bar">
        <div class="filter-item">
          <span class="label">统计年份</span>
          <el-date-picker
            v-model="year"
            type="year"
            placeholder="选择年份"
            value-format="YYYY"
            format="YYYY"
            @change="loadAll"
          />
        </div>
        <div class="filter-item">
          <span class="label">分类月份</span>
          <el-date-picker
            v-model="month"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            format="YYYY-MM"
            @change="loadCategory"
          />
        </div>
        <el-button :icon="RefreshCw" @click="loadAll">刷新</el-button>
      </div>
    </el-card>

    <!-- 年度汇总卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :span="6">
        <el-card class="mini-card income">
          <div class="mini-label">年度总收入</div>
          <div class="mini-value">¥{{ formatMoney(annualIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="mini-card expense">
          <div class="mini-label">年度总支出</div>
          <div class="mini-value">¥{{ formatMoney(annualExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="mini-card balance">
          <div class="mini-label">年度结余</div>
          <div class="mini-value" :class="annualBalance >= 0 ? 'positive' : 'negative'">
            ¥{{ formatMoney(annualBalance) }}
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="mini-card avg">
          <div class="mini-label">月均结余</div>
          <div class="mini-value">¥{{ formatMoney(annualBalance / 12) }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 月度趋势图 -->
    <el-card class="chart-card">
      <template #header>
        <span class="card-title">{{ year }} 年收支月度趋势</span>
      </template>
      <div ref="monthlyChart" class="chart"></div>
    </el-card>

    <!-- 分类占比 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">{{ month }} 支出分类占比</span>
          </template>
          <div ref="expensePie" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">{{ month }} 收入分类占比</span>
          </template>
          <div ref="incomePie" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import { RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getMonthlyStat,
  getCategoryStat,
  type MonthlyStat,
  type CategoryStatResponse
} from '@/api/statistics'

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.userId)

const year = ref(String(new Date().getFullYear()))
const month = ref(`${new Date().getFullYear()}-${String(new Date().getMonth() + 1).padStart(2, '0')}`)

const monthlyData = ref<MonthlyStat[]>([])
const categoryData = ref<CategoryStatResponse>({ income: [], expense: [] })

const annualIncome = computed(() =>
  monthlyData.value.reduce((s, m) => s + (m.income || 0), 0)
)
const annualExpense = computed(() =>
  monthlyData.value.reduce((s, m) => s + (m.expense || 0), 0)
)
const annualBalance = computed(() => annualIncome.value - annualExpense.value)

const formatMoney = (v: number) =>
  (v ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })

const monthlyChart = ref<HTMLElement>()
const expensePie = ref<HTMLElement>()
const incomePie = ref<HTMLElement>()
let mChart: echarts.ECharts | null = null
let eChart: echarts.ECharts | null = null
let iChart: echarts.ECharts | null = null

const renderMonthly = () => {
  if (!monthlyChart.value) return
  if (!mChart) mChart = echarts.init(monthlyChart.value)
  mChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `¥${(v || 0).toLocaleString()}` },
    legend: { data: ['收入', '支出'] },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: { type: 'category', data: monthlyData.value.map((m) => `${m.month}月`) },
    yAxis: { type: 'value' },
    series: [
      {
        name: '收入',
        type: 'bar',
        data: monthlyData.value.map((m) => m.income),
        itemStyle: { color: '#67C23A' }
      },
      {
        name: '支出',
        type: 'bar',
        data: monthlyData.value.map((m) => m.expense),
        itemStyle: { color: '#F56C6C' }
      }
    ]
  })
}

const renderPie = (el: HTMLElement | undefined, chart: echarts.ECharts | null, data: any[], title: string) => {
  if (!el) return
  if (!chart) chart = echarts.init(el)
  const list = (data || []).map((d) => ({ name: d.category, value: Number(d.amount || 0) }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['40%', '65%'],
        center: ['50%', '45%'],
        data: list.length ? list : [{ name: '暂无数据', value: 0 }],
        label: { formatter: '{b}\n{d}%' }
      }
    ]
  })
  return chart
}

const loadMonthly = async () => {
  if (!userId.value) return
  try {
    monthlyData.value = await getMonthlyStat(userId.value, Number(year.value))
    await nextTick()
    renderMonthly()
  } catch {
    // 拦截器已提示
  }
}

const loadCategory = async () => {
  if (!userId.value) return
  try {
    categoryData.value = await getCategoryStat(userId.value, month.value)
    await nextTick()
    eChart = renderPie(expensePie.value, eChart, categoryData.value.expense, '支出分类') as any
    iChart = renderPie(incomePie.value, iChart, categoryData.value.income, '收入分类') as any
  } catch {
    // 拦截器已提示
  }
}

const loadAll = () => {
  loadMonthly()
  loadCategory()
}

const handleResize = () => {
  mChart?.resize()
  eChart?.resize()
  iChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  mChart?.dispose()
  eChart?.dispose()
  iChart?.dispose()
})

watch(year, loadMonthly)
watch(month, loadCategory)
</script>

<style scoped>
.page-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}
.filter-card {
  margin-bottom: 20px;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.label {
  font-size: 14px;
  color: #606266;
}
.stat-row {
  margin-bottom: 20px;
}
.mini-card {
  border-radius: 12px;
}
.mini-label {
  font-size: 14px;
  color: #909399;
  margin-bottom: 8px;
}
.mini-value {
  font-size: 24px;
  font-weight: 600;
}
.income .mini-value {
  color: #67c23a;
}
.expense .mini-value {
  color: #f56c6c;
}
.balance .mini-value,
.avg .mini-value {
  color: #303133;
}
.positive {
  color: #67c23a;
}
.negative {
  color: #f56c6c;
}
.chart-card {
  border-radius: 12px;
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.chart {
  height: 360px;
}
</style>
