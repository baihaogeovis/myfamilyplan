<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="tip">
            当前操作成员：
            <el-tag v-if="memberStore.currentMemberId" type="success">
              {{ currentName }}
            </el-tag>
            <el-tag v-else type="warning">未选择</el-tag>
          </div>
          <el-button type="primary" :icon="Plus" @click="openAdd">新增成员</el-button>
        </div>
      </template>

      <el-table :data="memberStore.members" v-loading="loading" stripe>
        <el-table-column prop="memberName" label="姓名" width="120" />
        <el-table-column prop="relation" label="关系" width="120">
          <template #default="{ row }">
            {{ relationText(row.relation) }}
          </template>
        </el-table-column>
        <el-table-column prop="gender" label="性别" width="80" />
        <el-table-column prop="birthday" label="生日" width="140" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button
              link
              type="primary"
              :disabled="memberStore.currentMemberId === row.id"
              @click="setCurrent(row)"
            >
              设为当前
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

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
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addMember, updateMember, deleteMember, type FamilyMember } from '@/api/member'
import { useMemberStore } from '@/stores/member'

const memberStore = useMemberStore()
const loading = ref(false)

const currentName = computed(() => {
  const m = memberStore.members.find((x) => x.id === memberStore.currentMemberId)
  return m ? m.memberName : ''
})

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
  max-width: 1600px;
  margin: 0 auto;
}
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}
</style>
