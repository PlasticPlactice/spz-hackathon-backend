# --- ステージ1: アプリケーションをビルドする環境 ---
FROM openjdk:17-jdk-slim AS build

WORKDIR /app

# Maven Wrapperとpom.xmlをコピー
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# mvnwに実行権限を付与する
RUN chmod +x ./mvnw

# 依存関係を先にダウンロードしておくことで、ビルドを高速化
RUN ./mvnw dependency:go-offline

# アプリケーションのソースコードをコピー
COPY src ./src

# アプリケーションをビルド
RUN ./mvnw clean package -DskipTests


# --- ステージ2: アプリケーションを実行するだけの最小環境 ---
FROM openjdk:17-jdk-slim

WORKDIR /app

# ビルドステージから、完成したJARファイルだけをコピー
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# アプリケーションが使用するポートを公開
EXPOSE 8080

# コンテナが起動したときに実行するコマンド
CMD ["java", "-jar", "app.jar"]
