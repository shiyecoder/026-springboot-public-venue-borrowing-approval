<template>
  <div class="page">
    <div class="filter-bar">
      <el-input v-model="q.keyword" placeholder="场地编号 / 名称" clearable style="width:200px" @keyup.enter="load(1)" />
      <el-select v-model="q.campus" placeholder="全部校区" clearable style="width:190px">
        <el-option v-for="c in campuses" :key="c" :label="c" :value="c" />
      </el-select>
      <el-select v-model="q.activityType" placeholder="活动类型" clearable style="width:190px">
        <el-option v-for="t in types" :key="t" :label="t" :value="t" />
      </el-select>
      <el-select v-model="q.status" placeholder="场地状态" clearable style="width:180px">
        <el-option label="可用" value="可用" /><el-option label="维护中" value="维护中" /><el-option label="停用" value="停用" />
      </el-select>
      <el-input-number v-model="q.minCapacity" :min="0" :step="50" controls-position="right" placeholder="最小容量" style="width:150px" />
      <el-button type="primary" @click="load(1)">查询</el-button>
      <el-button @click="reset">重置</el-button>
      <div style="flex:1"></div>
      <el-button type="primary" @click="open()">新增场地</el-button>
      <el-button @click="exportCsv">导出</el-button>
    </div>

    <div class="card" style="padding:0">
      <el-table :data="rows" v-loading="loading" style="width:100%">
        <el-table-column label="现场图片" width="90">
          <template #default="{ row }"><img class="thumb" :src="row.image" :alt="row.name" /></template>
        </el-table-column>
        <el-table-column prop="code" label="场地编号" min-width="120"><template #default="{ row }"><span class="num">{{ row.code }}</span></template></el-table-column>
        <el-table-column prop="name" label="场地名称" min-width="170" />
        <el-table-column prop="campus" label="校区" min-width="90" />
        <el-table-column prop="capacity" label="容量" min-width="80" align="right"><template #default="{ row }"><span class="num">{{ row.capacity }}</span></template></el-table-column>
        <el-table-column prop="area" label="面积(㎡)" min-width="90" align="right"><template #default="{ row }"><span class="num">{{ row.area }}</span></template></el-table-column>
        <el-table-column prop="allowActivity" label="适用活动" min-width="120" />
        <el-table-column prop="equipment" label="设备清单" min-width="150" show-overflow-tooltip />
        <el-table-column prop="openTime" label="开放时段" min-width="110" />
        <el-table-column prop="feeStandard" label="费用标准" min-width="100" align="right">
          <template #default="{ row }"><span class="num">¥{{ row.feeStandard }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" min-width="90">
          <template #default="{ row }"><span class="tag" :class="statusTag(row.status)">{{ row.status }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="primary" @click="showItems(row)">物资</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="!rows.length" class="empty">当前筛选条件下无记录，清空筛选或新增场地</div>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
          v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
          @current-change="load()" @size-change="load(1)" />
      </div>
    </div>

    <!-- 新增/编辑抽屉：图片走上传组件，不允许填外链 -->
    <el-drawer v-model="drawer" :title="(form.id ? '编辑' : '新增') + '场地'" size="560px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="场地编号" required><el-input v-model="form.code" maxlength="50" placeholder="保存前自动去除首尾空格" /></el-form-item>
        <el-form-item label="场地名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="所在校区" required>
          <el-select v-model="form.campus" style="width:220px"><el-option v-for="c in campuses" :key="c" :label="c" :value="c" /></el-select>
        </el-form-item>
        <el-form-item label="楼宇/区域"><el-input v-model="form.building" /></el-form-item>
        <el-form-item label="容量" required><el-input-number v-model="form.capacity" :min="0" :step="10" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="面积(㎡)"><el-input-number v-model="form.area" :min="0" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="适用活动" required>
          <el-select v-model="form.allowActivity" multiple filterable allow-create style="width:100%">
            <el-option v-for="t in types" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="设备清单" required><el-input v-model="form.equipment" placeholder="例如：音响/投影/空调/舞台" /></el-form-item>
        <el-form-item label="开放时段"><el-input v-model="form.openTime" maxlength="100" placeholder="例如 08:00-22:00" /></el-form-item>
        <el-form-item label="管理部门"><el-input v-model="form.deptName" maxlength="100" /></el-form-item>
        <el-form-item label="费用标准"><el-input-number v-model="form.feeStandard" :min="0" :precision="2" controls-position="right" style="width:180px" /></el-form-item>
        <el-form-item label="场地状态">
          <el-select v-model="form.status" style="width:180px"><el-option label="可用" value="可用" /><el-option label="维护中" value="维护中" /><el-option label="停用" value="停用" /></el-select>
        </el-form-item>
        <el-form-item label="现场图片">
          <div style="display:flex;gap:12px;align-items:center">
            <img v-if="form.image" :src="form.image" class="thumb" style="width:88px;height:66px" />
            <el-upload :show-file-list="false" :http-request="uploadImage" accept="image/*">
              <el-button>上传图片</el-button>
            </el-upload>
            <span class="hint">仅支持本地图片，保存相对路径</span>
          </div>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="drawer = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="itemDlg" :title="`${current?.name || ''} · 物资库`" width="620px">
      <el-table :data="items" size="small">
        <el-table-column prop="itemName" label="物资" min-width="120" />
        <el-table-column prop="itemType" label="类别" min-width="100" />
        <el-table-column prop="totalQty" label="库存" width="90" align="right"><template #default="{ row }"><span class="num">{{ row.totalQty }}</span></template></el-table-column>
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="status" label="状态" width="90" />
      </el-table>
      <div class="hint" style="margin-top:10px">物资预留数量按同场地同时段合并计算，超出库存会在提交预留时被拦截。</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { venueApi, systemApi } from '../api'

