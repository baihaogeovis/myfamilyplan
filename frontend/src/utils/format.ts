/**
 * 通用金额格式化
 */
export const formatMoney = (value?: number | null): string =>
  (value ?? 0).toLocaleString('zh-CN', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  })
