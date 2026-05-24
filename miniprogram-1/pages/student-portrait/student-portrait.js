const { ensureLogin } = require('../../utils/auth')
const { request } = require('../../utils/request')

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
  },

  onLoad() {
    this.loadProfile()
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
        },
      })
    } catch (error) {
      console.error('Load profile failed:', error)
    } finally {
      this.setData({ loading: false })
    }
  },
})
