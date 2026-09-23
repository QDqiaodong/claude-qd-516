<template>
  <div class="cert-paper">
    <div class="cert-paper-head">
      <h1>烧成履历凭证</h1>
      <div class="cert-no">
        凭证编号：{{ cert.certificateNo }} ｜ 作品编号：{{ cert.snapArtworkCode }}
      </div>
      <div class="cert-stamp">
        {{ cert.status === 'CURRENT' ? `当前版本 · V${cert.versionNo}` : `历史版本 · V${cert.versionNo}` }}
      </div>
    </div>

    <template v-for="g in groups" :key="g.group">
      <div class="cert-section-title">{{ g.group }}</div>
      <table>
        <tbody>
          <tr v-for="pair in pairRows(g.items)" :key="pair[0][0]">
            <td class="k">{{ pair[0][0] }}</td>
            <td>{{ pair[0][1] || '—' }}</td>
            <td class="k">{{ pair[1] ? pair[1][0] : '' }}</td>
            <td>{{ pair[1] ? pair[1][1] || '—' : '' }}</td>
          </tr>
        </tbody>
      </table>
    </template>

    <div class="cert-paper-foot">
      <span>签发/更正人：{{ cert.issuedBy }}</span>
      <span>签发时间：{{ dt(cert.issuedAt) }}</span>
    </div>

    <div v-if="cert.changeReason" class="cert-paper-note">
      本版为更正版（V{{ cert.versionNo }}）。更正原因：{{ cert.changeReason }}
    </div>
    <div class="cert-paper-note">
      本凭证为烧成完成后签发的履历快照，记录签发当时的作品、课程、泥料、釉料、来源坯体与烧成批次信息；
      历史版本仅作追溯，内容以签发当时为准，不随后续资料修正而改变。
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { certRows, dt } from '../certFormat'

const props = defineProps({
  cert: { type: Object, required: true }
})

const groups = computed(() => certRows(props.cert))

// 两列键值对排版：把 [label, value] 列表两两并成一行
function pairRows(items) {
  const rows = []
  for (let i = 0; i < items.length; i += 2) {
    rows.push([items[i], items[i + 1] || null])
  }
  return rows
}
</script>
