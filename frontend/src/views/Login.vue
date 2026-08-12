<template>
  <div class="login-container">
    <div class="login-card">
      <!-- 第一步：家庭账号登录 -->
      <template v-if="step === 'account'">
        <div class="login-header">
          <h1 class="login-title">家庭成长系统</h1>
          <p class="login-subtitle">智能管理家庭财务,规划美好未来</p>
        </div>

        <el-form ref="formRef" :model="loginForm" :rules="rules" class="login-form">
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              placeholder="请输入家庭账号"
              size="large"
              :prefix-icon="User"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleLogin"
            >
              下一步
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-tips">
          <p>默认家庭账号: admin / admin123</p>
        </div>

        <div class="login-register">
          没有家庭账号？
          <el-link type="primary" :underline="false" @click="step = 'register'">立即注册新家庭</el-link>
        </div>
      </template>

      <!-- 第二步：选择当前操作成员(登录时校验该成员属于该家庭) -->
      <template v-else-if="step === 'member'">
        <div class="login-header">
          <h1 class="login-title">{{ pendingFamilyName }}</h1>
          <p class="login-subtitle">请选择当前操作成员</p>
        </div>

        <div class="member-list">
          <div
            v-for="m in pendingMembers"
            :key="m.id"
            class="member-item"
            :class="{ active: selectedMemberId === m.id }"
            @click="selectedMemberId = m.id as number"
          >
            <div class="member-avatar">{{ (m.memberName || '?').charAt(0) }}</div>
            <div class="member-info">
              <div class="member-name">{{ m.memberName }}</div>
              <div class="member-relation">{{ relationText(m.relation) }}</div>
            </div>
          </div>
          <el-empty v-if="pendingMembers.length === 0" description="该家庭下暂无成员" />
        </div>

        <div class="member-actions">
          <el-button size="large" class="login-button" @click="step = 'account'">
            返回上一步
          </el-button>
          <el-button
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            :disabled="!selectedMemberId"
            @click="handleMemberLogin"
          >
            进入系统
          </el-button>
        </div>
      </template>

      <!-- 第三步：注册新家庭 -->
      <template v-else-if="step === 'register'">
        <div class="login-header">
          <h1 class="login-title">注册新家庭</h1>
          <p class="login-subtitle">创建家庭账号, 一键添加家人</p>
        </div>

        <el-form ref="regFormRef" :model="regForm" :rules="regRules" class="login-form">
          <el-form-item prop="familyName">
            <el-input v-model="regForm.familyName" placeholder="家庭名称, 如: 幸福之家" size="large" :prefix-icon="Home" />
          </el-form-item>
          <el-form-item prop="username">
            <el-input v-model="regForm.username" placeholder="登录账号(不少于3位)" size="large" :prefix-icon="User" />
          </el-form-item>
          <el-form-item prop="ownerName">
            <el-input v-model="regForm.ownerName" placeholder="户主姓名(可选)" size="large" :prefix-icon="UserRound" />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model="regForm.password"
              type="password"
              placeholder="密码(不少于6位)"
              size="large"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          <el-form-item prop="confirmPassword">
            <el-input
              v-model="regForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-button"
              :loading="loading"
              @click="handleRegister"
            >
              注册并登录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="login-register">
          已有家庭账号？
          <el-link type="primary" :underline="false" @click="step = 'account'">返回登录</el-link>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Home, UserRound } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { login, type LoginParams } from '@/api/auth'
import { registerFamily, type RegisterParams } from '@/api/family'
import type { FamilyMember } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { useMemberStore } from '@/stores/member'

const router = useRouter()
const userStore = useUserStore()
const memberStore = useMemberStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

/** 当前步骤: account-家庭账号, member-选择成员, register-注册新家庭 */
const step = ref<'account' | 'member' | 'register'>('account')

/** 首次登录成功后返回的家庭信息(供成员选择) */
const pendingFamilyName = ref('')
const pendingMembers = ref<FamilyMember[]>([])
const selectedMemberId = ref<number | null>(null)

const loginForm = reactive({
  username: 'admin',
  password: 'admin123'
})

