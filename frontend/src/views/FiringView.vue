<template>
  <div>
    <!-- 顶部横向分类条：窑炉 -->
    <div class="catbar">
      <div class="catbar-item" :class="{ 'is-active': kilnId === null }" @click="selectKiln(null)">
        全部窑炉
        <span class="cat-sub">{{ batches.length }} 个批次</span>
      </div>
      <div
        v-for="k in kilns"
        :key="k.id"
        class="catbar-item"
        :class="{ 'is-active': kilnId === k.id }"
        @click="selectKiln(k.id)"
      >
        {{ k.name }}
        <span class="cat-sub">{{ k.maxTemp }}℃ / {{ k.volumeL }}L / {{ k.runningBatchCount }} 在烧</span>
      </div>
      <el-button type="primary" plain size="small" style="align-self: center" @click="openCreate">
        + 新建批次
      </el-button>
    </div>

    <el-breadcrumb v-if="currentKiln" class="studio-crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/firing' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>窑炉与烧成</el-breadcrumb-item>
      <el-breadcrumb-item>{{ currentKiln.name }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="studio-panel">
      <h3 class="studio-section-title">烧成时间轴（点节点看该阶段详情）</h3>
      <p class="studio-hint" style="margin-top: -6px">
        装窑 → 升温 → 保温 → 冷却 → 出窑，只能一级一级往前推；点已走过的节点看当时的温度与件数
      </p>

      <div
        v-for="b in batches"
        :key="b.id"
        class="batch-card"
        :class="{ 'is-running': b.stage !== 'OUT' }"
      >
        <div class="batch-head">
          <div>
            <span class="batch-title">{{ b.batchNo }}</span>
            <el-tag size="small" :type="b.fireType === 'BISQUE' ? 'warning' : 'success'" effect="light">
              {{ b.fireType === 'BISQUE' ? '素烧' : '釉烧' }}
            </el-tag>
            <el-tag size="small" :type="b.stage === 'OUT' ? 'info' : 'danger'" effect="dark">
              {{ stageText(b.stage) }}
            </el-tag>
          </div>
          <div class="batch-meta">
            {{ b.kilnName }} · 目标 {{ b.targetTemp }}℃ · 峰值 {{ b.peakTemp || '—' }}℃ ·
            {{ b.greenwareCount }}/{{ b.kilnCapacity }} 件
          </div>
          <div style="display: flex; gap: 8px">
            <el-button size="small" type="primary" plain @click="openLoad(b)">装窑</el-button>
            <el-button size="small" type="primary" @click="advance(b)">推进下一阶段</el-button>
            <el-button size="small" type="danger" plain @click="removeBatch(b)">删除</el-button>
          </div>
        </div>

        <div class="kiln-track" style="margin-top: 14px">
          <div
            v-for="(node, i) in nodesOf(b)"
            :key="node.key"
            class="kiln-node"
            :class="{
              'is-done': i < stageIndex(b.stage),
              'is-current': i === stageIndex(b.stage),
              'is-active': activeKey === b.id + ':' + node.key
            }"
            @click="showNode(b, node)"
          >
            <div class="kiln-node-title">{{ node.label }}</div>
            <div class="kiln-node-desc">{{ node.desc }}</div>
          </div>
        </div>
      </div>

      <el-empty v-if="!batches.length" description="该窑炉下暂无烧成批次" />
    </div>

    <!-- 节点详情 -->
    <el-drawer v-model="detailVisible" :title="detail.title" size="34%">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="批次号">{{ detail.batch ? detail.batch.batchNo : '—' }}</el-descriptions-item>
        <el-descriptions-item label="窑炉">{{ detail.batch ? detail.batch.kilnName : '—' }}</el-descriptions-item>
        <el-descriptions-item label="阶段">{{ detail.label }}</el-descriptions-item>
        <el-descriptions-item label="发生时间">{{ detail.desc }}</el-descriptions-item>
        <el-descriptions-item label="目标温度">{{ detail.batch ? detail.batch.targetTemp + '℃' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="峰值温度">{{ detail.batch ? (detail.batch.peakTemp || '—') : '—' }}</el-descriptions-item>
        <el-descriptions-item label="装窑件数">{{ detail.batch ? detail.batch.greenwareCount + ' 件' : '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.batch ? detail.batch.remark || '—' : '—' }}</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin: 16px 0 8px">批次内坯体</h4>
      <el-table :data="batchGreenwares" size="small" border>
        <el-table-column prop="code" label="编号" />
        <el-table-column prop="name" label="名称" />
        <el-table-column prop="clayName" label="泥料" />
      </el-table>
    </el-drawer>

    <!-- 新建批次 -->
    <el-dialog v-model="createVisible" title="新建烧成批次" width="460px">
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="批次号"><el-input v-model="createForm.batchNo" /></el-form-item>
        <el-form-item label="窑炉">
          <el-select v-model="createForm.kilnId" style="width: 100%">
            <el-option v-for="k in kilns" :key="k.id" :label="k.name + '（上限 ' + k.maxTemp + '℃）'" :value="k.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="烧成类型">
          <el-radio-group v-model="createForm.fireType">
            <el-radio value="BISQUE">素烧</el-radio>
            <el-radio value="GLAZE">釉烧</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="目标温度℃"><el-input-number v-model="createForm.targetTemp" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="createForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <p class="studio-hint">素烧目标温度 700~900℃，釉烧 1100~1320℃，且不得超过窑炉上限</p>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { kilnApi, firingBatchApi, greenwareApi } from '../api'

const STAGES = [
  { key: 'LOADING', label: '装窑' },
  { key: 'HEATING', label: '升温' },
  { key: 'SOAKING', label: '保温' },
  { key: 'COOLING', label: '冷却' },
  { key: 'OUT', label: '出窑' }
]

const TIME_FIELD = {
  LOADING: 'loadedAt',
  HEATING: 'heatingAt',
  SOAKING: 'soakingAt',
  COOLING: 'coolingAt',
  OUT: 'outAt'
}

const kilns = ref([])
const batches = ref([])
const kilnId = ref(null)
const activeKey = ref('')
const detailVisible = ref(false)
const detail = ref({})
const batchGreenwares = ref([])
const createVisible = ref(false)
const createForm = ref({ batchNo: '', kilnId: null, fireType: 'BISQUE', targetTemp: 800, remark: '' })

const currentKiln = computed(() => kilns.value.find((k) => k.id === kilnId.value) || null)

const stageText = (s) => (STAGES.find((x) => x.key === s) || {}).label || s
const stageIndex = (s) => {
  const i = STAGES.findIndex((x) => x.key === s)
  return i < 0 ? 0 : i
}

function nodesOf(b) {
  return STAGES.map((s) => ({
    key: s.key,
    label: s.label,
    desc: b[TIME_FIELD[s.key]] ? String(b[TIME_FIELD[s.key]]).replace('T', ' ') : '尚未到达'
  }))
}

async function loadKilns() {
  kilns.value = await kilnApi.list()
}

async function loadBatches() {
  batches.value = await firingBatchApi.list(kilnId.value ? { kilnId: kilnId.value } : {})
}

function selectKiln(id) {
  kilnId.value = id
  loadBatches()
}

async function showNode(b, node) {
  activeKey.value = b.id + ':' + node.key
  detail.value = { batch: b, label: node.label, title: `${b.batchNo} · ${node.label}`, desc: node.desc }
  batchGreenwares.value = await firingBatchApi.greenwares(b.id)
  detailVisible.value = true
}

function openCreate() {
  createForm.value = {
    batchNo: 'FB-' + Date.now().toString().slice(-8),
    kilnId: kilnId.value || (kilns.value.length ? kilns.value[0].id : null),
    fireType: 'BISQUE',
    targetTemp: 800,
    remark: ''
  }
  createVisible.value = true
}

async function submitCreate() {
  try {
    await firingBatchApi.create(createForm.value)
    createVisible.value = false
    ElMessage.success('批次已建，窑炉转入烧制中')
    await loadKilns()
    await loadBatches()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function advance(b) {
  const idx = stageIndex(b.stage)
  if (idx >= STAGES.length - 1) {
    ElMessage.info('该批次已出窑')
    return
  }
  const next = STAGES[idx + 1]
  try {
    let peakTemp = null
    if (next.key === 'OUT' && b.peakTemp == null) {
      const { value } = await ElMessageBox.prompt(`出窑前请登记【${b.batchNo}】的峰值温度（℃）`, '峰值温度', {
        inputPattern: /^\d+$/,
        inputErrorMessage: '只能填整数'
      })
      peakTemp = Number(value)
    }
    await firingBatchApi.stage(b.id, next.key, peakTemp)
    ElMessage.success('已推进到「' + next.label + '」')
    await loadKilns()
    await loadBatches()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function openLoad(b) {
  try {
    const rows = await greenwareApi.list({})
    const candidates = rows.filter((r) => r.firingBatchId == null)
    const options = candidates.map((r) => `${r.id}=${r.code} ${r.name}`).join('，')
    const { value } = await ElMessageBox.prompt(
      `可装窑坯体：${options || '（无可装窑坯体）'}`,
      `往 ${b.batchNo} 装窑`,
      { inputPattern: /^\d+$/, inputErrorMessage: '只能填坯体 id' }
    )
    await firingBatchApi.load(b.id, Number(value))
    ElMessage.success('装窑完成')
    await loadBatches()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function removeBatch(b) {
  try {
    await ElMessageBox.confirm(`确认删除批次【${b.batchNo}】？`, '提示', { type: 'warning' })
    await firingBatchApi.remove(b.id)
    ElMessage.success('已删除')
    await loadKilns()
    await loadBatches()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

onMounted(async () => {
  await loadKilns()
  await loadBatches()
})
</script>
