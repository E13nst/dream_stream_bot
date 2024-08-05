# Use an official OpenJDK runtime as a parent image
FROM openjdk:22-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the application's jar file to the container
COPY target/telegram-bot-dream-stream-0.0.1-SNAPSHOT.jar /app/telegram-bot-dream-stream.jar

# Copy the external application.properties file into the Docker container
COPY application.properties /app/config/application.properties

# Set the environment variable to point to the external configuration file
ENV SPRING_CONFIG_LOCATION=classpath:/application.properties,file:/app/config/application.properties

# Command to run the jar file
CMD ["java", "-jar", "/app/telegram-bot-dream-stream.jar"]
