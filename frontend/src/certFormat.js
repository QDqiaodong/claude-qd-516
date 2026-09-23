// 凭证字段展示口径，与后端 CertificateService.display 保持一致

export const ownerText = (s) =>
  ({ TAKEN: '学员带走', CONSIGN: '留馆寄售', SOLD: '已售出' }[s] || s || '—')

export const fireText = (s) => ({ BISQUE: '素烧', GLAZE: '釉烧' }[s] || s || '—')

export const stageText = (s) =>
  ({
    SHAPED: '已成型',
    DRYING: '晾坯中',
    BISQUE_READY: '可素烧',
    BISQUED: '已素烧',
    GLAZED: '已施釉',
    FIRING: '烧制中',
    FINISHED: '已完成'
  }[s] || s || '—')

const pad = (n) => String(n).padStart(2, '0')

/** 后端时间为 LocalDateTime，序列化后形如 "2026-03-21T09:00:00" */
export const dt = (v) => {
  if (!v) return '—'
  const s = String(v).replace('T', ' ')
  // 截到分钟
  return s.length >= 16 ? s.slice(0, 16) : s
}

export const temp = (v) => (v === null || v === undefined || v === '' ? '—' : `${v} ℃`)

export const glazeText = (name) => (name ? name : '未施釉（素烧坯体）')

/** 把一张凭证快照整理成打印/查看用的分组行 */
export function certRows(c) {
  if (!c) return []
  return [
    {
      group: '作品与课程',
      items: [
        ['作品编号', c.snapArtworkCode],
        ['作品名称', c.snapTitle],
        ['学员姓名', c.snapStudentName],
        ['作品归属', ownerText(c.snapOwnerStatus)],
        ['课程编号', c.snapCourseCode],
        ['课程名称', c.snapCourseTitle],
        ['授课老师', c.snapTeacher]
      ]
    },
    {
      group: '来源坯体与材料',
      items: [
        ['坯体编号', c.snapGreenwareCode],
        ['坯体名称', c.snapGreenwareName],
        ['坯体阶段', c.snapGreenwareStage ? stageText(c.snapGreenwareStage) : '—'],
        ['成型时间', dt(c.snapShapedAt)],
        ['泥料编号', c.snapClayCode],
        ['泥料', c.snapClayName],
        ['泥料建议温度', temp(c.snapClayTemp)],
        ['釉料编号', c.snapGlazeCode || '未施釉'],
        ['釉料', glazeText(c.snapGlazeName)],
        ['釉料建议温度', c.snapGlazeTemp === null || c.snapGlazeTemp === undefined ? '—' : temp(c.snapGlazeTemp)]
      ]
    },
    {
      group: '烧成批次与窑炉',
      items: [
        ['烧成批次', c.snapBatchNo],
        ['烧成类型', c.snapFireType ? fireText(c.snapFireType) : '—'],
        ['窑炉', c.snapKilnCode ? `${c.snapKilnCode} ${c.snapKilnName}` : c.snapKilnName],
        ['目标温度', temp(c.snapTargetTemp)],
        ['实际峰值温度', temp(c.snapPeakTemp)],
        ['装窑时间', dt(c.snapLoadedAt)],
        ['升温时间', dt(c.snapHeatingAt)],
        ['保温开始', dt(c.snapSoakingAt)],
        ['冷却开始', dt(c.snapCoolingAt)],
        ['出窑时间', dt(c.snapOutAt)]
      ]
    }
  ]
}
