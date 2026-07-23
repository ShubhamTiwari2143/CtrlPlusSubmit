FROM eclipse-temurin:21-jdk-jammy
RUN apt-get update && apt-get install -y tomcat10
RUN rm -rf /var/lib/tomcat10/webapps/ROOT/*
COPY ROOT.war /var/lib/tomcat10/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]