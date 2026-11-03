<template>
  <el-container class="layout-container">
    <el-aside :width="isNarrow ? '84px' : '248px'" class="layout-aside" :class="{ narrow: isNarrow }">
      <div class="logo-container" :class="{ narrow: isNarrow }">
        <span class="logo-mark">
          <Sprout :size="20" />
        </span>
        <span v-if="!isNarrow" class="logo-word">
          <strong>家庭成长</strong>
          <em>把一天过好</em>
        </span>
      </div>

      <nav class="rail" aria-label="家庭导航">
        <section v-for="section in sections" :key="section.label" class="rail-section">
          <p v-if="!isNarrow" class="section-label">{{ section.label }}</p>
          <router-link
            v-for="item in section.items"
            :key="item.to"
            :to="item.to"
            class="rail-link"
            :class="{ active: activeMenu === item.to }"
          >
            <component :is="item.icon" :size="18" />
            <span v-if="!isNarrow">{{ item.label }}</span>
          </router-link>
        </section>
      </nav>
    </el-aside>

    <el-container class="layout-body">
      <el-header class="layout-header">
        <div class="header-left">
          <div>
            <h2 class="page-title">{{ currentPageTitle }}</h2>
            <p class="page-kicker">{{ currentPageKicker }}</p>
          </div>
        </div>

        <div class="header-right">
          <el-dropdown v-if="memberStore.members.length > 0" @command="switchMember">
            <span class="member-info">
              <span class="member-avatar" :style="avatarStyle(memberStore.currentMemberId)">
                {{ currentMemberName.charAt(0) || '家' }}
              </span>
              <span class="member-text">
                <em>{{ currentRole }}</em>
                <strong>{{ currentMemberName || '请选择家人' }}</strong>
              </span>
              <ArrowDown :size="15" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="m in memberStore.members"
                  :key="m.id"
                  :command="m.id"
                  :disabled="m.id === memberStore.currentMemberId"
                >
                  <span class="menu-dot" :style="{ background: memberTone(m.id).dot }"></span>
                  {{ m.memberName }} · {{ relationLabel(m.relation) }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar">
                {{ (userStore.userInfo?.nickname || '家').charAt(0) }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <ArrowDown :size="15" />
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { Component } from 'vue'
import {
  Home, CalendarCheck, Baby, Gift, Sparkles, Sun,
  ArrowDownLeft, ArrowUpRight, Receipt, PieChart, PiggyBank,
  Users, Building2, BadgeCheck, Bell, ArrowDown, Sprout
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMemberStore } from '@/stores/member'
import { logout } from '@/api/auth'
import { householdRole, memberTone, relationLabel } from '@/utils/family'

interface NavItem {
  to: string
  label: string
  icon: Component
}

const sections: { label: string; items: NavItem[] }[] = [
  {
    label: '今天',
    items: [
      { to: '/dashboard', label: '今天', icon: Home },
      { to: '/workbench', label: '今日安排', icon: CalendarCheck }
    ]
  },
  {
    label: '成长',
    items: [
      { to: '/growth-plan', label: '成长计划', icon: Baby },
      { to: '/reward', label: '奖励小店', icon: Gift },
      { to: '/reward-items', label: '活动方案', icon: Sparkles }
    ]
  },
  {
    label: '生活',
    items: [{ to: '/life-plan', label: '想做的事', icon: Sun }]
  },
  {
    label: '账本',
    items: [
      { to: '/income', label: '收入', icon: ArrowDownLeft },
      { to: '/expense', label: '支出', icon: ArrowUpRight },
      { to: '/fixed-expense', label: '固定账单', icon: Receipt },
      { to: '/budget', label: '预算', icon: PiggyBank },
      { to: '/statistics', label: '月报', icon: PieChart }
    ]
  },
  {
    label: '我们家',
    items: [
      { to: '/family', label: '家庭', icon: Building2 },
      { to: '/members', label: '家人', icon: Users },
      { to: '/approval', label: '审批', icon: BadgeCheck },
      { to: '/notifications', label: '提醒', icon: Bell }
    ]
  }
]

const titles: Record<string, string> = {
  '/dashboard': '今天',
  '/workbench': '今日安排',
  '/income': '收入',
  '/expense': '支出',
  '/fixed-expense': '固定账单',
  '/budget': '预算',
  '/life-plan': '想做的事',
  '/growth-plan': '成长计划',
  '/reward': '奖励小店',
  '/reward-items': '活动方案',
  '/approval': '审批',
  '/family': '家庭',
  '/members': '家人',
  '/statistics': '月报',
  '/notifications': '提醒'
}

const kickers: Record<string, string> = {
  '/dashboard': '先看今天，再看这个月',
  '/workbench': '家里的事和工作的事，分开打勾',
  '/income': '钱进来的记录',
  '/expense': '钱出去的记录',
  '/fixed-expense': '每个月都会来的账单',
  '/budget': '这个月打算怎么花',
  '/life-plan': '家电、旅行，慢慢攒',
  '/growth-plan': '短期一件事，长期一个方向',
  '/reward': '额度像零花钱，花在你们约定的事上',
  '/reward-items': '先定规则，再让孩子来兑',
  '/approval': '大额支出，家长过一眼',
  '/family': '这个家的名字和账号',
  '/members': '每个人一种颜色',
  '/statistics': '这个月过得怎么样',
  '/notifications': '该做的事会在这里等你'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const memberStore = useMemberStore()

const activeMenu = computed(() => route.path)
const isNarrow = ref(false)
const syncNarrow = () => {
  isNarrow.value = window.innerWidth < 992
}

const currentMember = computed(() =>
  memberStore.members.find((x) => x.id === memberStore.currentMemberId)
)
const currentMemberName = computed(() => currentMember.value?.memberName || '')
const currentRole = computed(() =>
  currentMember.value ? householdRole(currentMember.value.relation) : '家人'
)

const avatarStyle = (id?: number | null) => {
  const tone = memberTone(id)
  return { background: tone.bg, color: tone.ink }
}

const switchMember = async (memberId: string | number) => {
  memberStore.setCurrentMember(Number(memberId))
  ElMessage.success('已换成这位家人来看今天')
  window.location.reload()
}

const currentPageTitle = computed(() => titles[route.path] || '家庭成长')
const currentPageKicker = computed(() => kickers[route.path] || '陪这个家慢慢长大')

const handleCommand = async (command: string) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await logout()
      userStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch {
      // 取消退出
    }
  } else if (command === 'profile') {
    router.push('/family')
  }
}

onMounted(() => {
  syncNarrow()
  window.addEventListener('resize', syncNarrow)
  memberStore.ensureLoaded().catch(() => {})
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncNarrow)
})
</script>

