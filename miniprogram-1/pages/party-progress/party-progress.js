const { partyProgressData } = require('../../utils/mock-data')
const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

function formatDateTime(value) {
  if (!value) {
    return ''
  }
  if (typeof value === 'string') {
    return value.replace('T', ' ').slice(0, 16)
  }
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) {
      return ''
    }
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hour = String(date.getHours()).padStart(2, '0')
    const minute = String(date.getMinutes()).padStart(2, '0')
    return `${year}-${month}-${day} ${hour}:${minute}`
  } catch (e) {
    return ''
  }
}

Page({
  data: {
    profile: partyProgressData.profile,
    stages: [],
    currentStageName: '',
    currentStageCode: '',
    currentStepName: '',
    stageHistory: {
      startTime: '',
      endTime: '',
      remark: '',
    },
    guidances: [],
  },

  onLoad() {
    this.loadPartyProgress()
  },

  async loadPartyProgress() {
    try {
      await ensureLogin()

      const [profile, tracker] = await Promise.all([
        request({ url: '/api/student/profile' }),
        request({ url: '/api/party/me/tracker' }),
      ])

      const stageHistory = tracker.currentStageHistory || {}
      const guidances = (tracker.guidances || []).map((item) => ({
        title: item.title || '注意事项',
        content: item.content || '',
        priority: item.priority || 1,
        icon: (item.priority || 1) === 1 ? '📌' : '💡',
        materials: item.materials || [],
      }))

      this.setData({
        profile: {
          name: profile.realName || '演示用户',
          major: profile.major || '',
          className: profile.className || '',
        },
        stages: tracker.stages || [],
        currentStageName: tracker.currentStageName || '未知阶段',
        currentStageCode: tracker.currentStageCode || '',
        currentStepName: tracker.currentStepName || '',
        stageHistory: {
          startTime: formatDateTime(stageHistory.startTime) || '—',
          endTime: stageHistory.endTime ? formatDateTime(stageHistory.endTime) : '—',
          remark: stageHistory.remark || '—',
        },
        guidances,
      })
    } catch (error) {
      console.error('Load party progress failed:', error)
      wx.showToast({
        title: '已切换到本地流程数据',
        icon: 'none',
      })
    }
  },
})
