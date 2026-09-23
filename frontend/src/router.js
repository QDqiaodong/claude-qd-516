import { createRouter, createWebHistory } from 'vue-router'
import MaterialsView from './views/MaterialsView.vue'
import GreenwareView from './views/GreenwareView.vue'
import FiringView from './views/FiringView.vue'
import CoursesView from './views/CoursesView.vue'
import ArtworkDetailView from './views/ArtworkDetailView.vue'
import CertificatePrintView from './views/CertificatePrintView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/materials' },
    { path: '/materials', name: 'materials', component: MaterialsView },
    { path: '/greenwares', name: 'greenwares', component: GreenwareView },
    { path: '/firing', name: 'firing', component: FiringView },
    { path: '/courses', name: 'courses', component: CoursesView },
    // 作品详情：凭证版本、来源核对、差异与历史版本都在这里
    { path: '/courses/artworks/:id', name: 'artwork-detail', component: ArtworkDetailView, props: true },
    // 凭证打印（独立纯净页），meta.plain 时不显示系统导航
    { path: '/courses/artworks/:id/certificates/:versionId/print',
      name: 'certificate-print', component: CertificatePrintView, props: true, meta: { plain: true } }
  ]
})

export default router
