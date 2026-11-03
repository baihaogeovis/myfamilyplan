/**
 * 小程序底部弹层关闭锁。node scripts/modal-guard.test.js
 */
const assert = require('assert')
const fs = require('fs')
const path = require('path')

const root = path.resolve(__dirname, '..')
const wechatUtil = fs.readFileSync(path.join(root, 'wechat-miniprogram/utils/modal.js'), 'utf8')
const alipayUtil = fs.readFileSync(path.join(root, 'alipay-miniprogram/utils/modal.js'), 'utf8')
assert.strictEqual(wechatUtil, alipayUtil, 'WeChat and Alipay modal helpers diverged')

const modal = require('../wechat-miniprogram/utils/modal')

function createPage(data) {
  return {
    data: Object.assign({}, data),
    updates: [],
    setData(patch) {
      Object.assign(this.data, patch)
      this.updates.push(Object.assign({}, patch))
    }
  }
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

async function main() {
  const open = createPage({ showForm: true, modalHold: false })
  modal.closeModal(open, { showForm: false })
  assert.strictEqual(open.data.showForm, false)
  assert.strictEqual(open.data.modalHold, true)
  assert.strictEqual(modal.canOpen(open), false)

  // 蒙层还在时的第二次 close 不能把 modalHold 卡住。
  modal.closeModal(open, { showForm: false })
  assert.strictEqual(open.data.modalHold, true)

  await sleep(modal.CLOSE_LOCK_MS + 40)
  assert.strictEqual(open.data.modalHold, false)
  assert.strictEqual(modal.canOpen(open), true)

  const closed = createPage({ showPwd: false, modalHold: false })
  modal.closeModal(closed, { showPwd: false })
  assert.strictEqual(closed.data.modalHold, false, 'closing an already closed dialog must not raise the mask')
  assert.strictEqual(modal.canOpen(closed), false)

  const pages = [
    'approval', 'budget', 'fixedExpense', 'growthPlan', 'lifePlan',
    'members', 'profile', 'record', 'reward', 'workbench'
  ]
  for (const platform of [
    { dir: 'wechat-miniprogram', markup: 'wxml', catchTap: 'catchtap', ifAttr: 'wx:if' },
    { dir: 'alipay-miniprogram', markup: 'axml', catchTap: 'catchTap', ifAttr: 'a:if' }
  ]) {
    for (const page of pages) {
      const base = path.join(root, platform.dir, 'pages', page, page)
      const js = fs.readFileSync(base + '.js', 'utf8')
      const markup = fs.readFileSync(base + '.' + platform.markup, 'utf8')
      assert.ok(js.includes("require('../../utils/modal')"), page + ' missing helper')
      assert.ok(js.includes('modal.closeModal'), page + ' missing closeModal')
      assert.ok(js.includes('modal.canOpen'), page + ' missing canOpen')
      assert.ok(!/this\.setData\(\{ show(?:Form|Pwd|ItemForm): false \}\)/.test(js), page + ' still closes with raw setData')
      assert.ok(markup.includes('modalHold'), page + ' mask is still destroyed immediately')
      assert.ok(markup.includes('is-closing'), page + ' missing closing class')
      assert.ok(markup.includes(platform.catchTap + '="close'), page + ' mask still uses bubble tap')
      assert.ok(!markup.includes(platform.ifAttr + '="{{!showForm}}"'), page + ' FAB should stay mounted')
    }
  }

  console.log('modal guard ok')
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
