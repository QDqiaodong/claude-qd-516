-- H2（MySQL 兼容模式）测试用表结构，仅含凭证链路所需表（幂等：先删除）
DROP TABLE IF EXISTS firing_certificate;
DROP TABLE IF EXISTS artwork;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS greenware;
DROP TABLE IF EXISTS firing_batch;
DROP TABLE IF EXISTS kiln;
DROP TABLE IF EXISTS material;

CREATE TABLE material (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  category_id   BIGINT       NOT NULL,
  code          VARCHAR(32)  NOT NULL,
  name          VARCHAR(64)  NOT NULL,
  kind          VARCHAR(16)  NOT NULL,
  stock_kg      DECIMAL(10,2) NOT NULL DEFAULT 0,
  shrink_rate   DECIMAL(5,2),
  firing_temp   INT,
  pair_clay_id  BIGINT,
  unit_price    DECIMAL(10,2) NOT NULL DEFAULT 0,
  safety_stock  DECIMAL(10,2) NOT NULL DEFAULT 0,
  status        VARCHAR(16)  NOT NULL DEFAULT 'NORMAL',
  created_at    TIMESTAMP,
  updated_at    TIMESTAMP
);

CREATE TABLE kiln (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(32) NOT NULL,
  name       VARCHAR(64) NOT NULL,
  kiln_type  VARCHAR(16) NOT NULL DEFAULT 'ELECTRIC',
  max_temp   INT         NOT NULL,
  volume_l   INT         NOT NULL,
  status     VARCHAR(16) NOT NULL DEFAULT 'IDLE',
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE firing_batch (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_no         VARCHAR(32) NOT NULL,
  kiln_id          BIGINT      NOT NULL,
  fire_type        VARCHAR(16) NOT NULL DEFAULT 'BISQUE',
  stage            VARCHAR(16) NOT NULL DEFAULT 'LOADING',
  target_temp      INT         NOT NULL,
  peak_temp        INT,
  greenware_count  INT         NOT NULL DEFAULT 0,
  loaded_at        TIMESTAMP,
  heating_at       TIMESTAMP,
  soaking_at       TIMESTAMP,
  cooling_at       TIMESTAMP,
  out_at           TIMESTAMP,
  remark           VARCHAR(255),
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP
);

CREATE TABLE greenware (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  code            VARCHAR(32) NOT NULL,
  name            VARCHAR(64) NOT NULL,
  clay_id         BIGINT      NOT NULL,
  glaze_id        BIGINT,
  area_id         BIGINT      NOT NULL,
  stage           VARCHAR(20) NOT NULL,
  moisture        DECIMAL(5,2) NOT NULL DEFAULT 0,
  height_cm       DECIMAL(6,2),
  firing_batch_id BIGINT,
  shaped_at       TIMESTAMP,
  created_at      TIMESTAMP,
  updated_at      TIMESTAMP
);

CREATE TABLE course (
  id         BIGINT AUTO_INCREMENT PRIMARY KEY,
  code       VARCHAR(32)    NOT NULL,
  title      VARCHAR(64)    NOT NULL,
  teacher    VARCHAR(32)    NOT NULL,
  level      VARCHAR(16)    NOT NULL DEFAULT 'BEGINNER',
  capacity   INT            NOT NULL,
  enrolled   INT            NOT NULL DEFAULT 0,
  price      DECIMAL(10, 2) NOT NULL DEFAULT 0,
  start_at   TIMESTAMP,
  status     VARCHAR(16)    NOT NULL DEFAULT 'OPEN',
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE artwork (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  code             VARCHAR(32)    NOT NULL,
  title            VARCHAR(64)    NOT NULL,
  student_name     VARCHAR(32)    NOT NULL,
  course_id        BIGINT         NOT NULL,
  greenware_id     BIGINT         NOT NULL,
  firing_batch_id  BIGINT         NOT NULL,
  owner_status     VARCHAR(16)    NOT NULL,
  consign_price    DECIMAL(10, 2),
  shelf_no         VARCHAR(32),
  finished_at      TIMESTAMP,
  created_at       TIMESTAMP,
  updated_at       TIMESTAMP
);

CREATE TABLE firing_certificate (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  artwork_id            BIGINT       NOT NULL,
  version_no            INT          NOT NULL,
  status                VARCHAR(16)  NOT NULL DEFAULT 'CURRENT',
  certificate_no        VARCHAR(40)  NOT NULL,
  issued_by             VARCHAR(32)  NOT NULL,
  issued_at             TIMESTAMP    NOT NULL,
  change_reason         VARCHAR(255),
  snap_artwork_code     VARCHAR(32)  NOT NULL,
  snap_title            VARCHAR(64)  NOT NULL,
  snap_student_name     VARCHAR(32)  NOT NULL,
  snap_owner_status     VARCHAR(16)  NOT NULL,
  snap_course_id        BIGINT,
  snap_course_code      VARCHAR(32),
  snap_course_title     VARCHAR(64),
  snap_teacher          VARCHAR(32),
  snap_greenware_id     BIGINT,
  snap_greenware_code   VARCHAR(32),
  snap_greenware_name   VARCHAR(64),
  snap_greenware_stage  VARCHAR(20),
  snap_shaped_at        TIMESTAMP,
  snap_clay_id          BIGINT,
  snap_clay_code        VARCHAR(32),
  snap_clay_name        VARCHAR(64),
  snap_clay_temp        INT,
  snap_glaze_id         BIGINT,
  snap_glaze_code       VARCHAR(32),
  snap_glaze_name       VARCHAR(64),
  snap_glaze_temp       INT,
  snap_batch_id         BIGINT,
  snap_batch_no         VARCHAR(32),
  snap_fire_type        VARCHAR(16),
  snap_target_temp      INT,
  snap_peak_temp        INT,
  snap_kiln_id          BIGINT,
  snap_kiln_code        VARCHAR(32),
  snap_kiln_name        VARCHAR(64),
  snap_loaded_at        TIMESTAMP,
  snap_heating_at       TIMESTAMP,
  snap_soaking_at       TIMESTAMP,
  snap_cooling_at       TIMESTAMP,
  snap_out_at           TIMESTAMP,
  created_at            TIMESTAMP,
  updated_at            TIMESTAMP
);
