<template>
  <div class="login-wrap">
    <div class="panel">
      <div class="left">
        <div class="logo">校内公共场地借用与审批管理系统</div>
        <p class="slogan">报告厅 · 展厅 · 中庭 · 操场 · 户外空地<br />一站式完成借用申请、风险审批与撤场验收</p>
        <ul>
          <li>时段冲突校验覆盖布场与撤场</li>
          <li>大型、户外、夜间活动自动追加安保审批</li>
          <li>按活动类型生成准备清单并全程留痕</li>
          <li>交接、巡场、验收与线下费用闭环</li>
        </ul>
        <div class="foot">校园资源管理处 · 内部业务系统</div>
      </div>
      <div class="right">
        <h2>账号登录</h2>
        <p class="sub">请使用校内统一账号登录，连续 5 次失败将锁定 10 分钟</p>
        <el-form :model="form" @submit.prevent>
          <el-form-item><el-input v-model="form.username" placeholder="用户名" size="large" /></el-form-item>
          <el-form-item><el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password @keyup.enter="doLogin" /></el-form-item>
          <el-button type="primary" size="large" class="btn" :loading="loading" @click="doLogin">登 录</el-button>
        </el-form>
        <div class="accounts">
          <div class="t">测试账号（密码 123456，管理员 admin123）</div>
          <div class="row" v-for="a in accounts" :key="a.u" @click="fill(a.u)">
            <span class="u">{{ a.u }}</span><span class="r">{{ a.role }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '../api'

const router = useRouter()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })
const accounts = [
  { u: 'applicant', role: '申请人' }, { u: 'dept01', role: '院系审批人' },
  { u: 'venue01', role: '场地管理员' }, { u: 'security01', role: '安保审批人' },
  { u: 'finance', role: '财务登记员' }, { u: 'admin', role: '系统管理员（admin123）' }
]
const fill = (u) => { form.username = u; form.password = u === 'admin' ? 'admin123' : '123456' }

const doLogin = async () => {
  if (!form.username || !form.password) return ElMessage.warning('请输入用户名与密码')
  loading.value = true
  try {
    const res = await authApi.login(form)
    localStorage.setItem('vn_token', res.token)
    localStorage.setItem('vn_user', JSON.stringify(res.user))
    ElMessage.success('登录成功')
    router.push('/')
  } finally { loading.value = false }
}
</script>

<style scoped>
.login-wrap{height:100%;display:flex;align-items:center;justify-content:center;background:#0F3542 url('/uploads/seed/login/bg.jpg') center/cover no-repeat;position:relative}
.login-wrap:before{content:'';position:absolute;inset:0;background:linear-gradient(120deg,rgba(15,53,66,.92),rgba(23,74,92,.72))}
.panel{position:relative;width:940px;height:540px;display:flex;background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 24px 60px rgba(0,0,0,.35)}
.left{width:490px;background:linear-gradient(160deg,#174A5C,#0F3542 72%);color:#fff;padding:46px 42px;position:relative}
.logo{font-size:19px;font-weight:600;line-height:1.5;letter-spacing:.3px}
.slogan{margin-top:14px;color:#A9C9CC;font-size:13px;line-height:1.9}
ul{margin:26px 0 0;padding:0;list-style:none;color:#CFE3E3;font-size:12.5px;line-height:2.3}
ul li:before{content:'';display:inline-block;width:5px;height:5px;border-radius:50%;background:#2A7F86;margin-right:9px;vertical-align:middle}
.foot{position:absolute;left:42px;bottom:32px;color:#7FA6AA;font-size:12px}
.right{flex:1;padding:46px 44px}
.right h2{font-size:19px;color:#174A5C;margin:0 0 6px}
.sub{color:#5D7578;font-size:12.5px;margin:0 0 26px}
.btn{width:100%;margin-top:6px}
.accounts{margin-top:22px;border-top:1px dashed #DCE7E7;padding-top:14px}
.accounts .t{font-size:12px;color:#5D7578;margin-bottom:8px}
.accounts .row{display:flex;justify-content:space-between;padding:5px 8px;border-radius:5px;cursor:pointer;font-size:12.5px}
.accounts .row:hover{background:#F0F6F6}
.accounts .u{color:#174A5C;font-weight:600}
.accounts .r{color:#8CA3A5}
</style>
