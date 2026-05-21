const { knowledgeBaseData } = require('../../utils/mock-data')
const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    categories: knowledgeBaseData.categories,
    selectedCategory: '全部',
    articles: knowledgeBaseData.articles,
    visibleArticles: knowledgeBaseData.articles,
    recommendations: [],
  },

  onLoad() {
    this.loadKnowledgeData()
  },

  async loadKnowledgeData() {
    try {
      await ensureLogin()
      const [articleData, recommendations] = await Promise.all([
        request({ url: '/api/knowledge/articles' }),
        request({ url: '/api/knowledge/recommendations', data: { limit: 6 } }).catch(() => []),
      ])

      const articles = (articleData.records || articleData.list || []).map((item) => this.normalizeArticle(item))
      const categories = ['全部', ...new Set(articles.map((item) => item.category))]

      this.setData({
        categories,
        selectedCategory: '全部',
        articles,
        visibleArticles: articles,
        recommendations: (recommendations || []).map((item) => this.normalizeRecommendation(item)),
      })
    } catch (error) {
      console.error('Load knowledge data failed:', error)
      wx.showToast({
        title: '已切换到本地数据',
        icon: 'none',
      })
    }
  },

  normalizeArticle(item) {
    const tags = this.splitTags(item.tags)
    return {
      id: item.id,
      category: item.categoryName || '未分类',
      source: this.typeLabel(item.contentType) || '平台',
      title: item.title,
      summary: item.summary || '',
      answer: item.summary || '点击进入详情查看',
      tags,
      tagText: tags.join(' · '),
      contentType: item.contentType || 'guide',
      keywords: [item.title, item.summary || '', tags.join(' ')].join(' ').split(/\s+/).filter(Boolean),
    }
  },

  normalizeRecommendation(item) {
    return {
      targetType: item.targetType,
      id: item.targetId,
      title: item.title,
      summary: item.summary || '',
      reason: item.recommendReason || '与你相关',
      tags: this.splitTags(item.tags).slice(0, 3),
      format: item.format || '',
      fileId: item.fileId || null,
    }
  },

  splitTags(tags) {
    if (!tags) return []
    return String(tags).split(/[,，]/).map((item) => item.trim()).filter(Boolean)
  },

  typeLabel(type) {
    const labels = { policy: '政策', process: '流程', faq: '问答', guide: '指南' }
    return labels[type] || '指南'
  },

  onKeywordInput(event) {
    const keyword = event.detail.value || ''
    this.setData({ keyword })
    this.applyFilters()
    clearTimeout(this.searchTimer)
    this.searchTimer = setTimeout(() => {
      if (keyword.trim()) {
        this.reportBehavior({ eventType: 'search', targetType: 'search', keyword, sourcePage: 'knowledge' })
      }
    }, 600)
  },

  onCategoryTap(event) {
    const { category } = event.currentTarget.dataset
    this.setData({ selectedCategory: category })
    this.applyFilters()
  },

  applyFilters() {
    const { keyword, selectedCategory, articles } = this.data
    const normalizedKeyword = keyword.toLowerCase()

    const visibleArticles = articles.filter((item) => {
      const matchCategory = selectedCategory === '全部' || item.category === selectedCategory
      const matchKeyword = !normalizedKeyword
        || item.title.toLowerCase().includes(normalizedKeyword)
        || item.summary.toLowerCase().includes(normalizedKeyword)
        || item.keywords.join(' ').toLowerCase().includes(normalizedKeyword)

      return matchCategory && matchKeyword
    })

    this.setData({ visibleArticles })
  },

  onArticleTap(event) {
    const { id } = event.currentTarget.dataset
    if (!id) return
    wx.navigateTo({ url: `/pages/knowledge-detail/knowledge-detail?id=${id}` })
  },

  onRecommendationTap(event) {
    const { id, targetType, fileId } = event.currentTarget.dataset
    this.reportBehavior({ eventType: 'click_recommendation', targetType, targetId: id, sourcePage: 'knowledge' })
    if (targetType === 'template' && fileId) {
      wx.navigateTo({ url: '/pages/template-download/template-download' })
      return
    }
    if (id) {
      wx.navigateTo({ url: `/pages/knowledge-detail/knowledge-detail?id=${id}` })
    }
  },

  async reportBehavior(data) {
    try {
      await request({ url: '/api/knowledge/behavior', method: 'POST', data })
    } catch (error) {
      console.warn('Report knowledge behavior failed:', error)
    }
  },
})
