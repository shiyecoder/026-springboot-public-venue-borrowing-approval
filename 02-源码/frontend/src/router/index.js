import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', component: () => import('../views/Login.vue') },
  {
    path: '/',
    component: () => import('../layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '场地利用看板' } },
      { path: 'venue', component: () => import('../views/VenueList.vue'), meta: { title: '场地资源库' } },
      { path: 'calendar', component: () => import('../views/CalendarPage.vue'), meta: { title: '预约日历' } },
      { path: 'apply', component: () => import('../views/ApplyList.vue'), meta: { title: '借用申请' } },
      { path: 'apply/:id', component: () => import('../views/ApplyDetail.vue'), meta: { title: '申请详情' } },
      { path: 'approval', component: () => import('../views/ApplyList.vue'), props: { mode: 'approval' }, meta: { title: '借用审批台' } },
      { path: 'prep', component: () => import('../views/PrepBoard.vue'), meta: { title: '准备任务板' } },
      { path: 'handover', component: () => import('../views/ListPage.vue'), props: { cfg: 'handover' }, meta: { title: '入场交接' } },
      { path: 'patrol', component: () => import('../views/PatrolPage.vue'), meta: { title: '活动巡场' } },
      { path: 'acceptance', component: () => import('../views/AcceptancePage.vue'), meta: { title: '撤场验收' } },
      { path: 'fee', component: () => import('../views/FeePage.vue'), meta: { title: '费用登记' } },
      { path: 'message', component: () => import('../views/MessagePage.vue'), meta: { title: '消息中心' } },
      { path: 'system/user', component: () => import('../views/ListPage.vue'), props: { cfg: 'user' }, meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('../views/RolePage.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', component: () => import('../views/ListPage.vue'), props: { cfg: 'menu' }, meta: { title: '菜单管理' } },
      { path: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心' } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('vn_token')
  if (!token && to.path !== '/login') return '/login'
  if (token && to.path === '/login') return '/'
  return true
})

export default router
