# 简查 · 业务查询系统

Spring Boot 3.5 / Java 21 后端，Vue 3 / Vite 前端。提供业务订单查询、客户关键词搜索、状态筛选、分页、订单详情及概览统计。内置 12 条中文演示订单。

## 运行

需要 Linux x86_64、Node.js 20.19+（或 22.12+）、Python 3 和 curl。

```bash
cd /workspace/ChatDemo
./scripts/setup.sh
./scripts/start.sh
```

应用运行于 8080 端口，Spring Boot 同时提供构建后的 Vue 页面和 `/api` 接口。安装脚本下载 Maven 3.9.11 并验证 SHA-512，使用 npm 锁文件安装，运行前端构建和后端测试。脚本同时下载并校验 Temurin JDK 21.0.9。工具和依赖缓存放在 `/workspace/tools`，可通过 `CHATDEMO_TOOLS_DIR` 更换目录。

前端开发时，在第二个终端执行：

```bash
cd /workspace/ChatDemo/frontend
npm run dev
```

Vite 开发服务器使用 5173 端口，并将 `/api` 转发至 8080 后端。修改 Java 代码后重新执行安装脚本构建并重启后端；修改 Vue 页面时开发服务器自动更新。

## 接口

- `GET /api/health`：应用和数据库健康检查
- `GET /api/summary`：全局订单数、金额（包含已取消订单）、待处理订单数、客户数
- `GET /api/orders?keyword=星河&status=待处理&page=1&size=8`：订单查询，关键词覆盖订单号、客户名及产品名
- `GET /api/orders/{id}`：订单详情，不存在返回 404

页码范围 1–100000，每页 1–100 条，关键词最多 100 字符。查询使用 SQL 参数绑定；关键词中的 `%` 和 `_` 作为普通字符匹配。

## 测试和数据

安装脚本执行 Spring Boot 集成测试：统计、筛选、分页、特殊字符、注入输入、详情以及错误参数。H2 内存数据库每次后端启动重新装载演示数据，无需外部数据库或密钥。

这是一个只读的演示系统，目前没有登录权限、订单编辑或真实业务数据库接入。部署到真实业务环境前应单独实现认证授权并配置持久化数据库。

## Render 免费部署

1. 将代码（包含根目录 `Dockerfile`）提交到 GitHub 的 `main` 分支。
2. Render Web Service 连接该仓库，Language 选择 **Docker**，实例选择 **Free**。
3. Root Directory 留空，Dockerfile Path 使用 `./Dockerfile`，Health Check Path 设置为 `/api/health`。
4. 点击 **Manual Deploy → Deploy latest commit**，等待状态变为 **Live**，打开服务的 `onrender.com` 地址。

镜像构建会执行前端构建和后端测试，并将 Vue 页面打包进 Spring Boot。端口自动读取 Render 注入的 `PORT`，无需手动配置。无需外部数据库或 API 密钥。免费实例会休眠，唤醒时需等待；演示数据会在重启后恢复。
