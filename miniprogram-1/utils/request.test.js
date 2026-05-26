const assert = require('node:assert/strict')
const test = require('node:test')

function loadRequestWithWx(wxMock) {
  global.wx = wxMock
  delete require.cache[require.resolve('./request')]
  return require('./request')
}

test('uses the deployed backend URL by default', () => {
  const { BASE_URL } = require('./config')

  assert.equal(BASE_URL, 'http://10.10.0.6')
})

test('maps request timeout to a clear backend connectivity message', async () => {
  const { request } = loadRequestWithWx({
    getStorageSync: () => '',
    request: (options) => {
      assert.equal(options.timeout, 10000)
      options.fail({ errMsg: 'request:fail timeout' })
    },
  })

  await assert.rejects(
    request({ url: '/api/auth/login', method: 'POST', withAuth: false }),
    (error) => {
      assert.equal(error.message, '请求超时，请检查后端服务是否启动或 BASE_URL 是否正确')
      assert.equal(error.errMsg, 'request:fail timeout')
      return true
    }
  )
})
