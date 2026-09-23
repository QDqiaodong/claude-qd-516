<template>
  <div class="cert-panel">
    <!-- 加载/空状态由父组件保证 artwork 存在 -->
    <template v-if="agg">
      <!-- ===== 状态总览 ===== -->
      <div class="cert-statusbar">
        <template v-if="!agg.issued">
          <el-tag type="info" effect="plain" size="large">未签发凭证</el-tag>
          <span class="cert-status-text">
            该作品是升级前登记或尚未签发烧成履历凭证，作品资料可照常查看流转；
            核对来源链完整一致后可补签首张凭证（V1）。
          </span>
        </template>
        <template v-else>
          <el-tag type="success" effect="dark" size="large">
            当前凭证 V{{ agg.current.versionNo }}
          </el-tag>
          <span class="cert-status-text">
            签发于 {{ fmt(agg.current.issuedAt) }} · 签发人 {{ agg.current.issuedBy }}
            <span v-if="agg.current.versionNo > 1">
              · 更正原因：{{ agg.current.changeReason }}
            </span>
          </span>
          <el-tag
            v-if="agg.sourceChanged"
            type="danger"
            effect="dark"
            size="large"
            style="margin-left: auto"
          >
            资料已变化 · 当前凭证仍是旧内容
          </el-tag>
          <el-tag v-else type="success" effect="plain" size="large" style="margin-left: auto">
            当前资料与凭证一致
          </el-tag>
        </template>
      </div>

      <!-- ===== 来源核对 ===== -->
      <el-divider content-position="left">来源链核对（作品 → 课程 / 坯体 → 泥料釉料 / 批次 → 窑炉）</el-divider>
      <el-alert
        v-if="!agg.provenance.issuable"
        :title="agg.provenance.blockMessage"
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom: 10px"
      />
      <el-table :data="agg.provenance.checks" border size="small" stripe>
        <el-table-column prop="segmentName" label="来源段" width="180" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="checkType(row.status)" effect="dark" size="small">
              {{ checkText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="message" label="核对说明" />
      </el-table>

      <!-- ===== 当前凭证快照 / 差异 ===== -->
      <template v-if="agg.issued">
        <el-divider content-position="left">凭证快照内容（打印以此为准，不随后续资料修改变化）</el-divider>
        <el-alert
          v-if="agg.sourceChanged"
          title="课程 / 材料 / 作品资料在凭证签发后被修正：下面的凭证仍是签发当时的原内容；如需让新资料生效，请对照差异后发起更正，系统会追加新版本，旧版保留为历史。"
          type="warning"
          :closable="false"
          show-icon
          style="margin-bottom: 10px"
        />
        <el-table v-if="agg.sourceChanged" :data="agg.diffs" border size="small" style="margin-bottom: 10px">
          <el-table-column prop="label" label="变化字段" width="150" />
          <el-table-column label="凭证旧内容（V{{ agg.current.versionNo }}）">
            <template #default="{ row }">
              <span class="diff-old">{{ row.oldValue || '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="当前可核实内容">
            <template #default="{ row }">
              <span class="diff-new">{{ row.newValue || '—' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <CertificateSnapshot :cert="agg.current" />

        <!-- ===== 历史版本 ===== -->
        <el-divider content-position="left">历史版本（只读，不会被页面当作当前版）</el-divider>
        <el-table :data="historyVersions" border size="small">
          <el-table-column label="版本" width="90">
            <template #default="{ row }">V{{ row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag type="info" effect="plain" size="small">
                {{ row.status === 'CURRENT' ? '当前版' : '已作废（历史版）' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="issuedBy" label="签发人" width="110" />
          <el-table-column label="签发时间" width="170">
            <template #default="{ row }">{{ fmt(row.issuedAt) }}</template>
          </el-table-column>
          <el-table-column prop="changeReason" label="更正原因">
            <template #default="{ row }">{{ row.changeReason || '首次签发' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170">
            <template #default="{ row }">
              <el-button link type="primary" @click="viewVersion(row)">查看</el-button>
              <el-button link type="primary" @click="printVersion(row)">打印</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- ===== 操作区 ===== -->
      <div class="cert-actions">
        <template v-if="!agg.issued">
          <el-button type="primary" :disabled="!agg.provenance.issuable" @click="openIssue">
            核对无误，签发首张凭证
          </el-button>
          <span v-if="!agg.provenance.issuable" class="studio-hint">
            来源链存在缺失或冲突，不能生成凭证，请先按上方核对说明补齐来源
          </span>
        </template>
        <template v-else>
          <el-button type="primary" plain @click="printVersion(agg.current)">
            打印当前凭证（V{{ agg.current.versionNo }}）
          </el-button>
          <el-button type="warning" :disabled="!agg.provenance.issuable" @click="openCorrect">
            发起更正并追加新版本
          </el-button>
          <span v-if="agg.sourceChanged" class="studio-hint">
            检测到 {{ agg.diffs.length }} 处资料变化，建议核对后更正
          </span>
          <span v-else-if="!agg.provenance.issuable" class="studio-hint">
            当前来源链已不完整，需先修复才能把新内容追加为新版本
          </span>
        </template>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { certificateApi } from '../api'
import CertificateSnapshot from './CertificateSnapshot.vue'

const props = defineProps({
  artworkId: { type: Number, required: true }
})
const emit = defineEmits(['version-changed'])

const router = useRouter()
const agg = ref(null)

const historyVersions = computed(() =>
  agg.value && agg.value.issued
    ? [...agg.value.versions].sort((a, b) => b.versionNo - a.versionNo)
    : []
)

async function load() {
  agg.value = await certificateApi.aggregate(props.artworkId)
}

onMounted(load)

function checkType(status) {
  return status === 'OK' ? 'success' : status === 'MISSING' ? 'danger' : 'warning'
}
function checkText(status) {
  return status === 'OK' ? '完整一致' : status === 'MISSING' ? '来源缺失' : '关联冲突'
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}

function viewVersion(row) {
  router.push(`/courses/artwork/${props.artworkId}/certificate/${row.versionNo}`)
}
function printVersion(row) {
  const url = router.resolve(
    `/courses/artwork/${props.artworkId}/certificate/${row.versionNo}?print=1`
  ).href
  window.open(url, '_blank')
}

async function askIssuer() {
  const { value } = await ElMessageBox.prompt('请输入签发工作人员姓名（将印在凭证上）', '凭证签发', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputValidator: (v) => (v && v.trim() ? true : '签发人不能为空')
  })
  return value.trim()
}

async function openIssue() {
  try {
    const issuer = await askIssuer()
    await certificateApi.issue(props.artworkId, {
      issuedBy: issuer,
      expectedCurrentId: agg.value.currentId
    })
    ElMessage.success('凭证 V1 已从当前可核实数据签发')
    await load()
    emit('version-changed', agg.value)
  } catch (e) {
    await handleActionError(e)
  }
}

async function openCorrect() {
  try {
    if (!agg.value.sourceChanged) {
      await ElMessageBox.confirm(
        '当前资料与凭证快照没有检测到差异，仍要发起更正吗？没有差异时服务器会拒绝生成新版本。',
        '提示',
        { type: 'warning' }
      ).catch(() => {
        throw new Error('__cancel__')
      })
    }
    const issuer = await askIssuer()
    const { value: reason } = await ElMessageBox.prompt(
      `请填写更正原因（将与 V${agg.value.current.versionNo} 一并留痕，旧版保留不覆盖）`,
      '发起凭证更正',
      {
        confirmButtonText: '确认追加新版本',
        cancelButtonText: '取消',
        inputType: 'textarea',
        inputValidator: (v) => (v && v.trim() ? true : '更正原因不能为空')
      }
    )
    await certificateApi.correct(props.artworkId, {
      issuedBy: issuer,
      reason: reason.trim(),
      expectedCurrentId: agg.value.currentId
    })
    ElMessage.success('已追加凭证新版本，旧版已标记为历史版')
    await load()
    emit('version-changed', agg.value)
  } catch (e) {
    await handleActionError(e)
  }
}

/**
 * 并发保护：另一名工作人员已更新版本时，旧页面的 expectedCurrentId 失效。
 * 这里明确提示版本变化，强制重新读取，禁止用旧内容继续覆盖。
 */
async function handleActionError(e) {
  if (e?.message === '__cancel__') {
    return
  }
  ElMessage.error(e?.message || '操作失败')
  if (e?.status === 400) {
    // 服务器返回 StaleVersionException 的消息即版本冲突（中文固定文案）
    if (/凭证版本已变化/.test(e.message || '')) {
      await load()
      emit('version-changed', agg.value)
    }
  }
}

defineExpose({ load })
</script>

<style scoped>
.cert-panel {
  margin-top: 6px;
}

.cert-statusbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.cert-status-text {
  font-size: 13px;
  color: #6b5d49;
}

.cert-actions {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.diff-old {
  color: #b5651d;
  text-decoration: line-through;
  text-decoration-color: rgba(181, 101, 29, 0.55);
}

.diff-new {
  color: #1f7a3d;
  font-weight: 600;
}
</style>
