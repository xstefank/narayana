FROM java:openjdk-8-jdk

EXPOSE 8080

RUN touch /audit.log && chmod 777 /audit.log

ADD target/lra-coordinator-swarm.jar /opt/lra-coordinator-swarm.jar

CMD java $JAVA_OPTIONS -jar /opt/lra-coordinator-swarm.jar -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv4Addresses=true
