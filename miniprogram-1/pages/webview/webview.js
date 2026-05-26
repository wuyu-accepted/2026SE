Page({
  data: {
    url: '',
  },

  onLoad(options) {
    const url = options.url ? decodeURIComponent(options.url) : ''
    if (!url) {
      wx.showToast({ title: '链接不可用', icon: 'none' })
      wx.navigateBack()
      return
    }
    this.setData({ url })
  },

  onLoadSuccess() {
    console.info('WebView loaded successfully')
  },

  onLoadError(e) {
    console.error('WebView load failed:', e.detail)
    wx.showToast({ title: '页面加载失败', icon: 'none' })
  },
})
