<template>
  <div class="page">
    <div class="card head">
      <div class="item"><div class="lab">业务编号</div><b class="num no">{{ a.applyNo }}</b></div>
      <div class="item"><div class="lab">当前状态</div><span class="tag" :class="statusTag(a.status)">{{ a.status }}</span></div>
      <div class="item"><div class="lab">当前责任人</div><b>{{ a.handlerName || '-' }}</b></div>
      <div class="item"><div class="lab">创建时间</div><span class="num">{{ formatTime(a.createTime) }}</span></div>
      <div class="item"><div class="lab">最近更新</div><span class="num">{{ formatTime(a.updateTime) }}</span></div>
      <div class="item"><div class="lab">处理截止</div><span class="num">{{ formatTime(a.deadline) }}</span></div>
      <div style="flex:1"></div>
      <el-button @click="$router.back()">返回</el-button>
      <el-button v-if="canApprove" type="primary" @click="openApprove">审批</el-button>
      <el-button v-if="a.status === '待交接'" type="primary" @click="openHandover">办理交接</el-button>
      <el-button v-if="a.status === '已入场'" type="primary" @click="startActivity">活动开始</el-button>
      <el-button v-if="['活动中', '已入场'].includes(a.status)" type="primary" @click="openAccept">撤场验收</el-button>
      <el-button v-if="a.status === '待验收'" type="primary" @click="openAccept">重新验收</el-button>
    </div>

    <div class="grid">
      <div>
        <div class="card" style="margin-bottom:12px">
          <div class="card-h"><h3>活动与场地信息</h3><span class="hint">方案变更需重新评估审批范围</span></div>
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="活动主题" :span="2">{{ a.title }}</el-descriptions-item>
            <el-descriptions-item label="活动类型">{{ a.activityType }}</el-descriptions-item>
            <el-descriptions-item label="申请单位">{{ a.deptName }}</el-descriptions-item>
            <el-descriptions-item label="申请人">{{ a.applicantName }}</el-descriptions-item>
            <el-descriptions-item label="参与人数">{{ a.peopleCount }} 人</el-descriptions-item>
            <el-descriptions-item label="借用场地" :span="2">{{ a.venueName }}（{{ venue.campus }} · 容量 {{ venue.capacity }}）</el-descriptions-item>
            <el-descriptions-item label="风险等级"><span class="tag" :class="a.riskLevel === '普通' ? 'tag-done' : 'tag-harv'">{{ a.riskLevel }}</span></el-descriptions-item>
            <el-descriptions-item label="布场时间">{{ formatTime(a.setupStart) }}</el-descriptions-item>
            <el-descriptions-item label="活动时间" :span="2">{{ formatTime(a.startTime) }} ~ {{ formatTime(a.endTime) }}</el-descriptions-item>
            <el-descriptions-item label="撤场结束">{{ formatTime(a.teardownEnd) }}</el-descriptions-item>
            <el-descriptions-item label="是否需安保审批">{{ a.needSecurity === 1 ? '是' : '否' }}</el-descriptions-item>
            <el-descriptions-item label="冲突校验"><span class="tag tag-done">含布撤场已校验</span></el-descriptions-item>
            <el-descriptions-item label="活动流程" :span="3">{{ a.processDesc || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物资需求" :span="3">{{ a.equipNeed || '-' }}</el-descriptions-item>
            <el-descriptions-item label="外来人员" :span="3">{{ a.outsiderInfo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="附件" :span="3">
              <a v-if="a.attachFile" :href="a.attachFile" target="_blank">{{ a.attachFile }}</a>
              <span v-else>未上传</span>
            </el-descriptions-item>
            <el-descriptions-item v-if="a.rejectReason" label="退回/驳回原因" :span="3">
              <span style="color:#A62B32">{{ a.rejectReason }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="card">
          <div class="card-h"><h3>操作时间轴</h3><span class="hint">事件不可由普通用户修改</span></div>
          <el-timeline>
            <el-timeline-item v-for="(t, i) in timeline" :key="i" :timestamp="formatTime(t.time)" placement="top">
              <b>{{ t.node }}</b>
              <span class="tag" :class="t.result === '通过' || t.result === '创建' ? 'tag-done' : t.result === '退回' ? 'tag-harv' : 'tag-err'"
                style="margin-left:8px">{{ t.result }}</span>
              <div class="hint">{{ t.who }}：{{ t.remark }}</div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <div>
        <div class="card" style="margin-bottom:12px">
          <div class="card-h"><h3>物资预留</h3><el-button link type="primary" @click="openReserve">新增预留</el-button></div>
          <el-table :data="reservations" size="small">
            <el-table-column prop="itemName" label="物资" min-width="100" />
            <el-table-column prop="qty" label="数量" width="70" align="right" />
            <el-table-column prop="status" label="状态" width="85">
              <template #default="{ row }"><span class="tag" :class="row.status === '已归还' ? 'tag-done' : 'tag-harv'">{{ row.status }}</span></template>
            </el-table-column>
          </el-table>
          <div v-if="!reservations.length" class="hint" style="padding:8px 0">暂无预留记录</div>
        </div>

        <div class="card" style="margin-bottom:12px">
          <div class="card-h"><h3>入场交接</h3></div>
          <template v-if="handover">
            <div class="stat-line"><span>场地现状</span><b>{{ handover.venueState }}</b></div>
            <div class="stat-line"><span>交付物资</span><b>{{ handover.itemsDelivered }}</b></div>
            <div class="stat-line"><span>电表示数</span><b class="num">{{ handover.electricReading }}</b></div>
            <div class="stat-line"><span>钥匙/卡</span><b>{{ handover.keyInfo }}</b></div>
            <div class="stat-line"><span>双方确认</span><b>{{ handover.applicantConfirm }} / {{ handover.managerConfirm }}</b></div>
            <img v-if="handover.photo" :src="handover.photo" class="shot" />
          </template>
          <div v-else class="hint">尚未办理入场交接</div>
        </div>

        <div class="card" style="margin-bottom:12px">
          <div class="card-h"><h3>撤场验收与费用</h3>
            <el-button v-if="a.status === '待验收'" link type="primary" @click="openPatrol">补录巡场</el-button>
          </div>
          <template v-if="acceptance">
            <div class="stat-line"><span>验收结果</span>
              <span class="tag" :class="acceptance.result === '通过' ? 'tag-done' : 'tag-err'">{{ acceptance.result }}</span></div>
            <div class="stat-line"><span>清洁/设施/物资</span><b>{{ flag(acceptance.cleanerOk) }}/{{ flag(acceptance.facilityOk) }}/{{ flag(acceptance.itemOk) }}</b></div>
            <div class="stat-line"><span>损坏赔偿</span><b class="num">¥{{ acceptance.compensateAmount }}</b></div>
            <div class="stat-line"><span>复核状态</span><b>{{ acceptance.reviewStatus }}</b></div>
            <div v-if="acceptance.damageDesc" class="hint">损坏说明：{{ acceptance.damageDesc }}</div>
          </template>
          <div v-else class="hint">尚未验收</div>
          <el-divider style="margin:10px 0" />
          <div class="stat-line" v-for="f in fees" :key="f.id">
            <span>{{ f.feeType }}</span>
            <b class="num">¥{{ f.amount }} · {{ f.payStatus }}</b>
          </div>
          <div v-if="!fees.length" class="hint">暂无费用记录（线下登记）</div>
        </div>

        <div class="card">
          <div class="card-h"><h3>创新点 · 准备清单</h3>
            <el-button link type="primary" @click="$router.push(`/prep?id=${a.id}`)">打开准备任务板</el-button></div>
          <div v-if="score">
            <div style="display:flex;align-items:baseline;gap:8px">
              <div class="num score">{{ score.totalScore }}</div>
              <span class="tag" :class="score.level === '准备不足' ? 'tag-err' : 'tag-done'">{{ score.manualLevel || score.level }}</span>
            </div>
            <div class="hint">规则版本 {{ score.ruleVersion }} · 原始等级 {{ score.level }}</div>
            <div class="hint">未满足：{{ score.unmet }}</div>
            <div class="hint">改进项：{{ score.advice }}</div>
          </div>
          <div v-else class="hint">尚未生成准备清单</div>
        </div>
      </div>
    </div>

    <el-dialog v-model="approveDlg" title="审批处理" width="520px">
      <el-form label-width="90px">
        <el-form-item label="审批结果">
          <el-radio-group v-model="ap.result"><el-radio-button label="通过" /><el-radio-button label="退回" /><el-radio-button label="驳回" /></el-radio-group>
        </el-form-item>
        <el-form-item label="审批意见"><el-input v-model="ap.opinion" type="textarea" :rows="3" placeholder="退回/驳回需不少于 5 个字" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="approveDlg = false">取消</el-button><el-button type="primary" @click="doApprove">确认</el-button></template>
    </el-dialog>

    <el-dialog v-model="handoverDlg" title="入场交接" width="560px">
      <el-form :model="ho" label-width="110px">
        <el-form-item label="场地现状"><el-input v-model="ho.venueState" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="交付物资"><el-input v-model="ho.itemsDelivered" /></el-form-item>
        <el-form-item label="电表示数"><el-input-number v-model="ho.electricReading" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="钥匙/门禁卡"><el-input v-model="ho.keyInfo" /></el-form-item>
        <el-form-item label="申请人确认"><el-input v-model="ho.applicantConfirm" placeholder="填写确认人姓名" /></el-form-item>
        <el-form-item label="管理员确认"><el-input v-model="ho.managerConfirm" /></el-form-item>
        <el-form-item label="交接照片">
          <div style="display:flex;gap:10px;align-items:center">
            <img v-if="ho.photo" :src="ho.photo" class="thumb" style="width:72px;height:54px" />
            <el-upload :show-file-list="false" :http-request="uploadHo"><el-button>上传照片</el-button></el-upload>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="handoverDlg = false">取消</el-button><el-button type="primary" @click="doHandover">确认交接</el-button></template>
    </el-dialog>

    <el-dialog v-model="acceptDlg" title="撤场验收" width="600px">
      <el-form :model="ac" label-width="110px">
        <el-form-item label="清洁合格"><el-switch v-model="ac.cleanerOk" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="设施完好"><el-switch v-model="ac.facilityOk" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="物资齐全"><el-switch v-model="ac.itemOk" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="钥匙归还"><el-switch v-model="ac.keyReturned" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="存在损坏"><el-switch v-model="ac.damageFlag" :active-value="1" :inactive-value="0" /></el-form-item>
        <template v-if="ac.damageFlag === 1">
          <el-form-item label="损坏说明"><el-input v-model="ac.damageDesc" type="textarea" :rows="2" placeholder="不少于 5 个字" /></el-form-item>
          <el-form-item label="赔偿金额"><el-input-number v-model="ac.compensateAmount" :min="0" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        </template>
        <el-form-item label="验收照片">
          <div style="display:flex;gap:10px;align-items:center">
            <img v-if="ac.photo" :src="ac.photo" class="thumb" style="width:72px;height:54px" />
            <el-upload :show-file-list="false" :http-request="uploadAc"><el-button>上传照片</el-button></el-upload>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="ac.remark" /></el-form-item>
      </el-form>
      <div class="hint">验收不通过或存在损坏时，会生成赔偿单并锁定押金，需管理员复核金额。</div>
      <template #footer><el-button @click="acceptDlg = false">取消</el-button><el-button type="primary" @click="doAccept">提交验收</el-button></template>
    </el-dialog>

    <el-dialog v-model="patrolDlg" title="活动巡场" width="540px">
      <el-form :model="pt" label-width="100px">
        <el-form-item label="问题类型">
          <el-select v-model="pt.issueType" style="width:180px">
            <el-option v-for="t in ['正常', '超员', '违规用电', '噪音', '通道堵塞']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="噪音值(dB)"><el-input-number v-model="pt.noiseLevel" :min="0" controls-position="right" style="width:160px" /></el-form-item>
        <el-form-item label="情况说明"><el-input v-model="pt.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="处理结果"><el-input v-model="pt.handleResult" type="textarea" :rows="2" placeholder="异常巡场必填" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="patrolDlg = false">取消</el-button><el-button type="primary" @click="doPatrol">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="reserveDlg" title="物资预留" width="480px">
      <el-form label-width="90px">
        <el-form-item label="物资">
          <el-select v-model="rv.itemId" style="width:220px"><el-option v-for="i in items" :key="i.id" :label="`${i.itemName}（库存 ${i.totalQty}）`" :value="i.id" /></el-select>
        </el-form-item>
        <el-form-item label="数量"><el-input-number v-model="rv.qty" :min="1" controls-position="right" style="width:160px" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="reserveDlg = false">取消</el-button><el-button type="primary" @click="doReserve">预留</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { applyApi, opsApi, venueApi, prepApi, systemApi } from '../api'
import { formatTime } from '../utils/format'

const route = useRoute()
const id = Number(route.params.id)
const user = JSON.parse(localStorage.getItem('vn_user') || '{}')
const a = ref({}); const venue = ref({}); const timeline = ref([])
const reservations = ref([]); const handover = ref(null); const acceptance = ref(null); const fees = ref([]); const score = ref(null)
const items = ref([])
const approveDlg = ref(false); const handoverDlg = ref(false); const acceptDlg = ref(false)
const patrolDlg = ref(false); const reserveDlg = ref(false)
const ap = reactive({ result: '通过', opinion: '' })
const ho = reactive({ venueState: '场地清洁，设施完好', itemsDelivered: '按预留清单交付', electricReading: 1200, keyInfo: '主门钥匙 1 把', applicantConfirm: '', managerConfirm: '', photo: '' })
const ac = reactive({ cleanerOk: 1, facilityOk: 1, itemOk: 1, keyReturned: 1, damageFlag: 0, damageDesc: '', compensateAmount: 0, photo: '', remark: '' })
const pt = reactive({ issueType: '正常', noiseLevel: 65, description: '', handleResult: '' })
const rv = reactive({ itemId: null, qty: 10 })

const statusTag = (s) => ({
  草稿: 'tag-plan', 待院系审批: 'tag-grow', 待场地审批: 'tag-grow', 待安保审批: 'tag-grow',
  待交接: 'tag-harv', 已入场: 'tag-harv', 活动中: 'tag-harv', 待验收: 'tag-harv',
  已完成: 'tag-done', 已驳回: 'tag-err', 已取消: 'tag-plan'
}[s] || 'tag-plan')
const flag = (v) => (v === 1 ? '合格' : '不合格')
const canApprove = computed(() => {
  const map = { 待院系审批: ['dept', 'admin'], 待场地审批: ['venue', 'admin'], 待安保审批: ['security', 'admin'] }
  return (map[a.value.status] || []).includes(user.role)
})

const load = async () => {
  const d = await applyApi.detail(id)
  a.value = d.apply; venue.value = d.venue || {}; timeline.value = d.timeline || []
  reservations.value = d.reservations || []; handover.value = d.handover
  acceptance.value = d.acceptance; fees.value = d.fees || []
  score.value = await prepApi.latest(id).catch(() => null)
  if (a.value.venueId) items.value = await venueApi.items(a.value.venueId)
}
const openApprove = () => { ap.result = '通过'; ap.opinion = ''; approveDlg.value = true }
const doApprove = async () => {
  if (ap.result !== '通过' && ap.opinion.trim().length < 5) return ElMessage.warning('退回或驳回必须填写不少于 5 个字的原因')
  await applyApi.approve(id, { result: ap.result, opinion: ap.opinion || '同意' })
  ElMessage.success('审批完成'); approveDlg.value = false; load()
}
const openHandover = () => { ho.applicantConfirm = a.value.applicantName; ho.managerConfirm = user.realName; handoverDlg.value = true }
const doHandover = async () => {
  await opsApi.handover(id, { ...ho }); ElMessage.success('交接完成'); handoverDlg.value = false; load()
}
const startActivity = async () => {
  await applyApi.transition(id, { target: '活动开始' }); ElMessage.success('活动已开始'); load()
}
const openAccept = () => { acceptDlg.value = true }
const doAccept = async () => {
  await opsApi.accept(id, { ...ac }); ElMessage.success('验收已提交'); acceptDlg.value = false; load()
}
const openPatrol = () => { patrolDlg.value = true }
const doPatrol = async () => {
  await opsApi.patrol(id, { ...pt }); ElMessage.success('巡场已记录'); patrolDlg.value = false; load()
}
const openReserve = () => { reserveDlg.value = true }
const doReserve = async () => {
  if (!rv.itemId) return ElMessage.warning('请选择物资')
  await applyApi.reserve(id, { ...rv }); ElMessage.success('预留成功'); reserveDlg.value = false; load()
}
const up = async (file, target, key) => {
  const fd = new FormData(); fd.append('file', file); fd.append('bizType', 'venue')
  target[key] = await systemApi.upload(fd); ElMessage.success('上传成功')
}
const uploadHo = ({ file }) => up(file, ho, 'photo')
const uploadAc = ({ file }) => up(file, ac, 'photo')

onMounted(load)
</script>

<style scoped>
.head{display:flex;align-items:center;gap:26px;flex-wrap:wrap}
.head .lab{font-size:12px;color:#8CA3A5;margin-bottom:4px}
.head .no{font-size:17px;color:#174A5C}
.grid{display:grid;grid-template-columns:1.55fr 1fr;gap:12px;align-items:start}
.score{font-size:30px;font-weight:600;color:#174A5C;line-height:1.2}
.shot{width:100%;border-radius:6px;margin-top:8px;border:1px solid #DCE7E7}
:deep(.el-descriptions__label){width:110px;color:#5D7578}
</style>
