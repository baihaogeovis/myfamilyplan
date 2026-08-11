<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="header-bar">
          <div class="filters">
            <el-radio-group v-model="filterRead" @change="load">
              <el-radio-button :value="'all'">全部</el-radio-button>
              <el-radio-button :value="'0'">未读</el-radio-button>
              <el-radio-button :value="'1'">已读</el-radio-button>
            </el-radio-group>
          </div>
          <div>
            <el-badge :value="unread" :hidden="unread === 0" :max="99" class="badge">
              <el-tag type="info">未读 {{ unread }}</el-tag>
            </el-badge>
            <el-button
              type="primary"
              :icon="CheckCheck"
              :disabled="unread === 0"
              style="margin-left: 12px"
              @click="markAll"
            >
              全部已读
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-if="!loading && tableData.length === 0" description="暂无提醒" />
      <el-table v-else :data="tableData" v-loading="loading" stripe>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.isRead === 1 ? 'info' : 'danger'" size="small">
              {{ row.isRead === 1 ? '已读' : '未读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.type" size="small" effect="plain">{{ row.type }}</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button
              v-if="row.isRead !== 1"
              link
              type="primary"
              @click="markOne(row)"
            >
              标为已读
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        class="pager"
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @current-change="load"
        @size-change="load"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { CheckCheck } from 'lucide-vue-next'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import {
  getNotificationList,
  markRead,
  markAllRead,
  deleteNotification,
  getUnreadCount,
  type Notification
} from '@/api/notification'

const userStore = useUserStore()
const userId = computed(() => userStore.userInfo?.userId)

const loading = ref(false)
const tableData = ref<Notification[]>([])
const total = ref(0)
const unread = ref(0)
const page = ref(1)
const size = ref(10)
const filterRead = ref<string>('all')

const load = async () => {
  if (!userId.value) return
  loading.value = true
  try {
    const isRead = filterRead.value === 'all' ? undefined : Number(filterRead.value)
    const res = await getNotificationList({
      userId: userId.value,
      page: page.value,
      size: size.value,
      isRead
    })
    tableData.value = res.records
    total.value = res.total
    await refreshUnread()
  } finally {
    loading.value = false
  }
}

const refreshUnread = async () => {
  if (!userId.value) return
  try {
    const data = await getUnreadCount(userId.value)
    unread.value = data.count || 0
  } catch {
    // ignore
  }
}

const markOne = async (row: Notification) => {
  try {
    await markRead(row.id as number)
    ElMessage.success('已标记为已读')
    load()
  } catch {
    // ignore
  }
}

const markAll = async () => {
  if (!userId.value) return
  try {
    await ElMessageBox.confirm('确定将所有提醒标记为已读?', '提示', { type: 'warning' })
    await markAllRead(userId.value)
    ElMessage.success('已全部标记为已读')
    load()
  } catch {
    // 取消
  }
}

const remove = async (row: Notification) => {
  try {
    await ElMessageBox.confirm('确定删除该提醒?', '提示', { type: 'warning' })
    await deleteNotification(row.id as number)
    ElMessage.success('删除成功')
    load()
  } catch {
    // 取消
  }
}

onMounted(load)
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
.badge {
  margin-right: 4px;
}
.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
