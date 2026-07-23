FROM tomcat:10.1-jdk21
RUN apt-get update && apt-get install -y unzip
RUN rm -rf /usr/local/tomcat/webapps/*
COPY ROOT.war /usr/local/tomcat/webapps/ROOT.war
RUN mkdir /usr/local/tomcat/webapps/ROOT && \
    unzip /usr/local/tomcat/webapps/ROOT.war -d /usr/local/tomcat/webapps/ROOT && \
    rm /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]