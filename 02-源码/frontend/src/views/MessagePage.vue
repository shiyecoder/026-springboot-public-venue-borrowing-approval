<template>
  <div class="page">
    <div class="card" style="padding:0">
      <el-table :data="rows">
        <el-table-column label="状态" width="90"><template #default="{ row }"><span class="tag" :class="row.readFlag ? 'tag-plan' : 'tag-err'">{{ row.readFlag ? '已读' : '未读' }}</span></template></el-table-column>
        <el-table-column prop="title" label="标题" width="180" />
        <el-table-column prop="content" label="内容" min-width="360" />
        <el-table-column prop="bizNo" label="业务编号" width="180"><template #default="{ row }"><span class="num">{{ row.bizNo }}</span></template></el-table-column>
        <el-table-column prop="createTime" label="时间" width="180"><template #default="{ row }">{{ formatTime(row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" width="110" align="right"><template #default="{ row }"><el-button link type="primary" @click="read(row)">标为已读</el-button></template></el-table-column>
      </el-table>
      <div class="pager" style="padding:12px 16px">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="q.page" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { systemApi } from '../api'
import { formatTime } from '../utils/format'

const rows = ref([]); const total = ref(0); const q = reactive({ page: 1, size: 10 })
const load = async () => { const r = await systemApi.messages(q); rows.value = r.list; total.value = r.total }
const read = async (row) => { await systemApi.readMsg(row.id); load() }
onMounted(load)
</script>
