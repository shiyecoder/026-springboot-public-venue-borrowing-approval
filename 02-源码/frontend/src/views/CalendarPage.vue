<template>
  <div class="page">
    <div class="filter-bar">
      <el-select v-model="venueId" placeholder="全部场地" clearable filterable style="width:240px" @change="load">
        <el-option v-for="v in venues" :key="v.id" :label="v.name" :value="v.id" />
      </el-select>
      <el-date-picker v-model="month" type="month" value-format="YYYY-MM" style="width:170px" @change="load" />
      <el-button @click="shift(-1)">上一月</el-button>
      <el-button @click="shift(1)">下一月</el-button>
      <div style="flex:1"></div>
      <div class="legend">
        <span><i style="background:#8FB3B3"></i>布场</span>
        <span><i style="background:#174A5C"></i>活动</span>
        <span><i style="background:#316B74"></i>撤场</span>
      </div>
    </div>

    <div class="card">
      <div class="cal">
        <div v-for="w in ['周一', '周二', '周三', '周四', '周五', '周六', '周日']" :key="w" class="hd">{{ w }}</div>
        <div v-for="(d, idx) in cells" :key="idx" class="day" :class="{ blank: !d.date, today: d.today }">
          <template v-if="d.date">
            <div class="d">{{ d.day }}</div>
            <div v-for="e in d.events" :key="e.key" class="ev" :class="e.kind" :title="`${e.no} ${e.title}`" @click="go(e.id)">
              {{ e.label }}
            </div>
          </template>
        </div>
      </div>
      <div class="hint" style="margin-top:12px">时段冲突校验范围为：布场开始 → 撤场结束；同场地已占用时段不可重复申请。</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { applyApi, venueApi } from '../api'

const router = useRouter()
const venues = ref([]); const venueId = ref(null)
const now = new Date()
const month = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const cells = ref([])

const pad = (n) => String(n).padStart(2, '0')
const dayKey = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`

const build = (events) => {
  const [y, m] = month.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const offset = (first.getDay() + 6) % 7 // 周一为第一列
  const days = new Date(y, m, 0).getDate()
  const todayStr = dayKey(new Date())
  const list = []
  for (let i = 0; i < offset; i++) list.push({ date: null })
  for (let d = 1; d <= days; d++) {
    const date = `${y}-${pad(m)}-${pad(d)}`
    const evs = []
    for (const e of events) {
      const s = (e.setup || e.start || '').slice(0, 10)
      const te = (e.teardown || e.end || '').slice(0, 10)
      const actS = (e.start || '').slice(0, 10)
      const actE = (e.end || '').slice(0, 10)
      if (s <= date && date < actS) evs.push({ ...e, key: e.id + 's', kind: 'setup', label: `布场 ${e.title}` })
      else if (actS <= date && date <= actE) evs.push({ ...e, key: e.id + 'a', kind: 'act', label: `活动 ${e.title}` })
      else if (actE < date && date <= te) evs.push({ ...e, key: e.id + 't', kind: 'out', label: `撤场 ${e.title}` })
    }
    list.push({ date, day: d, today: date === todayStr, events: evs })
  }
  cells.value = list
}
const load = async () => {
  const events = await applyApi.calendar({ venueId: venueId.value || undefined, month: month.value })
  build(events || [])
}
const shift = (n) => {
  const [y, m] = month.value.split('-').map(Number)
  const d = new Date(y, m - 1 + n, 1)
  month.value = `${d.getFullYear()}-${pad(d.getMonth() + 1)}`
  load()
}
const go = (id) => router.push(`/apply/${id}`)

onMounted(async () => { venues.value = await venueApi.options(); load() })
</script>

<style scoped>
.cal{display:grid;grid-template-columns:repeat(7,1fr);gap:8px}
.hd{font-size:12px;color:#5D7578;text-align:center;padding:4px 0}
.day{background:#fff;border:1px solid #DCE7E7;border-radius:6px;min-height:96px;padding:6px 7px}
.day.blank{background:#FAFCFC;border-style:dashed}
.day.today{border-color:#2A7F86;box-shadow:0 0 0 2px rgba(42,127,134,.12)}
.day .d{font-size:12px;color:#8CA3A5;font-variant-numeric:tabular-nums}
.ev{font-size:11.5px;border-radius:4px;padding:2px 5px;margin-top:5px;color:#fff;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;cursor:pointer}
.ev.setup{background:#8FB3B3}
.ev.act{background:#174A5C}
.ev.out{background:#316B74}
.legend{display:flex;gap:14px;font-size:12px;color:#5D7578}
.legend i{display:inline-block;width:10px;height:10px;border-radius:2px;margin-right:5px}
</style>
