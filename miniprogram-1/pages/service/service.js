Page({
  data: {
    blocks: [
      {
        title: '党团与思政',
        subtitle: '党团流程追踪与党员学习支持',
        items: [
          {
            code: 'partyProgress',
            name: '入党流程追踪',
            icon: '🧭',
            iconClass: 'icon-blue',
            path: '/pages/party-progress/party-progress',
          },
          {
            code: 'partyReport',
            name: '思想汇报提交',
            icon: '📝',
            iconClass: 'icon-indigo',
            path: '/pages/party-report/party-report',
          },
          {
            code: 'partyActivity',
            name: '党团活动申请',
            icon: '🗓️',
            iconClass: 'icon-cyan',
            path: '/pages/party-activity/party-activity',
          },
        ],
      },
      {
        title: '综合办事',
        subtitle: '常用办事申请与政策查询',
        items: [
          {
            code: 'certificate',
            name: '电子证明生成',
            icon: '🪪',
            iconClass: 'icon-teal',
            path: '/pages/e-certificate/e-certificate',
          },
          {
            code: 'leave',
            name: '请假审批流程',
            icon: '📝',
            iconClass: 'icon-orange',
            path: '/pages/leave-list/leave-list',
          },
          {
            code: 'policyKnowledge',
            name: '政策知识库',
            icon: '📖',
            iconClass: 'icon-cyan',
            path: '/pages/policy-knowledge/policy-knowledge',
          },
        ],
      },
      {
        title: '个人与成长',
        subtitle: '成长分析、画像与荣誉记录',
        items: [
          {
            code: 'studyAnalysis',
            name: '学业分析与预警',
            icon: '📈',
            iconClass: 'icon-green',
            path: '/pages/study-analysis/study-analysis',
          },
          {
            code: 'portrait',
            name: '学生画像',
            icon: '🧑‍🎓',
            iconClass: 'icon-purple',
            path: '/pages/student-portrait/student-portrait',
          },
          {
            code: 'honor',
            name: '奖励荣誉',
            icon: '🏅',
            iconClass: 'icon-pink',
            path: '/pages/honor/honor',
          },
        ],
      },
    ],
  },

  onEntryTap(event) {
    const { path } = event.currentTarget.dataset
    if (path) {
      wx.navigateTo({ url: path })
      return
    }
    wx.showToast({
      title: '功能建设中',
      icon: 'none',
    })
  },
})
