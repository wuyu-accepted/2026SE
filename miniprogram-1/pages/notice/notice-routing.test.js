const assert = require('node:assert/strict')
const test = require('node:test')

function loadNoticePage() {
  let pageConfig
  global.Page = (config) => {
    pageConfig = config
  }
  global.wx = {
    navigateTo: (options) => {
      global.__lastNavigateTo = options
    },
    showToast: () => {},
  }
  delete require.cache[require.resolve('./notice.js')]
  require('./notice.js')
  return pageConfig
}

test('opens notice detail from list item index with message id and fallback', () => {
  const page = loadNoticePage()
  page.data = {
    messages: [
      {
        id: 42,
        noticeId: 7,
        title: '测试通知',
        summary: '详情页应拿到消息编号',
        readStatus: 0,
        pinnedStatus: 0,
        date: '2026-05-24',
      },
    ],
  }

  page.handleMessageTap({
    currentTarget: {
      dataset: {
        index: 0,
      },
    },
  })

  assert.match(global.__lastNavigateTo.url, /id=42/)
  assert.match(global.__lastNavigateTo.url, /fallback=/)
})
