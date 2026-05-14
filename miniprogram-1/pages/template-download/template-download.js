const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

Page({
  data: {
    templates: [],
  },

  onShow() {
    this.loadTemplates()
  },

  async loadTemplates() {
    try {
      await ensureLogin()
      const templateData = await request({ url: '/api/knowledge/templates' })
      const templates = (templateData || []).map((item) => ({
        id: item.id,
        name: item.name,
        desc: item.description || '',
        format: item.format || '文件',
        fileId: item.fileId || null,
      }))
      this.setData({ templates })
    } catch (error) {
      console.error('Load templates failed:', error)
      wx.showToast({
        title: '获取模板失败',
        icon: 'none',
      })
    }
  },

  onTemplateTap(event) {
    const { fileId, name } = event.currentTarget.dataset
    if (!fileId) {
      wx.showToast({
        title: `${name}暂无文件`,
        icon: 'none',
      })
      return
    }

    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      wx.showToast({
        title: '登录已失效',
        icon: 'none',
      })
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
          wx.showToast({
            title: '下载失败',
            icon: 'none',
          })
          return
        }

        wx.hideLoading()

        const platform = (wx.getSystemInfoSync && wx.getSystemInfoSync().platform) || ''
        if (platform === 'devtools') {
          wx.showToast({
            title: '开发者工具不支持预览，请在真机打开',
            icon: 'none',
          })
          return
        }

        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: (error) => {
            console.error('Open document failed:', error)
            wx.showToast({
              title: '打开失败',
              icon: 'none',
            })
          },
        })
      },
      fail: (error) => {
        console.error('Download template failed:', error)
        wx.hideLoading()
        wx.showToast({
          title: '下载失败',
          icon: 'none',
        })
      },
    })
  },
})
