const { ensureLogin, logout, setCurrentUser } = require('../../utils/auth')
const { request } = require('../../utils/request')

const GRADE_PATTERN = /^\d{4}[本硕博]$/
const GRADE_MESSAGE = '年级格式如2023本/2022硕/2023博'

const TAB_PAGES = [
  '/pages/index/index',
  '/pages/notice/notice',
  '/pages/service/service',
  '/pages/profile/profile',
]

Page({
  data: {
    loading: false,
    saving: false,
    authTypeOptions: [
      { label: '普通学生', value: 'student' },
      { label: '学生骨干', value: 'cadre' },
    ],
    authTypeIndex: 0,
    profile: {
      realName: '',
      studentNo: '',
      phone: '',
      email: '',
      grade: '',
      major: '',
      className: '',
      authType: 'student',
      bio: '',
      hometown: '',
      dormitory: '',
      politicalStatus: '',
    },
    shortcuts: [
      {
        title: '我的请假',
        desc: '查看请假申请与审批进度',
        path: '/pages/leave-list/leave-list',
      },
      {
        title: '通知',
        desc: '查看通知消息与未读提醒',
        path: '/pages/notice/notice',
      },
      {
        title: '服务',
        desc: '请假、党团事务与模板下载入口',
        path: '/pages/service/service',
      },
    ],
  },

  onShow() {
    this.loadProfile()
  },

  async loadProfile() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const profile = await request({ url: '/api/student/profile' })
      const authTypeIndex = this.data.authTypeOptions.findIndex((item) => item.value === profile.authType)

      this.setData({
        profile,
        authTypeIndex: authTypeIndex >= 0 ? authTypeIndex : 0,
      })
    } catch (error) {
      if (error.code !== 'NOT_LOGGED_IN') {
        wx.showToast({
          title: error.message || '加载失败',
          icon: 'none',
        })
      }
    } finally {
      this.setData({ loading: false })
    }
  },

  onFieldInput(event) {
    const { field } = event.currentTarget.dataset
    this.setData({
      [`profile.${field}`]: event.detail.value,
    })
  },

  async onSave() {
    const { profile } = this.data
    if (!profile.realName.trim()) {
      wx.showToast({
        title: '姓名不能为空',
        icon: 'none',
      })
      return
    }
    if (!GRADE_PATTERN.test((profile.grade || '').trim())) {
      wx.showToast({
        title: GRADE_MESSAGE,
        icon: 'none',
      })
      return
    }

    this.setData({ saving: true })
    try {
      const savedProfile = await request({
        url: '/api/student/profile',
        method: 'PUT',
        data: {
          realName: profile.realName.trim(),
          phone: (profile.phone || '').trim(),
          email: (profile.email || '').trim(),
          grade: (profile.grade || '').trim(),
          major: (profile.major || '').trim(),
          className: (profile.className || '').trim(),
          politicalStatus: (profile.politicalStatus || '').trim(),
          bio: (profile.bio || '').trim(),
          hometown: (profile.hometown || '').trim(),
          dormitory: (profile.dormitory || '').trim(),
        },
      })

      this.setData({ profile: savedProfile })
      const currentUser = getApp().globalData.userInfo || {}
      setCurrentUser({
        ...currentUser,
        realName: savedProfile.realName,
        studentNo: savedProfile.studentNo,
        authType: savedProfile.authType,
        className: savedProfile.className,
        avatarUrl: savedProfile.avatarUrl || '',
      })

      wx.showToast({
        title: '保存成功',
        icon: 'success',
      })
    } catch (error) {
      wx.showToast({
        title: error.message || '保存失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saving: false })
    }
  },

  onShortcutTap(event) {
    const { path } = event.currentTarget.dataset
    if (!path) {
      return
    }

    const navigate = TAB_PAGES.includes(path) ? wx.switchTab : wx.navigateTo
    navigate({ url: path })
  },

  onLogout() {
    logout()
  },
})
