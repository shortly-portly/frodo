FROM openjdk:8-alpine

COPY target/uberjar/frodo.jar /frodo/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/frodo/app.jar"]
