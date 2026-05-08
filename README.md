# 个人智能健身管理系统

## 项目简介

本项目是一套**前后端分离**的个人健身管理应用：后端基于 **Spring Boot 3.2** 提供 REST API 与 **SSE 流式 AI 对话**；前端基于 **Vue 3 + Vite + Element Plus** 提供管理界面。典型能力包括：

- **用户**：注册、登录、JWT 鉴权、个人资料
- **健身计划**：结合身体数据与偏好由 AI 生成周期计划并落库
- **每日打卡**：今日任务、打卡提交、连续打卡统计（Redis）
- **AI 对话**：OpenAI 兼容接口流式回复，带业务上下文与限流

后端 API 统一前缀为 **`/fitness-api`**（见 `application.yml` 中 `server.servlet.context-path`），前端默认请求 `http://localhost:8080/fitness-api`。

## 技术栈

| 层级 | 技术                                                               |
| ---- | ------------------------------------------------------------------ |
| 后端 | JDK 17、Spring Boot 3.2、MyBatis-Plus、MySQL 8、Redis、JWT（jjwt） |
| 前端 | Vue 3、Vite 5、Pinia、Vue Router、Element Plus、Axios、Markdown-it |

## 必要运行环境

运行本项目前，本机需要具备下表中的环境；**版本不满足时可能导致编译或运行失败**。

| 环境        | 建议版本            | 用途                                  |
| ----------- | ------------------- | ------------------------------------- |
| **JDK**     | 17（LTS）           | 编译与运行 Spring Boot 后端           |
| **Maven**   | 3.6+                | 后端依赖管理与打包                    |
| **Node.js** | 18+（建议当前 LTS） | 前端构建与开发服务器                  |
| **npm**     | 随 Node 自带        | 安装前端依赖                          |
| **MySQL**   | 8.0+                | 业务数据持久化                        |
| **Redis**   | 6+（常用 7.x）      | 会话扩展能力、打卡 streak、对话限流等 |

**此外**还需在配置中提供（非“安装”，但属于运行必备）：

- 数据库账号密码（或环境变量 `MYSQL_USERNAME` / `MYSQL_PASSWORD`）
- **`JWT_SECRET`**：JWT 签名密钥（建议至少 32 字节随机串）
- **大模型（默认对接讯飞 MaaS OpenAI 兼容接口）**：在运行环境中设置 **`LLM_API_KEY`**（控制台常为 **`APIKey:APISecret`** 整段，含英文冒号，作为 `Authorization: Bearer …` 传入）。可选覆盖：`LLM_API_URL`（须为完整地址，如 `https://maas-api.cn-huabei-1.xf-yun.com/v2/chat/completions`）、`LLM_MODEL_NAME`（须与控制台 modelId 一致，如 `Qwen3.5-2B`）、`LLM_MAX_TOKENS`。未配置 Key 时，AI 对话可走固定话术降级（见 `llm.fallback-enabled`）；计划生成会回退内置模板。

数据库初始化：创建库名 **`fitness_mvp`**。仓库默认在 `application.yml` 中配置 **`root` / `123456`**（可用环境变量 `MYSQL_USERNAME`、`MYSQL_PASSWORD` 覆盖）。**注意：这是连接 MySQL 的账号，不是网站登录账号。** 网站登录使用「注册时的用户名」。

- **推荐一键脚本**：`fitness-management-backend/src/main/resources/db/fitness_mvp_all.sql`（已合并建表、`demo`/`demo123` 演示账号的幂等插入，以及旧版 `punch_record` 字段/索引的条件化补丁）。**Windows** 可将 `mysql` 加入 PATH 后双击根目录 **`初始化数据库.bat`**（已指向该合并脚本）。
- **拆分文件**（与合并版内容对应，按需单独维护或引用）：`fitness_mvp_schema.sql`、`seed_demo_user.sql`、`alter_punch_record_checkin_columns.sql`。仅补演示用户时可执行 `seed_demo_user.sql`（避免在 PowerShell 里把含 `$` 的哈希内联进 `-e` 参数）。

Redis 默认连接 **`127.0.0.1:6379`**，无密码时 `application.yml` 中密码留空即可。

---

### 环境安装参考（任选一种系统）

以下命令在 **PowerShell / CMD / 终端** 中执行；若已安装，可跳到「验证」命令自检。**包管理器未安装时**，可到各软件官网下载安装包（JDK：Adoptium / Oracle；Node：nodejs.org；MySQL / Redis：官方安装包或 Docker）。

#### Windows（推荐已安装 [winget](https://learn.microsoft.com/windows/package-manager/winget/)）

