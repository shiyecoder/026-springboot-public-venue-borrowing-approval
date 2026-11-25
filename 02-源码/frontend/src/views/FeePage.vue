<template>
  <div class="page">
    <div class="filter-bar">
      <el-select v-model="feeType" placeholder="费用类型" clearable style="width:180px" @change="load(1)">
        <el-option v-for="t in ['场地费', '押金', '物资费', '损坏赔偿']" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="payStatus" placeholder="收取状态" clearable style="width:180px" @change="load(1)">
        <el-option v-for="t in ['未收', '已收', '已退', '部分退']" :key="t" :label="t" :value="t" />
      </el-select>
      <el-button type="primary" @click="load(1)">查询</el-button>
      <div style="flex:1"></div>
      <span class="hint">仅登记线下收款，不进行线上支付；合计已收 ¥{{ received }}</span>
      <el-button type="primary" @click="open">登记费用</el-button>
    </div>

    <div class="card" style="padding:0">
      <el-table :data="rows" v-loading="loading">
        <el-table-column prop="applyNo" label="申请编号" min-width="130"><template #default="{ row }"><span class="num">{{ row.applyNo || ('#' + row.applyId) }}</span></template></el-table-column>
        <el-table-column prop="feeType" label="费用类型" min-width="110">
          <template #default="{ row }"><span class="tag" :class="row.feeType === '损坏赔偿' ? 'tag-err' : 'tag-harv'">{{ row.feeType }}</span></template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" align="right">
          <template #default="{ row }"><span class="num">¥{{ row.amount }}</span></template>
        </el-table-column>
        <el-table-column prop="payStatus" label="状态" width="100">
          <template #default="{ row }">
            <span class="tag" :class="row.payStatus === '已收' ? 'tag-done' : row.payStatus === '未收' ? 'tag-err' : 'tag-plan'">{{ row.payStatus }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="handler" label="经办人" width="100" />
        <el-table-column prop="payTime" label="收付时间" min-width="165"><template #default="{ row }"><span class="num">{{ formatTime(row.payTime) }}</span></template></el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      </el-table>
      <div v-if="!rows.length" class="empty">当前筛选条件下无费用记录</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="page" v-model:page-size="size" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="登记线下费用" width="540px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="关联申请" required>
          <el-select v-model="form.applyId" filterable style="width:100%">
            <el-option v-for="a in applies" :key="a.id" :label="`${a.applyNo} · ${a.title}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="费用类型">
          <el-select v-model="form.feeType" style="width:190px">
            <el-option v-for="t in ['场地费', '押金', '物资费', '损坏赔偿']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额"><el-input-number v-model="form.amount" :min="0.01" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="收取状态">
          <el-select v-model="form.payStatus" style="width:190px">
            <el-option v-for="t in ['未收', '已收', '已退', '部分退']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { opsApi, applyApi } from '../api'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const page = ref(1); const size = ref(10); const loading = ref(false)
const feeType = ref(''); const payStatus = ref('')
const dlg = ref(false); const applies = ref([])
const form = reactive({ applyId: null, feeType: '场地费', amount: 800, payStatus: '已收', remark: '' })
const received = computed(() => rows.value.filter((r) => r.payStatus === '已收').reduce((s, r) => s + Number(r.amount || 0), 0).toFixed(2))

const load = async (p) => {
  if (p) page.value = 1
  loading.value = true
  try {
    const r = await opsApi.fees({ page: page.value, size: size.value, feeType: feeType.value || undefined, payStatus: payStatus.value || undefined })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const open = () => { form.applyId = null; dlg.value = true }
const save = async () => {
  if (!form.applyId) return ElMessage.warning('请选择关联申请')
  await opsApi.saveFee({ ...form })
  ElMessage.success('登记成功'); dlg.value = false; load()
}
onMounted(async () => {
  const r = await applyApi.page({ page: 1, size: 100 })
  applies.value = r.list || []
  load()
})
</script>
