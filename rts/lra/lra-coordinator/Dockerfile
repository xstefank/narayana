FROM java:openjdk-8-jdk

EXPOSE 8080

RUN touch /audit.log && chmod 777 /audit.log

ADD target/lra-coordinator-swarm.jar /opt/lra-coordinator-swarm.jar

CMD java $JAVA_OPTIONS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar /opt/lra-coordinator-swarm.jar
