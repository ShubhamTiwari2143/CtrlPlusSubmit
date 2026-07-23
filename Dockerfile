FROM eclipse-temurin:21-jdk-jammy

ENV CATALINA_HOME /usr/local/tomcat
ENV PATH $CATALINA_HOME/bin:$PATH
RUN mkdir -p "$CATALINA_HOME"
WORKDIR $CATALINA_HOME

RUN apt-get update && apt-get install -y curl && \
    curl -O https://archive.apache.org/dist/tomcat/tomcat-10/v10.1.54/bin/apache-tomcat-10.1.54.tar.gz && \
    tar -xvf apache-tomcat-10.1.54.tar.gz --strip-components=1 && \
    rm apache-tomcat-10.1.54.tar.gz

RUN rm -rf /usr/local/tomcat/webapps/ROOT

COPY Root.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]