FROM java:8
VOLUME /tmp
ADD basic-0.1-SNAPSHOT.jar app.jar
RUN sh -c 'touch /app.jar'
ENTRYPOINT ["java", "-server", "-Xms256M", "-Xmx512M", "-XX:MaxDirectMemorySize=256M", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]