FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/rampart.jar /rampart/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/rampart/app.jar"]