<style scoped>
.layout-container {
  height: 100%;
  background: transparent;
}

.layout-aside {
  display: flex;
  flex-direction: column;
  background: rgba(255, 252, 248, 0.78);
  border-right: 1px solid var(--border-soft);
  overflow: hidden;
  backdrop-filter: blur(16px);
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 76px;
  padding: 0 20px;
  flex-shrink: 0;
}

.logo-container.narrow {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 14px;
  color: #fffdf8;
  background: var(--grass-700);
  flex-shrink: 0;
}

.logo-word {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.logo-word strong {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-strong);
}

.logo-word em {
  font-style: normal;
  font-size: 12px;
  color: var(--text-muted);
}

.rail {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px 28px;
}

.rail-section + .rail-section {
  margin-top: 16px;
}

.section-label {
  margin: 0 10px 6px;
  font-size: 11px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #9a9184;
}

.rail-link {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 40px;
  margin: 2px 0;
  padding: 0 12px;
  border-radius: 12px;
  color: var(--text-body);
  text-decoration: none;
  font-size: 14px;
}

.narrow .rail-link {
  justify-content: center;
  padding: 0;
}

.rail-link:hover {
  background: var(--surface-2);
  color: var(--text-strong);
}

.rail-link.active {
  background: var(--grass-700);
  color: #fffdf8;
  font-weight: 600;
}

.layout-body {
  min-width: 0;
  background: transparent;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 76px;
  padding: 0 28px;
  background: transparent;
  border-bottom: none;
}

.page-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 600;
  color: var(--text-strong);
  line-height: 1.15;
}

.page-kicker {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--text-muted);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-info,
.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 12px 6px 6px;
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-1);
  cursor: pointer;
  outline: none;
}

.member-avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
}

.member-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.member-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--clay);
}

.member-text strong,
.username {
  font-size: 13px;
  color: var(--text-strong);
  font-weight: 600;
}

.menu-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
}

.layout-main {
  padding: 0 28px 36px;
  overflow-y: auto;
}

.page-enter-active {
  transition: opacity var(--dur-2) var(--ease-out), transform var(--dur-3) var(--ease-out);
}

.page-leave-active {
  transition: opacity var(--dur-1) var(--ease-in);
}

.page-enter-from {
  opacity: 0;
  transform: translate3d(0, 10px, 0);
}

.page-leave-to {
  opacity: 0;
}

@media (max-width: 768px) {
  .layout-main {
    padding: 0 14px 24px;
  }

  .layout-header {
    height: auto;
    padding: 16px 14px 8px;
    align-items: flex-start;
  }

  .page-title {
    font-size: 22px;
  }

  .member-text em,
  .username {
    display: none;
  }
}
</style>
