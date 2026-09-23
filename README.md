# 陶艺工坊管理系统 · 出题用种子仓

一个陶艺工坊（陶吧 / 陶艺工作室）的全栈管理系统，含 4 个业务模块。

## 技术栈

- 后端：Spring Boot 3.3.0 + Java 17 + Maven，**JPA 自关联品类树**（`@ManyToOne` + `@OneToMany` 指向自身）
- 前端：Vue 3 + Element Plus + axios + Vite
- MySQL 8.0 + Redis 7 + nginx（Docker Compose 四服务）

## 端口

| 服务 | 端口 |
| --- | --- |
| 前端 | 8256 |
| 后端 | 8356（容器内 8080） |
| MySQL | 3556 |
| Redis | 6556 |

数据库 `pottery_studio`，包名 `com.pottery.studio`。

## 启动

```bash
chmod +x start.sh && docker compose up -d --build
```

## 业务模块与接口

| 模块 | 列表接口 |
| --- | --- |
| 泥料釉料台账 | `GET /api/materials` |
| 坯体与工位 | `GET /api/greenwares` |
| 窑炉与烧成 | `GET /api/firing-batches` |
| 课程与作品 | `GET /api/courses`、`GET /api/artworks` |
| 烧成履历凭证 | `GET /api/artworks/{id}/certificates`（作品详情面板） |

烧成履历凭证（作品详情 → 凭证版本 / 打印）：

- `GET /api/artworks/{id}/certificates` —— 当前版 + 历史版 + 来源链核对 + 快照与当前数据差异
- `GET /api/artworks/{id}/certificates/precheck` —— 签发前核对（不落库），逐段指出缺失来源 / 关联冲突
- `GET /api/artworks/{id}/certificates/current` —— 当前版本快照
- `GET /api/artworks/{id}/certificates/{certificateId}` —— 指定版本（含历史版）落库快照，用于打印
- `POST /api/artworks/{id}/certificates/issue?issuedBy=` —— 首签 / 老作品补签，来源核对不通过拒绝
- `POST /api/artworks/{id}/certificates/versions?expectedVersionNo=&issuedBy=&changeReason=` —— 更正追加新版本；版本过期返回 409

凭证规则：内容为签发当时的不可变快照（作品/学员/课程/泥料/釉料/来源坯体/批次/窑炉/目标与实际温度/各阶段时间）；
更正只追加新版本并把旧版置为 `SUPERSEDED`，不覆盖旧版；升级前无凭证的老作品照常流转，可从当前可核实数据首次补签。

自关联品类树接口：

- `GET /api/material-categories/tree` —— 材料分类整棵树
- `GET /api/material-categories/{parentId}/children` —— 按父节点查子节点
- `GET /api/work-areas/tree`、`GET /api/work-areas/{parentId}/children` —— 工位分区整棵树

## 交互形态

- 主色芥黄 `#f9a825`
- 导航：面包屑 + 顶部横向分类条（分类条来自自关联树，点进下级后出现面包屑）
- 页面范式：窑炉烧成时间轴（装窑 → 升温 → 保温 → 冷却 → 出窑），点节点看该阶段详情
