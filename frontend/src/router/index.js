import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ProductRegistrationView from '../views/ProductRegistrationView.vue'
import ValidateView from '../views/ValidateView.vue'

const routes = [
  {
    path: '/register',
    name: 'register',
    component: ProductRegistrationView
  },
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/validate',
    name: 'validate',
    component: ValidateView
  }
]

const router = createRouter({
  history: createWebHistory(process.env.BASE_URL),
  routes
})

export default router 