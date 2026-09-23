import { createRouter, createWebHistory } from 'vue-router'
import MaterialsView from './views/MaterialsView.vue'
import GreenwareView from './views/GreenwareView.vue'
import FiringView from './views/FiringView.vue'
import CoursesView from './views/CoursesView.vue'
import CertificatePrintView from './views/CertificatePrintView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/materials' },
    { path: '/materials', name: 'materials', component: MaterialsView },
    { path: '/greenwares', name: 'greenwares', component: GreenwareView },
    { path: '/firing', name: 'firing', component: FiringView },
    { path: '/courses', name: 'courses', component: CoursesView },
    // 凭证打印页：独立全屏页，内容是指定版本的持久化快照
    {
      path: '/courses/artwork/:artworkId/certificate/:versionNo',
      name: 'certificate-print',
      component: CertificatePrintView
    }
  ]
})

export default router
