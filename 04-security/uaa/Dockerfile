FROM tomcat:9-jdk8
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY target/dependency/cloudfoundry-identity-uaa.war /usr/local/tomcat/webapps/uaa.war
COPY uaa.yml /uaa/uaa.yml
ENV JAVA_OPTS="-Djava.awt.headless=true -XX:+UseG1GC -Dfile.encoding=UTF-8 -Duser.home=$TOMCAT_HOME -DUAA_CONFIG_FILE=/uaa/uaa.yml"
