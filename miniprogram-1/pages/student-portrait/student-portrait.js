const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

const infoFields = [
  { label: '学号', key: 'studentNo' },
  { label: '姓名', key: 'realName' },
  { label: '年级', key: 'grade' },
  { label: '专业', key: 'major' },
  { label: '班级', key: 'className' },
  { label: '手机号', key: 'phone' },
  { label: '邮箱', key: 'email' },
  { label: '政治面貌', key: 'politicalStatus' },
  { label: '身份', key: 'authType' },
  { label: '生源地', key: 'hometown' },
  { label: '宿舍', key: 'dormitory' },
]

Page({
  data: {
    loading: false,
    profile: null,
    infoFields,
    avatarTempPath: '',
    honorGroups: [],
    honorSaving: false,
    honorTerm: '',
    honorText: '',
    honorTermOptions: [],
  },

  onLoad(options) {
    this._targetSection = options && options.section ? String(options.section) : ''
    this.loadProfile()
    this.loadHonors()
    this.setData({ honorTermOptions: this.buildTermOptions() })
  },

  async loadProfile() {
    this.setData({ loading: true })
    try {
      await ensureLogin()
      const [me, profile] = await Promise.all([
        request({ url: '/api/auth/me' }).catch(() => ({})),
        request({ url: '/api/student/profile' }).catch(() => ({})),
      ])
      this.setData({
        profile: {
          realName: me.realName || profile.realName || '未设置',
          studentNo: me.studentNo || profile.studentNo || '未设置',
          grade: profile.grade || '未设置',
          major: profile.major || '未设置',
          className: profile.className || '未设置',
          phone: profile.phone || me.phone || '未设置',
          email: profile.email || me.email || '未设置',
          politicalStatus: profile.politicalStatus || '未设置',
          authType: profile.authType === 'cadre' ? '学生骨干' : profile.authType === 'student' ? '普通学生' : (profile.authType || '未设置'),
          hometown: profile.hometown || '未设置',
          dormitory: profile.dormitory || '未设置',
          bio: profile.bio || '',
          avatarUrl: profile.avatarUrl || '',
        },
      })
      this.refreshAvatarPreview(profile.avatarUrl)
    } catch (error) {
      console.error('Load profile failed:', error)
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

  async loadHonors() {
    try {
      await ensureLogin()
      const data = await request({ url: '/api/student/profile/honors' })
      const honorGroups = (Array.isArray(data) ? data : []).map((group) => ({
        term: group.term || '未归档',
        honors: (Array.isArray(group.honors) ? group.honors : []).map((item) => ({
          ...item,
          icon: '🏅',
        })),
      }))
      this.setData({ honorGroups })
      this.scrollToSectionIfNeeded()
    } catch (error) {
      console.error('Load honors failed:', error)
    }
  },

  scrollToSectionIfNeeded() {
    if (this._hasScrolled) return
    if (this._targetSection !== 'honor') return
    this._hasScrolled = true
    wx.pageScrollTo({
      selector: '#honor-section',
      duration: 300,
    })
  },

  onHonorInput(event) {
    this.setData({ honorText: event.detail.value || '' })
  },

  onHonorTermInput(event) {
    this.setData({ honorTerm: event.detail.value || '' })
  },

  onChooseHonorTerm() {
    const options = this.data.honorTermOptions || []
    if (!options.length) {
      return
    }
    wx.showActionSheet({
      itemList: options,
      success: (res) => {
        const index = res && typeof res.tapIndex === 'number' ? res.tapIndex : -1
        if (index >= 0 && index < options.length) {
          this.setData({ honorTerm: options[index] })
        }
      },
    })
  },

  async onAddHonor() {
    const content = String(this.data.honorText || '').trim()
    if (!content) {
      wx.showToast({ title: '请输入一句话描述', icon: 'none' })
      return
    }

    if (this.data.honorSaving) return
    this.setData({ honorSaving: true })
    try {
      await ensureLogin()
      await request({
        url: '/api/student/profile/honors',
        method: 'POST',
        data: {
          content,
          term: String(this.data.honorTerm || '').trim() || null,
        },
      })
      wx.showToast({ title: '已添加', icon: 'success' })
      this.setData({ honorText: '' })
      this.loadHonors()
    } catch (error) {
      wx.showToast({ title: error.message || '添加失败', icon: 'none' })
    } finally {
      this.setData({ honorSaving: false })
    }
  },

  onDeleteHonor(e) {
    const id = e.currentTarget.dataset.id
    if (!id) return
    wx.showModal({
      title: '提示',
      content: '确认删除该荣誉吗？',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await ensureLogin()
          await request({ url: `/api/student/profile/honors/${id}`, method: 'DELETE' })
          wx.showToast({ title: '已删除', icon: 'success' })
          this.loadHonors()
        } catch (error) {
          wx.showToast({ title: error.message || '删除失败', icon: 'none' })
        }
      },
    })
  },

  buildTermOptions() {
    const now = new Date()
    const year = now.getFullYear()
    const month = now.getMonth() + 1

    const academicStartYear = month >= 9 ? year : year - 1
    const years = [academicStartYear - 1, academicStartYear, academicStartYear + 1]
    const result = []
    years.forEach((startYear) => {
      result.push(`${startYear}-${startYear + 1}第一学期`)
      result.push(`${startYear}-${startYear + 1}第二学期`)
    })
    return result
  },
})
