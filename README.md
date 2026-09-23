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

烧成履历凭证（作品完成烧成后签发，版本化、快照式、append-only）：

- `GET /api/artworks/{id}/certificate` —— 凭证聚合：当前版、全部历史版、来源链逐段核对结果、当前资料与快照差异
- `GET /api/artworks/{id}/certificate/{versionNo}` —— 查看/打印指定版本（含历史版，内容为签发当时快照）
- `POST /api/artworks/{id}/certificate/issue` —— 首次签发（body：`issuedBy`、`expectedCurrentId`，首签传 null）
- `POST /api/artworks/{id}/certificate/correct` —— 发起更正，追加新版本（body：`issuedBy`、`reason`、`expectedCurrentId`）

凭证关键规则：

1. **签发前逐段核对来源链**：作品 → 课程 / 坯体 → 泥料（釉料）/ 烧成批次 → 窑炉。来源缺失或坯体批次关联冲突（如未施釉坯体挂到釉烧批次）时拒绝签发，并指出缺的是哪段来源，不生成看似完整的凭证。
2. **快照留痕**：凭证保存签发当时的作品名称、学员、泥料/釉料、课程、烧成类型、目标/实际温度与各阶段时间；之后业务资料被修正，旧凭证内容不变。
3. **版本只追加不覆盖**：更正时旧版置 `SUPERSEDED` 保留、新版本成为 `CURRENT`；无差异时拒绝生成新版本。
4. **并发保护**：签发/更正携带读取时的当前凭证 id（乐观锁）+ 作品行悲观锁；另一名工作人员已更新版本时旧页面请求被拒，提示「凭证版本已变化，请重新读取」，前端详情抽屉同时轮询版本号提示冲突。
5. **升级前遗留作品**：没有凭证也能照常查看、改归属流转；首签从当前可核实数据生成 V1，不重建历史。

自关联品类树接口：

- `GET /api/material-categories/tree` —— 材料分类整棵树
- `GET /api/material-categories/{parentId}/children` —— 按父节点查子节点
- `GET /api/work-areas/tree`、`GET /api/work-areas/{parentId}/children` —— 工位分区整棵树

## 交互形态

- 主色芥黄 `#f9a825`
- 导航：面包屑 + 顶部横向分类条（分类条来自自关联树，点进下级后出现面包屑）
- 页面范式：窑炉烧成时间轴（装窑 → 升温 → 保温 → 冷却 → 出窑），点节点看该阶段详情
