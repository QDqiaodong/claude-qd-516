<template>
  <div>
    <!-- 顶部横向分类条：课程 -->
    <div class="course-strip">
      <div class="course-card" :class="{ 'is-active': courseId === null }" @click="selectCourse(null)">
        <div class="course-card-title">全部课程</div>
        <div class="course-card-meta">共 {{ artworks.length }} 件作品</div>
      </div>
      <div
        v-for="c in courses"
        :key="c.id"
        class="course-card"
        :class="{ 'is-active': courseId === c.id }"
        @click="selectCourse(c.id)"
      >
        <div class="course-card-title">{{ c.title }}</div>
        <div class="course-card-meta">
          {{ c.teacher }} · {{ c.level === 'BEGINNER' ? '入门' : '进阶' }}<br />
          {{ c.enrolled }}/{{ c.capacity }} 人 · {{ c.artworkCount }} 件作品<br />
          {{ statusText(c.status) }}
        </div>
      </div>
    </div>

    <el-breadcrumb v-if="currentCourse" class="studio-crumb" separator="/">
      <el-breadcrumb-item :to="{ path: '/courses' }">首页</el-breadcrumb-item>
      <el-breadcrumb-item>课程与作品</el-breadcrumb-item>
      <el-breadcrumb-item>{{ currentCourse.title }}</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="studio-panel" v-if="currentCourse">
      <h3 class="studio-section-title">{{ currentCourse.title }} · 课程信息</h3>
      <el-descriptions :column="4" border size="small">
        <el-descriptions-item label="课程编号">{{ currentCourse.code }}</el-descriptions-item>
        <el-descriptions-item label="授课老师">{{ currentCourse.teacher }}</el-descriptions-item>
        <el-descriptions-item label="课时费">{{ currentCourse.price }} 元</el-descriptions-item>
        <el-descriptions-item label="开课时间">
          {{ currentCourse.startAt ? String(currentCourse.startAt).replace('T', ' ') : '—' }}
        </el-descriptions-item>
      </el-descriptions>
      <div class="studio-toolbar" style="margin-top: 12px">
        <el-button type="primary" @click="enroll(currentCourse)">课程报名</el-button>
        <el-button type="primary" plain @click="openCreate">登记作品</el-button>
        <span class="studio-hint">
          作品数不能超过该课程已报名人数（一人一件）；寄售必须填寄售价与货架位
        </span>
      </div>
    </div>

    <div class="studio-panel">
      <h3 class="studio-section-title">
        {{ currentCourse ? currentCourse.title + ' · 学员作品' : '全部学员作品' }}
      </h3>
      <div class="studio-toolbar">
        <el-radio-group v-model="ownerFilter" @change="loadArtworks">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="TAKEN">学员带走</el-radio-button>
          <el-radio-button value="CONSIGN">留馆寄售</el-radio-button>
          <el-radio-button value="SOLD">已售出</el-radio-button>
        </el-radio-group>
      </div>

      <el-table :data="artworks" border stripe size="small">
        <el-table-column prop="code" label="编号" width="105" />
        <el-table-column prop="title" label="作品" width="130" />
        <el-table-column prop="studentName" label="学员" width="100" />
        <el-table-column prop="courseTitle" label="课程" width="140" />
        <el-table-column prop="greenwareCode" label="坯体" width="105" />
        <el-table-column prop="batchNo" label="烧成批次" width="120" />
        <el-table-column label="归属" width="110">
          <template #default="{ row }">
            <el-tag :type="ownerType(row.ownerStatus)" effect="dark">{{ ownerText(row.ownerStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="consignPrice" label="价格" width="90" />
        <el-table-column prop="shelfNo" label="货架位" width="90" />
        <el-table-column label="烧成履历凭证" width="130">
          <template #default="{ row }">
            <el-tag v-if="row.certificateVersionNo" type="success" size="small">
              当前版 V{{ row.certificateVersionNo }}
            </el-tag>
            <el-tag v-else type="info" size="small">未签发</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="230">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">作品详情/凭证</el-button>
            <el-button link type="primary" @click="changeOwner(row)">改归属</el-button>
            <el-button link type="danger" @click="removeRow(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="createVisible" title="登记学员作品" width="500px">
      <el-form :model="form" label-width="110px">
        <el-form-item label="编号"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="作品名称"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="学员姓名"><el-input v-model="form.studentName" /></el-form-item>
        <el-form-item label="课程">
          <el-select v-model="form.courseId" style="width: 100%">
            <el-option v-for="c in courses" :key="c.id" :label="c.title" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源坯体">
          <el-select v-model="form.greenwareId" style="width: 100%">
            <el-option v-for="g in greenwares" :key="g.id" :label="g.code + ' ' + g.name" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="烧成批次">
          <el-select v-model="form.firingBatchId" style="width: 100%">
            <el-option v-for="b in batches" :key="b.id" :label="b.batchNo + '（' + b.stage + '）'" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属">
          <el-radio-group v-model="form.ownerStatus">
            <el-radio value="TAKEN">学员带走</el-radio>
            <el-radio value="CONSIGN">留馆寄售</el-radio>
            <el-radio value="SOLD">已售出</el-radio>
          </el-radio-group>
        </el-form-item>
        <template v-if="form.ownerStatus !== 'TAKEN'">
          <el-form-item label="价格"><el-input-number v-model="form.consignPrice" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="货架位"><el-input v-model="form.shelfNo" /></el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { courseApi, artworkApi, greenwareApi, firingBatchApi } from '../api'

const router = useRouter()

const courses = ref([])
const artworks = ref([])
const greenwares = ref([])
const batches = ref([])
const courseId = ref(null)
const ownerFilter = ref('')
const createVisible = ref(false)
const form = ref({})

const currentCourse = computed(() => courses.value.find((c) => c.id === courseId.value) || null)

const statusText = (s) => ({ OPEN: '报名中', ONGOING: '进行中', FINISHED: '已结束' }[s] || s)
const ownerText = (s) => ({ TAKEN: '学员带走', CONSIGN: '留馆寄售', SOLD: '已售出' }[s] || s)
const ownerType = (s) => ({ TAKEN: 'info', CONSIGN: 'warning', SOLD: 'success' }[s] || 'info')

async function loadCourses() {
  courses.value = await courseApi.list()
}

async function loadArtworks() {
  const params = {}
  if (courseId.value) {
    params.courseId = courseId.value
  }
  const rows = await artworkApi.list(params)
  artworks.value = ownerFilter.value ? rows.filter((r) => r.ownerStatus === ownerFilter.value) : rows
}

function selectCourse(id) {
  courseId.value = id
  loadArtworks()
}

function openDetail(row) {
  router.push(`/courses/artworks/${row.id}`)
}

async function enroll(course) {
  try {
    const { value } = await ElMessageBox.prompt(`为【${course.title}】报名几人？（容量 ${course.capacity}，已报 ${course.enrolled}）`, '课程报名', {
      inputPattern: /^\d+$/,
      inputErrorMessage: '只能填整数'
    })
    await courseApi.enroll(course.id, Number(value))
    ElMessage.success('报名成功')
    await loadCourses()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

function openCreate() {
  form.value = {
    code: 'AW-' + Date.now().toString().slice(-6),
    title: '',
    studentName: '',
    courseId: courseId.value || (courses.value.length ? courses.value[0].id : null),
    greenwareId: null,
    firingBatchId: null,
    ownerStatus: 'TAKEN',
    consignPrice: null,
    shelfNo: ''
  }
  createVisible.value = true
}

async function submit() {
  try {
    await artworkApi.create(form.value)
    createVisible.value = false
    ElMessage.success('作品已登记')
    await loadCourses()
    await loadArtworks()
  } catch (e) {
    ElMessage.error(e.message)
  }
}

async function changeOwner(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `把【${row.title}】归属改为？填 TAKEN / CONSIGN / SOLD`,
      '改归属',
      { inputPattern: /^(TAKEN|CONSIGN|SOLD)$/, inputErrorMessage: '只能填 TAKEN、CONSIGN 或 SOLD' }
    )
    let price = null
    if (value === 'CONSIGN' || value === 'SOLD') {
      const p = await ElMessageBox.prompt('请输入价格（元）', '价格', {
        inputPattern: /^\d+(\.\d+)?$/,
        inputErrorMessage: '只能填数字'
      })
      price = Number(p.value)
    }
    await artworkApi.owner(row.id, value, price)
    ElMessage.success('归属已更新')
    await loadArtworks()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

async function removeRow(row) {
  try {
    await ElMessageBox.confirm(`确认删除作品【${row.title}】？`, '提示', { type: 'warning' })
    await artworkApi.remove(row.id)
    ElMessage.success('已删除')
    await loadArtworks()
  } catch (e) {
    if (e && e.message) {
      ElMessage.error(e.message)
    }
  }
}

onMounted(async () => {
  greenwares.value = await greenwareApi.list({})
  batches.value = await firingBatchApi.list({})
  await loadCourses()
  await loadArtworks()
})
</script>
