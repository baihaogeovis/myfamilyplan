<template>
  <div class="home">
    <p class="hello">{{ greeting }}，{{ currentMemberName || '家里人' }}</p>

    <section class="stats" aria-label="概览">
      <button type="button" class="stat" @click="go('/statistics')">
        <span>结余</span>
        <strong>¥{{ formatMoney(balance) }}</strong>
        <em>本月收入减支出</em>
      </button>
      <button type="button" class="stat" @click="go('/approval')">
        <span>待审</span>
        <strong>{{ overview.pendingCount }}</strong>
        <em>等你点头的支出</em>
      </button>
      <button type="button" class="stat" @click="go('/life-plan')">
        <span>计划进度</span>
        <strong>{{ planProgress }}%</strong>
        <em>{{ planHint }}</em>
      </button>
      <button type="button" class="stat" @click="go('/notifications')">
        <span>未读</span>
        <strong>{{ unread }}</strong>
        <em>还没看的通知</em>
      </button>
    </section>

    <section class="next">
      <header>
        <h2>接下来</h2>
      </header>
      <el-empty v-if="nextItems.length === 0" description="接下来还是空的" :image-size="72" />
      <ul v-else>
        <li v-for="item in nextItems" :key="item.key">
          <button type="button" @click="go(item.to)">
            <em>{{ item.kind }}</em>
            <strong>{{ item.title }}</strong>
            <span>{{ item.meta }}</span>
          </button>
        </li>
      </ul>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useMemberStore } from '@/stores/member'
import { formatMoney } from '@/utils/format'
import { greetingForHour } from '@/utils/family'
import { getDashboardOverview, type DashboardOverview } from '@/api/dashboard'
import { getApprovalPage, type ApprovalRecord } from '@/api/approval'
import { getUnreadCount, getNotificationList, type Notification } from '@/api/notification'
import { getLifePlanList, type LifePlan } from '@/api/lifePlan'
import { getGrowthPlanList, type GrowthPlan } from '@/api/growthPlan'

interface NextItem {
  key: string
  kind: string
  title: string
  meta: string
  to: string
}

const router = useRouter()
const memberStore = useMemberStore()
const greeting = greetingForHour()

const overview = ref<DashboardOverview>({
  monthIncome: 0,
  monthExpense: 0,
  budgetRemaining: 0,
  pendingCount: 0,
  memberCount: 0
})
const unread = ref(0)
const planProgress = ref(0)
const openPlanCount = ref(0)
const nextItems = ref<NextItem[]>([])

const currentMemberName = computed(() => {
  const m = memberStore.members.find((x) => x.id === memberStore.currentMemberId)
  return m?.memberName || ''
})

const balance = computed(() => (overview.value.monthIncome || 0) - (overview.value.monthExpense || 0))
const planHint = computed(() =>
  openPlanCount.value > 0 ? `${openPlanCount.value} 个还在进行` : '还没有进行中的计划'
)

const go = (path: string) => router.push(path)

const openStatus = (status?: string) => status !== 'COMPLETED' && status !== 'CANCELLED'

const load = async () => {
  await memberStore.ensureLoaded().catch(() => {})
  const memberId = memberStore.currentMemberId

  const [overviewRes, unreadRes, notesRes, approvalRes, lifeRes, growthRes] = await Promise.all([
    getDashboardOverview().catch(() => null),
    getUnreadCount().catch(() => null),
    getNotificationList({ page: 1, size: 4, isRead: 0 }).catch(() => null),
    memberId
      ? getApprovalPage({ approverId: memberId, status: 'PENDING', page: 1, size: 4 }).catch(() => null)
      : Promise.resolve(null),
    memberId ? getLifePlanList(memberId).catch(() => []) : Promise.resolve([]),
    memberId ? getGrowthPlanList(memberId).catch(() => []) : Promise.resolve([])
  ])

  if (overviewRes) overview.value = overviewRes
  unread.value = Number((unreadRes as { count?: number } | null)?.count || 0)

  const life = (lifeRes || []) as LifePlan[]
  const growth = (growthRes || []) as GrowthPlan[]
  const scored: number[] = []
  let open = 0
  life.forEach((plan) => {
    if (plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : plan.status === 'IN_PROGRESS' ? 50 : 0)
    if (openStatus(plan.status)) open += 1
  })
  growth.forEach((plan) => {
    if (plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : Number(plan.progress || 0))
    if (openStatus(plan.status)) open += 1
  })
  planProgress.value = scored.length
    ? Math.round(scored.reduce((sum, n) => sum + n, 0) / scored.length)
    : 0
  openPlanCount.value = open

  const items: NextItem[] = []
  const approvals = ((approvalRes as { records?: ApprovalRecord[] } | null)?.records || [])
  approvals.forEach((row) => {
    items.push({
      key: `approval-${row.id}`,
      kind: '待审',
      title: row.purpose,
      meta: `¥${formatMoney(row.amount)}`,
      to: '/approval'
    })
  })
  const notes = ((notesRes as { records?: Notification[] } | null)?.records || [])
  notes.forEach((row) => {
    items.push({
      key: `note-${row.id}`,
      kind: '未读',
      title: row.title,
      meta: row.content || '通知',
      to: '/notifications'
    })
  })
  life.filter((plan) => openStatus(plan.status)).slice(0, 3).forEach((plan) => {
    items.push({
      key: `life-${plan.id}`,
      kind: '生活',
      title: plan.planName,
      meta: plan.planDate || '生活计划',
      to: '/life-plan'
    })
  })
  growth.filter((plan) => openStatus(plan.status)).slice(0, 3).forEach((plan) => {
    items.push({
      key: `growth-${plan.id}`,
      kind: '成长',
      title: plan.title,
      meta: `${plan.progress || 0}%`,
      to: '/growth-plan'
    })
  })
  nextItems.value = items.slice(0, 6)
}

onMounted(load)
</script>

<style scoped>
.home {
  width: 100%;
  max-width: 1080px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.hello {
  margin: 0;
  font-family: var(--font-display);
  font-size: 28px;
  color: var(--text-strong);
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
  min-height: 148px;
  padding: 20px 18px;
  border: 1px solid var(--border-soft);
  border-radius: 24px;
  background: var(--surface);
  box-shadow: var(--shadow-1);
  text-align: left;
  cursor: pointer;
}

.stat span,
.next em {
  font-style: normal;
  font-size: 13px;
  letter-spacing: 0.08em;
  color: #E85D4C;
}

.stat strong {
  font-family: var(--font-display);
  font-size: 32px;
  font-weight: 600;
  letter-spacing: -0.03em;
  color: var(--text-strong);
  font-variant-numeric: tabular-nums;
}

.stat em {
  font-style: normal;
  font-size: 13px;
  color: var(--text-muted);
}

.stat:hover {
  border-color: #f0b2a8;
}

.next {
  padding: 8px 4px 0;
}

.next h2 {
  margin: 0 0 12px;
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
}

.next ul {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.next button {
  width: 100%;
  display: grid;
  grid-template-columns: 52px 1fr auto;
  gap: 12px;
  align-items: center;
  padding: 16px 18px;
  border: 1px solid var(--border-soft);
  border-radius: 18px;
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  box-shadow: var(--shadow-1);
}

.next strong {
  font-size: 16px;
  color: var(--text-strong);
}

.next span {
  font-size: 13px;
  color: var(--text-muted);
}

@media (max-width: 900px) {
  .stats {
    grid-template-columns: 1fr 1fr;
  }

  .next button {
    grid-template-columns: 52px 1fr;
  }

  .next span {
    grid-column: 2;
  }
}
</style>
