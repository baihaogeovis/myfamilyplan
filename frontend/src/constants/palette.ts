/**
 * JS / Canvas 侧取色（echarts、style 绑定等无法使用 CSS 变量的场景）
 * 数值必须与 styles/theme.css 的 token 保持一致。
 */
export const PALETTE = {
  grass400: '#5fc673',
  grass500: '#2fae52',
  grass600: '#188043',
  grass700: '#146536',
  sprout: '#a3d93f',
  sun: '#f7b733',
  sky: '#35a7d7',
  berry: '#e8604c',
  plum: '#8b6be0',
  inkStrong: '#12301c',
  textMuted: '#5a6b60'
} as const

/** 图表系列配色：草绿为主，搭配嫩芽/阳光/湖水/浆果，色相跨度大以便区分 */
export const CHART_SERIES: string[] = [
  PALETTE.grass500,
  PALETTE.sprout,
  PALETTE.sun,
  PALETTE.sky,
  PALETTE.plum,
  PALETTE.berry,
  PALETTE.grass700,
  '#c58af0'
]

/** 收支语义色：收入用草绿、支出用莓果，均比旧版更稳且与主题同系 */
export const MONEY_COLORS = {
  income: PALETTE.grass600,
  expense: '#c0392b',
  budget: PALETTE.sky,
  pending: PALETTE.sun
} as const

/** 图表通用外观：柔和网格与低对比坐标轴 */
export const CHART_BASE = {
  axisLineColor: '#dcebdd',
  axisLabelColor: PALETTE.textMuted,
  splitLineColor: '#eef5ef'
} as const
