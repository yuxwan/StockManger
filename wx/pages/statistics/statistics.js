const { get } = require('../../utils/request')

function pad(n) { return n < 10 ? '0' + n : '' + n }
function fmtDate(d) { return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) }
function num(n, frac) {
  return Number(n || 0).toLocaleString('zh-CN', { minimumFractionDigits: frac, maximumFractionDigits: frac })
}
function fmtMoney(n) {
  const v = Number(n || 0)
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  return v.toLocaleString('zh-CN')
}
function fmtMoney2(n) {
  return Number(n || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const RANGES = [
  { key: 'today', label: '今日' },
  { key: 'week', label: '本周' },
  { key: 'month', label: '本月' },
  { key: 'quarter', label: '本季度' },
  { key: 'year', label: '本年' }
]
const RANK_STYLE = {
  1: { bg: '#111111', fg: '#ffffff' },
  2: { bg: '#4b5563', fg: '#ffffff' },
  3: { bg: '#9ca3af', fg: '#ffffff' }
}

Page({
  data: {
    rangeNames: RANGES.map(r => r.label),
    rangeIndex: 0,
    refreshing: false,
    rangeLabel: '今日',
    week: [],
    weekTotalText: '¥--',
    top: [],
    today: { revenueText: '--', orders: '--', avgText: '--', ytdText: '--', ytdColor: '#a9a9ad' }
  },

  onReady() {
    this._trendMap = {}
    this.fetchBase()
    this.fetchBlack(RANGES[0])
  },

  // 基础数据（周图 + 热销）：不受范围筛选影响
  async fetchBase() {
    try {
      const res = await get('/reports/summary', { dateRange: 'week' })
      const trend = res.revenueTrend || []
      this._trendMap = {}
      trend.forEach(t => { this._trendMap[t.date] = Number(t.revenue || 0) })

      const week = []
      for (let i = 6; i >= 0; i--) {
        const d = new Date()
        d.setDate(d.getDate() - i)
        const key = fmtDate(d)
        week.push({ label: (d.getMonth() + 1) + '/' + d.getDate(), revenue: this._trendMap[key] || 0 })
      }
      const weekTotal = week.reduce((s, x) => s + x.revenue, 0)
      const top = (res.topProducts || []).slice(0, 5).map((t, i) => {
        const rank = i + 1
        return {
          rank,
          name: t.name,
          qtyText: '售出 ' + t.qty + ' 个',
          revenueText: fmtMoney2(t.revenue),
          bg: RANK_STYLE[rank] ? RANK_STYLE[rank].bg : '#f0f0f0',
          fg: RANK_STYLE[rank] ? RANK_STYLE[rank].fg : '#8e8e93'
        }
      })
      this.setData({ week, weekTotalText: '¥' + fmtMoney(weekTotal), top }, () => this.drawChart())
    } catch (e) { /* request 已 toast */ }
  },

  // scroll-view 下拉刷新：重拉基础数据 + 当前范围黑卡
  async onRefresh() {
    if (this.data.refreshing) return
    this.setData({ refreshing: true })
    try {
      await Promise.all([
        this.fetchBase(),
        this.fetchBlack(RANGES[this.data.rangeIndex])
      ])
    } finally {
      this.setData({ refreshing: false })
    }
  },

  // 范围筛选：仅作用于黑色汇总卡
  onRangeChange(e) {
    const rangeIndex = Number(e.detail.value)
    const range = RANGES[rangeIndex]
    if (!range || rangeIndex === this.data.rangeIndex) return
    this.setData({ rangeIndex, rangeLabel: range.label })
    this.fetchBlack(range)
  },

  async fetchBlack(range) {
    try {
      const res = await get('/reports/summary', { dateRange: range.key })
      const today = {
        revenueText: num(res.totalRevenue, 2),
        orders: num(res.totalOrders, 0),
        avgText: num(res.avgOrderValue, 0),
        ytdText: '—',
        ytdColor: '#a9a9ad'
      }
      // 「较昨日」仅在今日口径有意义，其它范围显示 —
      if (range.key === 'today') {
        const now = new Date()
        const yest = new Date()
        yest.setDate(yest.getDate() - 1)
        const todayRev = this._trendMap[fmtDate(now)] || 0
        const yestRev = this._trendMap[fmtDate(yest)] || 0
        if (yestRev > 0) {
          const diff = ((todayRev - yestRev) / yestRev) * 100
          today.ytdText = (diff >= 0 ? '+' : '') + diff.toFixed(1) + '%'
          today.ytdColor = diff > 0 ? '#4ade80' : diff < 0 ? '#f87171' : '#a9a9ad'
        } else if (todayRev > 0) {
          today.ytdText = '+100.0%'
          today.ytdColor = '#4ade80'
        } else {
          today.ytdText = '--'
        }
      }
      this.setData({ today })
    } catch (e) { /* request 已 toast */ }
  },

  drawChart() {
    const { week } = this.data
    if (!week.length) return
    this.createSelectorQuery()
      .select('#weekChart')
      .fields({ node: true, size: true })
      .exec((res) => {
        if (!res || !res[0] || !res[0].node) return
        const canvas = res[0].node
        const ctx = canvas.getContext('2d')
        const w = res[0].width
        const h = res[0].height
        const dpr = (wx.getWindowInfo ? wx.getWindowInfo() : wx.getSystemInfoSync()).pixelRatio || 2
        canvas.width = w * dpr
        canvas.height = h * dpr
        ctx.scale(dpr, dpr)
        ctx.clearRect(0, 0, w, h)

        const padL = 8
        const padR = 8
        const padT = 26
        const padB = 24
        const chartW = w - padL - padR
        const chartH = h - padT - padB
        const max = Math.max.apply(null, week.map(x => x.revenue).concat(1))
        const top = max * 1.2
        const n = week.length
        const slot = chartW / n
        const gap = 12
        const barW = Math.max(12, slot - gap)
        const todayIdx = n - 1

        week.forEach((x, i) => {
          const cx = padL + slot * i + slot / 2
          const bh = chartH * (x.revenue / top)
          const x0 = cx - barW / 2
          const y0 = padT + chartH - bh
          const isToday = i === todayIdx

          ctx.beginPath()
          if (isToday) ctx.fillStyle = '#111111'
          else if (x.revenue > 0) ctx.fillStyle = '#e4e4e7'
          else ctx.fillStyle = '#f4f4f5'
          roundRect(ctx, x0, y0, barW, bh, Math.min(barW / 2, 6))
          ctx.fill()

          if (x.revenue > 0) {
            ctx.fillStyle = isToday ? '#111111' : '#8e8e93'
            ctx.font = '10px sans-serif'
            ctx.textAlign = 'center'
            ctx.fillText(fmtMoney(x.revenue), cx, y0 - 6)
          }

          ctx.fillStyle = isToday ? '#111111' : '#8e8e93'
          ctx.font = '10px sans-serif'
          ctx.textAlign = 'center'
          ctx.fillText(x.label, cx, h - 8)
        })
      })
  },

  /** 转发给好友/群 */
  onShareAppMessage() {
    const t = this.data.today
    const range = this.data.rangeLabel || '今日'
    const title = (t && t.revenueText && t.revenueText !== '--')
      ? '经营统计 · ' + range + '营业额 ¥' + t.revenueText
      : '经营统计'
    return { title, path: '/pages/statistics/statistics' }
  }
})

function roundRect(ctx, x, y, w, h, r) {
  const rr = Math.min(r, h / 2)
  ctx.moveTo(x, y + h)
  ctx.lineTo(x, y + rr)
  ctx.arcTo(x, y, x + rr, y, rr)
  ctx.lineTo(x + w - rr, y)
  ctx.arcTo(x + w, y, x + w, y + rr, rr)
  ctx.lineTo(x + w, y + h)
  ctx.closePath()
}
