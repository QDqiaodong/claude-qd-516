import { createRouter, createWebHistory } from 'vue-router'
import MaterialsView from './views/MaterialsView.vue'
import GreenwareView from './views/GreenwareView.vue'
import FiringView from './views/FiringView.vue'
import CoursesView from './views/CoursesView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/materials' },
    { path: '/materials', name: 'materials', component: MaterialsView },
    { path: '/greenwares', name: 'greenwares', component: GreenwareView },
    { path: '/firing', name: 'firing', component: FiringView },
    { path: '/courses', name: 'courses', component: CoursesView }
  ]
})

export default router
