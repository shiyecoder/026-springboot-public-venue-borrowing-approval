<template>
  <div class="page">
    <div class="filter-bar">
      <el-input v-model="q.keyword" placeholder="申请编号 / 活动主题" clearable style="width:220px" @keyup.enter="load(1)" />
      <el-select v-model="q.activityType" placeholder="活动类型" clearable style="width:190px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.venueId" placeholder="全部场地" clearable filterable style="width:230px">
        <el-option v-for="v in venues" :key="v.id" :label="v.name" :value="v.id" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div style="flex:1"></div>
      <el-button v-if="canApply" type="primary" @click="openCreate">新建借用申请</el-button>
    </div>

    <div class="card" style="padding:0">
      <el-tabs v-model="group" class="tabs" style="padding:0 16px" @tab-change="load(1)">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane label="待办" name="todo" />
        <el-tab-pane label="处理中" name="doing" />
        <el-tab-pane label="已完成" name="done" />
        <el-tab-pane label="异常" name="error" />
      </el-tabs>
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column prop="applyNo" label="申请编号" min-width="125"><template #default="{ row }"><span class="num">{{ row.applyNo }}</span></template></el-table-column>
        <el-table-column prop="title" label="活动主题" min-width="170" show-overflow-tooltip />
        <el-table-column prop="activityType" label="类型" min-width="70" />
        <el-table-column prop="venueName" label="场地" min-width="150" show-overflow-tooltip />
        <el-table-column prop="peopleCount" label="人数" min-width="70" align="right"><template #default="{ row }"><span class="num">{{ row.peopleCount }}</span></template></el-table-column>
        <el-table-column label="时段（含布撤场）" min-width="190">
          <template #default="{ row }">
            <span class="num">{{ fmtRange(row.setupStart, row.teardownEnd) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="riskLevel" label="风险" min-width="80">
          <template #default="{ row }"><span class="tag" :class="riskTag(row.riskLevel)">{{ row.riskLevel }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="152">
          <template #default="{ row }">
            <span class="tag" :class="statusTag(row.status)">{{ row.status }}</span>
            <span v-if="row.cancelFlag === 1" class="tag tag-err" style="margin-left:4px">超期</span>
          </template>
        </el-table-column>
        
        <el-table-column label="更新时间" min-width="168"><template #default="{ row }"><span class="num">{{ formatTime(row.updateTime) }}</span></template></el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push(`/apply/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === '草稿' && isMine(row)" link type="primary" @click="submit(row)">提交</el-button>
            <el-button v-if="canApprove(row)" link type="primary" @click="openApprove(row)">审批</el-button>
            <el-button v-if="row.status === '草稿'" link type="danger" @click="cancel(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!rows.length" class="empty">当前筛选条件下无记录，清空筛选或新建借用申请</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
          v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 新建申请抽屉 -->
    <el-drawer v-model="createDlg" title="新建借用申请" size="600px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="活动主题" required><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="活动类型" required>
          <el-select v-model="form.activityType" style="width:220px"><el-option v-for="t in types" :key="t" :label="t" :value="t" /></el-select>
        </el-form-item>
        <el-form-item label="借用场地" required>
          <el-select v-model="form.venueId" filterable style="width:100%">
            <el-option v-for="v in venues" :key="v.id" :label="`${v.name}（${v.campus} · 容量 ${v.capacity}）`" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="参与人数" required><el-input-number v-model="form.peopleCount" :min="1" :step="10" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="布场开始"><el-date-picker v-model="form.setupStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="活动开始" required><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="活动结束" required><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="撤场结束"><el-date-picker v-model="form.teardownEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        <el-form-item label="活动流程"><el-input v-model="form.processDesc" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="设备物资需求"><el-input v-model="form.equipNeed" type="textarea" :rows="2" placeholder="课桌 60 张、座椅 200 把、音响 1 套" /></el-form-item>
        <el-form-item label="外来人员"><el-input v-model="form.outsiderInfo" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="附件">
          <div style="display:flex;gap:10px;align-items:center">
            <el-upload :show-file-list="false" :http-request="uploadFile">
              <el-button>上传活动方案 / 安全预案</el-button>
            </el-upload>
            <span class="hint">{{ form.attachFile || '未上传' }}</span>
          </div>
        </el-form-item>
      </el-form>
      <div class="hint">提交时系统校验：时段冲突（含布撤场）、未结损坏赔偿、场地状态。</div>
      <template #footer>
        <el-button @click="createDlg = false">取消</el-button>
        <el-button @click="saveDraft">保存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="saveAndSubmit">提交审批</el-button>
      </template>
    </el-drawer>

    <!-- 审批抽屉：同时展示单据摘要，避免来回切页 -->
    <el-drawer v-model="approveDlg" title="审批处理" size="560px">
      <div class="card" style="margin-bottom:14px;background:#F8FBFB">
        <div class="stat-line"><span>申请编号</span><b class="num">{{ cur.applyNo }}</b></div>
        <div class="stat-line"><span>活动主题</span><b>{{ cur.title }}</b></div>
        <div class="stat-line"><span>场地与时段</span><b>{{ cur.venueName }} · {{ fmt(cur.startTime) }}</b></div>
        <div class="stat-line"><span>人数 / 风险</span><b>{{ cur.peopleCount }} 人 · {{ cur.riskLevel }}</b></div>
        <div class="stat-line"><span>当前节点</span><b>{{ cur.currentNode }}</b></div>
      </div>
      <el-form label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="ap.result">
            <el-radio-button label="通过" /><el-radio-button label="退回" /><el-radio-button label="驳回" />
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="ap.opinion" type="textarea" :rows="4" placeholder="退回或驳回必须填写不少于 5 个字的原因" />
        </el-form-item>
      </el-form>
      <div class="hint">通过后按规则流转：院系通过 → 场地审核 →（需安保时）安保审批 → 待交接。</div>
      <template #footer>
        <el-button @click="approveDlg = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doApprove">确认提交</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { applyApi, venueApi, systemApi } from '../api'
import { formatTime } from '../utils/format'

const props = defineProps({ mode: { type: String, default: '' } })
const types = ['讲座', '招聘', '社团', '展览', '体育', '晚会']
const user = JSON.parse(localStorage.getItem('vn_user') || '{}')
const rows = ref([]); const total = ref(0); const loading = ref(false); const saving = ref(false)
const venues = ref([]); const group = ref('')
const createDlg = ref(false); const approveDlg = ref(false); const cur = ref({})
const q = reactive({ page: 1, size: 10, keyword: '', activityType: '', venueId: null })
const form = reactive({})
const ap = reactive({ result: '通过', opinion: '' })

const canApply = computed(() => ['applicant', 'admin'].includes(user.role))
const isMine = (row) => row.applicantId === user.id
// 时段列省略年份（系统内均为当年度），保证「布场 ~ 撤场」完整可见
const fmt = (v) => (v ? String(v).slice(5, 16) : '-')
const fmtRange = (a, b) => {
  if (!a || !b) return '-'
  const s = String(a), e = String(b)
  const sd = s.slice(5, 10), ed = e.slice(5, 10)
  return sd === ed ? `${sd} ${s.slice(11, 16)} ~ ${e.slice(11, 16)}` : `${s.slice(5, 16)} ~ ${e.slice(5, 16)}`
}
const statusTag = (s) => ({
  草稿: 'tag-plan', 待院系审批: 'tag-grow', 待场地审批: 'tag-grow', 待安保审批: 'tag-grow',
  待交接: 'tag-harv', 已入场: 'tag-harv', 活动中: 'tag-harv', 待验收: 'tag-harv',
  已完成: 'tag-done', 已驳回: 'tag-err', 已取消: 'tag-plan'
}[s] || 'tag-plan')
const riskTag = (r) => (r === '普通' ? 'tag-done' : r === '大型' ? 'tag-err' : 'tag-harv')

const canApprove = (row) => {
  const map = { 待院系审批: ['dept', 'admin'], 待场地审批: ['venue', 'admin'], 待安保审批: ['security', 'admin'] }
  return (map[row.status] || []).includes(user.role)
}

const load = async (p) => {
  if (p) q.page = p
  loading.value = true
  try {
    const r = await applyApi.page({ ...q, group: props.mode === 'approval' ? (group.value || 'todo') : group.value })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const reset = () => { Object.assign(q, { page: 1, keyword: '', activityType: '', venueId: null }); load(1) }

const openCreate = () => {
  Object.keys(form).forEach((k) => delete form[k])
  createDlg.value = true
}
const uploadFile = async ({ file }) => {
  const fd = new FormData(); fd.append('file', file); fd.append('bizType', 'apply')
  form.attachFile = await systemApi.upload(fd)
  ElMessage.success('附件上传成功')
}
const saveDraft = async () => {
  if (!form.title || !form.venueId) return ElMessage.warning('活动主题与场地必填')
  saving.value = true
  try {
    const a = await applyApi.save(form)
    ElMessage.success('已保存草稿')
    createDlg.value = false; load(1)
    return a
  } finally { saving.value = false }
}
const saveAndSubmit = async () => {
  if (!form.title || !form.venueId || !form.startTime || !form.endTime) {
    return ElMessage.warning('请填写活动主题、场地与活动时间')
  }
  saving.value = true
  try {
    const a = await applyApi.save(form)
    await applyApi.submit(a.id)
    ElMessage.success('已提交，等待院系审批')
    createDlg.value = false; load(1)
  } finally { saving.value = false }
}
const submit = async (row) => {
  try {
    await applyApi.submit(row.id)
    ElMessage.success('已提交审批'); load()
  } catch (e) { /* 后端提示已由拦截器展示 */ }
}
const cancel = async (row) => {
  const { value } = await ElMessageBox.prompt('请填写作废原因（不少于 5 个字）', '作废申请', { inputPlaceholder: '例如：活动与院系安排冲突' })
  await applyApi.transition(row.id, { target: '取消', reason: value })
  ElMessage.success('已作废'); load()
}
const openApprove = (row) => { cur.value = row; ap.result = '通过'; ap.opinion = ''; approveDlg.value = true }
const doApprove = async () => {
  if (ap.result !== '通过' && ap.opinion.trim().length < 5) return ElMessage.warning('退回或驳回必须填写不少于 5 个字的原因')
  saving.value = true
  try {
    await applyApi.approve(cur.value.id, { result: ap.result, opinion: ap.opinion || '同意' })
    ElMessage.success('审批已提交'); approveDlg.value = false; load()
  } finally { saving.value = false }
}

onMounted(async () => {
  venues.value = await venueApi.options()
  load(1)
})
</script>

<style scoped>
.tabs :deep(.el-tabs__header){margin-bottom:10px}
.tabs :deep(.el-tabs__nav-wrap:after){height:1px}
</style>
