-- 陶艺工坊管理系统 表结构 + 种子数据
-- 数据访问路线：JPA 自关联品类树（material_category / work_area 均为 parent_id 指向自身的树）

SET NAMES utf8mb4;

DROP TABLE IF EXISTS artwork;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS firing_batch;
DROP TABLE IF EXISTS kiln;
DROP TABLE IF EXISTS greenware;
DROP TABLE IF EXISTS work_area;
DROP TABLE IF EXISTS material;
DROP TABLE IF EXISTS material_category;

-- ---------- 泥料 / 釉料分类（自关联树） ----------
CREATE TABLE material_category (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id  BIGINT      NULL COMMENT '父分类，顶层为 NULL',
  code       VARCHAR(32) NOT NULL COMMENT '分类编码',
  name       VARCHAR(64) NOT NULL COMMENT '分类名称',
  sort_no    INT         NOT NULL DEFAULT 0,
  created_at DATETIME    NULL,
  updated_at DATETIME    NULL,
  UNIQUE KEY uk_mc_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '泥料釉料分类（自关联树）';

-- ---------- 泥料 / 釉料 ----------
CREATE TABLE material (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id   BIGINT         NOT NULL COMMENT '所属分类',
  code          VARCHAR(32)    NOT NULL COMMENT '材料编号',
  name          VARCHAR(64)    NOT NULL COMMENT '材料名称',
  kind          VARCHAR(16)    NOT NULL COMMENT 'CLAY 泥料 / GLAZE 釉料',
  stock_kg      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '库存公斤数',
  shrink_rate   DECIMAL(5, 2)  NULL COMMENT '收缩率 %（仅泥料）',
  firing_temp   INT            NULL COMMENT '建议烧成温度 ℃',
  pair_clay_id  BIGINT         NULL COMMENT '配套泥料（仅釉料）',
  unit_price    DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '单价 元/kg',
  safety_stock  DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '安全库存 kg',
  status        VARCHAR(16)    NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL 正常 / LOW 低库存 / DEPLETED 耗尽',
  created_at    DATETIME       NULL,
  updated_at    DATETIME       NULL,
  UNIQUE KEY uk_m_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '泥料釉料台账';

-- ---------- 工位分区（自关联树） ----------
CREATE TABLE work_area (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  parent_id  BIGINT      NULL COMMENT '父分区，顶层为 NULL',
  code       VARCHAR(32) NOT NULL COMMENT '分区编码',
  name       VARCHAR(64) NOT NULL COMMENT '分区名称',
  capacity   INT         NULL COMMENT '可容纳坯体数（仅末级工位）',
  sort_no    INT         NOT NULL DEFAULT 0,
  created_at DATETIME    NULL,
  updated_at DATETIME    NULL,
  UNIQUE KEY uk_wa_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '工位分区（自关联树）';

-- ---------- 坯体 ----------
CREATE TABLE greenware (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  code            VARCHAR(32)    NOT NULL COMMENT '坯体编号',
  name            VARCHAR(64)    NOT NULL COMMENT '坯体名称',
  clay_id         BIGINT         NOT NULL COMMENT '所用泥料',
  glaze_id        BIGINT         NULL COMMENT '所用釉料',
  area_id         BIGINT         NOT NULL COMMENT '所在工位分区',
  stage           VARCHAR(20)    NOT NULL COMMENT 'SHAPED 已成型 / DRYING 晾坯中 / BISQUE_READY 可素烧 / BISQUED 已素烧 / GLAZED 已施釉 / FIRING 烧制中 / FINISHED 已完成',
  moisture        DECIMAL(5, 2)  NOT NULL DEFAULT 0 COMMENT '含水率 %',
  height_cm       DECIMAL(6, 2)  NULL COMMENT '高度 cm',
  firing_batch_id BIGINT         NULL COMMENT '所在烧成批次',
  shaped_at       DATETIME       NULL COMMENT '成型时间',
  created_at      DATETIME       NULL,
  updated_at      DATETIME       NULL,
  UNIQUE KEY uk_g_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '坯体';

-- ---------- 窑炉 ----------
CREATE TABLE kiln (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(32) NOT NULL COMMENT '窑炉编号',
  name       VARCHAR(64) NOT NULL COMMENT '窑炉名称',
  kiln_type  VARCHAR(16) NOT NULL DEFAULT 'ELECTRIC' COMMENT 'ELECTRIC 电窑 / GAS 燃气窑',
  max_temp   INT         NOT NULL COMMENT '最高温度 ℃',
  volume_l   INT         NOT NULL COMMENT '容积 升',
  status     VARCHAR(16) NOT NULL DEFAULT 'IDLE' COMMENT 'IDLE 空闲 / FIRING 烧制中 / MAINTAIN 检修',
  created_at DATETIME    NULL,
  updated_at DATETIME    NULL,
  UNIQUE KEY uk_k_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '窑炉';

-- ---------- 烧成批次 ----------
CREATE TABLE firing_batch (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no         VARCHAR(32) NOT NULL COMMENT '批次号',
  kiln_id          BIGINT      NOT NULL COMMENT '窑炉',
  fire_type        VARCHAR(16) NOT NULL DEFAULT 'BISQUE' COMMENT 'BISQUE 素烧 / GLAZE 釉烧',
  stage            VARCHAR(16) NOT NULL DEFAULT 'LOADING' COMMENT 'LOADING 装窑 / HEATING 升温 / SOAKING 保温 / COOLING 冷却 / OUT 出窑',
  target_temp      INT         NOT NULL COMMENT '目标温度 ℃',
  peak_temp        INT         NULL COMMENT '实际峰值温度 ℃',
  greenware_count  INT         NOT NULL DEFAULT 0 COMMENT '装窑件数',
  loaded_at        DATETIME    NULL,
  heating_at       DATETIME    NULL,
  soaking_at       DATETIME    NULL,
  cooling_at       DATETIME    NULL,
  out_at           DATETIME    NULL,
  remark           VARCHAR(255) NULL,
  created_at       DATETIME    NULL,
  updated_at       DATETIME    NULL,
  UNIQUE KEY uk_fb_no (batch_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '烧成批次（时间轴）';

-- ---------- 课程 ----------
CREATE TABLE course (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(32)    NOT NULL COMMENT '课程编号',
  title      VARCHAR(64)    NOT NULL COMMENT '课程名称',
  teacher    VARCHAR(32)    NOT NULL COMMENT '授课老师',
  level      VARCHAR(16)    NOT NULL DEFAULT 'BEGINNER' COMMENT 'BEGINNER 入门 / ADVANCED 进阶',
  capacity   INT            NOT NULL COMMENT '容量',
  enrolled   INT            NOT NULL DEFAULT 0 COMMENT '已报名人数',
  price      DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '课时费',
  start_at   DATETIME       NULL COMMENT '开课时间',
  status     VARCHAR(16)    NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN 报名中 / ONGOING 进行中 / FINISHED 已结束',
  created_at DATETIME       NULL,
  updated_at DATETIME       NULL,
  UNIQUE KEY uk_c_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '体验课';

-- ---------- 学员作品 ----------
CREATE TABLE artwork (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  code             VARCHAR(32)    NOT NULL COMMENT '作品编号',
  title            VARCHAR(64)    NOT NULL COMMENT '作品名称',
  student_name     VARCHAR(32)    NOT NULL COMMENT '学员姓名',
  course_id        BIGINT         NOT NULL COMMENT '所属课程',
  greenware_id     BIGINT         NOT NULL COMMENT '来源坯体',
  firing_batch_id  BIGINT         NOT NULL COMMENT '烧成批次',
  owner_status     VARCHAR(16)    NOT NULL COMMENT 'TAKEN 学员带走 / CONSIGN 留馆寄售 / SOLD 已售出',
  consign_price    DECIMAL(10, 2) NULL COMMENT '寄售价格',
  shelf_no         VARCHAR(32)    NULL COMMENT '货架位',
  finished_at      DATETIME       NULL COMMENT '完成时间',
  created_at       DATETIME       NULL,
  updated_at       DATETIME       NULL,
  UNIQUE KEY uk_aw_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '学员作品';

-- ================= 种子数据 =================

-- 材料分类树（4 层：陶土类 > 粗陶泥 > 宜兴粗陶）
INSERT INTO material_category (id, parent_id, code, name, sort_no, created_at, updated_at) VALUES
  (1,  NULL, 'MC-TT', '陶土类', 1, NOW(), NOW()),
  (2,  1,    'MC-TT-CT', '粗陶泥', 1, NOW(), NOW()),
  (3,  2,    'MC-TT-CT-YX', '宜兴粗陶', 1, NOW(), NOW()),
  (4,  2,    'MC-TT-CT-LQ', '龙泉粗陶', 2, NOW(), NOW()),
  (5,  1,    'MC-TT-XT', '细陶泥', 2, NOW(), NOW()),
  (6,  5,    'MC-TT-XT-BT', '白细陶', 1, NOW(), NOW()),
  (7,  NULL, 'MC-CN', '瓷泥类', 2, NOW(), NOW()),
  (8,  7,    'MC-CN-GB', '高白泥', 1, NOW(), NOW()),
  (9,  8,    'MC-CN-GB-TB', '特白泥', 1, NOW(), NOW()),
  (10, NULL, 'MC-YL', '釉料类', 3, NOW(), NOW()),
  (11, 10,   'MC-YL-TM', '透明釉', 1, NOW(), NOW()),
  (12, 10,   'MC-YL-YG', '哑光釉', 2, NOW(), NOW()),
  (13, 12,   'MC-YL-YG-JJ', '结晶釉', 1, NOW(), NOW()),
  (14, 10,   'MC-YL-SY', '色釉', 3, NOW(), NOW());

-- 泥料 / 釉料
INSERT INTO material (id, category_id, code, name, kind, stock_kg, shrink_rate, firing_temp, pair_clay_id, unit_price, safety_stock, status, created_at, updated_at) VALUES
  (1,  3,  'M-1001', '宜兴粗陶泥', 'CLAY',  120.00,  8.50, 1180, NULL, 12.00, 20.00, 'NORMAL', NOW(), NOW()),
  (2,  4,  'M-1002', '龙泉粗陶泥', 'CLAY',    3.00,  9.20, 1200, NULL, 14.00, 20.00, 'LOW',    NOW(), NOW()),
  (3,  8,  'M-1003', '高白泥',     'CLAY',   60.00, 12.50, 1280, NULL, 26.00, 15.00, 'NORMAL', NOW(), NOW()),
  (4,  9,  'M-1004', '特白泥',     'CLAY',    2.50, 13.00, 1300, NULL, 38.00, 10.00, 'LOW',    NOW(), NOW()),
  (5,  6,  'M-1005', '白细陶泥',   'CLAY',   45.00, 10.80, 1220, NULL, 18.00, 15.00, 'NORMAL', NOW(), NOW()),
  (6,  11, 'M-2001', '透明釉',     'GLAZE',  40.00,  NULL, 1240, 3,    45.00, 10.00, 'NORMAL', NOW(), NOW()),
  (7,  12, 'M-2002', '哑光白釉',   'GLAZE',  15.00,  NULL, 1230, 1,    52.00,  8.00, 'NORMAL', NOW(), NOW()),
  (8,  13, 'M-2003', '结晶釉',     'GLAZE',   6.00,  NULL, 1280, 3,    88.00,  5.00, 'NORMAL', NOW(), NOW()),
  (9,  14, 'M-2004', '青花色釉',   'GLAZE',   8.00,  NULL, 1260, 1,    66.00,  5.00, 'NORMAL', NOW(), NOW());

-- 工位分区树（3 层：成型区 > 拉坯工位 > 拉坯机-1号）
INSERT INTO work_area (id, parent_id, code, name, capacity, sort_no, created_at, updated_at) VALUES
  (1,  NULL, 'WA-CX', '成型区',   NULL, 1, NOW(), NOW()),
  (2,  1,    'WA-CX-LP', '拉坯工位', NULL, 1, NOW(), NOW()),
  (3,  2,    'WA-CX-LP-01', '拉坯机-1号', 2, 1, NOW(), NOW()),
  (4,  2,    'WA-CX-LP-02', '拉坯机-2号', 2, 2, NOW(), NOW()),
  (5,  1,    'WA-CX-SN', '手捏工位', 6, 2, NOW(), NOW()),
  (6,  NULL, 'WA-GZ', '干燥区',   NULL, 2, NOW(), NOW()),
  (7,  6,    'WA-GZ-A', '晾坯架-A', 8, 1, NOW(), NOW()),
  (8,  6,    'WA-GZ-B', '晾坯架-B', 5, 2, NOW(), NOW()),
  (9,  NULL, 'WA-SY', '施釉区',   NULL, 3, NOW(), NOW()),
  (10, 9,    'WA-SY-PY', '喷釉台', 3, 1, NOW(), NOW()),
  (11, 9,    'WA-SY-JY', '浸釉桶', 4, 2, NOW(), NOW());

-- 坯体（拉坯机-1号 容量 2 已放满 2 件，用于触发容量拒绝）
INSERT INTO greenware (id, code, name, clay_id, glaze_id, area_id, stage, moisture, height_cm, firing_batch_id, shaped_at, created_at, updated_at) VALUES
  (1,  'GW-0001', '宽口花器',   1, NULL, 3,  'DRYING',       18.50, 22.00, NULL, '2026-04-02 10:20:00', NOW(), NOW()),
  (2,  'GW-0002', '素心茶盏',   1, NULL, 3,  'DRYING',       14.20,  7.50, NULL, '2026-04-02 11:05:00', NOW(), NOW()),
  (3,  'GW-0003', '细颈瓶',     3, NULL, 4,  'SHAPED',       22.00, 26.50, NULL, '2026-04-03 09:40:00', NOW(), NOW()),
  (4,  'GW-0004', '高白泥杯',   3, NULL, 4,  'DRYING',       15.60,  9.00, NULL, '2026-04-03 14:12:00', NOW(), NOW()),
  (5,  'GW-0005', '手捏小猫',   2, NULL, 5,  'BISQUE_READY',  9.80, 11.20, NULL, '2026-04-01 15:30:00', NOW(), NOW()),
  (6,  'GW-0006', '青瓷碟',     3, 6,    7,  'BISQUED',       2.10,  3.50, 1,    '2026-03-28 10:00:00', NOW(), NOW()),
  (7,  'GW-0007', '粗陶罐',     1, 7,    7,  'GLAZED',        1.50, 18.40, NULL, '2026-03-27 16:20:00', NOW(), NOW()),
  (8,  'GW-0008', '高白泥碗',   3, 6,    7,  'BISQUE_READY',  3.40,  8.20, NULL, '2026-03-29 09:15:00', NOW(), NOW()),
  (9,  'GW-0009', '粗陶杯',     2, NULL, 8,  'DRYING',       16.00,  8.80, NULL, '2026-04-04 10:45:00', NOW(), NOW()),
  (10, 'GW-0010', '特白泥盏',   4, 8,    10, 'GLAZED',        1.20,  6.30, NULL, '2026-03-26 13:10:00', NOW(), NOW()),
  (11, 'GW-0011', '结晶釉盘',   3, 8,    10, 'BISQUED',       0.80,  2.80, 1,    '2026-03-25 11:00:00', NOW(), NOW()),
  (12, 'GW-0012', '青花小瓶',   1, 9,    11, 'BISQUE_READY',  8.90, 15.60, NULL, '2026-03-30 09:50:00', NOW(), NOW());

-- 窑炉
INSERT INTO kiln (id, code, name, kiln_type, max_temp, volume_l, status, created_at, updated_at) VALUES
  (1, 'K-01', '1号电窑',   'ELECTRIC', 1300, 120, 'IDLE',    NOW(), NOW()),
  (2, 'K-02', '2号燃气窑', 'GAS',      1320, 200, 'FIRING',  NOW(), NOW()),
  (3, 'K-03', '3号小电窑', 'ELECTRIC', 1240,  60, 'IDLE',    NOW(), NOW());

-- 烧成批次（2 号窑有进行中批次，用于触发"一窑一批次"拒绝）
INSERT INTO firing_batch (id, batch_no, kiln_id, fire_type, stage, target_temp, peak_temp, greenware_count, loaded_at, heating_at, soaking_at, cooling_at, out_at, remark, created_at, updated_at) VALUES
  (1, 'FB-20260320', 1, 'BISQUE', 'OUT',     800,  805,  6, '2026-03-20 09:00:00', '2026-03-20 13:00:00', '2026-03-20 18:00:00', '2026-03-21 02:00:00', '2026-03-21 09:00:00', '素烧常规件', NOW(), NOW()),
  (2, 'FB-20260405', 2, 'GLAZE',  'SOAKING', 1280, 1275, 9, '2026-04-05 08:30:00', '2026-04-05 14:00:00', '2026-04-06 01:00:00', NULL, NULL, '釉烧大件',   NOW(), NOW()),
  (3, 'FB-20260408', 3, 'BISQUE', 'LOADING', 780,  NULL, 3, '2026-04-08 09:00:00', NULL, NULL, NULL, NULL, '已装 3 件',   NOW(), NOW()),
  (4, 'FB-20260330', 3, 'GLAZE',  'OUT',     1220, 1218, 4, '2026-03-30 09:00:00', '2026-03-30 15:00:00', '2026-03-30 20:00:00', '2026-03-31 03:00:00', '2026-03-31 10:00:00', '釉烧小件',  NOW(), NOW());

-- 课程（周六拉坯体验课 12/12 已满；春节专场已结束）
INSERT INTO course (id, code, title, teacher, level, capacity, enrolled, price, start_at, status, created_at, updated_at) VALUES
  (1, 'C-2601', '周六拉坯体验课', '陈师傅', 'BEGINNER', 12, 12, 168.00, '2026-04-11 10:00:00', 'OPEN',     NOW(), NOW()),
  (2, 'C-2602', '手捏亲子课',     '林老师', 'BEGINNER', 10,  6, 128.00, '2026-04-12 14:00:00', 'OPEN',     NOW(), NOW()),
  (3, 'C-2603', '釉下彩绘进阶课', '周老师', 'ADVANCED',  8,  3, 268.00, '2026-04-18 09:30:00', 'OPEN',     NOW(), NOW()),
  (4, 'C-2604', '暑期陶艺营',     '陈师傅', 'BEGINNER', 16,  9, 198.00, '2026-07-06 09:00:00', 'OPEN',     NOW(), NOW()),
  (5, 'C-2505', '春节陶艺专场',   '周老师', 'BEGINNER', 12, 10, 208.00, '2026-02-08 10:00:00', 'FINISHED', NOW(), NOW());

-- 学员作品
INSERT INTO artwork (id, code, title, student_name, course_id, greenware_id, firing_batch_id, owner_status, consign_price, shelf_no, finished_at, created_at, updated_at) VALUES
  (1, 'AW-0001', '素心茶盏', '王小舟', 1, 2,  1, 'CONSIGN', 268.00, 'S-01', '2026-03-22 10:00:00', NOW(), NOW()),
  (2, 'AW-0002', '青瓷碟',   '李楠',   1, 6,  1, 'TAKEN',   NULL,    NULL,   '2026-03-22 10:00:00', NOW(), NOW()),
  (3, 'AW-0003', '粗陶罐',   '赵晓',   2, 7,  1, 'CONSIGN', 380.00, 'S-02', '2026-03-22 11:00:00', NOW(), NOW()),
  (4, 'AW-0004', '结晶釉盘', '孙宁',   3, 11, 1, 'SOLD',    520.00, NULL,   '2026-03-23 09:00:00', NOW(), NOW()),
  (5, 'AW-0005', '青花小瓶', '周敏',   3, 12, 1, 'CONSIGN', 460.00, 'S-03', '2026-03-23 15:00:00', NOW(), NOW()),
  (6, 'AW-0006', '手捏小猫', '刘小满', 2, 5,  1, 'TAKEN',   NULL,    NULL,   '2026-03-24 10:00:00', NOW(), NOW()),
  (7, 'AW-0007', '高白泥碗', '吴桐',   3, 8,  1, 'TAKEN',   NULL,    NULL,   '2026-03-24 16:00:00', NOW(), NOW());
