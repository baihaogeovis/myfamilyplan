<template>
  <div class="shell">
    <header class="topbar">
      <router-link to="/dashboard" class="brand">
        <span class="logo-mark"><Sprout :size="18" /></span>
        <span class="brand-name">家庭成长</span>
      </router-link>

      <nav class="primary" aria-label="主要导航">
        <router-link
          v-for="item in primary"
          :key="item.id"
          :to="item.to"
          class="primary-link"
          :class="{ active: sectionId === item.id }"
        >
          {{ item.label }}
        </router-link>
      </nav>

      <div class="tools">
        <el-dropdown v-if="memberStore.members.length > 0" @command="switchMember">
          <span class="member-info">
            <span class="member-avatar" :style="avatarStyle(memberStore.currentMemberId)">
              {{ currentMemberName.charAt(0) || '家' }}
            </span>
            <span class="member-text">
              <em>{{ currentRole }}</em>
              <strong>{{ currentMemberName || '选择家人' }}</strong>
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
            <span class="username">{{ userStore.userInfo?.nickname || '家庭' }}</span>
            <ArrowDown :size="15" />
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="account">家庭账号</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <div class="page-intro">
      <h1>{{ currentPageTitle }}</h1>
      <p>{{ currentPageKicker }}</p>
    </div>

    <nav v-if="subs.length > 1" class="subnav" aria-label="本节">
      <router-link
        v-for="item in subs"
        :key="item.to"
        :to="item.to"
        class="sub-link"
        :class="{ active: route.path === item.to }"
      >
        {{ item.label }}
      </router-link>
    </nav>

    <main class="shell-main">
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, Sprout } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMemberStore } from '@/stores/member'
import { logout } from '@/api/auth'
import { householdRole, memberTone, relationLabel } from '@/utils/family'

interface PrimaryItem {
  id: string
  label: string
  to: string
  match: string[]
}

interface SubItem {
  to: string
  label: string
}

const primary: PrimaryItem[] = [
  { id: 'work', label: '工作台', to: '/dashboard', match: ['/dashboard', '/workbench'] },
  { id: 'family', label: '家庭', to: '/members', match: ['/members', '/family'] },
  { id: 'plan', label: '计划', to: '/life-plan', match: ['/life-plan', '/growth-plan'] },
  {
    id: 'money',
    label: '钱',
    to: '/expense',
    match: ['/income', '/expense', '/fixed-expense', '/budget', '/fund-allocation', '/statistics', '/approval']
  },
  { id: 'reward', label: '奖励', to: '/reward', match: ['/reward', '/reward-items'] },
  { id: 'me', label: '我的', to: '/notifications', match: ['/notifications'] }
]

const subnav: Record<string, SubItem[]> = {
  work: [
    { to: '/dashboard', label: '概览' },
    { to: '/workbench', label: '今日安排' }
  ],
  family: [
    { to: '/members', label: '成员' },
    { to: '/family', label: '家庭账号' }
  ],
  plan: [
    { to: '/life-plan', label: '生活' },
    { to: '/growth-plan', label: '成长' }
  ],
  money: [
    { to: '/expense', label: '支出' },
    { to: '/income', label: '收入' },
    { to: '/fixed-expense', label: '固定账单' },
    { to: '/budget', label: '预算' },
    { to: '/fund-allocation', label: '分配' },
    { to: '/approval', label: '审批' },
    { to: '/statistics', label: '月报' }
  ],
  reward: [
    { to: '/reward', label: '奖励' },
    { to: '/reward-items', label: '活动方案' }
  ],
  me: [{ to: '/notifications', label: '通知' }]
}

const titles: Record<string, string> = {
  '/dashboard': '工作台',
  '/workbench': '工作台',
  '/income': '钱',
  '/expense': '钱',
  '/fixed-expense': '钱',
  '/budget': '钱',
  '/fund-allocation': '钱',
  '/statistics': '钱',
  '/approval': '钱',
  '/life-plan': '计划',
  '/growth-plan': '计划',
  '/reward': '奖励',
  '/reward-items': '奖励',
  '/family': '家庭',
  '/members': '家庭',
  '/notifications': '我的'
}

