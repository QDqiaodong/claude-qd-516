<template>
  <div v-loading="loading">
    <el-breadcrumb class="studio-crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/courses' }">课程与作品</el-breadcrumb-item>
      <el-breadcrumb-item>作品详情</el-breadcrumb-item>
      <el-breadcrumb-item v-if="artwork">{{ artwork.code }} {{ artwork.title }}</el-breadcrumb-item>
    </el-breadcrumb>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      style="margin-bottom: 12px"
    />

    <template v-if="artwork">
      <!-- 并发版本变化提示：别人已更新版本，本页内容已过时，必须重新读取后处理 -->
      <el-alert
        v-if="stale"
        type="warning"
        show-icon
        :closable="false"
        style="margin-bottom: 12px"
        title="凭证版本已变化，请重新读取后处理"
      >
        <div>
          您打开本页时凭证为{{ knownVersionNo ? 'V' + knownVersionNo : '「尚未签发」' }}，期间已被其他工作人员
          {{ latestVersionNo ? `更新为 V${latestVersionNo}` : '变更' }}。页面下方仍为旧内容，请勿据此签发或更正，以免旧数据取代后续记录。
          <el-button type="primary" size="small" style="margin-left: 8px" @click="reload">
            重新读取最新版本
          </el-button>
        </div>
      </el-alert>

      <!-- 作品基本信息：升级前已登记、没有凭证的作品也照常查看 -->
      <div class="studio-panel">
        <h3 class="studio-section-title">作品基本信息</h3>
        <el-descriptions :column="4" border size="small">
          <el-descriptions-item label="作品编号">{{ artwork.code }}</el-descriptions-item>
          <el-descriptions-item label="作品名称">{{ artwork.title }}</el-descriptions-item>
          <el-descriptions-item label="学员姓名">{{ artwork.studentName }}</el-descriptions-item>
          <el-descriptions-item label="归属">
            <el-tag :type="ownerType(artwork.ownerStatus)" effect="dark" size="small">
              {{ ownerText(artwork.ownerStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="所属课程">{{ artwork.courseTitle || '—' }}</el-descriptions-item>
          <el-descriptions-item label="来源坯体">{{ artwork.greenwareCode || '—' }}</el-descriptions-item>
          <el-descriptions-item label="烧成批次">{{ artwork.batchNo || '—' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ dt(artwork.finishedAt) }}</el-descriptions-item>
          <el-descriptions-item label="寄售/成交价">{{ artwork.consignPrice ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="货架位">{{ artwork.shelfNo || '—' }}</el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 来源链核对：泥料、釉料、来源坯体、课程、烧成批次逐段核对 -->
      <div class="studio-panel">
        <h3 class="studio-section-title">
          来源链核对
          <el-tag v-if="check" :type="check.ready ? 'success' : 'danger'" size="small" style="margin-left: 8px">
            {{ check.ready ? '核对通过，可签发凭证' : '核对未通过，不能签发' }}
          </el-tag>
          <el-button link type="primary" size="small" style="margin-left: 10px" @click="reload">重新核对</el-button>
        </h3>
        <ul class="cert-source-list">
          <li
            v-for="s in (check?.segments || [])"
            :key="s.code"
            class="cert-source-item"
            :class="segClass(s.state)"
          >
            <span class="cert-source-name">{{ s.label }}</span>
            <span>
              <span class="cert-source-detail">{{ s.detail || '—' }}</span>
              <span v-if="s.state !== 'OK'" class="cert-source-msg">
                {{ s.state === 'MISSING' ? '来源缺失：' : '关联冲突：' }}{{ s.message }}
              </span>
            </span>
          </li>
        </ul>
        <el-alert
          v-if="check && !check.ready"
          type="error"
          :closable="false"
          show-icon
          style="margin-top: 8px"
          title="存在缺失来源或关联冲突，系统不会生成看似完整的凭证，请先按上述红字补齐/修正对应来源段。"
        />
      </div>

      <!-- 烧成履历凭证 -->
      <div class="studio-panel">
        <h3 class="studio-section-title">
          烧成履历凭证
          <el-tag v-if="current" type="success" size="small" style="margin-left: 8px">
            当前版 V{{ current.versionNo }}
          </el-tag>
          <el-tag v-else type="info" size="small" style="margin-left: 8px">尚未签发</el-tag>
        </h3>

        <!-- 从未签发：允许从当前可核实数据首签 / 为升级前老作品补签 -->
        <template v-if="!current">
          <el-alert
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 10px"
            title="该作品还没有烧成履历凭证。作品登记与流转不受影响；来源核对通过后可从当前可核实数据补签首版，无需重建历史业务。"
          />
          <el-collapse style="margin-bottom: 10px">
            <el-collapse-item title="待签发快照预览（尚未生效；来源缺失段显示为空）" name="preview">
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item v-for="f in previewFields" :key="f.k" :label="f.k">
                  {{ f.v }}
                </el-descriptions-item>
              </el-descriptions>
            </el-collapse-item>
          </el-collapse>
          <el-button type="primary" :disabled="!check?.ready || stale" @click="openIssue">
            签发首版凭证
          </el-button>
          <span v-if="check && !check.ready" class="studio-hint">来源核对未通过，按钮不可用</span>
        </template>

        <!-- 已有当前版 -->
        <template v-else>
          <!-- 资料变化提示 -->
          <el-alert
            v-if="certView.driftCount > 0"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 10px"
            :title="`自当前版（V${current.versionNo}）签发后，有 ${certView.driftCount} 项资料被修正。旧凭证仍保留原内容，可发起更正追加新版本。`"
          />

          <div class="studio-toolbar">
            <el-button type="primary" plain @click="printVersion(current.id)">
              打印当前版 V{{ current.versionNo }}
            </el-button>
            <el-button type="primary" :disabled="!check?.ready || stale" @click="openCorrect">
              发起更正并追加新版本
            </el-button>
            <el-button plain @click="togglePaper">{{ showPaper ? '收起凭证全文' : '查看凭证全文' }}</el-button>
          </div>

          <el-collapse v-model="paperCollapse" style="margin-bottom: 8px">
            <el-collapse-item :title="`凭证全文（V${current.versionNo} · 签发当时快照）`" name="paper">
              <CertificatePaper :cert="current" />
            </el-collapse-item>
          </el-collapse>

          <!-- 差异对照 -->
          <div v-if="certView.drift">
            <div class="studio-toolbar" style="margin: 6px 0">
              <b style="font-size: 13px">当前版内容 与 当前可核实数据 对照</b>
              <el-checkbox v-model="showAllDrift" size="small">显示未变化字段</el-checkbox>
            </div>
            <el-table :data="driftRows" size="small" border stripe style="margin-bottom: 10px">
              <el-table-column prop="group" label="分组" width="150" />
              <el-table-column prop="label" label="字段" width="130" />
              <el-table-column prop="snapshotValue" width="200">
                <template #header>凭证当前版（V{{ current.versionNo }}）</template>
              </el-table-column>
              <el-table-column prop="currentValue" label="当前可核实数据" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.changed ? 'warning' : 'info'" size="small">
                    {{ row.changed ? '已变化' : '一致' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 历史版本 -->
          <div>
            <b style="font-size: 13px">历史版本（仅供追溯，不作为当前版）</b>
            <el-table :data="certView.history || []" size="small" border style="margin-top: 6px">
              <el-table-column label="版本" width="90">
                <template #default="{ row }">V{{ row.versionNo }}</template>
              </el-table-column>
              <el-table-column prop="certificateNo" label="凭证编号" width="160" />
              <el-table-column prop="issuedBy" label="签发/更正人" width="110" />
              <el-table-column label="签发时间" width="160">
                <template #default="{ row }">{{ dt(row.issuedAt) }}</template>
              </el-table-column>
              <el-table-column prop="changeReason" label="更正原因" />
              <el-table-column label="状态" width="100">
                <template #default>
                  <el-tag type="info" size="small">历史版</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" @click="printVersion(row.id)">查看/打印</el-button>
                </template>
              </el-table-column>
            </el-table>
            <div v-if="!certView.history || certView.history.length === 0" class="studio-hint" style="margin-top: 6px">
              暂无历史版本（当前为首版）。
            </div>
          </div>
        </template>
      </div>
    </template>

    <!-- 签发首版 -->
    <el-dialog v-model="issueVisible" title="签发烧成履历凭证（首版）" width="460px">
      <el-alert
        v-if="check && check.ready"
        type="success"
        :closable="false"
        show-icon
        title="来源链全部核对通过，将以当前可核实数据生成 V1 快照。"
        style="margin-bottom: 12px"
      />
      <el-form label-width="96px">
        <el-form-item label="签发人" required>
          <el-input v-model="issueStaff" placeholder="请填写签发工作人员姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="issueVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitIssue">确认签发</el-button>
      </template>
    </el-dialog>

    <!-- 发起更正 -->
    <el-dialog v-model="correctVisible" title="发起凭证更正（追加新版本）" width="760px" top="6vh">
      <el-alert
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
        :title="`将以当前可核实数据生成 V${(current?.versionNo || 0) + 1}，当前 V${current?.versionNo} 自动转为历史版；旧版内容保留、不会被覆盖。`"
      />
      <el-table :data="changedDriftRows" size="small" border max-height="240" style="margin-bottom: 10px">
        <el-table-column prop="label" label="变化字段" width="150" />
        <el-table-column prop="snapshotValue" label="旧版内容" />
        <el-table-column prop="currentValue" label="更正后内容" />
      </el-table>
      <div v-if="changedDriftRows.length === 0" class="studio-hint" style="margin-bottom: 10px">
        当前数据与当前版凭证完全一致，无需更正。
      </div>
      <el-form label-width="96px">
        <el-form-item label="更正人" required>
          <el-input v-model="correctStaff" placeholder="请填写发起更正的工作人员姓名" />
        </el-form-item>
        <el-form-item label="更正原因" required>
          <el-input v-model="correctReason" type="textarea" :rows="2" placeholder="如：学员姓名登记错误 / 批次峰值温度补录" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitting"
          :disabled="changedDriftRows.length === 0 || !correctStaff || !correctReason"
          @click="submitCorrect"
        >
          确认更正并签发 V{{ (current?.versionNo || 0) + 1 }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { artworkApi, certificateApi } from '../api'
import { ownerText, dt, temp, fireText, stageText, glazeText } from '../certFormat'
import CertificatePaper from '../components/CertificatePaper.vue'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()

const loading = ref(true)
const loadError = ref('')
const artwork = ref(null)
const certView = ref({})
const stale = ref(false)
const knownVersionNo = ref(null)
const latestVersionNo = ref(null)
const showPaper = ref(false)
const paperCollapse = ref([])
const showAllDrift = ref(false)
let pollTimer = null

const issueVisible = ref(false)
const issueStaff = ref('')
const correctVisible = ref(false)
const correctStaff = ref('')
const correctReason = ref('')
const submitting = ref(false)

const check = computed(() => certView.value.check || null)
const current = computed(() => certView.value.currentVersion || null)

const ownerType = (s) => ({ TAKEN: 'info', CONSIGN: 'warning', SOLD: 'success' }[s] || 'info')
const segClass = (state) => ({ OK: 'is-ok', MISSING: 'is-missing', CONFLICT: 'is-conflict' }[state] || '')

const driftRows = computed(() => {
  const rows = certView.value.drift || []
  return showAllDrift.value ? rows : rows.filter((r) => r.changed)
})
const changedDriftRows = computed(() => (certView.value.drift || []).filter((r) => r.changed))

// 待签发预览（未生效），把 pendingSnapshot 摊平成键值
const previewFields = computed(() => {
  const p = check.value?.pendingSnapshot
  if (!p) return []
  return [
    ['作品名称', p.snapTitle], ['学员', p.snapStudentName],
    ['课程', p.snapCourseTitle ? `${p.snapCourseCode} ${p.snapCourseTitle}（${p.snapTeacher}）` : ''],
    ['来源坯体', p.snapGreenwareCode ? `${p.snapGreenwareCode} ${p.snapGreenwareName}` : ''],
    ['坯体阶段', p.snapGreenwareStage ? stageText(p.snapGreenwareStage) : ''],
    ['泥料', p.snapClayName ? `${p.snapClayCode} ${p.snapClayName} ${temp(p.snapClayTemp)}` : ''],
    ['釉料', glazeText(p.snapGlazeName)],
    ['烧成批次', p.snapBatchNo], ['烧成类型', p.snapFireType ? fireText(p.snapFireType) : ''],
    ['窑炉', p.snapKilnCode ? `${p.snapKilnCode} ${p.snapKilnName}` : ''],
    ['目标温度', temp(p.snapTargetTemp)], ['实际峰值温度', temp(p.snapPeakTemp)],
    ['装窑时间', dt(p.snapLoadedAt)], ['升温时间', dt(p.snapHeatingAt)],
    ['保温开始', dt(p.snapSoakingAt)], ['冷却开始', dt(p.snapCoolingAt)],
    ['出窑时间', dt(p.snapOutAt)]
  ].map(([k, v]) => ({ k, v: v || '—' }))
})

async function loadAll(markStaleOnChange) {
  const [aw, cv] = await Promise.all([
    artworkApi.detail(props.id),
    certificateApi.view(props.id)
  ])
  artwork.value = aw
  const incomingVersion = cv.currentVersion?.versionNo ?? null
  if (markStaleOnChange && knownVersionNo.value !== null
      && incomingVersion !== knownVersionNo.value) {
    // 轮询发现版本变了：保留旧内容、置过时标记，要求人工重新读取
    latestVersionNo.value = incomingVersion
    stale.value = true
    return
  }
  certView.value = cv
  knownVersionNo.value = incomingVersion
  latestVersionNo.value = incomingVersion
}

async function reload() {
  loading.value = true
  loadError.value = ''
  try {
    await loadAll(false)
    stale.value = false
  } catch (e) {
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

function togglePaper() {
  showPaper.value = !showPaper.value
  paperCollapse.value = showPaper.value ? ['paper'] : []
}

function printVersion(certificateId) {
  // 打印按具体版本取，历史版也打印其落库快照，不受后续资料修正影响
  router.push(`/courses/artworks/${props.id}/certificates/${certificateId}/print`)
}

function openIssue() {
  issueStaff.value = ''
  issueVisible.value = true
}

async function submitIssue() {
  if (!issueStaff.value.trim()) {
    ElMessage.warning('请填写签发人')
    return
  }
  submitting.value = true
  try {
    const cert = await certificateApi.issue(props.id, issueStaff.value.trim())
    ElMessage.success(`已签发 V${cert.versionNo}`)
    issueVisible.value = false
    await reload()
  } catch (e) {
    ElMessage.error(e.message)
  } finally {
    submitting.value = false
  }
}

function openCorrect() {
  if (stale.value) {
    ElMessage.warning('凭证版本已变化，请先重新读取最新版本')
    return
  }
  correctStaff.value = ''
  correctReason.value = ''
  correctVisible.value = true
}

async function submitCorrect() {
  submitting.value = true
  try {
    // 携带打开页面时所依据的版本号；后端在版本已变时返回 409
    const cert = await certificateApi.correct(
      props.id, knownVersionNo.value,
      correctStaff.value.trim(), correctReason.value.trim()
    )
    ElMessage.success(`已追加新版本 V${cert.versionNo}，旧版已转为历史版`)
    correctVisible.value = false
    await reload()
  } catch (e) {
    if (e.status === 409) {
      // 别人已更新：关闭编辑、置过时标记并要求重新读取，旧数据不得取代后续记录
      correctVisible.value = false
      latestVersionNo.value = (await safePeekVersion()) ?? latestVersionNo.value
      stale.value = true
      ElMessageBox.alert(e.message + '。请重新读取最新版本后再处理。', '凭证版本已变化', {
        type: 'warning', confirmButtonText: '重新读取'
      }).then(reload).catch(reload)
    } else {
      ElMessage.error(e.message)
    }
  } finally {
    submitting.value = false
  }
}

async function safePeekVersion() {
  try {
    const cv = await certificateApi.view(props.id)
    return cv.currentVersion?.versionNo ?? null
  } catch {
    return null
  }
}

// 两名工作人员同时打开：周期性探测版本是否已被他人更新
// （含“打开时尚无凭证、期间他人已首签”的情况：null → V1 也算版本变化）
async function pollVersion() {
  if (issueVisible.value || correctVisible.value || submitting.value) {
    return
  }
  try {
    const cv = await certificateApi.view(props.id)
    const v = cv.currentVersion?.versionNo ?? null
    if (v !== knownVersionNo.value) {
      latestVersionNo.value = v
      stale.value = true
    }
  } catch {
    // 轮询失败不打扰页面操作
  }
}

onMounted(async () => {
  await reload()
  pollTimer = setInterval(pollVersion, 12000)
})

onBeforeUnmount(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})
</script>
