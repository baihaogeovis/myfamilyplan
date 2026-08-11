import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getMemberList, type FamilyMember } from '@/api/member'

export const useMemberStore = defineStore('member', () => {
  const members = ref<FamilyMember[]>([])
  const currentMemberId = ref<number | null>(null)

  const loadMembers = async (userId: number) => {
    const list = await getMemberList(userId)
    members.value = list || []
    if (members.value.length > 0) {
      currentMemberId.value = members.value[0].id as number
    } else {
      currentMemberId.value = null
    }
  }

  const setCurrentMember = (id: number) => {
    currentMemberId.value = id
  }

  return {
    members,
    currentMemberId,
    loadMembers,
    setCurrentMember
  }
})
