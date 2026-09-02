<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isNarrow ? '76px' : '232px'" class="layout-aside">
      <div class="logo-container" :class="{ narrow: isNarrow }">
        <span class="logo-mark">
          <Sprout :size="22" class="anim-sway" />
        </span>
        <span v-if="!isNarrow" class="logo-word">
          <strong>家庭成长系统</strong>
          <em>陪孩子慢慢长大</em>
        </span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isNarrow"
        :collapse-transition="false"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <Home :size="18" />
          <template #title><span>仪表盘</span></template>
        </el-menu-item>

        <el-menu-item index="/workbench">
          <ClipboardCheck :size="18" />
          <template #title><span>每日工作台</span></template>
        </el-menu-item>

        <el-sub-menu index="finance">
          <template #title>
            <Wallet :size="18" />
            <span>资金管理</span>
          </template>
          <el-menu-item index="/income">收入管理</el-menu-item>
          <el-menu-item index="/expense">支出管理</el-menu-item>
          <el-menu-item index="/fixed-expense">固定支出</el-menu-item>
          <el-menu-item index="/budget">预算规划</el-menu-item>
          <el-menu-item index="/statistics">统计报表</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="life">
          <template #title>
            <Sparkles :size="18" />
            <span>美好生活</span>
          </template>
          <el-menu-item index="/life-plan">生活计划</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="growth">
          <template #title>
            <Baby :size="18" />
            <span>儿童成长</span>
          </template>
          <el-menu-item index="/growth-plan">成长计划</el-menu-item>
          <el-menu-item index="/reward">奖励额度</el-menu-item>
          <el-menu-item index="/reward-items">活动方案</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <Settings :size="18" />
            <span>系统管理</span>
          </template>
          <el-menu-item index="/family">家庭管理</el-menu-item>
          <el-menu-item index="/approval">资金审批</el-menu-item>
          <el-menu-item index="/members">成员管理</el-menu-item>
          <el-menu-item index="/notifications">提醒中心</el-menu-item>
        </el-sub-menu>
      </el-menu>

      <div v-if="!isNarrow" class="aside-grass" aria-hidden="true"></div>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="layout-body">
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <span class="title-bar" aria-hidden="true"></span>
          <h2 class="page-title">{{ currentPageTitle }}</h2>
        </div>

        <div class="header-right">
          <!-- 当前操作成员切换 -->
          <el-dropdown v-if="memberStore.members.length > 0" @command="switchMember">
            <span class="member-info">
              <span class="member-avatar">{{ currentMemberName.charAt(0) || '家' }}</span>
              <span class="member-text">
                <em>当前成员</em>
                <strong>{{ currentMemberName || '请选择成员' }}</strong>
              </span>
              <el-icon class="el-icon--right"><ArrowDown :size="15" /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item
                  v-for="m in memberStore.members"
                  :key="m.id"
                  :command="m.id"
                  :disabled="m.id === memberStore.currentMemberId"
                >
                  {{ m.memberName }}（{{ relationText(m.relation) }}）
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="34" :src="userStore.userInfo?.avatar" />
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
              <el-icon class="el-icon--right"><ArrowDown :size="15" /></el-icon>
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

      <!-- 主要内容：路由切换做轻量位移动效 -->
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
import {
  Home, Wallet, ClipboardCheck, Sparkles, Baby, Settings, ArrowDown, Sprout
} from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { useMemberStore } from '@/stores/member'
import { logout } from '@/api/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const memberStore = useMemberStore()

const activeMenu = computed(() => route.path)

/** 窄屏收起侧栏为图标条，避免菜单挤压内容区 */
const isNarrow = ref(false)
const syncNarrow = () => {
  isNarrow.value = window.innerWidth < 992
}

const currentMemberName = computed(() => {
  const m = memberStore.members.find((x) => x.id === memberStore.currentMemberId)
  return m ? m.memberName : ''
})

const relationText = (relation?: string) => {
  const map: Record<string, string> = {
    OWNER: '户主',
    SPOUSE: '配偶',
    CHILD: '子女',
    PARENT: '父母',
    OTHER: '其他'
  }
  return relation ? map[relation] || relation : '其他'
}

