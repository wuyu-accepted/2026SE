const { homeData } = require('../../utils/mock-data')
const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

const TAB_PAGES = [
  '/pages/index/index',
  '/pages/notice/notice',
  '/pages/service/service',
  '/pages/profile/profile',
]

Page({
  data: {
    banner: homeData.banner,
    quickEntries: homeData.quickEntries,
    todoStats: homeData.todoStats,
    latestNotices: homeData.latestNotices,
    downloads: homeData.downloads,
    loading: false,
    searchKeyword: '',
  },

  onLoad() {
    this.loadHomeData()
  },

  async loadHomeData() {
    this.setData({ loading: true })

    try {
      await ensureLogin()
      const data = await request({ url: '/api/home' })

      this.setData({
        banner: this.buildBanner(data.banner),
        quickEntries: this.buildQuickEntries(data.quickEntries),
        todoStats: this.buildTodoStats(data.todoStats),
        latestNotices: this.buildLatestNotices(data.latestNotices),
        downloads: this.buildDownloads(data.downloads),
      })
    } catch (error) {
      console.error('Load home data failed:', error)
      this.setData({
        banner: homeData.banner,
        quickEntries: homeData.quickEntries,
        todoStats: homeData.todoStats,
        latestNotices: homeData.latestNotices,
        downloads: homeData.downloads,
      })
      wx.showToast({
        title: '已使用本地首页数据',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  buildBanner(remoteBanner = {}) {
    return {
      ...homeData.banner,
      title: remoteBanner.title || homeData.banner.title,
      subtitle: remoteBanner.subtitle || homeData.banner.subtitle,
    }
  },

  buildQuickEntries(remoteEntries = []) {
    const pathMap = {
      knowledge: '/pages/knowledge/knowledge',
      notice: '/pages/notice/notice',
      service: '/pages/service/service',
    }

    if (!remoteEntries.length) {
      return homeData.quickEntries
    }

    return remoteEntries.map((item) => ({
      title: item.name || item.code || '入口',
      desc: item.description || '点击进入对应服务。',
      icon: (item.name || item.code || '入').slice(0, 1),
      path: pathMap[item.code] || '',
    }))
  },

  buildTodoStats(remoteStats = {}) {
    return [
      { label: '未读', value: String(remoteStats.unreadMessages || 0), hint: '消息' },
      { label: '提醒', value: String(remoteStats.upcomingDeadlines || 0), hint: '党团流程' },
      { label: '汇报', value: String(remoteStats.pendingReports || 0), hint: '待处理' },
      {
        label: '反馈',
        value: String(remoteStats.pendingFeedbacks || 0),
        hint: remoteStats.pendingFeedbackRole === 'cadre' ? '待处理' : '待关注',
        path: remoteStats.pendingFeedbackRole === 'cadre' ? '/pages/feedback-pending/feedback-pending' : '',
      },
    ]
  },

  buildLatestNotices(remoteNotices = []) {
    return (remoteNotices || []).map((item) => ({
      tag: item.tag || '通知',
      date: item.publishDate || item.date || '',
      title: item.title || '未命名',
      summary: item.summary || '',
    }))
  },

  buildDownloads(remoteDownloads = []) {
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

  handleTodoTap(event) {
    const { path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
    }
  },

  handleEntryTap(event) {
    const { path } = event.currentTarget.dataset

    if (path) {
      const navigate = TAB_PAGES.includes(path) ? wx.switchTab : wx.navigateTo
      navigate({ url: path })
    }
  },

  handleDownloadTap() {
    wx.navigateTo({ url: '/pages/knowledge/knowledge' })
  },
})
