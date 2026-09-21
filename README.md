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

自关联品类树接口：

- `GET /api/material-categories/tree` —— 材料分类整棵树
- `GET /api/material-categories/{parentId}/children` —— 按父节点查子节点
- `GET /api/work-areas/tree`、`GET /api/work-areas/{parentId}/children` —— 工位分区整棵树

## 交互形态

- 主色芥黄 `#f9a825`
- 导航：面包屑 + 顶部横向分类条（分类条来自自关联树，点进下级后出现面包屑）
- 页面范式：窑炉烧成时间轴（装窑 → 升温 → 保温 → 冷却 → 出窑），点节点看该阶段详情
