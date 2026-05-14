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
    serviceHighlights: homeData.serviceHighlights,
    loading: false,
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
      wx.showToast({
        title: '已切换到本地首页数据',
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
      desc: `编码：${item.code || '未知'}`,
      icon: (item.name || item.code || '入').slice(0, 1),
      status: pathMap[item.code] ? '可用' : '建设中',
      path: pathMap[item.code] || '',
    }))
  },

  buildTodoStats(remoteStats = {}) {
    return [
      {
        label: '未读',
        value: String(remoteStats.unreadMessages || 0),
        hint: '消息',
      },
      {
        label: '提醒',
        value: String(remoteStats.upcomingDeadlines || 0),
        hint: '党团流程',
      },
      {
        label: '汇报',
        value: String(remoteStats.pendingReports || 0),
        hint: '待处理',
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

  handleEntryTap(event) {
    const { path, title } = event.currentTarget.dataset

    if (path) {
      const navigate = TAB_PAGES.includes(path) ? wx.switchTab : wx.navigateTo
      navigate({ url: path })
      return
    }

    wx.showToast({
      title: `${title}功能建设中`,
      icon: 'none',
    })
  },

  handleDownloadTap(event) {
    const { name } = event.currentTarget.dataset

    wx.showToast({
      title: `${name}下载功能建设中`,
      icon: 'none',
    })
  },
})
