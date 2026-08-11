<template>
  <div class="page-container">
    <el-row :gutter="16">
      <!-- 家庭信息 -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>家庭信息</span>
              <el-button link type="primary" :icon="Edit" @click="openEditInfo">编辑</el-button>
            </div>
          </template>
          <div v-loading="infoLoading" class="family-info">
            <el-avatar :size="64" :src="info.avatar" class="family-avatar">
              {{ (info.familyName || '家').charAt(0) }}
            </el-avatar>
            <el-descriptions :column="1" class="info-desc">
              <el-descriptions-item label="家庭名称">{{ info.familyName }}</el-descriptions-item>
              <el-descriptions-item label="登录账号">{{ info.username }}</el-descriptions-item>
              <el-descriptions-item label="备注">{{ info.remark || '暂无' }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 修改密码 -->
      <el-col :xs="24" :md="12">
        <el-card>
          <template #header>修改登录密码</template>
          <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="不少于6位" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="再次输入新密码" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdLoading" @click="submitPassword">保存密码</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>

    <!-- 家庭成员 -->
    <el-card class="member-card">
      <template #header>
        <div class="card-header">
          <span>家庭成员</span>
          <div>
            <el-button :icon="Plus" type="primary" @click="openAdd">新增成员</el-button>
            <el-button link type="primary" @click="router.push('/members')">前往成员管理</el-button>
          </div>
        </div>
      </template>

      <el-table :data="memberStore.members" v-loading="memberLoading" stripe>
        <el-table-column prop="memberName" label="姓名" width="140" />
        <el-table-column prop="relation" label="关系" width="120">
          <template #default="{ row }">{{ relationText(row.relation) }}</template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="birthday" label="生日" width="140" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="memberStore.currentMemberId === row.id"
              @click="setCurrent(row)"
            >
              设为当前
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑家庭信息 -->
    <el-dialog v-model="infoDialogVisible" title="编辑家庭信息" width="480px">
      <el-form :model="infoForm" label-width="80px">
        <el-form-item label="家庭名称">
          <el-input v-model="infoForm.familyName" placeholder="请输入家庭名称" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="infoForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="infoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInfo">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增成员 -->
    <el-dialog v-model="memberDialogVisible" title="新增成员" width="480px">
      <el-form :model="memberForm" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="memberForm.memberName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="memberForm.relation" placeholder="请选择" style="width: 100%">
            <el-option label="本人" value="OWNER" />
            <el-option label="配偶" value="SPOUSE" />
            <el-option label="子女" value="CHILD" />
            <el-option label="父母" value="PARENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="memberForm.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="memberForm.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="memberForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMember">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Edit } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getFamilyInfo, updateFamilyInfo, changePassword, type FamilyInfo } from '@/api/family'
import { addMember, deleteMember, type FamilyMember } from '@/api/member'
import { useMemberStore } from '@/stores/member'

const router = useRouter()
const memberStore = useMemberStore()

/** 家庭信息 */
const info = reactive<FamilyInfo>({ familyId: 0, familyName: '', username: '', avatar: '', remark: '' })
const infoLoading = ref(false)

const loadInfo = async () => {
  infoLoading.value = true
  try {
    const data = await getFamilyInfo()
    Object.assign(info, data || {})
  } finally {
    infoLoading.value = false
  }
}

/** 编辑家庭信息 */
const infoDialogVisible = ref(false)
const infoForm = reactive({ familyName: '', remark: '' })

const openEditInfo = () => {
  infoForm.familyName = info.familyName || ''
  infoForm.remark = info.remark || ''
  infoDialogVisible.value = true
}

const submitInfo = async () => {
  if (!infoForm.familyName.trim()) {
    ElMessage.warning('家庭名称不能为空')
    return
  }
  try {
    await updateFamilyInfo({ familyName: infoForm.familyName, remark: infoForm.remark })
    ElMessage.success('家庭信息已更新')
    infoDialogVisible.value = false
    await loadInfo()
  } catch {
    // 拦截器已提示
  }
}

/** 修改密码 */
const pwdFormRef = ref<FormInstance>()
const pwdLoading = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: string, callback: (e?: Error) => void) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const submitPassword = async () => {
  if (!pwdFormRef.value) return
  await pwdFormRef.value.validate(async (valid) => {
    if (!valid) return
    pwdLoading.value = true
    try {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      ElMessage.success('密码修改成功, 请重新登录')
      Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
      // 修改密码后强制重新登录
      localStorage.removeItem('token')
      router.push('/login')
    } catch {
      // 拦截器已提示
    } finally {
      pwdLoading.value = false
    }
  })
}

/** 成员管理 */
const memberLoading = ref(false)
const memberDialogVisible = ref(false)
const memberForm = reactive<FamilyMember>({
  memberName: '',
  relation: '',
  gender: '',
  birthday: '',
  remark: ''
})

const relationText = (val?: string) => {
  const map: Record<string, string> = {
    OWNER: '户主',
    SPOUSE: '配偶',
    CHILD: '子女',
    PARENT: '父母',
    OTHER: '其他'
  }
  return val ? map[val] || val : ''
}

const loadMembers = async () => {
  memberLoading.value = true
  try {
    await memberStore.loadMembers()
  } finally {
    memberLoading.value = false
  }
}

const openAdd = () => {
  Object.assign(memberForm, { memberName: '', relation: '', gender: '', birthday: '', remark: '' })
  memberDialogVisible.value = true
}

const submitMember = async () => {
  if (!memberForm.memberName.trim()) {
    ElMessage.warning('请输入姓名')
    return
  }
  try {
    await addMember(memberForm)
    ElMessage.success('添加成功')
    memberDialogVisible.value = false
    await loadMembers()
  } catch {
    // 拦截器已提示
  }
}

const setCurrent = (row: FamilyMember) => {
  memberStore.setCurrentMember(row.id as number)
  ElMessage.success(`已切换至「${row.memberName}」`)
}

const remove = async (row: FamilyMember) => {
  try {
    await ElMessageBox.confirm(`确定删除成员「${row.memberName}」?`, '提示', { type: 'warning' })
    await deleteMember(row.id as number)
    ElMessage.success('删除成功')
    await loadMembers()
  } catch {
    // 取消
  }
}

onMounted(() => {
  loadInfo()
  loadMembers()
})
</script>

<style scoped>
.page-container {
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.family-info {
  display: flex;
  align-items: center;
  gap: 24px;
}
.family-avatar {
  flex-shrink: 0;
  font-size: 24px;
  background: linear-gradient(135deg, #667eea, #764ba2);
}
.info-desc {
  flex: 1;
}
.member-card {
  margin-top: 16px;
}
</style>
