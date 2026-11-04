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
        <el-button :icon="Download" :loading="exporting" @click="doExport">导出 Excel</el-button>
      </div>
    </el-card>

    <!-- 年度汇总卡片 -->
    <el-row :gutter="20" class="stat-row">
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="mini-card income">
          <div class="mini-label">年度总收入</div>
          <div class="mini-value">¥{{ formatMoney(annualIncome) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="mini-card expense">
          <div class="mini-label">年度总支出</div>
          <div class="mini-value">¥{{ formatMoney(annualExpense) }}</div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <el-card class="mini-card balance">
          <div class="mini-label">年度结余</div>
          <div class="mini-value" :class="annualBalance >= 0 ? 'positive' : 'negative'">
            ¥{{ formatMoney(annualBalance) }}
          </div>
        </el-card>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
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
      <el-col :xs="24" :md="12">
        <el-card class="chart-card">
          <template #header>
            <span class="card-title">{{ month }} 支出分类占比</span>
          </template>
          <div ref="expensePie" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
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
import { clearSummaryCache, onSummaryInvalidated } from '@/utils/summaryCache'
import { echarts, type ECharts } from '@/utils/echarts'
import { Download, RefreshCw } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import { formatMoney } from '@/utils/format'
import { CHART_BASE, CHART_SERIES, MONEY_COLORS } from '@/constants/palette'
import {
  getMonthlyStat,
  getCategoryStat,
  type MonthlyStat,
  type CategoryStatResponse
} from '@/api/statistics'
import { exportStatistics } from '@/api/export'

const exporting = ref(false)
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


const monthlyChart = ref<HTMLElement>()
const expensePie = ref<HTMLElement>()
const incomePie = ref<HTMLElement>()
let mChart: ECharts | null = null
let eChart: ECharts | null = null
let iChart: ECharts | null = null

const renderMonthly = () => {
  if (!monthlyChart.value) return
  if (!mChart) mChart = echarts.init(monthlyChart.value)
  mChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `¥${(v || 0).toLocaleString()}` },
    legend: { data: ['收入', '支出'], icon: 'roundRect', itemWidth: 14, itemHeight: 8, textStyle: { color: CHART_BASE.axisLabelColor } },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    animationDuration: 700,
    animationEasing: 'quarticOut',
    xAxis: {
      type: 'category',
      data: monthlyData.value.map((m) => `${m.month}月`),
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
      {
        name: '收入',
        type: 'bar',
        barMaxWidth: 22,
        data: monthlyData.value.map((m) => m.income),
        itemStyle: { color: MONEY_COLORS.income, borderRadius: [7, 7, 0, 0] }
      },
      {
        name: '支出',
        type: 'bar',
        barMaxWidth: 22,
        data: monthlyData.value.map((m) => m.expense),
        itemStyle: { color: MONEY_COLORS.expense, borderRadius: [7, 7, 0, 0] }
      }
    ]
  })
}

const renderPie = (el: HTMLElement | undefined, chart: ECharts | null, data: { category: string; amount: number }[], title: string) => {
  if (!el) return
  if (!chart) chart = echarts.init(el)
  const list = (data || []).map((d) => ({ name: d.category, value: Number(d.amount || 0) }))
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ¥{c} ({d}%)' },
    color: CHART_SERIES,
    legend: { bottom: 0, type: 'scroll', icon: 'circle', itemWidth: 9, textStyle: { color: CHART_BASE.axisLabelColor } },
    animationDuration: 700,
    animationEasing: 'quarticOut',
    series: [
      {
        name: title,
        type: 'pie',
        radius: ['46%', '70%'],
        padAngle: 2,
        itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 2 },
        center: ['50%', '45%'],
        data: list.length ? list : [{ name: '暂无数据', value: 0 }],
        label: { formatter: '{b}\n{d}%' }
      }
    ]
  })
  return chart
}

const loadMonthly = async () => {
  try {
    monthlyData.value = await getMonthlyStat(Number(year.value))
    await nextTick()
    renderMonthly()
  } catch {
    // 拦截器已提示
  }
}

const loadCategory = async () => {
  try {
    categoryData.value = await getCategoryStat(month.value)
    await nextTick()
    eChart = renderPie(expensePie.value, eChart, categoryData.value.expense, '支出分类') as any
    iChart = renderPie(incomePie.value, iChart, categoryData.value.income, '收入分类') as any
  } catch {
    // 拦截器已提示
  }
}

const loadAll = () => {
  clearSummaryCache()
  loadMonthly()
  loadCategory()
}

const doExport = async () => {
  exporting.value = true
  try {
    await exportStatistics({
      year: Number(year.value),
      month: month.value
    })
  } finally {
    exporting.value = false
  }
}

const handleResize = () => {
  mChart?.resize()
  eChart?.resize()
  iChart?.resize()
}

const stopSummaryRefresh = onSummaryInvalidated(() => {
  loadMonthly()
  loadCategory()
})

onMounted(() => {
  loadMonthly()
  loadCategory()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  stopSummaryRefresh()
  window.removeEventListener('resize', handleResize)
  mChart?.dispose()
  eChart?.dispose()
  iChart?.dispose()
})

watch(year, loadMonthly)
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
  color: var(--text-body);
}
.stat-row {
  margin-bottom: 20px;
}
.mini-card {
  border-radius: var(--r-lg);
}
.mini-label {
  font-size: 14px;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.mini-value {
  font-size: 24px;
  font-weight: 600;
}
.income .mini-value {
  color: var(--grass-600);
}
.expense .mini-value {
  color: var(--berry-ink);
}
.balance .mini-value,
.avg .mini-value {
  color: var(--text-strong);
}
.positive {
  color: var(--grass-600);
}
.negative {
  color: var(--berry-ink);
}
.chart-card {
  border-radius: var(--r-lg);
}
.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-strong);
}
.chart {
  height: 360px;
}

@media (max-width: 760px) {
  .filter-item {
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    gap: 6px;
  }

  .label {
    white-space: nowrap;
  }

  .stat-row :deep(.el-col) {
    margin-bottom: 12px;
  }

  .mini-value {
    font-size: 18px;
    overflow-wrap: anywhere;
  }

  .chart {
    height: 260px;
  }
}
</style>
