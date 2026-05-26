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
    this.setData({ password: String(event.detail.value || '') })
  },

  handleLogin(event) {
    return this.onSubmit(event)
  },

  async onSubmit(event) {
    if (this.data.submitting) {
      return
    }
    const formValues = (event && event.detail && event.detail.value) || {}
    const studentNo = String(formValues.studentNo || this.data.studentNo || '').replace(/\D/g, '').trim()
    const password = String(formValues.password || this.data.password || '')

    this.setData({ studentNo, password })

    if (!studentNo || !password.trim()) {
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
        studentNo,
        password: password.trim(),
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
