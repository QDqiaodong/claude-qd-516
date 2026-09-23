<template>
  <div class="cert-snapshot">
    <el-descriptions :column="3" border size="small">
      <el-descriptions-item label="作品编号">{{ cert.artworkCode }}</el-descriptions-item>
      <el-descriptions-item label="作品名称">{{ cert.artworkTitle }}</el-descriptions-item>
      <el-descriptions-item label="学员姓名">{{ cert.studentName }}</el-descriptions-item>

      <el-descriptions-item label="作品归属">{{ ownerText(cert.ownerStatus) }}</el-descriptions-item>
      <el-descriptions-item label="完成时间">{{ fmt(cert.finishedAt) }}</el-descriptions-item>
      <el-descriptions-item label="凭证版本">
        V{{ cert.versionNo }}
        <el-tag v-if="cert.status === 'SUPERSEDED'" size="small" type="info" effect="plain">
          历史版
        </el-tag>
      </el-descriptions-item>

      <el-descriptions-item label="来源课程">
        {{ cert.courseCode }} · {{ cert.courseTitle }}
      </el-descriptions-item>
      <el-descriptions-item label="授课老师">{{ cert.teacher }}</el-descriptions-item>
      <el-descriptions-item label="来源坯体">
        {{ cert.greenwareCode }} · {{ cert.greenwareName }}
      </el-descriptions-item>

      <el-descriptions-item label="泥料">
        {{ cert.clayCode }} · {{ cert.clayName }}
      </el-descriptions-item>
      <el-descriptions-item label="釉料">
        <template v-if="cert.glazeId">{{ cert.glazeCode }} · {{ cert.glazeName }}</template>
        <template v-else>未施釉</template>
      </el-descriptions-item>
      <el-descriptions-item label="烧成批次">{{ cert.batchNo }}</el-descriptions-item>

      <el-descriptions-item label="窑炉">{{ cert.kilnName }}</el-descriptions-item>
      <el-descriptions-item label="烧成类型">
        {{ cert.fireType === 'BISQUE' ? '素烧' : '釉烧' }}
      </el-descriptions-item>
      <el-descriptions-item label="目标温度">{{ cert.targetTemp }}℃</el-descriptions-item>

      <el-descriptions-item label="实际峰值温度">
        {{ cert.peakTemp == null ? '—' : cert.peakTemp + '℃' }}
      </el-descriptions-item>
      <el-descriptions-item label="装窑时间">{{ fmt(cert.loadedAt) }}</el-descriptions-item>
      <el-descriptions-item label="升温时间">{{ fmt(cert.heatingAt) }}</el-descriptions-item>

      <el-descriptions-item label="保温时间">{{ fmt(cert.soakingAt) }}</el-descriptions-item>
      <el-descriptions-item label="冷却时间">{{ fmt(cert.coolingAt) }}</el-descriptions-item>
      <el-descriptions-item label="出窑时间">{{ fmt(cert.outAt) }}</el-descriptions-item>
    </el-descriptions>

    <div class="cert-signoff">
      <span>签发人：{{ cert.issuedBy }}</span>
      <span>签发时间：{{ fmt(cert.issuedAt) }}</span>
      <span v-if="cert.changeReason">更正原因：{{ cert.changeReason }}</span>
    </div>
  </div>
</template>

<script setup>
defineProps({
  cert: { type: Object, required: true }
})

function ownerText(s) {
  return { TAKEN: '学员带走', CONSIGN: '留馆寄售', SOLD: '已售出' }[s] || s
}
function fmt(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<style scoped>
.cert-signoff {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-top: 10px;
  font-size: 13px;
  color: #6b5d49;
}
</style>
