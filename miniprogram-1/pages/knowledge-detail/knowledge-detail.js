const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

Page({
  data: {
    loading: true,
    article: null,
  },

  onLoad(options) {
    const { id } = options || {}
    if (!id) {
      wx.showToast({ title: '缺少文章编号', icon: 'none' })
      this.setData({ loading: false })
      return
    }
    this.loadArticleDetail(id)
  },

  async loadArticleDetail(id) {
    this.setData({ loading: true })

    try {
      await ensureLogin()
      const article = await request({ url: `/api/knowledge/articles/${id}` })
      const tags = this.splitTags(article.tags)

      this.setData({
        article: {
          id: article.id,
          title: article.title || '',
          summary: article.summary || '',
          categoryName: article.categoryName || '未分类',
          source: article.source || '平台',
          answer: article.answer || '',
          content: article.content || '',
          publishTime: article.publishTime || '',
          viewCount: article.viewCount || 0,
          fileId: article.fileId || null,
          contentMode: article.contentMode || 'file',
          editorType: article.editorType || '',
          renderedContent: article.renderedContent || '',
          contentType: this.typeLabel(article.contentType),
          tags,
          targetText: this.targetText(article),
          scenarioCodes: article.scenarioCodes || '',
        },
      })
    } catch (error) {
      console.error('Load article detail failed:', error)
      wx.showToast({ title: '加载失败', icon: 'none' })
    } finally {
      this.setData({ loading: false })
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

  targetText(article) {
    const parts = []
    if (article.targetGrades) parts.push(`年级：${article.targetGrades}`)
    if (article.targetMajors) parts.push(`专业：${article.targetMajors}`)
    if (article.targetPoliticalStatuses) parts.push(`政治面貌：${article.targetPoliticalStatuses}`)
    if (article.targetPartyStages) parts.push(`党团阶段：${article.targetPartyStages}`)
    return parts.join('；') || '适用于全体学生'
  },

  onDownloadTap() {
    const article = this.data.article
    if (!article) return
    if (article.contentMode === 'editor') {
      wx.showToast({ title: '请在网页端下载可编辑源文件', icon: 'none' })
      return
    }
    if (!article.fileId) {
      wx.showToast({ title: '暂无资料文件', icon: 'none' })
      return
    }
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      wx.showToast({ title: '登录已失效', icon: 'none' })
      return
    }
    wx.showLoading({ title: '下载中' })
    wx.downloadFile({
      url: `${BASE_URL}/api/files/${article.fileId}/download`,
      header: { Authorization: token },
      success: (res) => {
        wx.hideLoading()
        if (res.statusCode !== 200) {
          wx.showToast({ title: '下载失败', icon: 'none' })
          return
        }
        const platform = (wx.getSystemInfoSync && wx.getSystemInfoSync().platform) || ''
        if (platform === 'devtools') {
          wx.showToast({ title: '开发者工具不支持预览，请在真机打开', icon: 'none' })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => wx.showToast({ title: '打开失败', icon: 'none' }),
        })
      },
      fail: () => {
        wx.hideLoading()
        wx.showToast({ title: '下载失败', icon: 'none' })
      },
    })
  },
})