const campuses = ['东校区', '西校区', '南校区', '北校区']
const types = ['讲座', '招聘', '社团', '展览', '体育', '晚会']
const rows = ref([]); const total = ref(0); const loading = ref(false)
const drawer = ref(false); const itemDlg = ref(false); const items = ref([]); const current = ref(null)
const q = reactive({ page: 1, size: 10, keyword: '', campus: '', activityType: '', status: '', minCapacity: null })
const form = reactive({})

const statusTag = (s) => ({ 可用: 'tag-done', 维护中: 'tag-harv', 停用: 'tag-err' }[s] || 'tag-plan')

const load = async (p) => {
  if (p) q.page = p
  loading.value = true
  try {
    const r = await venueApi.page({ ...q, activityType: q.activityType || undefined, minCapacity: q.minCapacity || undefined })
    rows.value = r.list || []; total.value = r.total || 0
  } finally { loading.value = false }
}
const reset = () => { Object.assign(q, { page: 1, keyword: '', campus: '', activityType: '', status: '', minCapacity: null }); load(1) }
const open = (row) => {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, JSON.parse(JSON.stringify(row)))
  else Object.assign(form, { status: '可用', capacity: 100, allowActivity: '', feeStandard: 0 })
  drawer.value = true
}
const uploadImage = async ({ file }) => {
  const fd = new FormData(); fd.append('file', file); fd.append('bizType', 'venue')
  form.image = await systemApi.upload(fd)
  ElMessage.success('图片上传成功')
}
const save = async () => {
  if (!form.code || !form.name) return ElMessage.warning('场地编号与名称必填')
  form.allowActivity = Array.isArray(form.allowActivity) ? form.allowActivity.join('/') : form.allowActivity
  await venueApi.save(form)
  ElMessage.success('保存成功'); drawer.value = false; load()
}
const showItems = async (row) => { current.value = row; items.value = await venueApi.items(row.id); itemDlg.value = true }
const exportCsv = () => {
  const head = '场地编号,场地名称,校区,容量,面积,适用活动,状态,费用标准'
  const body = rows.value.map((r) => [r.code, r.name, r.campus, r.capacity, r.area, r.allowActivity, r.status, r.feeStandard].join(',')).join('\n')
  const blob = new Blob(['\ufeff' + head + '\n' + body], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = '场地资源库.csv'; a.click()
}
onMounted(() => load(1))
</script>
