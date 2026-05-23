const { knowledgeBaseData } = require('../../utils/mock-data')
const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

Page({
  data: {
    keyword: '',
    categories: knowledgeBaseData.categories.map((item) => (typeof item === 'string' ? { id: null, name: item } : item)),
    selectedCategory: '全部',
    selectedCategoryId: null,
    articles: knowledgeBaseData.articles,
    visibleArticles: knowledgeBaseData.articles,
    recommendations: [],
    loading: false,
    searchTips: [],
    correctedKeyword: '',
    usingLocalFallback: false,
  },

  onLoad() {
    this.loadKnowledgeData()
  },

  onUnload() {
    clearTimeout(this.searchTimer)
    clearTimeout(this.reportTimer)
  },

  async loadKnowledgeData(options = {}) {
    const keyword = options.keyword !== undefined ? options.keyword : this.data.keyword
    const selectedCategory = options.category !== undefined ? options.category : this.data.selectedCategory
    const selectedCategoryId = this.resolveCategoryId(this.data.categories, selectedCategory)
    this.setData({ loading: true })

    try {
      await ensureLogin()
      const params = {
        pageNum: 1,
        pageSize: 30,
      }
      if (keyword && keyword.trim()) {
        params.keyword = keyword.trim()
      }
      if (selectedCategoryId) {
        params.categoryId = selectedCategoryId
      }

      const [categoryResult, articleResult, recommendationResult] = await Promise.allSettled([
        request({ url: '/api/knowledge/categories' }),
        request({ url: '/api/knowledge/articles', data: params }),
        request({ url: '/api/knowledge/recommendations', data: { limit: 6 } }),
      ])

      const categories = categoryResult.status === 'fulfilled'
        ? this.normalizeCategories(categoryResult.value)
        : this.data.categories
      const currentCategoryId = this.resolveCategoryId(categories, selectedCategory)
      const rawArticles = articleResult.status === 'fulfilled'
        ? (articleResult.value.records || articleResult.value.list || []).map((item) => this.normalizeArticle(item))
        : (knowledgeBaseData.articles || []).map((item) => ({
            ...item,
            categoryId: item.categoryId || null,
            searchHighlight: '',
            scoreExplanation: '',
            correctedKeyword: '',
          }))
      const articles = this.filterArticles(rawArticles, selectedCategory, currentCategoryId, keyword)
      const recommendations = recommendationResult.status === 'fulfilled' ? recommendationResult.value : []

      this.setData({
        categories,
        selectedCategory: selectedCategory || '全部',
        selectedCategoryId: currentCategoryId,
        articles,
        visibleArticles: articles,
        recommendations: (recommendations || []).map((item) => this.normalizeRecommendation(item)),
        searchTips: articles.filter((item) => item.searchHighlight || item.scoreExplanation).slice(0, 3),
        correctedKeyword: articles.find((item) => item.correctedKeyword)?.correctedKeyword || '',
        usingLocalFallback: articleResult.status !== 'fulfilled',
      })
      if (articleResult.status !== 'fulfilled') {
        wx.showToast({
          title: '已切换到本地数据',
          icon: 'none',
        })
      }
    } catch (error) {
      console.error('Load knowledge data failed:', error)
      this.applyLocalFallback(keyword, selectedCategory)
      wx.showToast({
        title: '已切换到本地数据',
        icon: 'none',
      })
    } finally {
      this.setData({ loading: false })
    }
  },

  normalizeCategories(categoryData) {
    const remoteCategories = (categoryData || [])
      .map((item) => ({
        id: item.id || null,
        name: item.name || '未分类',
        code: item.code || '',
        status: item.status,
      }))
      .filter((item) => item.status === 1)
    const merged = [
      { id: null, name: '全部' },
      { id: null, name: '未分类' },
      ...remoteCategories,
    ]
    return merged.filter((item, index, array) => array.findIndex((entry) => entry.name === item.name) === index)
  },

  normalizeArticle(item) {
    const tags = this.splitTags(item.tags)
    return {
      id: item.id,
      categoryId: item.categoryId || null,
      category: item.categoryName || '未分类',
      source: this.typeLabel(item.contentType) || '平台',
      title: item.title,
      summary: item.summary || '',
      answer: item.summary || '点击进入详情查看',
      tags,
      tagText: tags.join(' · '),
      contentType: item.contentType || 'guide',
      searchHighlight: this.stripHtml(item.searchHighlight || ''),
      scoreExplanation: item.scoreExplanation || '',
      searchTip: this.stripHtml(item.searchHighlight || '') || item.scoreExplanation || '',
      correctedKeyword: item.correctedKeyword || '',
      keywords: [item.title, item.summary || '', item.searchHighlight || '', tags.join(' ')].join(' ').split(/\s+/).filter(Boolean),
    }
  },

  filterArticles(articles, selectedCategory, selectedCategoryId, keyword) {
    const normalizedKeyword = String(keyword || '').trim().toLowerCase()
    return (articles || []).filter((item) => {
      const matchCategory = (() => {
        if (selectedCategory === '全部') return true
        if (selectedCategory === '未分类') return !item.categoryId || item.category === '未分类'
        if (selectedCategoryId && item.categoryId) return String(item.categoryId) === String(selectedCategoryId)
        return item.category === selectedCategory
      })()
      const matchKeyword = !normalizedKeyword
        || String(item.title || '').toLowerCase().includes(normalizedKeyword)
        || String(item.summary || '').toLowerCase().includes(normalizedKeyword)
        || String(item.answer || '').toLowerCase().includes(normalizedKeyword)
        || String(item.searchHighlight || '').toLowerCase().includes(normalizedKeyword)
        || String(item.scoreExplanation || '').toLowerCase().includes(normalizedKeyword)
        || (item.tags || []).join(' ').toLowerCase().includes(normalizedKeyword)
      return matchCategory && matchKeyword
    })
  },

  normalizeRecommendation(item) {
    return {
      targetType: item.targetType,
      targetLabel: item.targetType === 'template' ? '模板' : '知识',
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

  stripHtml(value) {
    return String(value || '').replace(/<[^>]+>/g, '')
  },

  resolveCategoryId(categories, categoryName) {
    if (!Array.isArray(categories)) return null
    const match = categories.find((item) => (item && item.name ? item.name : item) === categoryName)
    return match && match.id ? match.id : null
  },

  typeLabel(type) {
    const labels = { policy: '政策', process: '流程', faq: '问答', guide: '指南' }
    return labels[type] || '指南'
  },

  onKeywordInput(event) {
    const keyword = event.detail.value || ''
    this.setData({ keyword })
    clearTimeout(this.searchTimer)
    this.searchTimer = setTimeout(() => {
      this.loadKnowledgeData({ keyword })
    }, 350)
    clearTimeout(this.reportTimer)
    this.reportTimer = setTimeout(() => {
      if (keyword.trim()) {
        this.reportBehavior({ eventType: 'search', targetType: 'search', keyword, sourcePage: 'knowledge' })
      }
    }, 700)
  },

  onCategoryTap(event) {
    const { category } = event.currentTarget.dataset
    const selectedCategoryId = this.resolveCategoryId(this.data.categories, category)
    this.setData({ selectedCategory: category, selectedCategoryId })
    this.loadKnowledgeData({ category })
  },

  applyLocalFallback(keyword = this.data.keyword, selectedCategory = this.data.selectedCategory) {
    const categories = knowledgeBaseData.categories.map((item) => (typeof item === 'string' ? { id: null, name: item } : item))
    const selectedCategoryId = this.resolveCategoryId(categories, selectedCategory)
    const articles = (knowledgeBaseData.articles || []).map((item) => ({
      ...item,
      categoryId: item.categoryId || null,
      searchHighlight: '',
      scoreExplanation: '',
      correctedKeyword: '',
    }))
    const visibleArticles = this.filterArticles(articles, selectedCategory, selectedCategoryId, keyword)
    this.setData({
      articles,
      visibleArticles,
      categories,
      selectedCategory: selectedCategory || '全部',
      selectedCategoryId,
      searchTips: [],
      correctedKeyword: '',
      usingLocalFallback: true,
    })
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
