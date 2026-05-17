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
    profile: {
      name: '',
      major: '',
      className: '',
    },
    stages: [],
    currentStageName: '',
    currentStageCode: '',
    currentStepName: '',
    studentNo: '',
    loadError: '',
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
      this.setData({ loadError: '' })
      await ensureLogin()

      const [me, profile, tracker] = await Promise.all([
        request({ url: '/api/auth/me' }),
        request({ url: '/api/student/profile' }),
        request({ url: '/api/party/me/progress' }),
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
          name: me.realName || profile.realName || '未命名',
          major: profile.major || '',
          className: me.className || profile.className || '',
        },
        studentNo: me.studentNo || '',
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
      this.setData({
        loadError: error && error.message ? error.message : '请求失败',
      })
      wx.showToast({ title: this.data.loadError, icon: 'none' })
    }
  },
})
