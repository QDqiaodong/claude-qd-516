<template>
  <div>
    <!-- 顶部横向分类条：工位分区树 -->
    <div class="catbar">
      <div class="catbar-item" :class="{ 'is-active': path.length === 0 }" @click="pick(0, null)">
        全部工位
        <span class="cat-sub">{{ greenwares.length }} 件坯体</span>
      </div>
      <div
        v-for="node in levels[0] || []"
        :key="node.id"
        class="catbar-item"
        :class="{ 'is-active': path[0] && path[0].id === node.id }"
        @click="pick(0, node)"
      >
        {{ node.name }}
        <span class="cat-sub">{{ node.greenwareCount }} 件 / {{ node.children.length }} 子区</span>
      </div>
    </div>

    <template v-for="(level, idx) in levels.slice(1)" :key="idx">
      <div v-if="level.length" class="catbar">
        <div
          v-for="node in level"
          :key="node.id"
          class="catbar-item"
          :class="{ 'is-active': path[idx + 1] && path[idx + 1].id === node.id }"
          @click="pick(idx + 1, node)"
        >
          {{ node.name }}
          <span class="cat-sub">
            {{ node.capacity == null ? '非末级' : node.greenwareCount + '/' + node.capacity }}
          </span>
        </div>
      </div>
    </template>

    <el-breadcrumb v-if="path.length" class="studio-crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/greenwares' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>坯体与工位</el-breadcrumb-item>
      <el-breadcrumb-item v-for="p in path" :key="p.id">{{ p.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="studio-panel">
      <h3 class="studio-section-title">
        {{ path.length ? path[path.length - 1].name + ' · 坯体' : '全部坯体' }}
      </h3>
      <div class="studio-toolbar">
        <el-button type="primary" @click="openCreate">+ 新坯体</el-button>
        <el-select v-model="stageFilter" clearable placeholder="按干燥阶段筛" style="width: 160px" @change="loadList">
          <el-option v-for="s in STAGES" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <span class="studio-hint">
          干燥阶段只能一级一级推进：已成型 → 晾坯中 → 可素烧 → 已素烧 → 已施釉 → 烧制中 → 已完成
        </span>
      </div>

      <el-table :data="greenwares" border stripe size="small">
        <el-table-column prop="code" label="编号" width="105" />
        <el-table-column prop="name" label="坯体" width="130" />
        <el-table-column prop="clayName" label="泥料" width="110" />
        <el-table-column prop="glazeName" label="釉料" width="110" />
        <el-table-column prop="areaName" label="所在工位" width="120" />
        <el-table-column label="干燥阶段" width="110">
          <template #default="{ row }">
            <el-tag :type="stageType(row.stage)" effect="light">{{ stageText(row.stage) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="含水率" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.moisture > 12 ? '#e6a23c' : '#67c23a' }">{{ row.moisture }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="heightCm" label="高度cm" width="90" />
        <el-table-column label="操作" min-width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="advance(row)">推进阶段</el-button>
            <el-button link type="primary" @click="move(row)">转区</el-button>
            <el-button link type="primary" @click="adjust(row)">改含水率</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑坯体' : '新建坯体'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编号"><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="泥料">
          <el-select v-model="form.clayId" style="width: 100%">
            <el-option v-for="m in clays" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="釉料">
          <el-select v-model="form.glazeId" clearable style="width: 100%">
            <el-option v-for="m in glazes" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="工位分区">
          <el-select v-model="form.areaId" style="width: 100%">
            <el-option v-for="a in leafAreas" :key="a.id" :label="a.label" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="含水率%"><el-input-number v-model="form.moisture" :min="0" :max="40" :precision="2" /></el-form-item>
        <el-form-item label="高度cm"><el-input-number v-model="form.heightCm" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { greenwareApi, workAreaApi, materialApi } from '../api'

const STAGES = [
  { value: 'SHAPED', label: '已成型' },
  { value: 'DRYING', label: '晾坯中' },
  { value: 'BISQUE_READY', label: '可素烧' },
  { value: 'BISQUED', label: '已素烧' },
  { value: 'GLAZED', label: '已施釉' },
  { value: 'FIRING', label: '烧制中' },
  { value: 'FINISHED', label: '已完成' }
]

const STAGE_NEXT = {
  SHAPED: 'DRYING',
  DRYING: 'BISQUE_READY',
  BISQUE_READY: 'BISQUED',
  BISQUED: 'GLAZED',
  GLAZED: 'FIRING',
  FIRING: 'FINISHED'
}

const tree = ref([])
const path = ref([])
const greenwares = ref([])
const areas = ref([])
const materials = ref([])
const stageFilter = ref('')
const formVisible = ref(false)
const form = ref({})

const levels = computed(() => {
  const out = [tree.value]
  let cursor = tree.value
  for (let i = 0; i < path.value.length; i++) {
    const hit = cursor.find((n) => n.id === path.value[i].id)
    if (!hit || !hit.children.length) {
      break
    }
    out.push(hit.children)
    cursor = hit.children
  }
  return out
})

const clays = computed(() => materials.value.filter((m) => m.kind === 'CLAY'))
const glazes = computed(() => materials.value.filter((m) => m.kind === 'GLAZE'))

const leafAreas = computed(() => {
  const out = []
  const walk = (nodes, prefix) => {
    for (const n of nodes) {
      const label = prefix ? prefix + ' / ' + n.name : n.name
      if (!n.children.length) {
        out.push({ id: n.id, label: label + (n.capacity != null ? ` (${n.greenwareCount}/${n.capacity})` : '') })
      } else {
        walk(n.children, label)
      }
    }
  }
  walk(tree.value, '')
  return out
})

const stageText = (s) => (STAGES.find((x) => x.value === s) || {}).label || s
const stageType = (s) =>
  ({ SHAPED: 'info', DRYING: 'warning', BISQUE_READY: 'primary', BISQUED: 'success', GLAZED: 'success', FIRING: 'danger', FINISHED: 'success' }[s] || 'info')

function pick(levelIdx, node) {
  if (node === null) {
    path.value = []
  } else if (path.value[levelIdx] && path.value[levelIdx].id === node.id) {
    path.value = path.value.slice(0, levelIdx)
  } else {
    path.value = path.value.slice(0, levelIdx).concat([node])
  }
  loadList()
}

async function loadTree() {
  tree.value = await workAreaApi.tree()
  areas.value = await workAreaApi.list()
}

async function loadList() {
  const current = path.value[path.value.length - 1]
  const params = {}
  if (current) {
    params.areaId = current.id
  }
  const rows = await greenwareApi.list(params)
  greenwares.value = stageFilter.value ? rows.filter((r) => r.stage === stageFilter.value) : rows
}

function openCreate() {
  form.value = {
    code: '',
    name: '',
    clayId: clays.value.length ? clays.value[0].id : null,
    glazeId: null,
    areaId: leafAreas.value.length ? leafAreas.value[0].id : null,
    moisture: 20,
    heightCm: 10
  }
  formVisible.value = true
}

async function submit() {
  try {
    await greenwareApi.create(form.value)
    formVisible.value = false
    ElMessage.success('已创建')
    await loadList()
    await loadTree()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function advance(row) {
  const next = STAGE_NEXT[row.stage]
  if (!next) {
    ElMessage.info('已经是最终阶段了')
    return
  }
  try {
    await ElMessageBox.confirm(
      `把【${row.code}】从「${stageText(row.stage)}」推进到「${stageText(next)}」？`,
      '推进干燥阶段',
      { type: 'warning' }
    )
    await greenwareApi.stage(row.id, next)
    ElMessage.success('阶段已推进')
    await loadList()
    await loadTree()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function move(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `输入目标工位 id（末级工位）：${leafAreas.value.map((a) => a.id + '=' + a.label).join('，')}`,
      '坯体转区',
      { inputPattern: /^\d+$/, inputErrorMessage: '只能填数字 id' }
    )
    await greenwareApi.move(row.id, Number(value))
    ElMessage.success('已转区')
    await loadList()
    await loadTree()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function adjust(row) {
  try {
    const { value } = await ElMessageBox.prompt(`设置【${row.code}】当前含水率（%）`, '含水率', {
      inputPattern: /^\d+(\.\d+)?$/,
      inputErrorMessage: '只能填数字'
    })
    await greenwareApi.update(row.id, { moisture: Number(value) })
    ElMessage.success('已更新')
    await loadList()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function removeRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除坯体【${row.code}】？`, '提示', { type: 'warning' })
    await greenwareApi.remove(row.id)
    ElMessage.success('已删除')
    await loadList()
    await loadTree()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

onMounted(async () => {
  materials.value = await materialApi.list({})
  await loadTree()
  await loadList()
})
</script>
