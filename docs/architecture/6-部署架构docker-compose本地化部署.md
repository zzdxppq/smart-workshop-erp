# 6. 部署架构（Docker Compose，本地化部署）

> **V1.3.7 关键变化**：删除"阿里云 ACR / 阿里云 SLS / 阿里云 OSS"等公有云依赖；改为"本地 Docker Registry + 本地文件存储 + 异地冷备硬盘"。

```
                              [Internet] (V1.0.0 可选 · V1.0 局域网单机)
                                  |
                                  v
                          [Nginx :80/:443]
                              |       |
                  /web/*     |       |    /api/*
                              |       |
                  [Web SPA]  |       |    [Nginx upstream]
                              |       |        |
                              |       |        v
                              |       |    [erp-gateway × 2 :8080]
                              |       |        |
                              |       |        +---> [erp-platform × 1 :8081]
                              |       |        +---> [erp-business × 2 :8082]
                              |       |        +---> [erp-production × 2 :8083]
                              |       |
                  [APK 下载页]|       |    [Nginx upstream]
                              v       v
                          [MySQL Master :3306]
                                  |
                          async replication
                                  |
                          [MySQL Slave :3306 (read_only)]

+----------------+    +-----------------+    +-----------------+
| Redis :6379    |    | MinIO :9000     |    | XXL-JOB :8088   |
| - Cache        |    | :9001 console   |    | Admin Console   |
| - Stream       |    | - drawings      |    +-----------------+
| - Lock         |    | - contracts     |
+----------------+    | - reports       |
                      | - **对账签字扫描件桶 (V1.3.6 · AES-256-GCM · 5 年)** |
                      +-----------------+

+----------------+    +-----------------+    +-----------------+
| Nacos :8848    |    | Prometheus      |    | SkyWalking      |
| :9848 gRPC     |--->| :9090           |    | OAP + UI        |
| - Registry     |    | Grafana :3000   |    +-----------------+
| - Config       |    +-----------------+
+----------------+

（XXL-JOB 容器在 5.2 节已列出 · 14 个任务）
```

**资源分配（8 核 32G 服务器）**：

| 容器 | 内存限制 | CPU 限制 | 数量 |
|------|---------|---------|------|
| MySQL Master | 4 GB | 2 核 | 1 |
| MySQL Slave | 2 GB | 1 核 | 1 |
| Redis | 2 GB | 1 核 | 1 |
| MinIO | 2 GB | 0.5 核 | 1 |
| XXL-JOB | 1 GB | 0.5 核 | 1 |
| **Nacos** | **1 GB** | **0.3 核** | **1** |
| Nginx | 512 MB | 0.5 核 | 1 |
| erp-gateway | 1 GB | 0.5 核 | 2 |
| erp-business | 2 GB | 1 核 | 2 |
| erp-production | 2 GB | 1 核 | 2 |
| erp-platform | 1 GB | 0.5 核 | 1 |
| Prometheus | 1 GB | 0.5 核 | 1 |
| Grafana | 512 MB | 0.3 核 | 1 |
| SkyWalking | 2 GB | 0.5 核 | 1 |
| **合计** | **~22 GB** | **~9.3 核** | **16+ 容器** |

---
