<template>
  <div class="page">
    <div class="cols">
      <div class="card">
        <div class="card-h"><h3>个人资料</h3></div>
        <div class="avatar-row">
          <el-upload :show-file-list="false" :http-request="uploadAvatar" accept="image/*">
            <img class="big-avatar" :src="form.avatar || '/uploads/seed/avatar/01.jpg'" />
          </el-upload>
          <div>
            <div class="nm">{{ user.realName }}</div>
            <div class="hint">{{ user.roleName }} · {{ user.deptName }}</div>
            <el-button size="small" style="margin-top:8px" @click="save">保存资料</el-button>
          </div>
        </div>
        <el-form label-width="90px" style="margin-top:18px">
          <el-form-item label="姓名"><el-input v-model="form.realName" style="width:240px" /></el-form-item>
          <el-form-item label="联系方式"><el-input v-model="form.phone" style="width:240px" /></el-form-item>
          <el-form-item label="账号"><el-input :value="user.username" disabled style="width:240px" /></el-form-item>
        </el-form>
      </div>
      <div class="card">
        <div class="card-h"><h3>修改密码</h3></div>
        <el-form label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password style="width:240px" /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password style="width:240px" /></el-form-item>
          <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password style="width:240px" /></el-form-item>
          <el-form-item><el-button type="primary" @click="changePwd">保存密码</el-button></el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http, { authApi } from '../api'

const user = ref(JSON.parse(localStorage.getItem('vn_user') || '{}'))
const form = reactive({ realName: '', phone: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

const load = async () => {
  const me = await authApi.me()
  user.value = me
  Object.assign(form, { realName: me.realName, phone: me.phone, avatar: me.avatar })
}
const uploadAvatar = async (opt) => {
  const fd = new FormData(); fd.append('file', opt.file)
  form.avatar = await http.post('/system/upload?bizType=avatar', fd)
  await save()
}
const save = async () => {
  await authApi.updateProfile(form)
  localStorage.setItem('vn_user', JSON.stringify({ ...user.value, ...form }))
  ElMessage.success('资料已保存，右上角头像已同步')
  load()
}
const changePwd = async () => {
  if (pwd.newPassword !== pwd.confirm) return ElMessage.warning('两次输入的新密码不一致')
  await authApi.changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改')
  pwd.oldPassword = pwd.newPassword = pwd.confirm = ''
}
onMounted(load)
</script>

<style scoped>
.cols { display: grid; grid-template-columns: 1fr 420px; gap: 12px; align-items: start; }
.avatar-row { display: flex; gap: 16px; align-items: center; }
.big-avatar { width: 72px; height: 72px; border-radius: 50%; object-fit: cover; border: 2px solid var(--primary-pale); cursor: pointer; }
.nm { font-size: 16px; font-weight: 600; }
.hint { font-size: 12px; color: var(--text3); }
</style>
