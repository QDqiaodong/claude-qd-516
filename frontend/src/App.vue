<template>
  <div>
    <header v-if="!isPlain" class="studio-header">
      <div class="studio-brand">
        <span class="studio-brand-mark">陶</span>
        <span>
          <b>陶艺工坊管理系统</b>
          <em>Pottery Studio · 泥料 · 坯体 · 窑炉 · 作品</em>
        </span>
      </div>

      <!-- 顶部横向分类条：四大业务模块 -->
      <nav class="catbar studio-nav">
        <div
          v-for="m in modules"
          :key="m.path"
          class="catbar-item"
          :class="{ 'is-active': isActive(m.path) }"
          @click="go(m.path)"
        >
          {{ m.name }}
          <span class="cat-sub">{{ m.sub }}</span>
        </div>
      </nav>
    </header>

    <main class="studio-page" :class="{ 'studio-page-plain': isPlain }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

// 凭证打印页是纯净页：不显示系统导航，打印时整页即凭证
const isPlain = computed(() => route.meta?.plain === true)

const modules = [
  { path: '/materials', name: '泥料釉料台账', sub: '种类 · 存量 · 收缩率' },
  { path: '/greenwares', name: '坯体与工位', sub: '干燥阶段 · 分区容量' },
  { path: '/firing', name: '窑炉与烧成', sub: '温度曲线 · 时间轴' },
  { path: '/courses', name: '课程与作品', sub: '体验课 · 寄售' }
]

const isActive = (path) => route.path === path || route.path.startsWith(path + '/')
const go = (path) => router.push(path)
</script>

<style scoped>
.studio-header {
  background: #fff;
  border-bottom: 3px solid var(--el-color-primary);
  padding: 14px 22px 0;
}

.studio-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 6px;
}

.studio-brand span {
  line-height: 1.35;
}

.studio-brand b {
  font-size: 17px;
}

.studio-brand em {
  display: block;
  font-style: normal;
  font-size: 12px;
  color: #9a8b76;
}

.studio-brand-mark {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.studio-nav {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 12px;
}
</style>
