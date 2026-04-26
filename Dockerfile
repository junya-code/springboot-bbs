FROM amazoncorretto:25

# OS のタイムゾーンを JST にする
RUN ln -sf /usr/share/zoneinfo/Asia/Tokyo /etc/localtime \
    && echo "Asia/Tokyo" > /etc/timezone

# JVM にもタイムゾーンを強制
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Tokyo"

WORKDIR /app
COPY target/bbs-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]
