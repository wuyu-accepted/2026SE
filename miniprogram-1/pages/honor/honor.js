const { request } = require('../../utils/request')
const { ensureLogin } = require('../../utils/auth')

Page({
  data: {
    list: [],
    loading: false,
  },

  onLoad() {
    this.loadList()
  },

  async loadList() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const data = await request({ url: '/api/honor' })
      this.setData({ list: (data || []).map((item) => ({
        ...item,
        levelTag: item.awardLevel || '其他',
        yearLabel: item.awardDate ? item.awardDate.slice(0, 4) : '',
      })) })
    } catch (error) {
      console.error('Load honor list failed:', error)
    } finally {
      this.setData({ loading: false })
    }
  },
})
