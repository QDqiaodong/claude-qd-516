<template>
  <div>
    <div class="cert-printbar">
      <el-button @click="back">返回作品详情</el-button>
      <el-button type="primary" :disabled="!cert" @click="doPrint">打印 / 另存为 PDF</el-button>
    </div>

    <div v-loading="loading" style="padding: 0 16px 30px">
      <el-alert
        v-if="error"
        :title="error"
        type="error"
        :closable="false"
        show-icon
        style="max-width: 820px; margin: 16px auto"
      />
      <CertificatePaper v-if="cert" :cert="cert" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import CertificatePaper from '../components/CertificatePaper.vue'
import { certificateApi } from '../api'

const props = defineProps({
  id: { type: String, required: true },
  versionId: { type: String, required: true }
})

const router = useRouter()
const cert = ref(null)
const loading = ref(true)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    // 按具体版本 id 取落库快照；历史版打印的也是当时内容，不随后续资料修正而变
    cert.value = await certificateApi.version(props.id, props.versionId)
  } catch (e) {
    error.value = e.message || '凭证读取失败'
  } finally {
    loading.value = false
  }
}

function doPrint() {
  // 打印的是已签发落库的指定版本快照，不是临时拼的当前作品信息
  window.print()
}

function back() {
  router.push(`/courses/artworks/${props.id}`)
}

onMounted(load)
</script>
