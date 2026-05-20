const { request } = require('../../utils/request')
const { BASE_URL, TOKEN_KEY } = require('../../utils/config')

Page({
  data: {
    form: {
      title: '',
      content: '',
    },
    fileName: '',
    pickedFilePath: '',
    localFilePath: '',
    uploading: false,
    submitting: false,
    loadingList: false,
    reports: [],
  },

  onShow() {
    this.loadReports()
  },

  onTitleInput(e) {
    this.setData({ 'form.title': e.detail.value })
  },

  onContentInput(e) {
    this.setData({ 'form.content': e.detail.value })
  },

  onChooseFile() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      success: (res) => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) {
          return
        }
        const name = file.name || ''
        const lower = name.toLowerCase()
        if (!(lower.endsWith('.pdf') || lower.endsWith('.doc') || lower.endsWith('.docx'))) {
          wx.showToast({ title: '仅支持 PDF/Word', icon: 'none' })
          return
        }
        const pickedPath = file.path
        this.setData({
          fileName: name,
          pickedFilePath: pickedPath,
          localFilePath: '',
        })

        const fs = wx.getFileSystemManager()
        const ext = lower.lastIndexOf('.') >= 0 ? lower.slice(lower.lastIndexOf('.')) : ''
        const safeExt = ext || '.bin'
        const destPath = `${wx.env.USER_DATA_PATH}/party-report-${Date.now()}-${Math.floor(Math.random() * 100000)}${safeExt}`

        wx.saveFile({
          tempFilePath: pickedPath,
          success: (saveRes) => {
            this.setData({ localFilePath: saveRes.savedFilePath })
          },
          fail: () => {
            fs.access({
              path: pickedPath,
              success: () => {
                fs.copyFile({
                  srcPath: pickedPath,
                  destPath,
                  success: () => this.setData({ localFilePath: destPath }),
                  fail: (err) => {
                    this.setData({ fileName: '', pickedFilePath: '', localFilePath: '' })
                    wx.showToast({ title: (err && err.errMsg) || '文件不可用，请重新选择', icon: 'none' })
                  },
                })
              },
              fail: (err) => {
                this.setData({ fileName: '', pickedFilePath: '', localFilePath: '' })
                wx.showToast({ title: (err && err.errMsg) || '文件不存在，请重新选择', icon: 'none' })
              },
            })
          },
        })
      },
      fail: (err) => {
        wx.showToast({ title: (err && err.errMsg) || '选择文件失败', icon: 'none' })
      },
    })
  },

  async uploadAttachmentIfNeeded() {
    const localPath = this.data.localFilePath || this.data.pickedFilePath
    if (!localPath) {
      return null
    }
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      throw new Error('未登录')
    }
    this.setData({ uploading: true })
    try {
      const uploadRes = await new Promise((resolve, reject) => {
        wx.uploadFile({
          url: `${BASE_URL}/api/files/upload?bizType=report`,
          filePath: localPath,
          name: 'file',
          header: { Authorization: token },
          success: (res) => {
            try {
              const payload = JSON.parse(res.data || '{}')
              if (res.statusCode >= 200 && res.statusCode < 300 && payload.code === 0) {
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
      return uploadRes && uploadRes.id ? uploadRes.id : null
    } finally {
      this.setData({ uploading: false })
    }
  },

  async onSubmit() {
    const title = String(this.data.form.title || '').trim()
    const content = String(this.data.form.content || '').trim()
    if (!title) {
      wx.showToast({ title: '标题不能为空', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const fileId = await this.uploadAttachmentIfNeeded()
      await request({
        url: '/api/party/me/reports',
        method: 'POST',
        data: {
          title,
          content: content || null,
          fileId,
        },
      })
      wx.showToast({ title: '提交成功', icon: 'success' })
      this.setData({
        form: { title: '', content: '' },
        fileName: '',
        localFilePath: '',
      })
      this.loadReports()
    } catch (error) {
      wx.showToast({ title: error.message || '提交失败', icon: 'none' })
    } finally {
      this.setData({ submitting: false })
    }
  },

  async loadReports() {
    this.setData({ loadingList: true })
    try {
      const list = await request({ url: '/api/party/me/reports' })
      const mapped = (Array.isArray(list) ? list : []).map((item) => ({
        ...item,
        statusText: this.statusText(item.status),
      }))
      this.setData({ reports: mapped })
    } catch (error) {
      wx.showToast({ title: error.message || '加载失败', icon: 'none' })
    } finally {
      this.setData({ loadingList: false })
    }
  },

  statusText(status) {
    if (status === 0) return '待审核'
    if (status === 1) return '已通过'
    if (status === 2) return '已驳回'
    return '未知'
  },

  onOpenAttachment(e) {
    const fileId = e.currentTarget.dataset.fileid
    if (!fileId) {
      return
    }
    const token = wx.getStorageSync(TOKEN_KEY)
    if (!token) {
      wx.showToast({ title: '未登录', icon: 'none' })
      return
    }
    wx.downloadFile({
      url: `${BASE_URL}/api/files/${fileId}/download`,
      header: { Authorization: token },
      success: (res) => {
        if (res.statusCode !== 200) {
          wx.showToast({ title: '下载失败', icon: 'none' })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          showMenu: true,
          fail: () => wx.showToast({ title: '打开失败', icon: 'none' }),
        })
      },
      fail: () => wx.showToast({ title: '下载失败', icon: 'none' }),
    })
  },
})
