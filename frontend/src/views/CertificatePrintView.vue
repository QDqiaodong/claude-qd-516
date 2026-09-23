<template>
  <div class="print-page">
    <!-- 屏幕上的操作条，打印时自动隐藏 -->
    <div class="print-toolbar no-print">
      <el-button @click="goBack">返回作品详情</el-button>
      <el-button type="primary" @click="doPrint">打印 / 另存为 PDF</el-button>
      <el-tag v-if="cert && cert.status === 'SUPERSEDED'" type="info" effect="plain">
        正在查看历史版本 V{{ cert.versionNo }}（非当前版，仅作留存）
      </el-tag>
      <el-tag v-else-if="cert" type="success" effect="dark">
        当前版本 V{{ cert.versionNo }}
      </el-tag>
    </div>

    <div v-loading="loading" class="cert-paper">
      <template v-if="cert">
        <header class="paper-head">
          <h1>陶艺工坊 · 烧成履历凭证</h1>
          <div class="paper-sub">
            凭证编号：{{ cert.artworkCode }}-V{{ cert.versionNo }}
            <span class="paper-status" v-if="cert.status === 'SUPERSEDED'">（历史版，已被新版本取代）</span>
          </div>
        </header>

        <section>
          <h2>一、作品与学员</h2>
          <table>
            <tbody>
              <tr>
                <th>作品编号</th><td>{{ cert.artworkCode }}</td>
                <th>作品名称</th><td>{{ cert.artworkTitle }}</td>
              </tr>
              <tr>
                <th>学员姓名</th><td>{{ cert.studentName }}</td>
                <th>作品归属</th><td>{{ ownerText(cert.ownerStatus) }}</td>
              </tr>
              <tr>
                <th>完成时间</th><td>{{ fmt(cert.finishedAt) }}</td>
                <th>所属课程</th><td>{{ cert.courseCode }} · {{ cert.courseTitle }}（{{ cert.teacher }}）</td>
              </tr>
            </tbody>
          </table>
        </section>

        <section>
          <h2>二、来源坯体与材料</h2>
          <table>
            <tbody>
              <tr>
                <th>来源坯体</th><td>{{ cert.greenwareCode }} · {{ cert.greenwareName }}</td>
                <th>泥料</th><td>{{ cert.clayCode }} · {{ cert.clayName }}</td>
              </tr>
              <tr>
                <th>釉料</th>
                <td :colspan="3">
                  <template v-if="cert.glazeId">{{ cert.glazeCode }} · {{ cert.glazeName }}</template>
                  <template v-else>未施釉（素烧）</template>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <section>
          <h2>三、烧成批次记录（目标 / 实际温度与时间）</h2>
          <table>
            <tbody>
              <tr>
                <th>烧成批次</th><td>{{ cert.batchNo }}</td>
                <th>窑炉</th><td>{{ cert.kilnName }}</td>
              </tr>
              <tr>
                <th>烧成类型</th>
                <td>{{ cert.fireType === 'BISQUE' ? '素烧（BISQUE）' : '釉烧（GLAZE）' }}</td>
                <th>目标温度 / 实际峰值</th>
                <td>{{ cert.targetTemp }}℃ / {{ cert.peakTemp == null ? '—' : cert.peakTemp + '℃' }}</td>
              </tr>
              <tr>
                <th>装窑</th><td>{{ fmt(cert.loadedAt) }}</td>
                <th>升温</th><td>{{ fmt(cert.heatingAt) }}</td>
              </tr>
              <tr>
                <th>保温</th><td>{{ fmt(cert.soakingAt) }}</td>
                <th>冷却</th><td>{{ fmt(cert.coolingAt) }}</td>
              </tr>
              <tr>
                <th>出窑</th><td :colspan="3">{{ fmt(cert.outAt) }}</td>
              </tr>
            </tbody>
          </table>
        </section>

        <footer class="paper-foot">
          <div>
            <p v-if="cert.changeReason">本版更正原因：{{ cert.changeReason }}</p>
            <p v-else>本凭证为首次签发版本。</p>
            <p class="paper-note">
              本凭证内容为 V{{ cert.versionNo }} 版签发当时的资料快照；历史版本永久留存，后续资料修正以新版本凭证为准。
            </p>
          </div>
          <div class="paper-sign">
            <p>签发工作人员：{{ cert.issuedBy }}</p>
            <p>签发时间：{{ fmt(cert.issuedAt) }}</p>
            <p class="paper-stamp">陶艺工坊 烧成履历专用</p>
          </div>
        </footer>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { certificateApi } from '../api'

const route = useRoute()
const router = useRouter()
const cert = ref(null)
const loading = ref(true)

function ownerText(s) {
  return { TAKEN: '学员带走', CONSIGN: '留馆寄售', SOLD: '已售出' }[s] || s
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}
function goBack() {
  router.push(`/courses?artwork=${route.params.artworkId}`)
}
function doPrint() {
  window.print()
}

onMounted(async () => {
  try {
    cert.value = await certificateApi.version(
      Number(route.params.artworkId),
      Number(route.params.versionNo)
    )
    if (route.query.print === '1') {
      // 等内容渲染后直接唤起打印
      setTimeout(() => window.print(), 350)
    }
  } catch (e) {
    ElMessage.error(e.message || '凭证版本读取失败')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.print-page {
  background: #ece6da;
  min-height: 100vh;
  padding: 18px;
}

.print-toolbar {
  max-width: 820px;
  margin: 0 auto 14px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.cert-paper {
  max-width: 820px;
  margin: 0 auto;
  background: #fff;
  padding: 42px 48px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  color: #2b2114;
}

.paper-head {
  text-align: center;
  border-bottom: 3px double #b4713c;
  padding-bottom: 14px;
  margin-bottom: 18px;
}

.paper-head h1 {
  font-size: 26px;
  letter-spacing: 4px;
  margin: 0 0 8px;
}

.paper-sub {
  font-size: 13px;
  color: #7a6a54;
}

.paper-status {
  color: #b03a2e;
}

section {
  margin-bottom: 18px;
}

section h2 {
  font-size: 15px;
  margin: 0 0 8px;
  border-left: 4px solid #f9a825;
  padding-left: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

th,
td {
  border: 1px solid #c9b89a;
  padding: 7px 10px;
  text-align: left;
}

th {
  width: 18%;
  background: #faf3e6;
  font-weight: 600;
  white-space: nowrap;
}

.paper-foot {
  display: flex;
  justify-content: space-between;
  gap: 30px;
  margin-top: 26px;
  font-size: 13px;
}

.paper-sign {
  text-align: right;
  white-space: nowrap;
}

.paper-sign p,
.paper-foot p {
  margin: 4px 0;
}

.paper-stamp {
  margin-top: 10px !important;
  display: inline-block;
  border: 2px solid #b53a2e;
  color: #b53a2e;
  border-radius: 8px;
  padding: 6px 14px;
  transform: rotate(-6deg);
  font-weight: 700;
  letter-spacing: 2px;
}

.paper-note {
  color: #7a6a54;
}

@media print {
  .no-print {
    display: none !important;
  }
  .print-page {
    background: #fff;
    padding: 0;
  }
  .cert-paper {
    box-shadow: none;
    max-width: none;
    padding: 10mm 12mm;
  }
}
</style>
