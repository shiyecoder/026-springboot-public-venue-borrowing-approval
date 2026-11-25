<template>
  <div class="app">
    <aside class="side">
      <div class="brand"><span class="dot">场</span><span class="t">场地借用与审批</span></div>
      <div class="menu">
        <template v-for="m in topMenus" :key="m.id">
          <router-link v-if="!childrenOf(m.id).length" class="item" :class="{ on: isOn(m.path) }" :to="m.path">
            <span class="ico" v-html="iconSvg(m.icon)"></span><span>{{ m.name }}</span>
          </router-link>
          <div v-else>
            <div class="grp">{{ m.name }}</div>
            <router-link v-for="c in childrenOf(m.id)" :key="c.id" class="item" :class="{ on: isOn(c.path) }" :to="c.path">
              <span class="ico" v-html="iconSvg(c.icon)"></span><span>{{ c.name }}</span>
            </router-link>
          </div>
        </template>
      </div>
    </aside>
    <div class="main">
      <header class="top">
        <div class="crumb">{{ $route.meta.title || '' }}</div>
        <div class="spacer"></div>
        <div class="ic" @click="$router.push('/message')">
          <span v-html="iconSvg('bell')"></span>
          <em v-if="unread > 0" class="badge">{{ unread }}</em>
        </div>
        <div class="me" @click="$router.push('/profile')">
          <img :src="user.avatar || '/uploads/seed/avatar/01.jpg'" alt="" />
          <span>{{ user.realName }} · {{ user.roleName }}</span>
        </div>
        <a class="out" @click="logout">退出</a>
      </header>
      <main class="content"><router-view /></main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authApi, systemApi } from '../api'

const route = useRoute()
const router = useRouter()
const menus = ref([])
const user = ref(JSON.parse(localStorage.getItem('vn_user') || '{}'))
const unread = ref(0)

const topMenus = computed(() => menus.value.filter((m) => m.parentId === 0))
const childrenOf = (id) => menus.value.filter((m) => m.parentId === id)
const isOn = (path) => route.path === path || route.path.startsWith(path + '/')

const icons = {
  dash: '<rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/>',
  'i-venue': '<path d="M3 20h18M6 20V9l6-5 6 5v11M9 20v-5h6v5"/>',
  'i-calendar': '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/>',
  'i-apply': '<path d="M6 2h9l5 5v15H6a2 2 0 01-2-2V4a2 2 0 012-2z"/><path d="M14 2v6h6M8 13h8M8 17h5"/>',
  'i-approve': '<path d="M9 17l-5-5 1.5-1.5L9 14l9-9 1.5 1.5L9 17z"/>',
  'i-prep': '<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V4h6v3M8 12h8M8 16h5"/>',
  'i-handover': '<path d="M12 3l9 4v3H3V7l9-4zM4 10v7H3v2h18v-2h-1v-7h-2v7h-3v-7h-2v7H9v-7H6v7H5v-7"/>',
  'i-patrol': '<circle cx="12" cy="11" r="3"/><path d="M12 2a8 8 0 00-8 8c0 6 8 12 8 12s8-6 8-12a8 8 0 00-8-8z"/>',
  'i-accept': '<path d="M12 2l8 3v6c0 5-3.5 9.7-8 11-4.5-1.3-8-6-8-11V5l8-3z"/><path d="M8.5 12l2.5 2.5 4.5-5"/>',
  'i-fee': '<circle cx="12" cy="12" r="9"/><path d="M12 7v10M9.5 9.5h5M9.5 14h5"/>',
  'i-system': '<circle cx="12" cy="12" r="3"/><path d="M12 3v2.5M12 18.5V21M3 12h2.5M18.5 12H21M5.6 5.6l1.8 1.8M16.6 16.6l1.8 1.8M18.4 5.6l-1.8 1.8M7.4 16.6l-1.8 1.8"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 4-6 8-6s8 2 8 6"/>',
  bell: '<path d="M6 16V11a6 6 0 1112 0v5l2 3H4l2-3z"/>'
}
const iconSvg = (name) =>
  `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">${icons[name] || icons.set}</svg>`

const logout = () => {
  localStorage.removeItem('vn_token')
  localStorage.removeItem('vn_user')
  router.push('/login')
}

onMounted(async () => {
  menus.value = await authApi.menus()
  try {
    const p = await systemApi.messages({ page: 1, size: 20 })
    unread.value = p.list.filter((x) => x.readFlag === 0).length
  } catch (e) {
    unread.value = 0
  }
})
</script>

<style scoped>
.app { display: flex; min-height: 100vh; }
.side { width: 220px; flex: 0 0 220px; background: linear-gradient(180deg,#123B47 0%,#0E2F39 100%); color: #D5E6E8; display: flex; flex-direction: column; }
.brand { height: 56px; display: flex; align-items: center; gap: 10px; padding: 0 18px; color: #fff; font-weight: 600; border-bottom: 1px solid rgba(255,255,255,.08); }
.brand .dot { width: 22px; height: 22px; border-radius: 6px; background: #7FD0CB; color: #123B47; display: grid; place-items: center; font-size: 12px; }
.menu { padding: 8px 0; overflow-y: auto; }
.grp { padding: 12px 18px 6px; font-size: 12px; color: #77A2A9; letter-spacing: .5px; }
.item { display: flex; align-items: center; gap: 10px; height: 40px; padding: 0 18px; color: #D5E6E8; text-decoration: none; border-left: 3px solid transparent; font-size: 14px; }
.item:hover { background: rgba(255,255,255,.07); color: #fff; }
.item.on { background: rgba(127,208,203,.16); border-left-color: #7FD0CB; color: #fff; font-weight: 500; }
.main { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.top { height: 56px; background: var(--primary); color: #fff; display: flex; align-items: center; gap: 16px; padding: 0 24px; }
.crumb { font-size: 13px; color: rgba(255,255,255,.72); }
.spacer { flex: 1; }
.ic { width: 32px; height: 32px; border-radius: 6px; display: grid; place-items: center; background: rgba(255,255,255,.12); position: relative; cursor: pointer; color: #fff; }
.badge { position: absolute; top: -4px; right: -4px; background: var(--danger); color: #fff; font-size: 10px; height: 15px; min-width: 15px; border-radius: 8px; display: grid; place-items: center; font-style: normal; }
.me { display: flex; align-items: center; gap: 8px; font-size: 13px; cursor: pointer; }
.me img { width: 28px; height: 28px; border-radius: 50%; object-fit: cover; border: 1px solid rgba(255,255,255,.4); }
.out { color: rgba(255,255,255,.78); font-size: 13px; cursor: pointer; }
.content { padding: 24px; flex: 1; background: var(--bg); }
</style>
