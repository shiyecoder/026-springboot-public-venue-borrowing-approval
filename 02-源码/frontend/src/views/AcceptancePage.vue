<template>
  <div class="page">
    <div class="filter-bar">
      <span class="lbl">撤场验收记录</span>
      <span class="hint">有损坏时自动生成赔偿单并锁定押金，赔偿金额需管理员复核</span>
      <div style="flex:1"></div>
      <el-button @click="load">刷新</el-button>
    </div>
    <div class="card" style="padding:0">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="applyNo" label="申请编号" min-width="130"><template #default="{ row }"><span class="num">{{ row.applyNo || ('#' + row.applyId) }}</span></template></el-table-column>
        <el-table-column label="清洁/设施/物资/钥匙" min-width="180">
          <template #default="{ row }">{{ f(row.cleanerOk) }} / {{ f(row.facilityOk) }} / {{ f(row.itemOk) }} / {{ f(row.keyReturned) }}</template>
        </el-table-column>
        <el-table-column label="损坏" width="90">
          <template #default="{ row }"><span class="tag" :class="row.damageFlag === 1 ? 'tag-err' : 'tag-done'">{{ row.damageFlag === 1 ? '有' : '无' }}</span></template>
        </el-table-column>
        <el-table-column prop="compensateAmount" label="赔偿金额" width="110" align="right">
          <template #default="{ row }"><span class="num">¥{{ row.compensateAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="result" label="验收结果" width="100">
          <template #default="{ row }"><span class="tag" :class="row.result === '通过' ? 'tag-done' : 'tag-err'">{{ row.result }}</span></template>
        </el-table-column>
        <el-table-column prop="reviewStatus" label="复核" width="100">
          <template #default="{ row }"><span class="tag" :class="row.reviewStatus === '已确认' ? 'tag-done' : 'tag-harv'">{{ row.reviewStatus }}</span></template>
        </el-table-column>
        <el-table-column prop="damageDesc" label="损坏说明" min-width="200" show-overflow-tooltip />
        <el-table-column prop="acceptor" label="验收人" width="95" />
        <el-table-column prop="acceptTime" label="验收时间" min-width="165"><template #default="{ row }"><span class="num">{{ formatTime(row.acceptTime) }}</span></template></el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.reviewStatus === '待复核'" link type="primary" @click="openReview(row)">复核金额</el-button>
            <span v-else class="hint">已完成</span>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!rows.length" class="empty">暂无验收记录</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="page" v-model:page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="赔偿金额复核" width="520px">
      <el-form label-width="100px">
        <el-form-item label="损坏说明"><span>{{ cur.damageDesc }}</span></el-form-item>
        <el-form-item label="复核金额"><el-input-number v-model="amount" :min="1" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="复核意见"><el-input v-model="remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <div class="hint">复核确认后该申请转为已完成，赔偿单保持「未收」直至线下收款登记。</div>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="doReview">确认复核</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { opsApi } from '../api'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const page = ref(1); const size = ref(10); const loading = ref(false)
const dlg = ref(false); const cur = ref({}); const amount = ref(0); const remark = ref('')
const f = (v) => (v === 1 ? '合格' : '不合格')

const load = async () => {
  loading.value = true
  try {
    const r = await opsApi.accepts({ page: page.value, size: size.value })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const openReview = (row) => { cur.value = row; amount.value = row.compensateAmount; remark.value = '复核确认赔偿金额'; dlg.value = true }
const doReview = async () => {
  await opsApi.reviewAccept(cur.value.id, { amount: amount.value, remark: remark.value })
  ElMessage.success('复核完成，押金可释放'); dlg.value = false; load()
}
onMounted(load)
</script>