/** 切换当前操作成员(刷新页面使各模块按新成员重新加载) */
const switchMember = async (memberId: string | number) => {
  const id = Number(memberId)
  memberStore.setCurrentMember(id)
  ElMessage.success('已切换操作成员')
  // 刷新当前页, 让数据按新成员重新加载
  window.location.reload()
}

const currentPageTitle = computed(() => {
  const titles: Record<string, string> = {
    '/dashboard': '仪表盘',
    '/workbench': '每日工作台',
    '/income': '收入管理',
    '/expense': '支出管理',
    '/fixed-expense': '固定支出',
    '/budget': '预算规划',
    '/life-plan': '美好生活计划',
    '/growth-plan': '儿童成长计划',
    '/reward': '奖励额度',
    '/reward-items': '活动方案',
    '/approval': '资金审批',
    '/family': '家庭管理',
    '/members': '成员管理',
    '/statistics': '统计报表',
    '/notifications': '提醒中心'
  }
  return titles[route.path] || '家庭成长系统'
})

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
  }
}

onMounted(() => {
  syncNarrow()
  window.addEventListener('resize', syncNarrow)
  // 刷新后恢复成员列表, 供顶栏切换当前操作成员
  memberStore.ensureLoaded().catch(() => {})
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', syncNarrow)
})
</script>

<style scoped>
.layout-container {
  height: 100%;
}

/* ---------------- 侧边栏 ---------------- */
.layout-aside {
  position: relative;
  display: flex;
  flex-direction: column;
  background: linear-gradient(178deg, #146536 0%, var(--grass-800) 58%, var(--grass-900) 100%);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width var(--dur-3) var(--ease-out);
}

/* 侧栏顶部的柔光，让深绿不至于死板 */
.layout-aside::before {
  content: '';
  position: absolute;
  inset: 0;
  pointer-events: none;
  background:
    radial-gradient(420px 180px at 20% -6%, rgba(163, 217, 63, 0.26), transparent 70%),
    radial-gradient(320px 160px at 100% 40%, rgba(95, 198, 115, 0.16), transparent 70%);
}

.logo-container {
  position: relative;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 68px;
  padding: 0 18px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.10);
  flex-shrink: 0;
}

.logo-container.narrow {
  justify-content: center;
  padding: 0;
}

.logo-mark {
  display: grid;
  place-items: center;
  width: 38px;
  height: 38px;
  border-radius: 13px;
  color: var(--grass-900);
  background: linear-gradient(135deg, var(--sprout), var(--grass-400));
  box-shadow: 0 6px 16px rgba(163, 217, 63, 0.34);
  flex-shrink: 0;
}

.logo-word {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
  animation: grass-slide-left var(--dur-3) var(--ease-out) both;
}

.logo-word strong {
  font-size: 16px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.6px;
}

.logo-word em {
  font-style: normal;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.62);
  letter-spacing: 0.4px;
}

.sidebar-menu {
  position: relative;
  border-right: none;
  background: transparent;
  padding: 10px 0 24px;
  flex: 1;
}

:deep(.el-menu) {
  background: transparent;
  border-right: none;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  height: 46px;
  line-height: 46px;
  margin: 3px 10px;
  padding: 0 12px !important;
  border-radius: 13px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 14px;
  transition: color var(--dur-1) linear, background-color var(--dur-2) var(--ease-out),
    transform var(--dur-2) var(--ease-out);
}

:deep(.el-menu-item svg),
:deep(.el-sub-menu__title svg) {
  flex-shrink: 0;
  opacity: 0.86;
  transition: transform var(--dur-2) var(--ease-spring), opacity var(--dur-1) linear;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  color: #ffffff;
  background: rgba(163, 217, 63, 0.16);
  transform: translateX(3px);
}

:deep(.el-menu-item:hover svg),
:deep(.el-sub-menu__title:hover svg) {
  transform: scale(1.14);
  opacity: 1;
}

