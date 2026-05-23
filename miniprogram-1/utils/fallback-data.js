const noticeFallback = [
  { id: 1, noticeId: 1, title: '学院综合服务平台上线试运行', summary: '可在首页查看通知、待办和知识库入口。学院综合服务平台已进入试运行阶段，欢迎同学们体验并反馈建议。', readStatus: 0, pinnedStatus: 0, date: '2026-04-01' },
  { id: 2, noticeId: 2, title: '本周提交思想汇报提醒', summary: '积极分子同学请按时提交本周思想汇报，如有问题可在知识库查看填写说明。', readStatus: 0, pinnedStatus: 0, date: '2026-04-05' },
  { id: 3, noticeId: 3, title: '关于2026春季学期学生事务办理时间安排的通知', summary: '请同学们按时间节点办理学籍异动、缓考与证明申请等事务。', readStatus: 0, pinnedStatus: 0, date: '2026-04-10' },
]

const noticeDetailFallback = {
  id: 1,
  noticeId: 1,
  title: '学院综合服务平台上线试运行',
  summary: '可在首页查看通知、待办和知识库入口。',
  content: '学院综合服务平台已进入试运行阶段。平台整合了通知公告、知识库查询、党团事务办理、请假审批等常用功能。欢迎同学们积极使用并提供反馈建议。',
  noticeType: '系统公告',
  tag: '平台公告',
  priority: 1,
  publishTime: '2026-04-01 09:00:00',
  readStatus: 0,
}

const profileFallback = {
  realName: '测试学生',
  studentNo: 'stu1',
  phone: '13900000001',
  email: 'stu1@example.com',
  grade: '2023本',
  major: '计算机科学与技术',
  className: '2023级1班',
  authType: 'student',
  bio: '',
  hometown: '',
  dormitory: '',
  politicalStatus: '共青团员',
}

const partyProgressFallback = {
  currentStageName: '未开始',
  currentStageCode: '',
  stages: [],
  guidances: [],
}

module.exports = {
  noticeFallback,
  noticeDetailFallback,
  profileFallback,
  partyProgressFallback,
}
