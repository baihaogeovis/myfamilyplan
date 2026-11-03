<template>
  <div class="page-container">
    <div class="family-head">
      <p>点一个人，今天就以这位家人的身份来看。</p>
      <div class="family-actions">
        <el-button round @click="router.push('/family')">家庭账号</el-button>
        <el-button type="primary" round :icon="Plus" @click="openAdd">添加家人</el-button>
      </div>
    </div>

    <div class="people" v-loading="loading">
      <article
        v-for="m in memberStore.members"
        :key="m.id"
        class="person"
        :class="{ on: m.id === memberStore.currentMemberId }"
      >
        <span class="avatar" :style="toneStyle(m.id)">{{ (m.memberName || '?').charAt(0) }}</span>
        <div class="who">
          <strong>{{ m.memberName }}</strong>
          <em>{{ householdRole(m.relation) }} · {{ relationText(m.relation) }}</em>
          <span v-if="m.birthday || m.remark">{{ [m.birthday, m.remark].filter(Boolean).join(' · ') }}</span>
        </div>
        <div class="ops">
          <el-tag v-if="m.id === memberStore.currentMemberId" round effect="plain">正在用</el-tag>
          <el-button v-else round @click="setCurrent(m)">设为当前</el-button>
          <el-button link type="primary" @click="openEdit(m)">编辑</el-button>
          <el-button link type="danger" @click="remove(m)">删除</el-button>
        </div>
      </article>
      <el-empty v-if="!loading && memberStore.members.length === 0" description="还没有家人" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑成员' : '新增成员'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="姓名">
          <el-input v-model="form.memberName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="关系">
          <el-select v-model="form.relation" placeholder="请选择" style="width: 100%">
            <el-option label="本人" value="OWNER" />
            <el-option label="配偶" value="SPOUSE" />
            <el-option label="子女" value="CHILD" />
            <el-option label="父母" value="PARENT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="性别">
          <el-radio-group v-model="form.gender">
            <el-radio value="男">男</el-radio>
            <el-radio value="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="生日">
          <el-date-picker v-model="form.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addMember, updateMember, deleteMember, type FamilyMember } from '@/api/member'
import { useMemberStore } from '@/stores/member'
import { householdRole, memberTone } from '@/utils/family'

const router = useRouter()
const memberStore = useMemberStore()
const loading = ref(false)

const toneStyle = (id?: number) => {
  const tone = memberTone(id)
  return { background: tone.bg, color: tone.ink }
}

const relationText = (val?: string) => {
  const map: Record<string, string> = {
    OWNER: '本人',
    SPOUSE: '配偶',
    CHILD: '子女',
    PARENT: '父母',
    OTHER: '其他'
  }
  return val ? map[val] || val : ''
}

const dialogVisible = ref(false)
const form = reactive<FamilyMember>({
  memberName: '',
  relation: '',
  gender: '',
  birthday: '',
  remark: ''
})

const load = async () => {
  loading.value = true
  try {
    await memberStore.loadMembers()
  } finally {
    loading.value = false
  }
}

const setCurrent = (row: FamilyMember) => {
  memberStore.setCurrentMember(row.id as number)
  ElMessage.success(`已切换至「${row.memberName}」`)
}

const openAdd = () => {
  Object.assign(form, {
    id: undefined,
    memberName: '',
    relation: '',
    gender: '',
    birthday: '',
    remark: ''
  })
  dialogVisible.value = true
}

const openEdit = (row: FamilyMember) => {
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submit = async () => {
  try {
    if (form.id) {
      await updateMember(form)
      ElMessage.success('更新成功')
    } else {
      await addMember(form)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    await load()
  } catch {
    // 错误已在拦截器提示
  }
}

const remove = async (row: FamilyMember) => {
  try {
    await ElMessageBox.confirm('确定删除该成员?', '提示', { type: 'warning' })
    await deleteMember(row.id as number)
    ElMessage.success('删除成功')
    await load()
  } catch {
    // 取消
  }
}

onMounted(() => {
  if (memberStore.members.length === 0) load()
})
</script>

<style scoped>
.page-container {
  width: 100%;
  max-width: 880px;
  margin: 0 auto;
}

.family-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
}

.family-head p {
  margin: 0;
  color: var(--text-body);
}

.family-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.people {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.person {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 18px 18px;
  border-radius: 22px;
  background: var(--surface);
  border: 1px solid var(--border-soft);
  box-shadow: var(--shadow-1);
}

.person.on {
  border-color: var(--grass-300);
}

.avatar {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 18px;
  font-size: 20px;
  font-weight: 700;
  flex-shrink: 0;
}

.who {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  flex: 1;
}

.who strong {
  font-size: 18px;
  color: var(--text-strong);
}

.who em,
.who span {
  font-style: normal;
  font-size: 13px;
  color: var(--text-muted);
}

.ops {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

@media (max-width: 720px) {
  .family-head,
  .person {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
