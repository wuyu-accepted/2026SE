const { ensureLogin, logout, setCurrentUser } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

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
    avatarTempPath: '',
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
      this.refreshAvatarPreview(profile.avatarUrl)
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

  refreshAvatarPreview(avatarUrl) {
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!avatarUrl || !token) {
      this.setData({ avatarTempPath: '' })
      return
    }
    const url = avatarUrl.startsWith('http') ? avatarUrl : `${BASE_URL}${avatarUrl}`
    wx.downloadFile({
      url,
      header: { Authorization: token },
      success: (res) => {
        if (res.statusCode === 200) {
          this.setData({ avatarTempPath: res.tempFilePath })
        }
      },
    })
  },

  onChangeAvatar() {
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const filePath = (res.tempFilePaths || [])[0]
        if (!filePath) {
          return
        }
        const token = wx.getStorageSync(TOKEN_KEY)
        if (!token) {
          wx.showToast({ title: '未登录', icon: 'none' })
          return
        }
        this.setData({ saving: true })
        try {
          await ensureLogin()
          const profile = await new Promise((resolve, reject) => {
            wx.uploadFile({
              url: `${BASE_URL}/api/student/profile/avatar`,
              filePath,
              name: 'file',
              header: { Authorization: token },
              success: (uploadRes) => {
                try {
                  const payload = JSON.parse(uploadRes.data || '{}')
                  if (uploadRes.statusCode >= 200 && uploadRes.statusCode < 300 && payload.code === 0) {
                    resolve(payload.data)
                    return
                  }
                  reject(new Error(payload.message || '上传失败'))
                } catch (e) {
                  reject(new Error('上传失败'))
                }
              },
              fail: (err) => reject(new Error((err && err.errMsg) || '上传失败')),
            })
          })

          this.setData({ profile })
          this.refreshAvatarPreview(profile.avatarUrl)

          const currentUser = getApp().globalData.userInfo || {}
          setCurrentUser({
            ...currentUser,
            realName: profile.realName,
            studentNo: profile.studentNo,
            authType: profile.authType,
            className: profile.className,
            avatarUrl: profile.avatarUrl || '',
          })

          wx.showToast({ title: '头像已更新', icon: 'success' })
        } catch (error) {
          wx.showToast({ title: error.message || '上传失败', icon: 'none' })
        } finally {
          this.setData({ saving: false })
        }
      },
      fail: (err) => {
        if (err && err.errMsg) {
          wx.showToast({ title: err.errMsg, icon: 'none' })
        }
      },
    })
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
      this.refreshAvatarPreview(savedProfile.avatarUrl)
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
    wx.showModal({
      title: '提示',
      content: '确定退出登录吗？',
      success(res) {
        if (res.confirm) {
          logout()
        }
      },
    })
  },
})