:deep(.el-menu-item.is-active) {
  color: #ffffff;
  font-weight: 700;
  background: linear-gradient(120deg, var(--grass-500), #1f9450);
  box-shadow: 0 8px 18px rgba(15, 61, 34, 0.38);
}

:deep(.el-menu-item.is-active svg) {
  opacity: 1;
  color: var(--sprout-soft);
}

/* 激活项左侧的小芽指示条 */
:deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: -10px;
  top: 50%;
  width: 4px;
  height: 20px;
  margin-top: -10px;
  border-radius: 0 4px 4px 0;
  background: var(--sprout);
  animation: grass-grow var(--dur-3) var(--ease-spring) both;
  transform-origin: left center;
}

:deep(.el-sub-menu .el-menu) {
  background: rgba(8, 40, 22, 0.42);
  border-radius: 14px;
  margin: 2px 10px;
  padding: 4px 0;
}

:deep(.el-sub-menu .el-menu .el-menu-item) {
  height: 38px;
  line-height: 38px;
  margin: 2px 6px;
  font-size: 13px;
}

:deep(.el-sub-menu.is-opened > .el-sub-menu__title) {
  color: #ffffff;
}

:deep(.el-sub-menu__icon-arrow) {
  transition: transform var(--dur-2) var(--ease-out);
  color: rgba(255, 255, 255, 0.6);
}

/* 侧栏底部草叶剪影 */
.aside-grass {
  position: sticky;
  bottom: 0;
  height: 42px;
  margin-top: -42px;
  pointer-events: none;
  background:
    repeating-linear-gradient(78deg, rgba(163, 217, 63, 0.30) 0 2px, transparent 2px 9px),
    linear-gradient(180deg, transparent, rgba(8, 40, 22, 0.5));
  mask-image: linear-gradient(180deg, transparent, #000 55%);
  opacity: 0.55;
}

/* ---------------- 顶栏 ---------------- */
.layout-body {
  min-width: 0;
}

.layout-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 68px;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--border-soft);
  box-shadow: 0 2px 12px rgba(18, 60, 34, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.title-bar {
  width: 5px;
  height: 22px;
  border-radius: 5px;
  background: linear-gradient(180deg, var(--sprout), var(--grass-500));
  box-shadow: 0 0 0 3px rgba(95, 198, 115, 0.16);
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-strong);
  letter-spacing: 0.4px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.member-info,
.user-info {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 6px 12px 6px 6px;
  border-radius: var(--r-pill, 999px);
  border: 1px solid transparent;
  cursor: pointer;
  outline: none;
  transition: background-color var(--dur-2) var(--ease-out), border-color var(--dur-2) linear,
    transform var(--dur-1) var(--ease-out);
}

.member-info:hover,
.user-info:hover {
  background: var(--grass-50);
  border-color: var(--grass-200);
  transform: translateY(-1px);
}

.member-info:focus-visible,
.user-info:focus-visible {
  border-color: var(--grass-400);
  box-shadow: 0 0 0 3px rgba(95, 198, 115, 0.28);
}

.member-avatar {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  color: var(--grass-900);
  background: linear-gradient(135deg, var(--sprout-soft), var(--grass-200));
}

.member-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.member-text em {
  font-style: normal;
  font-size: 11px;
  color: var(--sun-ink);
}

.member-text strong {
  font-size: 13px;
  color: var(--text-strong);
}

.username {
  font-size: 14px;
  color: var(--text-body);
}

/* ---------------- 内容区 ---------------- */
.layout-main {
  padding: 22px;
  overflow-y: auto;
}

/* 路由切换：进场从下方轻推，离场快速淡出 */
.page-enter-active {
  transition: opacity var(--dur-2) var(--ease-out), transform var(--dur-3) var(--ease-out);
}

.page-leave-active {
  transition: opacity var(--dur-1) var(--ease-in), transform var(--dur-1) var(--ease-in);
}

.page-enter-from {
  opacity: 0;
  transform: translate3d(0, 14px, 0);
}

.page-leave-to {
  opacity: 0;
  transform: translate3d(0, -8px, 0);
}

@media (max-width: 768px) {
  .layout-main {
    padding: 14px;
  }

  .layout-header {
    padding: 0 14px;
  }

  .member-text em {
    display: none;
  }
}
</style>
