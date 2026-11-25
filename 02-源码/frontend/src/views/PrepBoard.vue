<template>
  <div class="page">
    <div class="filter-bar">
      <el-select v-model="applyId" filterable placeholder="选择借用申请" style="width:360px" @change="onPick">
        <el-option v-for="r in applies" :key="r.id" :label="`${r.applyNo} · ${r.title}`" :value="r.id" />
      </el-select>
      <el-button type="primary" :loading="loading" :disabled="!applyId" @click="evaluate">执行计算</el-button>
      <el-button @click="ruleDlg = true">规则配置</el-button>
      <div style="flex:1"></div>
      <span class="hint">规则版本 {{ version }} · 采纳率 {{ board.adoptRate || 0 }}% · 人工调整率 {{ board.adjustRate || 0 }}%</span>
    </div>

    <div class="grid3">
      <div class="card">
        <div class="card-h"><h3>输入依据</h3><span class="hint">缺失项不参与计算</span></div>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="活动类型">{{ app.activityType || '-' }}</el-descriptions-item>
          <el-descriptions-item label="参与人数">{{ app.peopleCount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="借用场地">{{ app.venueName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="场地设备">{{ venue.equipment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="特殊需求">{{ app.needSecurity === 1 ? '用电搭建 / 外来人员 / 安保介入' : '无' }}</el-descriptions-item>
          <el-descriptions-item label="使用时段">{{ (app.startTime || '').slice(11, 16) || '-' }} 起</el-descriptions-item>
          <el-descriptions-item label="历史问题">{{ (app.id % 4 === 0) ? '近半年有 1 次噪音记录' : '无异常记录' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="card">
        <div class="card-h"><h3>计算结果</h3>
          <el-button link type="primary" :disabled="!score" @click="adjustDlg = true">人工修正</el-button></div>
        <div v-if="score">
          <div style="display:flex;align-items:baseline;gap:10px">
            <div class="num big">{{ score.totalScore }}</div>
            <span class="tag" :class="score.level === '准备不足' ? 'tag-err' : 'tag-done'">{{ score.manualLevel || score.level }}</span>
          </div>
          <div class="hint">规则 {{ score.ruleVersion }} · 计算于 {{ formatTime(score.createTime) }}</div>
          <div style="margin-top:12px">
            <div v-for="(v, k) in factors" :key="k" style="margin-bottom:9px">
              <div class="stat-line"><span>{{ nameOf(k) }}</span><span class="num">{{ v.score }} / {{ v.weight }}</span></div>
              <div class="bar"><i :style="{ width: pct(v) }"></i></div>
            </div>
          </div>
          <div v-if="score.manualLevel" class="hint" style="margin-top:10px">
            人工修正：{{ score.level }} → {{ score.manualLevel }}；{{ score.manualReason }}（{{ score.manualBy }}）
          </div>
        </div>
        <div v-else class="empty" style="padding:40px 0">选择申请后执行计算，生成准备清单</div>
      </div>

      <div class="card">
        <div class="card-h"><h3>生成的准备清单</h3><span class="hint">缺口按场地库存比对</span></div>
        <el-table :data="checklist" size="small" max-height="330">
          <el-table-column prop="name" label="准备项" min-width="110" />
          <el-table-column label="数量" width="86" align="right">
            <template #default="{ row }"><span class="num">{{ row.need }} {{ row.unit }}</span></template>
          </el-table-column>
          <el-table-column prop="source" label="来源" min-width="100" />
          <el-table-column label="缺口" width="100">
            <template #default="{ row }">
              <span class="tag" :class="row.gap === '可满足' ? 'tag-done' : row.gap === '缺' ? 'tag-err' : 'tag-harv'">
                {{ row.gap === '缺' ? `缺 ${row.shortage}` : row.gap }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="grid2">
      <div class="card">
        <div class="card-h"><h3>结果解释</h3><span class="hint">不只显示一个分数</span></div>
        <template v-if="score">
          <div class="group-title">主要影响因素</div>
          <p class="hint" style="font-size:12.5px;line-height:1.9">{{ explain }}</p>
          <div class="group-title" style="margin-top:12px">未满足条件</div>
          <p class="hint" style="font-size:12.5px;line-height:1.9">{{ score.unmet }}</p>
          <div class="group-title" style="margin-top:12px">可改进项</div>
          <p class="hint" style="font-size:12.5px;line-height:1.9">{{ score.advice }}</p>
          <div class="hint" style="margin-top:8px">输入快照：{{ score.inputSnapshot }}</div>
        </template>
        <div v-else class="empty" style="padding:36px 0">暂无计算结果</div>
      </div>

      <div class="card">
        <div class="card-h"><h3>准备结果分布与采纳情况</h3></div>
        <div class="stat-line"><span>累计计算次数</span><b class="num">{{ board.total || 0 }}</b></div>
        <div class="stat-line"><span>推荐采纳率</span><b class="num">{{ board.adoptRate || 0 }}%</b></div>
        <div class="stat-line"><span>人工调整率</span><b class="num">{{ board.adjustRate || 0 }}%</b></div>
        <el-divider style="margin:10px 0" />
        <div v-for="(v, k) in (board.distribution || {})" :key="k" style="margin-bottom:8px">
          <div class="stat-line"><span>{{ k }}</span><span class="num">{{ v }} 条</span></div>
          <div class="bar"><i :style="{ width: Math.min(100, (v / Math.max(1, board.total)) * 100) + '%' }"></i></div>
        </div>
        <div class="hint" style="margin-top:8px">同一输入与规则版本必定得到同一结果；修改权重只影响新计算。</div>
      </div>
    </div>

    <el-dialog v-model="adjustDlg" title="人工修正结果" width="520px">
      <el-form label-width="90px">
        <el-form-item label="原始等级"><b>{{ score?.level }}</b></el-form-item>
        <el-form-item label="修正为">
          <el-select v-model="adj.level" style="width:200px">
            <el-option v-for="l in ['准备充分', '准备较充分', '准备不足']" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="修正原因"><el-input v-model="adj.reason" type="textarea" :rows="3" placeholder="不少于 5 个字，原始结果不会被覆盖" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="adjustDlg = false">取消</el-button><el-button type="primary" @click="doAdjust">提交</el-button></template>
    </el-dialog>

    <el-drawer v-model="ruleDlg" title="准备清单规则配置" size="520px">
      <div class="hint">六项因素权重合计必须等于 100%；修改后版本号 +1 并记录生效时间。</div>
      <el-table :data="rules" size="small" style="margin-top:12px">
        <el-table-column prop="ruleName" label="因素" min-width="110" />
        <el-table-column label="权重(%)" width="150">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" :precision="2" size="small" controls-position="right" style="width:110px" />
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="70" />
      </el-table>
      <div class="sum" :class="{ bad: sumWeight !== 100 }">当前合计：{{ sumWeight }}%</div>
      <template #footer>
        <el-button @click="ruleDlg = false">取消</el-button>
        <el-button type="primary" :disabled="sumWeight !== 100" @click="saveRules">保存并生效</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyApi, venueApi, prepApi } from '../api'
import { formatTime } from '../utils/format'

const route = useRoute()
const applies = ref([]); const applyId = ref(null); const app = ref({}); const venue = ref({})
const score = ref(null); const factors = ref({}); const checklist = ref([])
const board = ref({}); const rules = ref([]); const version = ref('-')
const loading = ref(false)
const adjustDlg = ref(false); const ruleDlg = ref(false)
const adj = reactive({ level: '准备较充分', reason: '' })

const NAME = { activityType: '活动类型匹配', people: '参与人数匹配度', equipment: '场地设备满足度', special: '安全与特殊需求', timeSlot: '使用时段', history: '历史问题影响' }
const nameOf = (k) => NAME[k] || k
const pct = (v) => `${Math.min(100, Math.round((v.score / Math.max(1, v.weight)) * 100))}%`
const sumWeight = computed(() => rules.value.reduce((s, r) => s + Number(r.weight || 0), 0).toFixed(2).replace(/\.00$/, ''))
const explain = computed(() => {
  const f = factors.value
  if (!f.people) return '尚未计算'
  const parts = []
  parts.push(`参与人数 ${app.value.peopleCount} 人，人数项得分 ${f.people.score}/${f.people.weight}`)
  parts.push(`场地设备项 ${f.equipment?.score}/${f.equipment?.weight}，${((venue.value.equipment || '').includes('音响') ? '音响齐备' : '设备需补充')}`)
  if (f.special) parts.push(`安全与特殊需求项 ${f.special.score}/${f.special.weight}`)
  if (f.activityType) parts.push(`活动类型为${app.value.activityType}，类型项 ${f.activityType.score}/${f.activityType.weight}`)
  return parts.join('；') + '。'
})

const onPick = async () => {
  score.value = null; factors.value = {}; checklist.value = []
  const r = applies.value.find((x) => x.id === applyId.value)
  if (!r) return
  app.value = r
  venue.value = (await venueApi.detail(r.venueId).catch(() => ({}))) || {}
  const old = await prepApi.latest(applyId.value).catch(() => null)
  if (old) applyScore(old)
}
const applyScore = (s) => {
  score.value = s
  try {
    const dj = JSON.parse(s.detailJson || '{}')
    factors.value = dj.factors || {}
    checklist.value = dj.checklist || []
  } catch (e) { factors.value = {}; checklist.value = [] }
}
const evaluate = async () => {
  loading.value = true
  try {
    const res = await prepApi.evaluate(applyId.value)
    applyScore(res.score)
    ElMessage.success('计算完成，已保存留痕')
    loadBoard()
  } finally { loading.value = false }
}
const doAdjust = async () => {
  if (adj.reason.trim().length < 5) return ElMessage.warning('修正原因不少于 5 个字')
  const s = await prepApi.adjust(score.value.id, { level: adj.level, reason: adj.reason })
  score.value = s; adjustDlg.value = false; ElMessage.success('已保存人工修正'); loadBoard()
}
const saveRules = async () => {
  const res = await prepApi.saveRules(rules.value.map((r) => ({ id: r.id, weight: r.weight })))
  rules.value = res; version.value = res[0]?.version || version.value
  ruleDlg.value = false; ElMessage.success('规则已生效')
}
const loadBoard = async () => {
  board.value = await prepApi.board()
  const rr = await prepApi.rules()
  rules.value = rr
  version.value = rr[0]?.version || '-'
}

onMounted(async () => {
  const page = await applyApi.page({ page: 1, size: 100 })
  applies.value = page.list || []
  await loadBoard()
  const q = Number(route.query.id)
  applyId.value = q && applies.value.some((x) => x.id === q) ? q : (applies.value[0]?.id ?? null)
  if (applyId.value) await onPick()
})
</script>

<style scoped>
.grid3{display:grid;grid-template-columns:1fr 1fr 1.15fr;gap:12px;align-items:start}
.grid2{display:grid;grid-template-columns:1.3fr 1fr;gap:12px;align-items:start}
.big{font-size:38px;font-weight:600;color:#174A5C;line-height:1.1}
.bar{height:7px;border-radius:4px;background:#E9F1F1;overflow:hidden}
.bar i{display:block;height:100%;background:#2A7F86}
.sum{margin-top:12px;font-size:13px;color:#247A58;font-weight:600}
.sum.bad{color:#A62B32}
</style>
