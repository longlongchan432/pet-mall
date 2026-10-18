# 后端（Spring Boot 2.7.18 + JDK 1.8）镜像：两阶段构建
# 不在本地先打包再 COPY jar —— 让镜像自洽，虚拟机/CI 上同一条命令就能出镜像
FROM maven:3.8.6-openjdk-8 AS build
WORKDIR /src
# 先只拷 pom 拉依赖：改代码不会重下依赖。若这行在某些网络下报错，删掉它直接和源码一起 package 也能成功
COPY pom.xml .
RUN mvn -B -q dependency:resolve
COPY src ./src
RUN mvn -B -q package -DskipTests

# 运行阶段只要 JRE8，镜像小一半
FROM eclipse-temurin:8-jre
WORKDIR /app
COPY --from=build /src/target/pet-travel-station-1.0.0.jar app.jar
EXPOSE 8080
# 配置全部由 docker-compose.yml 用环境变量注入（TZ / FILE_STORAGE_TYPE / FILE_UPLOAD_PATH /
# SPRING_DATASOURCE_URL），这里不为某台机器写死任何值
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
