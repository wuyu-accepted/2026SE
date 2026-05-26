const { BASE_URL, REQUEST_TIMEOUT, TOKEN_KEY } = require('./config')

function normalizeNetworkError(error) {
  const errMsg = (error && error.errMsg) || ''
  const isTimeout = /timeout/i.test(errMsg)
  const isDomainBlocked = /not in domain list/i.test(errMsg)

  return {
    message: isTimeout
      ? '请求超时，请检查后端服务是否启动或 BASE_URL 是否正确'
      : isDomainBlocked
        ? '当前小程序环境拦截了该请求：请在微信开发者工具开启“不校验合法域名”，或为正式版配置 HTTPS 合法 request 域名'
        : (errMsg || '网络请求失败'),
    ...error,
  }
}

function doRequest(options) {
  const {
    url,
    method = 'GET',
    data,
    header = {},
    withAuth = true,
    timeout,
  } = options

  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync(TOKEN_KEY)
    const requestHeader = { ...header }

    if (withAuth && token) {
      requestHeader.Authorization = token
    }

    wx.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header: requestHeader,
      timeout: timeout || REQUEST_TIMEOUT,
      success: (res) => {
        const response = res.data || {}

        if (res.statusCode >= 200 && res.statusCode < 300 && response.code === 0) {
          resolve(response.data)
          return
        }

        reject({
          statusCode: res.statusCode,
          message: response.message || '请求失败',
          ...response,
        })
      },
      fail: (error) => {
        reject(normalizeNetworkError(error))
      },
    })
  })
}

async function request(options) {
  try {
    return await doRequest(options)
  } catch (error) {
    const authExpired = error && (error.statusCode === 401 || error.code === 40100 || error.code === 40101)

    if (options.withAuth !== false && authExpired) {
      const { clearLoginState, redirectToLogin } = require('./auth')
      clearLoginState()
      redirectToLogin()
    }

    throw error
  }
}

module.exports = {
  request,
}
