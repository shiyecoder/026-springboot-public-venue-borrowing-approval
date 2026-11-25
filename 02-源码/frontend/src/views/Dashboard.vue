<template>
  <div class="page dash">
    <div class="filter-bar">
      <span class="lbl">统计范围</span>
      <el-select v-model="range" style="width:180px" @change="load">
        <el-option label="最近 30 天" value="30d" /><el-option label="本月" value="month" />
        <el-option label="本季度" value="quarter" /><el-option label="本年" value="year" />
      </el-select>
      <span class="hint">数据更新于 {{ formatTime(d.updateTime) }}</span>
      <div style="flex:1"></div>
      <el-button @click="$router.push('/venue')">场地明细</el-button>
      <el-button type="primary" @click="$router.push('/apply')">申请明细</el-button>
    </div>

    <div class="kpi-row">
      <div class="kpi"><div class="t">场地利用率</div><div class="v">{{ d.utilization }}<span class="u">%</span></div><div class="d">已完成借用 / 可用档期</div></div>
      <div class="kpi"><div class="t">申请量</div><div class="v">{{ d.applyCount }}</div><div class="d">全部借用申请</div></div>
      <div class="kpi"><div class="t">冲突与驳回</div><div class="v">{{ d.conflictCount }}</div><div class="d">时段冲突与超容驳回</div></div>
      <div class="kpi"><div class="t">取消率</div><div class="v">{{ d.cancelRate }}<span class="u">%</span></div><div class="d">申请人主动取消</div></div>
      <div class="kpi"><div class="t">准备任务完成率</div><div class="v">{{ d.prepDoneRate }}<span class="u">%</span></div><div class="d">验收通过占比</div></div>
      <div class="kpi"><div class="t">热门场地</div><div class="v small">{{ hot?.name || '-' }}</div><div class="d">借用 {{ hot?.value || 0 }} 次</div></div>
    </div>

    <div class="chart-grid">
      <div class="card"><div class="chart-t">近 6 个月申请趋势</div><div ref="c1" class="chart-box"></div></div>
      <div class="card"><div class="chart-t">申请状态分布</div><div ref="c2" class="chart-box"></div></div>
      <div class="card"><div class="chart-t">活动类型分布</div><div ref="c3" class="chart-box"></div></div>
    </div>
    <div class="chart-grid">
      <div class="card"><div class="chart-t">院系使用量 Top5</div><div ref="c4" class="chart-box"></div></div>
      <div class="card"><div class="chart-t">审批时效（小时）</div><div ref="c5" class="chart-box"></div></div>
      <div class="card"><div class="chart-t">准备清单结果分布 · 采纳率 {{ d.adoptRate }}%</div><div ref="c6" class="chart-box"></div></div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { venueApi } from '../api'
import { formatTime } from '../utils/format'

const range = ref('30d')
const d = ref({})
const hot = computed(() => (d.value.hotVenue || [])[0])
const refs = {}
const charts = {}
const c1 = ref(null), c2 = ref(null), c3 = ref(null), c4 = ref(null), c5 = ref(null), c6 = ref(null)
const COLOR = ['#174A5C', '#2A7F86', '#316B74', '#8FB3B3', '#247A58', '#B4761F', '#A62B32']

const init = (el, key) => {
  if (!el) return null
  if (!charts[key]) charts[key] = echarts.init(el)
  return charts[key]
}
const draw = () => {
  const D = d.value
  init(c1.value, 'c1')?.setOption({
    grid: { left: 34, right: 12, top: 24, bottom: 24 },
    tooltip: { trigger: 'axis' }, legend: { right: 0, top: 0, itemHeight: 8, textStyle: { fontSize: 11 } },
    xAxis: { type: 'category', data: (D.trend || []).map((x) => x.month.slice(5) + '月'), axisLine: { lineStyle: { color: '#DCE7E7' } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF5F5' } }, axisLabel: { fontSize: 11 } },
    series: [
      { name: '申请量', type: 'line', smooth: true, data: (D.trend || []).map((x) => x.apply), itemStyle: { color: '#2A7F86' }, areaStyle: { color: 'rgba(42,127,134,.10)' } },
      { name: '完成量', type: 'line', smooth: true, data: (D.trend || []).map((x) => x.done), itemStyle: { color: '#174A5C' }, lineStyle: { type: 'dashed' } }
    ]
  })
  init(c2.value, 'c2')?.setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{ type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'], label: { show: false },
      data: (D.statusDist || []).map((x, i) => ({ ...x, itemStyle: { color: COLOR[i % COLOR.length] } })) }]
  })
  init(c3.value, 'c3')?.setOption({
    grid: { left: 40, right: 12, top: 18, bottom: 24 }, tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (D.typeDist || []).map((x) => x.name), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF5F5' } }, axisLabel: { fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 24, data: (D.typeDist || []).map((x, i) => ({ value: x.value, itemStyle: { color: COLOR[i % 3] } })) }]
  })
  init(c4.value, 'c4')?.setOption({
    grid: { left: 96, right: 24, top: 12, bottom: 12 }, tooltip: {},
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF5F5' } }, axisLabel: { fontSize: 11 } },
    yAxis: { type: 'category', data: (D.deptTop || []).map((x) => x.name).reverse(), axisLabel: { fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 14, itemStyle: { color: '#2A7F86' },
      data: (D.deptTop || []).map((x) => x.value).reverse() }]
  })
  init(c5.value, 'c5')?.setOption({
    grid: { left: 40, right: 12, top: 18, bottom: 24 }, tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: (D.efficiency || []).map((x) => x.node), axisLabel: { fontSize: 11 } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#EEF5F5' } }, axisLabel: { fontSize: 11 } },
    series: [{ type: 'bar', barWidth: 30, data: (D.efficiency || []).map((x) => x.hours),
      itemStyle: { color: '#316B74' }, label: { show: true, position: 'top', fontSize: 11 } }]
  })
  init(c6.value, 'c6')?.setOption({
    tooltip: { trigger: 'item' }, legend: { bottom: 0, itemHeight: 8, textStyle: { fontSize: 11 } },
    series: [{ type: 'pie', roseType: 'radius', radius: ['26%', '64%'], center: ['50%', '44%'], label: { fontSize: 11, color: '#5D7578' },
      data: (D.prepDist || []).map((x, i) => ({ ...x, itemStyle: { color: COLOR[i % COLOR.length] } })) }]
  })
}
const load = async () => { d.value = await venueApi.dashboard(); draw() }
const resize = () => Object.values(charts).forEach((c) => c.resize())

onMounted(async () => { await load(); window.addEventListener('resize', resize) })
onBeforeUnmount(() => { window.removeEventListener('resize', resize); Object.values(charts).forEach((c) => c.dispose()) })
</script>

<style scoped>
.dash{gap:10px}
.kpi .v .u{font-size:13px;margin-left:2px}
.kpi .v.small{font-size:15px;line-height:32px}
.chart-grid{gap:10px}
.chart-box{height:212px}
</style>
