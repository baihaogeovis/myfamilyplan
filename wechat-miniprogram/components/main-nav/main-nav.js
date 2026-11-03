const items = [
  { key: 'work', label: '工作台', url: '/pages/index/index' },
  { key: 'family', label: '家庭', url: '/pages/members/members' },
  { key: 'plan', label: '计划', url: '/pages/lifePlan/lifePlan' },
  { key: 'money', label: '钱', url: '/pages/record/record' },
  { key: 'reward', label: '奖励', url: '/pages/reward/reward' },
  { key: 'me', label: '我的', url: '/pages/profile/profile' }
]

Component({
  properties: {
    current: { type: String, value: '' }
  },
  data: { items },
  methods: {
    go(e) {
      const key = e.currentTarget.dataset.key
      const url = e.currentTarget.dataset.url
      if (!url || key === this.data.current) return
      wx.reLaunch({ url })
    }
  }
})
