const { homeData } = require('../../utils/mock-data')
const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

const SERVICE_INFO = {
  partyProgress: { name: '入党流程追踪', icon: '🧭', path: '/pages/party-progress/party-progress' },
  partyReport: { name: '思想汇报提交', icon: '📝', path: '/pages/party-report/party-report' },
  partyActivity: { name: '党团活动申请', icon: '🗓️', path: '/pages/party-activity/party-activity' },
  certificate: { name: '电子证明生成', icon: '🪪', path: '/pages/e-certificate/e-certificate' },
  leave: { name: '请假审批流程', icon: '📝', path: '/pages/leave-list/leave-list' },
  policyKnowledge: { name: '政策知识库', icon: '📖', path: '/pages/knowledge/knowledge' },
  studyAnalysis: { name: '学业分析与预警', icon: '📈', path: '/pages/study-analysis/study-analysis' },
  portrait: { name: '学生画像', icon: '🧑‍🎓', path: '/pages/student-portrait/student-portrait' },
}

const DEFAULT_QUICK_CODES = ['partyProgress', 'partyReport', 'partyActivity']

Page({
  data: {
    banners: [
      { id: 0, title: '欢迎使用学院服务平台', subtitle: '便捷获取政策信息与党团服务', targetType: 'none' },
      { id: 1, title: '知识库全新上线', subtitle: '查询政策说明、办事指南与模板材料', targetType: 'none' },
      { id: 2, title: '党团事务一站式办理', subtitle: '入党流程追踪、思想汇报在线提交', targetType: 'none' },
    ],
    currentSwiperIndex: 0,
    quickEntries: DEFAULT_QUICK_CODES.map((c) => ({ code: c, ...SERVICE_INFO[c] })),
    allServiceEntries: [],
    todoStats: homeData.todoStats,
    latestNotices: homeData.latestNotices,
    downloads: homeData.downloads,
    loading: false,
    searchKeyword: '',
    navBarHeight: 56,
    navTitleTop: 26,
  },

  onLoad() {
    this.initNavMetrics()
    this.loadFromStorage()
    this.loadHomeData()
  },

  onShow() {
    if (this._loadedOnce) {
      this.loadFromStorage()
    }
  },

  initNavMetrics() {
    const info = wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()
    let menu = null
    try {
      menu = wx.getMenuButtonBoundingClientRect ? wx.getMenuButtonBoundingClientRect() : null
    } catch (e) {}

    const statusBarHeight = info.statusBarHeight || 0
    const navBarHeight = menu && menu.bottom
      ? menu.bottom + 6
      : statusBarHeight + 48
    const navTitleTop = menu && menu.top
      ? Math.max(statusBarHeight + 2, menu.top - 6)
      : statusBarHeight + 8

    this.setData({ navBarHeight, navTitleTop })
  },

  loadFromStorage() {
    const raw = wx.getStorageSync('quick_entry_codes')
    if (raw) {
      try {
        const list = JSON.parse(raw)
        if (list.length) {
          this.setData({ quickEntries: this.buildQuickEntries(list.map((c) => ({ code: c }))) })
          return
        }
      } catch (e) {}
    }
  },

  onSwiperChange(event) {
    this.setData({ currentSwiperIndex: event.detail.current })
  },

  onBannerTap() {
    const banner = this.data.banners[this.data.currentSwiperIndex]
    if (!banner) return
    if (banner.targetType === 'knowledge' && banner.targetId) {
      wx.navigateTo({ url: `/pages/knowledge-detail/knowledge-detail?id=${banner.targetId}` })
    } else if (banner.targetType === 'notice' && banner.targetId) {
      wx.navigateTo({ url: `/pages/notice-detail/notice-detail?id=${banner.targetId}` })
    }
  },

  async loadHomeData() {
    this.setData({ loading: true })

    try {
      await ensureLogin()
      const data = await request({ url: '/api/home' })

      this.setData({
        banners: (data.banners || []).length ? data.banners : this.data.banners,
        allServiceEntries: data.allServiceEntries || [],
        todoStats: this.buildTodoStats(data.todoStats),
        latestNotices: this.buildLatestNotices(data.latestNotices),
        downloads: this.buildDownloads(data.downloads),
      })
    } catch (error) {
      console.error('Load home data failed:', error)
    } finally {
      this.setData({ loading: false })
      this._loadedOnce = true
    }
  },

  async refreshQuickEntries() {
    try {
      const data = await request({ url: '/api/home' })
      const quickEntries = this.buildQuickEntries(data.quickEntries || [])
      this.setData({ quickEntries })
    } catch (error) {
      const store = wx.getStorageSync('quick_entry_codes')
      if (store) {
        const codes = JSON.parse(store)
        const localEntries = this.buildQuickEntries(codes.map((c) => ({ code: c })))
        this.setData({ quickEntries: localEntries })
      }
    }
  },

  buildQuickEntries(remoteEntries) {
    return (remoteEntries || []).map((item) => {
      const info = SERVICE_INFO[item.code] || {}
      return {
        code: item.code || '',
        title: item.name || info.name || item.code || '入口',
        icon: item.icon || info.icon || '📌',
        path: item.path || info.path || '',
      }
    }).filter((item) => item.path)
  },

  buildTodoStats(remoteStats) {
    const isCadre = remoteStats && remoteStats.pendingFeedbackRole === 'cadre'
    const list = [
      { label: '未读', value: String((remoteStats && remoteStats.unreadMessages) || 0), hint: '消息', path: '/pages/notice/notice' },
      { label: '提醒', value: String((remoteStats && remoteStats.upcomingDeadlines) || 0), hint: '党团流程', path: '/pages/party-progress/party-progress' },
      { label: '汇报', value: String((remoteStats && remoteStats.pendingReports) || 0), hint: '待处理', path: '/pages/party-report/party-report' },
    ]
    if (isCadre) {
      list.push({
        label: '反馈',
        value: String((remoteStats && remoteStats.pendingFeedbacks) || 0),
        hint: '待处理',
        path: '/pages/feedback-pending/feedback-pending',
      })
    }
    return list
  },

  buildLatestNotices(remoteNotices) {
    return (remoteNotices || []).map((item) => ({
      tag: item.tag || '通知',
      date: item.publishDate || item.date || '',
      title: item.title || '未命名',
      summary: item.summary || '',
      id: item.id,
    }))
  },

  buildDownloads(remoteDownloads) {
    return (remoteDownloads || []).map((item) => ({
      name: item.name || '模板',
      desc: item.description || '',
    }))
  },

  onSearchInput(event) {
    this.setData({ searchKeyword: event.detail.value || '' })
  },

  handleSearchTap() {
    const keyword = this.data.searchKeyword || ''
    const url = '/pages/search/search' + (keyword.trim() ? `?keyword=${encodeURIComponent(keyword.trim())}` : '')
    wx.navigateTo({ url })
  },

  handleBannerTap() {
    this.onBannerTap()
  },

  handleEntryTap(event) {
    const { path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
    }
  },

  handleEditEntryTap() {
    wx.navigateTo({ url: '/pages/quick-edit/quick-edit' })
  },

  handleTodoTap(event) {
    const { path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
    }
  },

  handleLatestNoticeTap(event) {
    const { id } = event.currentTarget.dataset
    if (!id) {
      wx.showToast({ title: '通知不存在', icon: 'none' })
      return
    }
    wx.navigateTo({ url: `/pages/notice-detail/notice-detail?noticeId=${encodeURIComponent(String(id))}` })
  },


  handleGestureStart(event) {
    const touch = event.touches && event.touches[0]
    if (!touch) return
    this._gestureStart = { x: touch.clientX, y: touch.clientY, time: Date.now() }
  },

  handleGestureEnd(event) {
    const start = this._gestureStart
    const touch = event.changedTouches && event.changedTouches[0]
    if (!start || !touch) return
    const deltaX = touch.clientX - start.x
    const deltaY = touch.clientY - start.y
    const duration = Date.now() - start.time
    if (duration < 600 && deltaX < -60 && Math.abs(deltaY) < 80) {
      wx.navigateTo({ url: '/pages/ai-chat/ai-chat' })
    }
    this._gestureStart = null
  },

  openAiAssistant() {
    wx.navigateTo({ url: '/pages/ai-chat/ai-chat' })
  },
  handleNoticeTap(event) {
    const { id } = event.currentTarget.dataset
    if (id) {
      wx.navigateTo({ url: `/pages/notice-detail/notice-detail?noticeId=${id}` })
    }
  },

  handleDownloadTap() {
    wx.navigateTo({ url: '/pages/knowledge/knowledge' })
  },
})
