<template>
  <div class="page">
    <div class="filter-bar">
      <span class="lbl">活动巡场记录</span>
      <span class="hint">仅「已入场 / 活动中」的申请可录入；异常类型必须填写处理结果</span>
      <div style="flex:1"></div>
      <el-button type="primary" @click="open">录入巡场</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="applyNo" label="申请编号" min-width="130"><template #default="{ row }"><span class="num">{{ row.applyNo || ('#' + row.applyId) }}</span></template></el-table-column>
        <el-table-column prop="patrolTime" label="巡场时间" min-width="165"><template #default="{ row }"><span class="num">{{ formatTime(row.patrolTime) }}</span></template></el-table-column>
        <el-table-column prop="patrolUser" label="巡场人" min-width="100" />
        <el-table-column prop="issueType" label="问题类型" min-width="110">
          <template #default="{ row }"><span class="tag" :class="row.issueType === '正常' ? 'tag-done' : 'tag-err'">{{ row.issueType }}</span></template>
        </el-table-column>
        <el-table-column prop="noiseLevel" label="噪音(dB)" width="100" align="right" />
        <el-table-column prop="description" label="情况说明" min-width="220" show-overflow-tooltip />
        <el-table-column prop="handleResult" label="处理结果" min-width="220" show-overflow-tooltip />
      </el-table>
      <div v-if="!rows.length" class="empty">暂无巡场记录</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="page" v-model:page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="录入巡场记录" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="借用申请" required>
          <el-select v-model="form.applyId" filterable style="width:100%">
            <el-option v-for="a in applies" :key="a.id" :label="`${a.applyNo} · ${a.title}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="问题类型">
          <el-select v-model="form.issueType" style="width:190px">
            <el-option v-for="t in ['正常', '超员', '违规用电', '噪音', '通道堵塞']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="噪音值(dB)"><el-input-number v-model="form.noiseLevel" :min="0" controls-position="right" style="width:160px" /></el-form-item>
        <el-form-item label="超员"><el-switch v-model="form.overCapacity" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="违规用电"><el-switch v-model="form.illegalPower" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="通道堵塞"><el-switch v-model="form.aisleBlocked" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="情况说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="处理结果"><el-input v-model="form.handleResult" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { opsApi, applyApi } from '../api'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const page = ref(1); const size = ref(10); const loading = ref(false)
const dlg = ref(false); const applies = ref([])
const form = reactive({ applyId: null, issueType: '正常', noiseLevel: 65, overCapacity: 0, illegalPower: 0, aisleBlocked: 0, description: '', handleResult: '' })

const load = async () => {
  loading.value = true
  try {
    const r = await opsApi.patrols({ page: page.value, size: size.value })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const open = () => { form.applyId = null; dlg.value = true }
const save = async () => {
  if (!form.applyId) return ElMessage.warning('请选择借用申请')
  await opsApi.patrol(form.applyId, { ...form })
  ElMessage.success('已记录'); dlg.value = false; load()
}
onMounted(async () => {
  const r = await applyApi.page({ page: 1, size: 100, group: 'doing' })
  applies.value = r.list || []
  load()
})
</script>
