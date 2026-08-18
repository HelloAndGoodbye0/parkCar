# ============================================================
# 后端多阶段构建：Maven 构建 → JRE 运行
# ============================================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build

# 使用阿里云镜像加速依赖下载
COPY .tools/settings.xml /root/.m2/settings.xml

# 先拷贝 pom.xml 预下载依赖（利用 Docker 层缓存加速后续构建）
COPY pom.xml .
RUN mvn -B dependency:go-offline -q

COPY src ./src
RUN mvn -B clean package -DskipTests -q

# ---- 运行阶段 ----
FROM eclipse-temurin:17-jre
WORKDIR /app
ENV TZ=Asia/Shanghai
COPY --from=builder /build/target/parkcar-backend-1.0.0.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
