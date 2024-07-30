# Use an official OpenJDK runtime as a parent image
FROM openjdk:22-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the application's jar file to the container
COPY target/telegram-bot-dream-stream-0.0.1-SNAPSHOT.jar /app/telegram-bot-dream-stream.jar

# Command to run the jar file
CMD ["java", "-jar", "/app/telegram-bot-dream-stream.jar"]

