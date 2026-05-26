const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const test = require('node:test')

const projectRoot = path.resolve(__dirname, '../..')

test('registers the AI chat page in app.json', () => {
  const appJson = JSON.parse(fs.readFileSync(path.join(projectRoot, 'app.json'), 'utf8'))

  assert.ok(appJson.pages.includes('pages/ai-chat/ai-chat'))
})

test('renders the home AI assistant floating entry', () => {
  const wxml = fs.readFileSync(path.join(__dirname, 'index.wxml'), 'utf8')

  assert.match(wxml, /class="ai-fab"/)
  assert.match(wxml, /bindtap="openAiAssistant"/)
})
