import request from './request'

export interface FundAllocation {
  id?: number
  memberId: number
  category: string
  percentage: number
  period: string
  year: number
  month?: number | null
}

export interface FundAllocationSummary {
  items: FundAllocation[]
  totalPercentage: number
  remainingPercentage: number
  complete: boolean
}

export function getFundAllocation(params: {
  memberId: number
  period?: string
  year?: number
  month?: number
}) {
  return request.get<FundAllocationSummary>('/fund-allocation/list', { params })
}

export function addFundAllocation(data: FundAllocation) {
  return request.post('/fund-allocation', data)
}

export function updateFundAllocation(data: FundAllocation) {
  return request.put('/fund-allocation', data)
}

export function deleteFundAllocation(id: number) {
  return request.delete(`/fund-allocation/${id}`)
}
