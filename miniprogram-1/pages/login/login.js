const { getSession, login } = require('../../utils/auth')

Page({
  data: {
    studentNo: '',
    password: '',
    submitting: false,
  },

  onShow() {
    const { token } = getSession()
    if (token) {
      wx.switchTab({ url: '/pages/index/index' })
    }
  },

  onStudentNoInput(event) {
    this.setData({ studentNo: String(event.detail.value || '').replace(/\D/g, '') })
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value })
  },

  async onSubmit() {
    const { studentNo, password } = this.data
    if (!studentNo.trim() || !password.trim()) {
      wx.showToast({
        title: '请输入学号和密码',
        icon: 'none',
      })
      return
    }
    if (!/^\d+$/.test(studentNo.trim())) {
      wx.showToast({
        title: '学号只能填写数字',
        icon: 'none',
      })
      return
    }

    this.setData({ submitting: true })
    try {
      await login({
        studentNo: studentNo.trim(),
        password,
      })
      wx.switchTab({ url: '/pages/index/index' })
    } catch (error) {
      wx.showToast({
        title: error.message || '登录失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
