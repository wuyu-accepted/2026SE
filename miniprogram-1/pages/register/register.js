const { register } = require('../../utils/auth')

Page({
  data: {
    form: {
      realName: '',
      studentNo: '',
      password: '',
      confirmPassword: '',
      grade: '',
      major: '',
      className: '',
      authType: 'student',
      bio: '',
      phone: '',
      email: '',
      hometown: '',
      dormitory: '',
    },
    authTypeOptions: [
      { label: '普通学生', value: 'student' },
      { label: '学生骨干', value: 'cadre' },
    ],
    authTypeIndex: 0,
    submitting: false,
  },

  onFieldInput(event) {
    const { field } = event.currentTarget.dataset
    this.setData({
      [`form.${field}`]: event.detail.value,
    })
  },

  onAuthTypeChange(event) {
    const authTypeIndex = Number(event.detail.value)
    this.setData({
      authTypeIndex,
      'form.authType': this.data.authTypeOptions[authTypeIndex].value,
    })
  },

  async onSubmit() {
    const { form } = this.data
    if (!form.realName.trim() || !form.studentNo.trim() || !form.password || !form.confirmPassword) {
      wx.showToast({
        title: '请先填写姓名、学号和密码',
        icon: 'none',
      })
      return
    }
    if (form.password.length < 6) {
      wx.showToast({
        title: '密码至少 6 位',
        icon: 'none',
      })
      return
    }
    if (form.password !== form.confirmPassword) {
      wx.showToast({
        title: '两次输入的密码不一致',
        icon: 'none',
      })
      return
    }
    if (form.email.trim() && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
      wx.showToast({
        title: '邮箱格式不正确',
        icon: 'none',
      })
      return
    }

    this.setData({ submitting: true })
    try {
      await register({
        ...form,
        realName: form.realName.trim(),
        studentNo: form.studentNo.trim(),
        grade: form.grade.trim(),
        major: form.major.trim(),
        className: form.className.trim(),
        bio: form.bio.trim(),
        phone: form.phone.trim(),
        email: form.email.trim(),
        hometown: form.hometown.trim(),
        dormitory: form.dormitory.trim(),
      })

      wx.showToast({
        title: '注册成功',
        icon: 'success',
      })
      setTimeout(() => {
        wx.switchTab({ url: '/pages/index/index' })
      }, 300)
    } catch (error) {
      wx.showToast({
        title: error.message || '注册失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },
})
