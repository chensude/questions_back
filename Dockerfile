# 使用官方Maven镜像作为构建环境
FROM maven:3.8-openjdk-8 AS build

# 设置工作目录
WORKDIR /app

# 复制pom.xml和源代码
COPY pom.xml .
COPY src ./src

# 构建应用
RUN mvn clean package -DskipTests

# 使用JDK8运行环境
FROM openjdk:8-jre-slim

WORKDIR /app

# 从构建阶段复制jar文件
COPY --from=build /app/target/*.jar app.jar

# 暴露端口（根据您的应用配置修改）
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java","-jar","app.jar"] 