const rules = {
  username: [
    { required: true, message: '请输入家庭账号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const relationText = (relation?: string) => {
  const map: Record<string, string> = {
    OWNER: '户主',
    SPOUSE: '配偶',
    CHILD: '子女',
    PARENT: '父母',
    OTHER: '其他'
  }
  return relation ? (map[relation] || relation) : '其他'
}

/**
 * 第一步: 校验家庭账号密码, 返回家庭成员列表
 */
const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await login(loginForm)
      pendingFamilyName.value = res.familyName || loginForm.username
      pendingMembers.value = res.members || []
      selectedMemberId.value = null

      if (pendingMembers.value.length === 1) {
        // 仅一个成员: 直接以其身份登录(校验其属于该家庭)
        await doMemberLogin(pendingMembers.value[0].id as number)
      } else if (pendingMembers.value.length > 1) {
        step.value = 'member'
      } else {
        ElMessage.warning('该家庭下暂无成员, 请联系管理员添加')
      }
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

/**
 * 第二步: 选择成员后登录, 后端校验该成员属于该家庭
 */
const handleMemberLogin = async () => {
  if (!selectedMemberId.value) {
    ElMessage.warning('请选择当前操作成员')
    return
  }
  await doMemberLogin(selectedMemberId.value)
}

const doMemberLogin = async (memberId: number) => {
  loading.value = true
  try {
    const params: LoginParams = {
      username: loginForm.username,
      password: loginForm.password,
      memberId
    }
    const res = await login(params)
    // 校验通过才写入登录态
    userStore.setToken(res.token)
    userStore.setLoginInfo({
      familyId: res.familyId,
      familyName: res.familyName,
      username: res.username,
      avatar: res.avatar
    })
    memberStore.setCurrentMember(memberId)
    // 后台加载该家庭成员列表
    memberStore.loadMembers().catch(() => {})
    ElMessage.success(`欢迎, ${res.currentMemberName || ''}`)
    router.push('/')
  } catch (error) {
    console.error('选择成员登录失败:', error)
  } finally {
    loading.value = false
  }
}

/**
 * 注册新家庭: 注册成功自动创建户主成员并直接登录
 */
const regFormRef = ref<FormInstance>()
const regForm = reactive({
  familyName: '',
  username: '',
  ownerName: '',
  password: '',
  confirmPassword: ''
})

const regRules = {
  familyName: [{ required: true, message: '请输入家庭名称', trigger: 'blur' }],
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { min: 3, message: '账号长度不能少于3位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (value !== regForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const handleRegister = async () => {
  if (!regFormRef.value) return

  await regFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const params: RegisterParams = {
        familyName: regForm.familyName,
        username: regForm.username,
        password: regForm.password,
        ownerName: regForm.ownerName || undefined
      }
      const res = await registerFamily(params)
      // 注册即登录
      userStore.setToken(res.token)
      userStore.setLoginInfo({
        familyId: res.familyId,
        familyName: res.familyName,
        username: res.username,
        avatar: res.avatar
      })
      if (res.currentMemberId) {
        memberStore.setCurrentMember(res.currentMemberId)
      }
      memberStore.loadMembers().catch(() => {})
      ElMessage.success(`家庭「${res.familyName}」创建成功`)
      router.push('/')
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  })
}
</script>

<style scoped>
.login-container {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  width: 600px;
  height: 600px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  top: -200px;
  right: -100px;
}

.login-container::after {
  content: '';
  position: absolute;
  width: 400px;
  height: 400px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 50%;
  bottom: -150px;
  left: -100px;
}

.login-card {
  width: 420px;
  padding: 40px;
  background: white;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 8px 0;
}

.login-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.login-form {
  margin-top: 30px;
}

.login-button {
  width: 100%;
  height: 44px;
  font-size: 16px;
  border-radius: 8px;
}

.login-tips {
  margin-top: 20px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 10px;
  text-align: center;
  border: 1px solid #f1f5f9;
}

.login-tips p {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}

.login-register {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.member-list {
  margin-top: 10px;
  max-height: 320px;
  overflow-y: auto;
}

.member-item {
  display: flex;
  align-items: center;
  padding: 14px 18px;
  margin-bottom: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.member-item:hover {
  border-color: #667eea;
  background: #f8faff;
  transform: translateX(4px);
}

.member-item.active {
  border-color: #667eea;
  background: #eef2ff;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2);
}

.member-avatar {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  flex-shrink: 0;
}

.member-info {
  flex: 1;
}

.member-name {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
}

.member-relation {
  font-size: 13px;
  color: #909399;
  margin-top: 3px;
}

.member-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
}

.member-actions .login-button {
  flex: 1;
}
</style>
