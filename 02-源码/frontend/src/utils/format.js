// 全局时间格式化：全系统统一调用，避免各页面各写一套
export function formatTime(v, withTime = true) {
  if (!v) return '-'
  const d = typeof v === 'string' ? new Date(v.replace(/-/g, '/').replace('T', ' ')) : new Date(v)
  if (isNaN(d.getTime())) return String(v)
  const p = (n) => String(n).padStart(2, '0')
  const date = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
  return withTime ? `${date} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}` : date
}

export function formatDate(v) {
  return formatTime(v, false)
}

export function money(v) {
  if (v === null || v === undefined || v === '') return '-'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}

export const batchStatusTag = (s) => ({
  '计划中': 'tag-plan', '种植中': 'tag-grow', '采收中': 'tag-harv', '已结束': 'tag-done'
}[s] || 'tag-plan')

export const taskStatusTag = (s) => ({
  '待派工': 'tag-plan', '待执行': 'tag-grow', '待复核': 'tag-harv', '已完成': 'tag-done'
}[s] || 'tag-plan')
