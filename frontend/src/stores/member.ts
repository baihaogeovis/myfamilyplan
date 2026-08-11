import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMemberList, type FamilyMember } from '@/api/member'

const STORAGE_KEY = 'currentMemberId'

export const useMemberStore = defineStore('member', () => {
  const members = ref<FamilyMember[]>([])
  const currentMemberId = ref<number | null>(Number(localStorage.getItem(STORAGE_KEY)) || null)

  const loadMembers = async () => {
    const list = await getMemberList()
    members.value = list || []
    if (members.value.length > 0) {
      // 优先恢复上次选中的成员；已被删除则回退到第一个
      const saved = currentMemberId.value
      if (saved && members.value.some((m) => m.id === saved)) {
        currentMemberId.value = saved
      } else {
        currentMemberId.value = members.value[0].id as number
      }
    } else {
      currentMemberId.value = null
    }
  }

  // 确保成员列表已加载（页面刷新后自动恢复），已加载则不做重复请求
  const ensureLoaded = async () => {
    if (members.value.length > 0) return
    await loadMembers()
  }

  const setCurrentMember = (id: number) => {
    currentMemberId.value = id
    if (id) {
      localStorage.setItem(STORAGE_KEY, String(id))
    } else {
      localStorage.removeItem(STORAGE_KEY)
    }
  }

  return {
    members,
    currentMemberId,
    loadMembers,
    ensureLoaded,
    setCurrentMember
  }
})
