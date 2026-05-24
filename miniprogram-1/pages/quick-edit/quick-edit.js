const { request } = require('../../utils/request')

const DEFAULT_CODES = ['partyProgress', 'partyReport', 'partyActivity']

const ALL_SERVICES = [
  { code: 'partyProgress', name: '入党流程追踪', icon: '🧭', path: '/pages/party-progress/party-progress' },
  { code: 'partyReport', name: '思想汇报提交', icon: '📝', path: '/pages/party-report/party-report' },
  { code: 'partyActivity', name: '党团活动申请', icon: '🗓️', path: '/pages/party-activity/party-activity' },
  { code: 'certificate', name: '电子证明生成', icon: '🪪', path: '/pages/e-certificate/e-certificate' },
  { code: 'leave', name: '请假审批流程', icon: '📝', path: '/pages/leave-list/leave-list' },
  { code: 'policyKnowledge', name: '政策知识库', icon: '📖', path: '/pages/knowledge/knowledge' },
  { code: 'studyAnalysis', name: '学业分析与预警', icon: '📈', path: '/pages/study-analysis/study-analysis' },
  { code: 'portrait', name: '学生画像', icon: '🧑‍🎓', path: '/pages/student-portrait/student-portrait' },
]

Page({
  data: {
    allServices: [],
    selectedCodes: [],
    saving: false,
  },

  onLoad() {
    this.loadData()
  },

  async loadData() {
    try {
      let selectedCodes = []
      try {
        const homeData = await request({ url: '/api/home' })
        selectedCodes = (homeData.quickEntries || []).map((e) => e.code).filter(Boolean)
      } catch (e) {
        const store = wx.getStorageSync('quick_entry_codes')
        if (store) selectedCodes = JSON.parse(store)
      }
      if (!selectedCodes.length) selectedCodes = DEFAULT_CODES
      const allServices = ALL_SERVICES.map((s) => ({
        ...s,
        selected: selectedCodes.includes(s.code),
      }))
      this.setData({ allServices, selectedCodes })
    } catch (error) {
      const allServices = ALL_SERVICES.map((s) => ({ ...s, selected: DEFAULT_CODES.includes(s.code) }))
      this.setData({ allServices, selectedCodes: DEFAULT_CODES })
    }
  },

  onToggleTap(event) {
    const { code } = event.currentTarget.dataset
    const allServices = this.data.allServices.map((s) => {
      if (s.code === code) s.selected = !s.selected
      return s
    })
    const selectedCodes = allServices.filter((s) => s.selected).map((s) => s.code)
    this.setData({ allServices, selectedCodes })
  },

  async onSaveTap() {
    if (this.data.saving) return
    this.setData({ saving: true })
    try {
      const selected = this.data.allServices.filter((s) => s.selected)
      await request({
        url: '/api/home/quick-entries',
        method: 'POST',
        data: selected.map((s) => ({
          code: s.code,
          name: s.name,
          icon: s.icon,
          path: s.path,
        })),
      })
      wx.setStorageSync('quick_entry_codes', JSON.stringify(this.data.selectedCodes))
      wx.showToast({ title: '保存成功', icon: 'success' })
      wx.navigateBack()
    } catch (error) {
      wx.setStorageSync('quick_entry_codes', JSON.stringify(this.data.selectedCodes))
      wx.showToast({ title: '已保存到本地', icon: 'success' })
      wx.navigateBack()
    } finally {
      this.setData({ saving: false })
    }
  },
})
