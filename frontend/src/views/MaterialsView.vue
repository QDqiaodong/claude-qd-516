<template>
  <div>
    <!-- 顶部横向分类条：材料分类树 -->
    <div class="catbar">
      <div class="catbar-item" :class="{ 'is-active': path.length === 0 }" @click="pick(0, null)">
        全部材料
        <span class="cat-sub">{{ materials.length }} 种</span>
      </div>
      <div
        v-for="node in levels[0] || []"
        :key="node.id"
        class="catbar-item"
        :class="{ 'is-active': path[0] && path[0].id === node.id }"
        @click="pick(0, node)"
      >
        {{ node.name }}
        <span class="cat-sub">{{ node.materialCount }} 种 / {{ node.children.length }} 子类</span>
      </div>
      <el-button type="primary" plain size="small" style="align-self: center" @click="openCategory(null)">
        + 一级分类
      </el-button>
    </div>

    <!-- 进入下级后出现的横向分类条 + 面包屑 -->
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
          <span class="cat-sub">{{ node.materialCount }} 种 / {{ node.children.length }} 子类</span>
        </div>
        <el-button
          v-if="path[idx]"
          type="primary"
          plain
          size="small"
          style="align-self: center"
          @click="openCategory(path[idx].id)"
        >
          + 子分类
        </el-button>
        <el-button
          v-if="path[idx]"
          type="danger"
          plain
          size="small"
          style="align-self: center"
          @click="removeCategory(path[idx])"
        >
          删除该分类
        </el-button>
      </div>
    </template>

    <el-breadcrumb v-if="path.length" class="studio-crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/materials' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>材料台账</el-breadcrumb-item>
      <el-breadcrumb-item v-for="p in path" :key="p.id">{{ p.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="studio-panel">
      <h3 class="studio-section-title">
        {{ path.length ? path[path.length - 1].name + ' · 材料明细' : '全部材料' }}
      </h3>
      <div class="studio-toolbar">
        <el-button type="primary" @click="openMaterial()">+ 登记材料</el-button>
        <el-select v-model="kindFilter" clearable placeholder="按类型筛" style="width: 140px" @change="loadMaterials">
          <el-option label="泥料" value="CLAY" />
          <el-option label="釉料" value="GLAZE" />
        </el-select>
        <span class="studio-hint">当前分类共 {{ materials.length }} 种材料</span>
      </div>

      <el-table :data="materials" border stripe size="small">
        <el-table-column prop="code" label="编号" width="110" />
        <el-table-column prop="name" label="名称" width="140" />
        <el-table-column prop="categoryName" label="分类" width="130" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.kind === 'CLAY' ? 'warning' : 'success'" effect="light">
              {{ row.kind === 'CLAY' ? '泥料' : '釉料' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="库存/安全库存" width="150">
          <template #default="{ row }">
            {{ row.stockKg }} / {{ row.safetyStock }} kg
          </template>
        </el-table-column>
        <el-table-column label="收缩率" width="100">
          <template #default="{ row }">
            {{ row.shrinkRate == null ? '—' : row.shrinkRate + '%' }}
          </template>
        </el-table-column>
        <el-table-column prop="firingTemp" label="烧成温度℃" width="110" />
        <el-table-column prop="pairClayName" label="配套泥料" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="dark">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="200">
          <template #default="{ row }">
            <el-button link type="primary" @click="consume(row)">领用出库</el-button>
            <el-button link type="primary" @click="openMaterial(row)">编辑</el-button>
            <el-button link type="danger" @click="removeMaterial(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 材料编辑弹窗 -->
    <el-dialog v-model="matVisible" :title="matForm.id ? '编辑材料' : '登记材料'" width="520px">
      <el-form :model="matForm" label-width="110px">
        <el-form-item label="编号"><el-input v-model="matForm.code" :disabled="!!matForm.id" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="matForm.name" /></el-form-item>
        <el-form-item label="所属分类">
          <el-select v-model="matForm.categoryId" placeholder="请选择分类" style="width: 100%">
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.label" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="matForm.kind">
            <el-radio value="CLAY">泥料</el-radio>
            <el-radio value="GLAZE">釉料</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="matForm.kind === 'CLAY'" label="收缩率%">
          <el-input-number v-model="matForm.shrinkRate" :min="0" :max="30" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="配套泥料">
          <el-select v-model="matForm.pairClayId" clearable style="width: 100%">
            <el-option v-for="m in clayOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="烧成温度℃"><el-input-number v-model="matForm.firingTemp" :min="0" /></el-form-item>
        <el-form-item label="库存kg"><el-input-number v-model="matForm.stockKg" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="安全库存kg"><el-input-number v-model="matForm.safetyStock" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="matForm.unitPrice" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="matVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMaterial">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分类弹窗 -->
    <el-dialog v-model="catVisible" title="新建分类" width="420px">
      <el-form :model="catForm" label-width="90px">
        <el-form-item label="编码"><el-input v-model="catForm.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="catForm.name" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="catForm.sortNo" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="catVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { materialApi, materialCategoryApi } from '../api'

const tree = ref([])
const path = ref([])
const materials = ref([])
const allMaterials = ref([])
const kindFilter = ref('')

const matVisible = ref(false)
const catVisible = ref(false)
const matForm = ref({})
const catForm = ref({ sortNo: 0 })

/** 逐级展开的横向分类条：levels[0] 是顶层，之后每进一级多一条 */
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

const flatCategories = computed(() => {
  const out = []
  const walk = (nodes, depth) => {
    for (const n of nodes) {
      out.push({ id: n.id, label: '　'.repeat(depth) + n.name })
      walk(n.children, depth + 1)
    }
  }
  walk(tree.value, 0)
  return out
})

const clayOptions = computed(() => allMaterials.value.filter((m) => m.kind === 'CLAY'))

const statusText = (s) => ({ NORMAL: '正常', LOW: '低库存', DEPLETED: '已耗尽' }[s] || s)
const statusType = (s) => ({ NORMAL: 'success', LOW: 'warning', DEPLETED: 'danger' }[s] || 'info')

function pick(levelIdx, node) {
  if (node === null) {
    path.value = []
  } else if (path.value[levelIdx] && path.value[levelIdx].id === node.id) {
    path.value = path.value.slice(0, levelIdx)
  } else {
    path.value = path.value.slice(0, levelIdx).concat([node])
  }
  loadMaterials()
}

async function loadTree() {
  tree.value = await materialCategoryApi.tree()
}

async function loadMaterials() {
  const current = path.value[path.value.length - 1]
  const params = {}
  if (current) {
    params.categoryId = current.id
  }
  const rows = await materialApi.list(params)
  materials.value = kindFilter.value ? rows.filter((m) => m.kind === kindFilter.value) : rows
  allMaterials.value = await materialApi.list({})
}

function openMaterial(row) {
  matForm.value = row
    ? { ...row }
    : {
        code: '',
        name: '',
        categoryId: path.value.length ? path.value[path.value.length - 1].id : null,
        kind: 'CLAY',
        shrinkRate: 8,
        firingTemp: 1200,
        stockKg: 0,
        safetyStock: 10,
        unitPrice: 0
      }
  matVisible.value = true
}

async function submitMaterial() {
  try {
    if (matForm.value.id) {
      await materialApi.update(matForm.value.id, matForm.value)
    } else {
      await materialApi.create(matForm.value)
    }
    matVisible.value = false
    ElMessage.success('已保存')
    await loadMaterials()
    await loadTree()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function consume(row) {
  try {
    const { value } = await ElMessageBox.prompt(`请输入【${row.name}】本次领用数量（kg）`, '领用出库', {
      inputPattern: /^\d+(\.\d+)?$/,
      inputErrorMessage: '只能填数字'
    })
    await materialApi.consume(row.id, value)
    ElMessage.success('出库完成')
    await loadMaterials()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function removeMaterial(row) {
  try {
    await ElMessageBox.confirm(`确认删除材料【${row.name}】？`, '提示', { type: 'warning' })
    await materialApi.remove(row.id)
    ElMessage.success('已删除')
    await loadMaterials()
    await loadTree()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

function openCategory(parentId) {
  catForm.value = { code: '', name: '', sortNo: 0, parentId }
  catVisible.value = true
}

async function submitCategory() {
  try {
    await materialCategoryApi.create(catForm.value)
    catVisible.value = false
    ElMessage.success('分类已创建')
    await loadTree()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function removeCategory(node) {
  try {
    await ElMessageBox.confirm(`确认删除分类【${node.name}】？`, '提示', { type: 'warning' })
    await materialCategoryApi.remove(node.id)
    ElMessage.success('已删除')
    path.value = path.value.slice(0, path.value.indexOf(node))
    await loadTree()
    await loadMaterials()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

onMounted(async () => {
  await loadTree()
  await loadMaterials()
})
</script>
