# Use an official OpenJDK runtime as a parent image
FROM openjdk:22-jdk

# Set the working directory in the container
WORKDIR /app

# Установка SSH клиента и curl
# RUN apk update
# RUN apk add openssh

COPY proxy.sh /app/proxy.sh
RUN chmod +x /app/proxy.sh

# Copy the application's jar file to the container
COPY target/telegram-bot-dream-stream-0.0.1-SNAPSHOT.jar /app/telegram-bot-dream-stream.jar

# Command to run the jar file
CMD ["java", "-jar", "/app/telegram-bot-dream-stream.jar"]
# CMD ["/bin/sh", "-c", "/app/proxy.sh && java -jar /app/telegram-bot-dream-stream.jar"]

