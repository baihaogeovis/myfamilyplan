/**
 * 底部弹层关闭防穿透。
 *
 * 蒙层用 wx:if / a:if 在 tap 回调里立刻拆掉时，同一次触摸会落到
 * 下面的悬浮按钮或列表上，弹层会再滑出来，然后又关上。
 *
 * closeModal 先关掉内容，蒙层再留一小段时间（modalHold），并忽略打开。
 * 新弹层请沿用：
 *   <view class="modal-mask {{showForm ? '' : 'is-closing'}}"
 *         wx:if="{{showForm || modalHold}}" catchtap="closeForm">
 *     <view class="modal-body" wx:if="{{showForm}}" catchtap="noop">
 *   打开入口第一行：if (!modal.canOpen(this)) return
 *   关闭：modal.closeModal(this, { showForm: false })
 * 支付宝把 wx:if / catchtap 换成 a:if / catchTap。
 */
const CLOSE_LOCK_MS = 320

function closeModal(page, patch) {
  const flags = Object.keys(patch).filter((key) => key !== 'modalHold')
  const wasOpen = flags.some((key) => page.data && page.data[key])
  page._modalLockUntil = Date.now() + CLOSE_LOCK_MS
  // 蒙层还在时，穿透过来的第二次 close 只续锁，不能取消卸蒙层的定时器。
  if (!wasOpen) return

  if (page._modalHoldTimer) {
    clearTimeout(page._modalHoldTimer)
    page._modalHoldTimer = null
  }
  const generation = (page._modalGen || 0) + 1
  page._modalGen = generation
  page.setData(Object.assign({}, patch, { modalHold: true }))
  page._modalHoldTimer = setTimeout(() => {
    page._modalHoldTimer = null
    if (page._modalGen !== generation) return
    page.setData({ modalHold: false })
  }, CLOSE_LOCK_MS)
}

function canOpen(page) {
  return !(page._modalLockUntil && Date.now() < page._modalLockUntil)
}

module.exports = {
  CLOSE_LOCK_MS,
  closeModal,
  canOpen
}
