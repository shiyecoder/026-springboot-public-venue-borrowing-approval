<template>
  <div class="page">
    <div class="card">
      <div class="card-h"><h3>角色管理</h3><span class="hint">为角色分配菜单，菜单按角色动态下发</span></div>
      <el-table :data="roles">
        <el-table-column prop="roleCode" label="角色编码" width="140" />
        <el-table-column prop="roleName" label="角色名称" width="160" />
        <el-table-column prop="remark" label="职责说明" min-width="320" />
        <el-table-column label="操作" width="140" align="right"><template #default="{ row }"><el-button link type="primary" @click="open(row)">分配菜单</el-button></template></el-table-column>
      </el-table>
    </div>
    <el-drawer v-model="dlg" :title="`为「${cur?.roleName}」分配菜单`" size="520px">
      <el-checkbox-group v-model="checked">
        <div v-for="m in menus" :key="m.id" class="mrow">
          <el-checkbox :label="m.id"><span :style="{ paddingLeft: m.parentId ? '16px' : '0' }">{{ m.name }}</span></el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer><el-button @click="dlg=false">取消</el-button><el-button type="primary" @click="save">保存</el-button></template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '../api'

const roles = ref([]); const menus = ref([]); const checked = ref([]); const dlg = ref(false); const cur = ref(null)

const load = async () => { roles.value = await systemApi.roles(); menus.value = await systemApi.menus() }
const open = (row) => {
  cur.value = row
  checked.value = menus.value.filter((m) => (m.roleCodes || '').includes(row.roleCode)).map((m) => m.id)
  dlg.value = true
}
const save = async () => {
  await systemApi.assign(cur.value.id, checked.value)
  ElMessage.success('菜单分配已保存')
  dlg.value = false
  load()
}
onMounted(load)
</script>

<style scoped>
.mrow { padding: 4px 0; }
.hint { font-size: 12px; color: var(--text3); }
</style>