const kickers: Record<string, string> = {
  '/dashboard': '先看今天，再看这个月',
  '/workbench': '家里的事和工作的事，分开打勾',
  '/income': '钱进来的记录',
  '/expense': '钱出去的记录',
  '/fixed-expense': '每个月都会来的账单',
  '/budget': '这个月打算怎么花',
  '/fund-allocation': '收入按分类分一份',
  '/life-plan': '家电、旅行，慢慢攒',
  '/growth-plan': '短期一件事，长期一个方向',
  '/reward': '额度花在你们约好的事上',
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

const sectionId = computed(() => primary.find((item) => item.match.includes(route.path))?.id || 'work')
const subs = computed(() => subnav[sectionId.value] || [])

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

const switchMember = (memberId: string | number) => {
  memberStore.setCurrentMember(Number(memberId))
  ElMessage.success('已换成这位家人')
  window.location.reload()
}

const currentPageTitle = computed(() => titles[route.path] || '家庭成长')
const currentPageKicker = computed(() => kickers[route.path] || '陪这个家慢慢长大')

const handleCommand = async (command: string) => {
  if (command === 'account') {
    router.push('/family')
    return
  }
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
  }
}

onMounted(() => {
  memberStore.ensureLoaded().catch(() => {})
})
</script>

<style scoped>
.shell {
  min-height: 100%;
  display: flex;
  flex-direction: column;
}

.topbar {
  position: sticky;
  top: 0;
  z-index: 30;
  display: flex;
  align-items: center;
  gap: 28px;
  padding: 18px 32px 14px;
  background: rgba(243, 238, 228, 0.9);
  backdrop-filter: blur(16px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: var(--text-strong);
  flex-shrink: 0;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  color: #fffdf8;
  background: #E85D4C;
}

.brand-name {
  font-family: var(--font-display);
  font-size: 18px;
  font-weight: 600;
}

.primary {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.primary-link {
  padding: 8px 14px;
  border-radius: 999px;
  color: var(--text-body);
  text-decoration: none;
  font-size: 15px;
  font-weight: 600;
}

.primary-link:hover {
  background: rgba(255, 252, 248, 0.7);
  color: var(--text-strong);
}

.primary-link.active {
  background: #E85D4C;
  color: #fffdf8;
}

.tools {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.member-info,
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 6px;
  border-radius: 999px;
  background: var(--surface);
  border: 1px solid var(--border-soft);
  cursor: pointer;
  outline: none;
}

.user-info {
  padding: 8px 12px;
}

.member-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
}

.member-text {
  display: flex;
  flex-direction: column;
  line-height: 1.15;
}

.member-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--clay);
}

.member-text strong,
.username {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-strong);
}

.menu-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border-radius: 50%;
}

.page-intro {
  padding: 8px 32px 0;
}

.page-intro h1 {
  margin: 0;
  font-size: 34px;
  font-weight: 600;
  line-height: 1.15;
}

.page-intro p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 14px;
}

.subnav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 16px 32px 0;
}

.sub-link {
  padding: 7px 12px;
  border-radius: 999px;
  background: transparent;
  color: var(--text-body);
  text-decoration: none;
  font-size: 13px;
  border: 1px solid transparent;
}

.sub-link.active {
  background: var(--teal-soft);
  border-color: transparent;
  color: var(--teal);
  font-weight: 600;
}

.shell-main {
  padding: 18px 32px 48px;
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

@media (max-width: 980px) {
  .topbar {
    flex-wrap: wrap;
    gap: 12px;
    padding: 14px 16px 10px;
  }

  .primary {
    order: 3;
    width: 100%;
    overflow-x: auto;
  }

  .page-intro,
  .subnav,
  .shell-main {
    padding-left: 16px;
    padding-right: 16px;
  }

  .page-intro h1 {
    font-size: 28px;
  }

  .member-text em {
    display: none;
  }
}
</style>
