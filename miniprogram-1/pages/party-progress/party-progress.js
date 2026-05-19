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

      const [meResult, profileResult, trackerResult] = await Promise.all([
        request({ url: '/api/auth/me' })
          .then((data) => ({ ok: true, data }))
          .catch((error) => ({ ok: false, error })),
        request({ url: '/api/student/profile' })
          .then((data) => ({ ok: true, data }))
          .catch((error) => ({ ok: false, error })),
        request({ url: '/api/party/me/progress' })
          .then((data) => ({ ok: true, data }))
          .catch((error) => ({ ok: false, error })),
      ])

      if (!trackerResult.ok) {
        throw trackerResult.error || new Error('加载入党进度失败')
      }

      const me = meResult.ok ? (meResult.data || {}) : {}
      const profile = profileResult.ok ? (profileResult.data || {}) : {}
      const tracker = trackerResult.data || {}

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

      if (!profileResult.ok) {
        console.warn('Load student profile failed:', profileResult.error)
      }
      if (!meResult.ok) {
        console.warn('Load auth profile failed:', meResult.error)
      }
    } catch (error) {
      console.error('Load party progress failed:', error)
      const message = error && error.message ? error.message : '请求失败'
      this.setData({
        loadError: message,
      })
      wx.showToast({ title: message, icon: 'none' })
    }
  },
})
