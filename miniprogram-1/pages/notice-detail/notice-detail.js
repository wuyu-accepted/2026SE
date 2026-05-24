const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

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

function priorityLabel(priority) {
  const map = {
    0: '普通',
    1: '重要',
    2: '紧急',
  }
  return map[priority] || '普通'
}

function normalizeDetail(data) {
  const detail = data || {}
  return {
    ...detail,
    noticeType: detail.noticeType || '通知',
    tag: detail.tag || '',
    priorityText: priorityLabel(detail.priority),
    publishText: formatDateTime(detail.publishTime || detail.createdAt || detail.date),
    readText: formatDateTime(detail.readTime),
  }
}


function feedbackStatusText(status) {
  const map = {
    pending_cadre: '待骨干处理',
    pending_counselor: '待辅导员处理',
    resolved_by_cadre: '骨干已处理',
    resolved_by_counselor: '辅导员已处理',
    closed: '已关闭',
  }
  return map[status] || '处理中'
}

function parseFallback(value) {
  if (!value) {
    return null
  }

  try {
    return JSON.parse(decodeURIComponent(String(value)))
  } catch (error) {
    console.warn('Parse notice fallback failed:', error)
    return null
  }
}

Page({
  data: {
    messageId: null,
    noticeId: null,
    detail: null,
    loading: false,
    feedbacks: [],
    feedbackType: 'ordinary',
    feedbackContent: '',
    submittingFeedback: false,
  },

  onLoad(options) {
    const messageId = options.id ? decodeURIComponent(String(options.id)) : ''
    const noticeId = options.noticeId ? decodeURIComponent(String(options.noticeId)) : ''
    if (!messageId && !noticeId) {
      wx.showToast({ title: '通知不存在', icon: 'none' })
      return
    }
    console.info('Notice detail page loaded:', messageId || noticeId)
    const fallback = parseFallback(options.fallback)
    this.setData({
      messageId,
      noticeId: noticeId || (fallback && fallback.noticeId ? String(fallback.noticeId) : null),
      detail: fallback ? normalizeDetail(fallback) : null,
    })
    this.loadDetail()
  },

  async loadDetail() {
    const { messageId, noticeId } = this.data
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const targetId = messageId || noticeId
      if (!targetId) {
        throw new Error('通知不存在')
      }
      console.info('Load notice detail request:', targetId)
      const data = await request({ url: `/api/messages/${targetId}` })
      console.info('Load notice detail success:', data && data.id ? data.id : targetId)
      const resolvedMessageId = data && data.id ? String(data.id) : targetId
      this.setData({ detail: normalizeDetail(data), messageId: resolvedMessageId })
      this.loadFeedbacks()
    } catch (error) {
      console.error('Load message detail failed:', error)
      wx.showToast({
        title: error.message || '获取通知详情失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },


  async loadFeedbacks() {
    const { messageId, noticeId } = this.data
    const targetId = messageId || noticeId
    if (!targetId) {
      return
    }
    try {
      const data = await request({ url: `/api/messages/${targetId}/feedbacks` })
      const feedbacks = (data || []).map((item) => ({
        ...item,
        typeText: item.feedbackType === 'private' ? '私密问题' : '普通问题',
        statusText: feedbackStatusText(item.status),
        createdText: formatDateTime(item.createdAt),
      }))
      this.setData({ feedbacks })
    } catch (error) {
      console.warn('Load notice feedbacks failed:', error)
    }
  },

  onFeedbackTypeChange(event) {
    this.setData({ feedbackType: event.detail.value })
  },

  onFeedbackContentInput(event) {
    this.setData({ feedbackContent: event.detail.value })
  },

  async submitFeedback() {
    const content = String(this.data.feedbackContent || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入反馈内容', icon: 'none' })
      return
    }
    this.setData({ submittingFeedback: true })
    try {
      await ensureLogin()
      const targetId = this.data.messageId || this.data.noticeId
      await request({
        url: `/api/messages/${targetId}/feedback`,
        method: 'POST',
        data: {
          feedbackType: this.data.feedbackType,
          content,
        },
      })
      wx.showToast({ title: '反馈已提交', icon: 'none' })
      this.setData({ feedbackContent: '', feedbackType: 'ordinary' })
      this.loadFeedbacks()
    } catch (error) {
      console.error('Submit notice feedback failed:', error)
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submittingFeedback: false })
    }
  },

  downloadAttachment() {
    const fileId = this.data.detail && this.data.detail.attachmentFileId
    if (!fileId) {
      wx.showToast({ title: '暂无附件', icon: 'none' })
      return
    }

    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      wx.showToast({ title: '登录已失效', icon: 'none' })
      return
    }

    wx.showLoading({ title: '下载中' })
    wx.downloadFile({
      url: `${BASE_URL}/api/files/${fileId}/download`,
      header: {
        Authorization: token,
      },
      success: (res) => {
        if (res.statusCode !== 200) {
          wx.hideLoading()
          wx.showToast({ title: '下载失败', icon: 'none' })
          return
        }

        wx.hideLoading()
        const platform = (wx.getSystemInfoSync && wx.getSystemInfoSync().platform) || ''
        if (platform === 'devtools') {
          wx.showToast({ title: '开发者工具不支持预览，请在真机打开', icon: 'none' })
          return
        }

        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: (error) => {
            console.error('Open notice attachment failed:', error)
            wx.showToast({ title: '打开失败', icon: 'none' })
          },
        })
      },
      fail: (error) => {
        console.error('Download notice attachment failed:', error)
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      },
    })
  },
})
