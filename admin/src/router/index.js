import Vue from 'vue'
import VueRouter from 'vue-router'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    component: () => import('../views/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/Dashboard.vue')
      },
      {
        path: 'admin/recharge-audit', // 新增充值审核路由
        name: 'AdminRechargeAudit',
        component: () => import('../views/admin/RechargeAudit.vue'),
        meta: { role: [2] } // 仅管理员可访问
      },
      {
        path: 'admin/review-audit',
        name: 'AdminReviewAudit',
        component: () => import('../views/admin/AdminReviewAudit.vue'),
        meta: { role: [2] } // 仅管理员可访问
      },
      // 商家路由
      {
        path: 'merchant/dashboard',
        name: 'MerchantDashboard',
        component: () => import('../views/merchant/MerchantDashboard.vue'),
        meta: { role: [1] }
      },
      {
        path: 'merchant/products',
        name: 'MerchantProducts',
        component: () => import('../views/merchant/ProductList.vue'),
        meta: { role: [1] }
      },
      {
        path: 'merchant/product/add',
        name: 'MerchantProductAdd',
        component: () => import('../views/merchant/ProductForm.vue'),
        meta: { role: [1] }
      },
      {
        path: 'merchant/product/edit/:id',
        name: 'MerchantProductEdit',
        component: () => import('../views/merchant/ProductForm.vue'),
        meta: { role: [1] }
      },
      {
        path: 'merchant/orders',
        name: 'MerchantOrders',
        component: () => import('../views/merchant/OrderList.vue'),
        meta: { role: [1] }
      },
      // 管理员路由
      {
        path: 'admin/users',
        name: 'AdminUsers',
        component: () => import('../views/admin/UserList.vue'),
        meta: { role: [2] }
      },
      {
        path: 'admin/merchants',
        name: 'AdminMerchants',
        component: () => import('../views/admin/MerchantList.vue'),
        meta: { role: [2] }
      },
      {
        path: 'admin/products',
        name: 'AdminProducts',
        component: () => import('../views/admin/ProductList.vue'),
        meta: { role: [2] }
      },
      {
        path: 'admin/orders',
        name: 'AdminOrders',
        component: () => import('../views/admin/OrderList.vue'),
        meta: { role: [2] }
      },
      {
        path: 'admin/categories',
        name: 'AdminCategories',
        component: () => import('../views/admin/CategoryList.vue'),
        meta: { role: [2] }
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

router.beforeEach((to, from, next) => {
  if (to.matched.some(record => record.meta && record.meta.requiresAuth)) {
    if (!store.state.user.token) {
      next({ name: 'Login' })
    } else {
      const userRole = store.state.user.info?.role
      // 查找有role限制的路由记录
      const roleRecord = to.matched.find(record => record.meta && record.meta.role)
      if (roleRecord && roleRecord.meta.role) {
        const allowedRoles = roleRecord.meta.role
        if (Array.isArray(allowedRoles) && userRole !== null && userRole !== undefined) {
          if (!allowedRoles.includes(userRole)) {
            next({ name: 'Dashboard' })
            return
          }
        }
      }
      next()
    }
  } else {
    next()
  }
})

export default router

