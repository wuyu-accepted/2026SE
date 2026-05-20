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
  },

  onLoad() {
    this.loadKnowledgeData()
  },

  async loadKnowledgeData() {
    try {
      await ensureLogin()

      const articleData = await request({ url: '/api/knowledge/articles' })

      const articles = (articleData.records || articleData.list || []).map((item) => ({
        id: item.id,
        category: item.categoryName || '未分类',
        source: '平台',
        title: item.title,
        summary: item.summary || '',
        answer: item.summary || '点击进入详情查看',
        keywords: [item.title, item.summary || ''].join(' ').split(/\s+/).filter(Boolean),
      }))

      const categories = ['全部', ...new Set(articles.map((item) => item.category))]

      this.setData({
        categories,
        selectedCategory: '全部',
        articles,
        visibleArticles: articles,
      })
    } catch (error) {
      console.error('Load knowledge data failed:', error)
      wx.showToast({
        title: '已切换到本地数据',
        icon: 'none',
      })
    }
  },

  onKeywordInput(event) {
    this.setData({
      keyword: event.detail.value.trim(),
    })

    this.applyFilters()
  },

  onCategoryTap(event) {
    this.setData({
      selectedCategory: event.currentTarget.dataset.category,
    })

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
    if (!id) {
      return
    }

    wx.navigateTo({
      url: `/pages/knowledge-detail/knowledge-detail?id=${id}`,
    })
  },
})
