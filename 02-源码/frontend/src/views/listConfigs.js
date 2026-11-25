// 配置化列表：把字段、映射和接口集中在这里，页面只负责渲染
import { systemApi, opsApi } from '../api'
import { formatTime } from '../utils/format'

export const configs = {
  user: {
    title: '用户',
    fetch: (q) => systemApi.users(q),
    columns: [
      { prop: 'username', label: '账号', minWidth: 120 },
      { prop: 'realName', label: '姓名', minWidth: 100 },
      { prop: 'roleName', label: '角色', minWidth: 110 },
      { prop: 'deptName', label: '所属单位', minWidth: 150 },
      { prop: 'phone', label: '联系电话', minWidth: 130 },
      { prop: 'status', label: '状态', minWidth: 90, type: 'status' },
      { prop: 'lastLogin', label: '最近登录', minWidth: 160, type: 'time' },
      { prop: 'createTime', label: '创建时间', minWidth: 160, type: 'time' }
    ],
    fields: [
      { prop: 'username', label: '账号', required: true },
      { prop: 'realName', label: '姓名', required: true },
      { prop: 'role', label: '角色编码', type: 'select', options: ['admin', 'applicant', 'dept', 'venue', 'security', 'finance'] },
      { prop: 'roleName', label: '角色名称' },
      { prop: 'deptName', label: '所属单位' },
      { prop: 'phone', label: '联系电话' }
    ],
    save: (d) => systemApi.saveUser(d),
    noDelete: true
  },
  menu: {
    title: '菜单',
    fetch: (q) => systemApi.menus().then((list) => ({ list, total: list.length })),
    columns: [
      { prop: 'name', label: '菜单名称', minWidth: 150 },
      { prop: 'path', label: '路由地址', minWidth: 170 },
      { prop: 'parentId', label: '上级ID', minWidth: 90, align: 'right' },
      { prop: 'sortNo', label: '排序', minWidth: 80, align: 'right' },
      { prop: 'roleCodes', label: '可见角色', minWidth: 240 },
      { prop: 'hidden', label: '是否隐藏', minWidth: 100, type: 'status' }
    ],
    noSave: true,
    noDelete: true
  },
  handover: {
    title: '交接记录',
    fetch: (q) => opsApi.handovers(q),
    columns: [
      { prop: 'applyNo', label: '申请编号', minWidth: 130, formatter: (r) => r.applyNo || ('#' + r.applyId) },
      { prop: 'venueState', label: '场地现状', minWidth: 220, showOverflow: true },
      { prop: 'itemsDelivered', label: '交付物资', minWidth: 180 },
      { prop: 'electricReading', label: '电表示数', minWidth: 110, align: 'right' },
      { prop: 'keyInfo', label: '钥匙/卡', minWidth: 170 },
      { prop: 'applicantConfirm', label: '申请人确认', minWidth: 110 },
      { prop: 'managerConfirm', label: '管理员确认', minWidth: 110 },
      { prop: 'handoverTime', label: '交接时间', minWidth: 160, type: 'time' }
    ],
    noSave: true,
    noDelete: true
  }
}

export const fmt = formatTime
