const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

function formatDate(value) {
  if (!value) return ''
  if (typeof value === 'string') {
    const normalized = value.replace('T', ' ')
    return normalized.length >= 10 ? normalized.slice(0, 10) : normalized
  }
  try {
    const date = new Date(value)
    if (Number.isNaN(date.getTime())) return ''
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    return `${year}-${month}-${day}`
  } catch (e) {
    return ''
  }
}

const FALLBACK_MESSAGES = [
  {
    id: 0,
    noticeId: 0,
    title: '学院综合服务平台上线试运行',
    summary: '可在首页查看通知、待办和知识库入口。学院综合服务平台已进入试运行阶段。',
    readStatus: 0,
    pinnedStatus: 0,
    date: '2026-04-01',
  },
  {
    id: 1,
    noticeId: 1,
    title: '本周提交思想汇报提醒',
    summary: '积极分子同学请按时提交本周思想汇报，如有问题可在知识库查看填写说明。',
    readStatus: 0,
    pinnedStatus: 0,
    date: '2026-04-05',
  },
  {
    id: 2,
    noticeId: 2,
    title: '关于2026春季学期学生事务办理时间安排的通知',
    summary: '请同学们按时间节点办理学籍异动、缓考与证明申请等事务。',
    readStatus: 0,
    pinnedStatus: 0,
    date: '2026-04-10',
  },
]

Page({
  data: {
    messages: [],
    keyword: '',
    sortBy: 'time',
    loading: false,
  },

  onShow() {
    this.loadMessages()
  },

  async loadMessages() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const data = await request({
        url: '/api/messages',
        data: {
          keyword: this.data.keyword,
          sortBy: this.data.sortBy,
          limit: 100,
        },
      })
      const messages = (data || []).map((item) => ({
        id: item.id,
        noticeId: item.noticeId,
        title: item.title || '未命名通知',
        summary: item.summary || '',
        attachmentFileId: item.attachmentFileId || null,
        readStatus: typeof item.readStatus === 'number' ? item.readStatus : 0,
        pinnedStatus: typeof item.pinnedStatus === 'number' ? item.pinnedStatus : 0,
        date: formatDate(item.createdAt || item.readTime),
      }))
      if (messages.length) {
        this.setData({ messages })
      } else {
        this.setData({ messages: FALLBACK_MESSAGES })
      }
    } catch (error) {
      console.error('Load messages failed:', error)
      this.setData({ messages: FALLBACK_MESSAGES })
    } finally {
      this.setData({ loading: false })
    }
  },

  handleKeywordInput(event) {
    this.setData({ keyword: event.detail.value || '' })
  },

  handleClearKeyword() {
    this.setData({ keyword: '' })
    this.loadMessages()
  },

  handleSearchConfirm() {
    this.loadMessages()
  },

  handleSortChange(event) {
    const sortBy = event.currentTarget.dataset.sort
    this.setData({ sortBy })
    this.loadMessages()
  },

  handleMessageTap(event) {
    const { id, noticeid } = event.currentTarget.dataset
    const url = noticeid
      ? `/pages/notice-detail/notice-detail?noticeId=${noticeid}`
      : `/pages/notice-detail/notice-detail?id=${id}`
    wx.navigateTo({ url })
  },

  handlePinTap(event) {
    const { id, pinned } = event.currentTarget.dataset
    const url = pinned ? `/api/messages/${id}/unpin` : `/api/messages/${id}/pin`
    request({ url, method: 'POST' })
      .then(() => this.loadMessages())
      .catch(() => {})
  },
})
