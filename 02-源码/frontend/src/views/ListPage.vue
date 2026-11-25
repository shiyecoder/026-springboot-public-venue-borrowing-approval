<template>
  <div class="page">
    <div class="filter-bar">
      <el-input v-model="q.keyword" placeholder="输入关键字搜索" clearable style="width:240px" @keyup.enter="load(1)" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="q.keyword = ''; load(1)">重置</el-button>
      <div style="flex:1"></div>
      <el-button v-if="!cfg.noSave" type="primary" @click="open()">新增{{ cfg.title }}</el-button>
      <el-button @click="exportCsv">导出</el-button>
      <span class="hint">共 {{ total }} 条</span>
    </div>

    <div class="card" style="padding:0">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column v-for="c in cfg.columns" :key="c.prop" :prop="c.prop" :label="c.label"
          :min-width="c.minWidth" :align="c.align || 'left'" :show-overflow-tooltip="c.showOverflow">
          <template #default="{ row }">
            <span v-if="c.type === 'time'" class="num">{{ formatTime(row[c.prop]) }}</span>
            <span v-else-if="c.type === 'status'" class="tag" :class="row[c.prop] === 1 ? 'tag-done' : 'tag-plan'">
              {{ c.prop === 'hidden' ? (row[c.prop] === 1 ? '隐藏' : '显示') : (row[c.prop] === 1 ? '启用' : '停用') }}
            </span>
            <span v-else-if="c.prop === 'applyId'" class="num">#{{ row[c.prop] }}</span>
            <span v-else>{{ row[c.prop] ?? '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="!cfg.noSave" label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="primary" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!rows.length" class="empty">当前筛选条件下无记录，清空筛选后重试</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
          v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="(form.id ? '编辑' : '新增') + cfg.title" width="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item v-for="f in cfg.fields || []" :key="f.prop" :label="f.label" :required="f.required">
          <el-select v-if="f.type === 'select'" v-model="form[f.prop]" style="width:210px">
            <el-option v-for="o in f.options" :key="o" :label="o" :value="o" />
          </el-select>
          <el-input v-else v-model="form[f.prop]" :placeholder="f.placeholder" />
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="dlg = false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { configs } from './listConfigs'
import { formatTime } from '../utils/format'
import { systemApi } from '../api'

const props = defineProps({ cfg: { type: String, required: true } })
const cfg = computed(() => configs[props.cfg])
const rows = ref([]); const total = ref(0); const loading = ref(false); const dlg = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '' })
const form = reactive({})

const load = async (p) => {
  if (p) q.page = p
  loading.value = true
  try {
    const r = await cfg.value.fetch({ ...q })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const open = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, JSON.parse(JSON.stringify(row)))
  dlg.value = true
}
const save = async () => {
  await cfg.value.save(form)
  ElMessage.success('保存成功'); dlg.value = false; load()
}
const resetPwd = async (row) => {
  if (!row.id) return
  await systemApi.resetPwd(row.id)
  ElMessage.success('密码已重置为 123456')
}
const exportCsv = () => {
  const head = cfg.value.columns.map((c) => c.label).join(',')
  const body = rows.value.map((r) => cfg.value.columns.map((c) => `"${r[c.prop] ?? ''}"`).join(',')).join('\n')
  const blob = new Blob(['\ufeff' + head + '\n' + body], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = `${cfg.value.title}.csv`; a.click()
}

watch(() => props.cfg, () => { q.page = 1; load() })
onMounted(load)
</script>
