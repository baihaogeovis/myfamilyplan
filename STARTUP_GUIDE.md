# 项目启动指南

## 一、环境准备

### 1. 安装JDK 17
下载地址: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html

验证安装:
```bash
java -version
```

### 2. 安装Maven
下载地址: https://maven.apache.org/download.cgi

配置环境变量后验证:
```bash
mvn -version
```

### 3. 安装MySQL 8.0
下载地址: https://dev.mysql.com/downloads/mysql/

启动MySQL服务并记住root密码。

### 4. 安装Node.js 18+
下载地址: https://nodejs.org/

验证安装:
```bash
node -v
npm -v
```

## 二、数据库初始化

### 1. 登录MySQL
```bash
mysql -u root -p
```

### 2. 执行初始化脚本
```bash
mysql -u root -p < database/init.sql
```

或者在MySQL命令行中执行:
```sql
source D:/企业能源事业部/代码/myfamilyplan/database/init.sql
```

### 3. 验证数据库
```sql
SHOW DATABASES;
USE family_plan;
SHOW TABLES;
SELECT * FROM user_info;
```

## 三、后端启动

### 1. 修改数据库配置
编辑文件: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/family_plan?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    username: root
    password: 你的MySQL密码
```

### 2. 启动后端服务
```bash
cd backend
mvn spring-boot:run
```

### 3. 验证启动
看到以下输出表示启动成功:
```
====================================
家庭成长系统启动成功!
API文档: http://localhost:8080/doc.html
====================================
```

### 4. 访问API文档
浏览器打开: http://localhost:8080/doc.html

## 四、前端启动

### 1. 安装依赖
```bash
cd frontend
npm install
```

### 2. 启动开发服务器
```bash
npm run dev
```

### 3. 验证启动
看到以下输出表示启动成功:
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

### 4. 访问系统
浏览器打开: http://localhost:5173

## 五、登录系统

### 默认账号
- 用户名: `admin`
- 密码: `admin123`

## 六、常见问题

### 1. 后端启动失败
- 检查MySQL服务是否启动
- 检查数据库配置是否正确
- 检查8080端口是否被占用

### 2. 前端无法连接后端
- 确保后端服务已启动
- 检查vite.config.ts中的代理配置
- 查看浏览器控制台是否有跨域错误

### 3. 登录失败
- 检查数据库是否初始化
- 检查user_info表是否有默认管理员账户
- 确认用户名密码是否正确

### 4. Maven下载依赖慢
配置阿里云镜像,在`backend/pom.xml`的`<build>`标签前添加:
```xml
<mirrors>
  <mirror>
    <id>aliyunmaven</id>
    <mirrorOf>central</mirrorOf>
    <name>阿里云公共仓库</name>
    <url>https://maven.aliyun.com/repository/public</url>
  </mirror>
</mirrors>
```

### 5. npm安装依赖慢
使用淘宝镜像:
```bash
npm config set registry https://registry.npmmirror.com
npm install
```

## 七、开发建议

### 后端开发
1. 使用IDEA打开backend目录
2. 等待Maven依赖下载完成
3. 修改代码后自动热部署(如果配置了devtools)
4. 查看控制台日志调试问题

### 前端开发
1. 使用VSCode打开frontend目录
2. 安装推荐插件: Vue Language Features (Volar), Tailwind CSS IntelliSense
3. 保存文件后自动热更新
4. 使用F12打开浏览器开发者工具调试

## 八、下一步开发

基础架构已完成,接下来需要实现:
1. 收入管理模块
2. 支出管理模块
3. 预算规划模块
4. 资金审批模块
5. 成员管理模块
6. 统计报表模块
7. 提醒中心模块

每个模块的开发顺序:
1. 创建Entity实体类
2. 创建Mapper接口
3. 创建Service接口和实现
4. 创建Controller控制器
5. 创建前端API接口
6. 创建前端页面组件

## 九、技术支持

- Spring Boot文档: https://spring.io/projects/spring-boot
- MyBatis Plus文档: https://baomidou.com/
- Vue 3文档: https://cn.vuejs.org/
- Element Plus文档: https://element-plus.org/zh-CN/
- ECharts文档: https://echarts.apache.org/
