# Wuuees-Log-Starter（轻量级日志查询功能）

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen.svg" alt="Spring Boot Version">
  <img src="https://img.shields.io/badge/Java-17-orange.svg" alt="Java Version">
  <img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License">
</p>

<p align="center">
  <img src="https://spring.io/images/projects/spring-framework-640ad29203047381bb43fbf4ccc70464.svg" width="100" alt="Spring Boot Logo">
</p>

<p align="center">
  一个轻量级的日志读取和监控服务，基于Spring Boot构建，提供实时日志查看和查询功能。
</p>

<p align="center">
  <a href="#核心功能"><strong>核心功能</strong></a> •
  <a href="#技术架构"><strong>技术架构</strong></a> •
  <a href="#快速开始"><strong>快速开始</strong></a> •
  <a href="#api接口"><strong>API接口</strong></a>
</p>

---

## 🌟 项目概述

Wuuees-Log-Starter是一个专门用于日志查看和监控的轻量级服务模块，主要面向开发人员和运维人员，解决了传统日志查看方式不够直观、缺乏实时性的问题。通过WebSocket技术实现实时日志推送，并提供HTTP接口用于日志查询和下载。

### 🎯 解决的问题
- 传统日志查看方式不直观
- 缺乏实时日志监控能力
- 难以在微服务架构中统一管理日志

### ✨ 项目亮点
- 🔥 **实时监控** - 基于WebSocket的实时日志推送
- 🔍 **灵活查询** - 支持关键字、级别、时间范围过滤
- ⚙️ **高度可配置** - 支持自定义日志路径和文件类型
- 📦 **易于集成** - Spring-Boot-Starter模式，开箱即用

## 🚀 核心功能

### 1. 实时日志监控
- 基于WebSocket的实时日志推送功能
- 支持选择特定日志文件进行监控
- 实时显示新增的日志内容

### 2. 日志查询与过滤
- 支持按关键字搜索日志内容
- 支持按日志级别过滤（如ERROR、INFO等）
- 支持按时间范围过滤日志
- 支持分页查询
- 支持倒序查看日志

### 3. 日志文件管理
- 获取日志文件列表
- 支持日志文件下载
- 支持下载过滤后的日志内容

### 4. 配置灵活
- 可配置日志文件路径
- 可配置允许访问的文件类型
- 可配置最大文件大小限制
- 可开启或关闭安全检查

## ⚙️ 技术架构

### 核心技术栈
- Java 17
- Spring Boot 3.5.5
- Spring Web MVC
- Spring WebSocket
- Apache Commons IO 2.11.0
- Apache Commons Lang3
- Lombok

### 架构模式
- MVC模式（Spring Web MVC）
- 发布-订阅模式（WebSocket实时推送）
- 模块化设计（Starter模式）

### 主要组件

#### 1. 配置层 (config)
- [LogConfigProperties](src/main/java/com/wuuees/log/config/LogConfigProperties.java)：日志服务配置属性类，用于读取和管理日志相关配置
- [WebSocketConfig](src/main/java/com/wuuees/log/config/WebSocketConfig.java)：WebSocket配置类，配置STOMP端点和消息代理

#### 2. 控制层 (controller)
- [LogController](src/main/java/com/wuuees/log/controller/LogController.java)：提供RESTful API接口，包括获取日志文件列表、查询日志内容、下载日志文件等功能
- [WebSocketController](src/main/java/com/wuuees/log/controller/WebSocketController.java)：处理WebSocket消息，控制日志实时监控的开始和停止

#### 3. 服务层 (service)
- [LogService](src/main/java/com/wuuees/log/service/LogService.java)：核心日志服务类，实现日志文件列表获取、日志查询、日志下载等业务逻辑
- [LogMonitorService](src/main/java/com/wuuees/log/service/LogMonitorService.java)：日志监控服务类，实现基于文件系统监控的实时日志推送功能

#### 4. 工具层 (util)
- [LogParser](src/main/java/com/wuuees/log/util/LogParser.java)：日志解析工具类，用于解析日志行的时间、级别等信息

#### 5. 数据传输层 (dto)
- [LogQueryDto](src/main/java/com/wuuees/log/dto/LogQueryDto.java)：日志查询相关的数据传输对象，包括查询请求和响应
- [LogLineInfo](src/main/java/com/wuuees/log/dto/LogLineInfo.java)：单行日志信息的数据传输对象

## 📁 项目结构

```
wuuees-log-starter/
├── src/
│   ├── main/
│   │   ├── java/com/wuuees/log/
│   │   │   ├── config/              # 配置类
│   │   │   ├── controller/          # 控制器类
│   │   │   ├── dto/                 # 数据传输对象
│   │   │   ├── service/             # 业务服务类
│   │   │   ├── util/                # 工具类
│   │   │   └── WuueesLogStarterApplication.java  # 主应用类
│   │   └── resources/
│   │       ├── static/              # 静态资源文件
│   │       │   └── index.html       # 前端页面
│   │       └── application.yaml     # 应用配置文件
│   └── test/                        # 测试代码
└── pom.xml                          # Maven配置文件
```

## 🛠 快速开始

### 环境要求
- JDK 17 或更高版本
- Maven 3.x

### 安装步骤
1. 克隆项目到本地
```bash
git clone <repository-url>
```

2. 进入项目目录
```bash
cd wuuees-log-starter
```

3. 编译和打包
```bash
mvn clean package
```

4. 运行项目
```bash
mvn spring-boot:run
```

### 配置说明

在`application.yaml`中可以配置以下参数：

```yaml
wuuees:
  log:
    viewer:
      log-path: ./logs              # 日志文件根路径
      allowed-extensions:           # 允许访问的日志文件扩展名
        - .log
        - .txt
      max-lines: 1000               # 单次查询的最大行数
      max-file-size: 100            # 文件最大大小（MB）
      enable-security: true         # 是否启用安全检查
```

## 📡 API接口

### 日志文件相关
- `GET /api/logs/files` - 获取日志文件列表
- `GET /api/logs/download/{fileName}` - 下载日志文件

### 日志查询相关
- `POST /api/logs/query` - 查询日志内容

### WebSocket端点
- `/ws-log-monitor` - WebSocket连接端点

## 🖥 前端界面

项目提供了一个简单的前端界面(index.html)，包含以下功能：
- 日志文件选择
- 实时日志监控开关
- 日志查询（关键字、级别、时间范围）
- 日志内容展示
- 日志文件下载

可以通过浏览器直接访问应用根路径查看前端界面。

## 🤝 使用方式

1. 将该Starter集成到Spring Boot项目中
2. 配置日志文件路径和其他相关参数
3. 启动应用后访问前端页面或调用API接口使用日志功能

## 📄 许可证

本项目采用Apache License 2.0许可证，详情请参见[LICENSE](LICENSE)文件。
