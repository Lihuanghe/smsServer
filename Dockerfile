FROM openjdk:8-jdk-alpine
ENV CMPPHOME=/opt/cmpp
RUN mkdir ${CMPPHOME}  && echo "Asia/Shanghai" > /etc/timezone
ADD target/sms-0.0.1.jar ${CMPPHOME}
WORKDIR ${CMPPHOME}
ENTRYPOINT ["java","-jar","sms-0.0.1.jar"]