```powershell
# JDK 17（Eclipse Temurin）
winget install EclipseAdoptium.Temurin.17.JDK --accept-package-agreements

# Maven
winget install Apache.Maven --accept-package-agreements

# Node.js LTS（含 npm）
winget install OpenJS.NodeJS.LTS --accept-package-agreements

# MySQL 8（安装向导中设置 root 密码；安装后需将密码写入 application.yml 或环境变量）
winget install Oracle.MySQL

# Redis：官方无原生安装包时，可用 Memurai（兼容 Redis 协议）或 Docker 跑 Redis
winget install Memurai.MemuraiDeveloper
# 若已安装 Docker Desktop，可用：docker run -d --name redis-dev -p 6379:6379 redis:7-alpine
```

**安装后请新开一个终端**，使 `PATH` 生效，再执行下方「验证」命令。

导入表结构（在仓库根目录执行，路径按本机调整）：

```bash
docker exec -i mysql-dev mysql -uroot -prootpass --default-character-set=utf8mb4 < fitness-management-backend/src/main/resources/db/fitness_mvp_all.sql
```

---

### 安装是否成功（验证命令）

```bash
java -version    # 应出现 openjdk 17 或 17.x
mvn -v           # 应显示 Maven 与 Java 17
node -v          # 建议 v18+
npm -v
mysql --version  # 应 8.x
redis-cli ping   # 应返回 PONG（Redis 已启动时）
```

## 项目结构

```
fitness-management-system/
├── fitness-management-backend/    # Spring Boot 后端
├── fitness-management-frontend/   # Vue 3 前端
└── README.md
```

## 启动与关闭

以下命令均在项目根目录 **`fitness-management-system`** 下说明；**开发时建议先启后端，再启前端**。

### 后端（Spring Boot）

**启动（开发，前台运行）：**

```bash
cd fitness-management-backend
mvn spring-boot:run
```

成功后可访问：`http://localhost:8080/fitness-api`（具体接口以控制器为准）。

**关闭：** 在运行该命令的终端窗口按 **`Ctrl + C`**，等待进程结束即可。

**可选：先打包再运行 JAR（适合联调或部署）：**

```bash
cd fitness-management-backend
mvn -DskipTests package
java -jar target/fitness-management-backend-0.0.1-SNAPSHOT.jar
```

关闭方式同样为终端中 **`Ctrl + C`**（若以后台服务运行，请使用对应系统的服务停止命令）。

### 前端（Vite 开发服务器）

**启动：**

```bash
cd fitness-management-frontend
npm install
npm run dev
```

默认开发地址：**`http://localhost:5173`**（见 `vite.config.js`）。

**关闭：** 在运行 `npm run dev` 的终端按 **`Ctrl + C`**。

**生产构建与本地预览：**

```bash
cd fitness-management-frontend
npm run build
npm run preview
```

`preview` 停止方式亦为 **`Ctrl + C`**。

### Windows 一键脚本（根目录）

| 文件 | 说明 |
|------|------|
| **`一键启动.bat`** | 打开两个新窗口：分别执行 `mvn spring-boot:run` 与 `npm run dev`（首次会自动 `npm install`） |
| **`一键关闭.bat`** | 尝试结束占用 **8080**、**5173** 端口的监听进程（依赖 PowerShell，需 Windows 10/11） |

双击运行即可；若路径含空格，请保持项目目录完整拷贝。**关闭脚本**可能结束所有占用上述端口的进程，请避免与其他项目共用同一端口。

### 在 Cursor 内置浏览器中打开前端

1. 先按上文或「一键启动」启动开发服务，确保 **`http://localhost:5173`** 可访问。  
2. 在 Cursor 中按 **`Ctrl+Shift+P`**（macOS 为 **`Cmd+Shift+P`**），输入并选择 **`Simple Browser: Show`**。  
3. 在地址栏输入 **`http://localhost:5173`** 并确认。

若内置 Simple Browser 不可用，可在系统浏览器中直接访问同一地址。

## 常见问题

- **跨域**：后端已配置允许本地前端源（如 `http://localhost:5173`）；若更换端口或域名，请同步修改后端 CORS 与前端 `request.js` 中的 `baseURL`。
- **登录后 401**：检查 `JWT_SECRET` 是否已配置、令牌是否过期、请求头是否携带 `Authorization: Bearer <token>`。
- **讯飞 MaaS 大模型**：`application.yml` 已默认 `api-url` + `Qwen3.5-2B`。请在**启动后端前**设置环境变量（勿把 Key 写入仓库）。PowerShell 示例：  
  `$env:LLM_API_KEY="你的APIKey:你的APISecret"`  
  若控制台 modelId 与默认不一致，再设：`$env:LLM_MODEL_NAME="控制台显示的模型ID"`。

---

文档随项目迭代可继续补充部署、环境变量清单与接口说明。
