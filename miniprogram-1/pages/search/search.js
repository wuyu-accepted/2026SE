const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { knowledgeBaseData } = require('../../utils/mock-data')

const SERVICE_ENTRIES = [
  { code: 'leave', name: '请假申请', path: '/pages/leave-list/leave-list', keywords: ['请假', '审批', '假条', '出勤'] },
  { code: 'partyProgress', name: '入党流程追踪', path: '/pages/party-progress/party-progress', keywords: ['党团', '入党', '流程', '进度'] },
  { code: 'partyReport', name: '思想汇报提交', path: '/pages/party-report/party-report', keywords: ['思想汇报', '汇报', '党团'] },
  { code: 'partyActivity', name: '党团活动申请', path: '/pages/party-activity/party-activity', keywords: ['党团活动', '活动', '申请'] },
  { code: 'certificate', name: '电子证明生成', path: '/pages/e-certificate/e-certificate', keywords: ['证明', '电子证明', '开具'] },
  { code: 'policyKnowledge', name: '政策知识库', path: '/pages/policy-knowledge/policy-knowledge', keywords: ['政策', '知识库', '文件', '模板'] },
  { code: 'studyAnalysis', name: '学业分析与预警', path: '/pages/study-analysis/study-analysis', keywords: ['学业', '分析', '预警'] },
  { code: 'portrait', name: '学生画像', path: '/pages/student-portrait/student-portrait', keywords: ['画像', '学生画像'] },
  { code: 'honor', name: '奖励荣誉', path: '/pages/honor/honor', keywords: ['荣誉', '奖励'] },
  { code: 'template', name: '模板下载', path: '/pages/template-download/template-download', keywords: ['模板', '下载', '文档'] },
]

function splitTags(tags) {
  if (!tags) return []
  return String(tags).split(/[,，]/).map((item) => item.trim()).filter(Boolean)
}

Page({
  data: {
    keyword: '',
    loading: false,
    activeType: 'all',
    knowledgeResults: [],
    noticeResults: [],
    serviceResults: [],
    hasSearched: false,
    usingLocalFallback: false,
  },

  onLoad(options) {
    const keyword = options && options.keyword ? decodeURIComponent(options.keyword) : ''
    if (keyword) {
      this.setData({ keyword })
      this.performSearch(keyword)
    }
  },

  onKeywordInput(event) {
    this.setData({ keyword: event.detail.value || '' })
  },

  onTypeTap(event) {
    const { type } = event.currentTarget.dataset
    this.setData({ activeType: type })
  },

  async onSearchTap() {
    await this.performSearch(this.data.keyword)
  },

  async performSearch(keyword) {
    const normalized = String(keyword || '').trim()
    if (!normalized) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [knowledgeData, noticeData] = await Promise.all([
        request({ url: '/api/knowledge/articles', data: { keyword: normalized, pageNum: 1, pageSize: 20 } }),
        request({ url: '/api/messages/search', data: { keyword: normalized, limit: 20 } }),
      ])
      const knowledgeResults = (knowledgeData.records || knowledgeData.list || []).map((item) => this.normalizeKnowledge(item))
      const noticeResults = (noticeData || []).map((item) => this.normalizeNotice(item))
      const serviceResults = this.searchServices(normalized)
      this.setData({
        knowledgeResults,
        noticeResults,
        serviceResults,
        hasSearched: true,
        usingLocalFallback: false,
        activeType: 'all',
      })
    } catch (error) {
      console.error('Search failed:', error)
      this.setData({
        knowledgeResults: this.searchKnowledgeLocal(normalized),
        noticeResults: this.searchNoticesLocal(normalized),
        serviceResults: this.searchServices(normalized),
        hasSearched: true,
        usingLocalFallback: true,
        activeType: 'all',
      })
      wx.showToast({ title: '已切换本地搜索', icon: 'none' })
    } finally {
      this.setData({ loading: false })
    }
  },

  normalizeKnowledge(item) {
    return {
      id: item.id,
      title: item.title || '未命名知识',
      summary: item.summary || '',
      categoryName: item.categoryName || '未分类',
      tags: splitTags(item.tags),
      searchHighlight: item.searchHighlight || '',
      scoreExplanation: item.scoreExplanation || '',
      source: '知识库',
      path: `/pages/knowledge-detail/knowledge-detail?id=${item.id}`,
    }
  },

  normalizeNotice(item) {
    return {
      id: item.id,
      title: item.title || '未命名通知',
      summary: item.summary || '',
      tag: item.tag || '通知',
      source: '通知',
      path: `/pages/notice-detail/notice-detail?id=${item.id}`,
    }
  },

  searchServices(keyword) {
    const lower = keyword.toLowerCase()
    return SERVICE_ENTRIES.filter((item) => {
      const joined = [item.name, item.code].concat(item.keywords || []).join(' ').toLowerCase()
      return joined.includes(lower)
    }).map((item) => ({
      id: item.code,
      title: item.name,
      summary: `入口：${item.path}`,
      source: '服务',
      path: item.path,
    }))
  },

  searchKnowledgeLocal(keyword) {
    const lower = keyword.toLowerCase()
    return (knowledgeBaseData.articles || [])
      .filter((item) => {
        const text = [item.title, item.summary, item.answer, item.category, ...(item.tags || [])].join(' ').toLowerCase()
        return text.includes(lower)
      })
      .map((item) => ({
        id: item.id,
        title: item.title,
        summary: item.summary,
        categoryName: item.category || '未分类',
        tags: item.tags || [],
        searchHighlight: '',
        scoreExplanation: '',
        source: '知识库',
        path: `/pages/knowledge-detail/knowledge-detail?id=${item.id}`,
      }))
  },

  searchNoticesLocal(keyword) {
    const lower = keyword.toLowerCase()
    const recent = knowledgeBaseData.noticeResults || []
    return recent.filter((item) => {
      const text = [item.title, item.summary, item.tag].join(' ').toLowerCase()
      return text.includes(lower)
    }).map((item) => ({
      id: item.id,
      title: item.title,
      summary: item.summary,
      tag: item.tag,
      source: '通知',
      path: `/pages/notice-detail/notice-detail?id=${item.id}`,
    }))
  },

  onResultTap(event) {
    const { path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
    }
  },
})
