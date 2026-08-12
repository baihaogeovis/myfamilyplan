<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside width="220px" class="layout-aside">
      <div class="logo-container">
        <h1 class="logo-text">家庭成长系统</h1>
      </div>
      
      <el-menu
        :default-active="activeMenu"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <Home />
          <span>仪表盘</span>
        </el-menu-item>

        <el-menu-item index="/workbench">
          <ClipboardCheck />
          <span>每日工作台</span>
        </el-menu-item>

        <el-sub-menu index="finance">
          <template #title>
            <Wallet />
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
            <Sparkles />
            <span>美好生活</span>
          </template>
          <el-menu-item index="/life-plan">生活计划</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="growth">
          <template #title>
            <Baby />
            <span>儿童成长</span>
          </template>
          <el-menu-item index="/growth-plan">成长计划</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system">
          <template #title>
            <Settings />
            <span>系统管理</span>
          </template>
          <el-menu-item index="/family">家庭管理</el-menu-item>
          <el-menu-item index="/approval">资金审批</el-menu-item>
          <el-menu-item index="/members">成员管理</el-menu-item>
          <el-menu-item index="/notifications">提醒中心</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <h2 class="page-title">{{ currentPageTitle }}</h2>
        </div>
        
        <div class="header-right">
          <!-- 当前操作成员切换 -->
          <el-dropdown v-if="memberStore.members.length > 0" @command="switchMember">
            <span class="member-info">
              <el-tag effect="plain" type="warning">当前成员</el-tag>
              <span class="username">{{ currentMemberName || '请选择成员' }}</span>
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
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
              <el-avatar :size="32" :src="userStore.userInfo?.avatar" />
              <span class="username">{{ userStore.userInfo?.nickname }}</span>
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

      <!-- 主要内容 -->
      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  Home, Wallet, ClipboardCheck, Sparkles, Baby, Settings, ArrowDown
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
  // 刷新后恢复成员列表, 供顶栏切换当前操作成员
  memberStore.ensureLoaded().catch(() => {})
})
</script>

<style scoped>
.layout-container {
  height: 100%;
}

.layout-aside {
  background: linear-gradient(180deg, #001529 0%, #002140 100%);
  overflow-y: auto;
}

.logo-container {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.logo-text {
  color: white;
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  letter-spacing: 1px;
}

.sidebar-menu {
  border-right: none;
  background: transparent;
}

:deep(.el-menu-item),
:deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.65);
  height: 48px;
  line-height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  color: white;
  background: rgba(24, 144, 255, 0.6) !important;
}

:deep(.el-menu-item.is-active) {
  color: white;
  background: #1890ff !important;
}

:deep(.el-sub-menu .el-menu) {
  background: rgba(0, 0, 0, 0.25);
}

:deep(.el-sub-menu .el-menu .el-menu-item) {
  margin: 2px 8px;
  border-radius: 6px;
  height: 40px;
  line-height: 40px;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  padding: 0 24px;
  height: 64px;
}

.page-title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #606266;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.member-info:hover {
  background: #f5f7fa;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.user-info:hover {
  background: #f5f7fa;
}

.username {
  font-size: 14px;
  color: #606266;
}

.layout-main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
