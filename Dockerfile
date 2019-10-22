FROM openjdk:8-jdk-alpine
ENV CMPPHOME=/opt/cmpp
RUN mkdir ${CMPPHOME}  && echo "Asia/Shanghai" > /etc/timezone
ADD target/demo-0.0.1.jar ${CMPPHOME}
WORKDIR ${CMPPHOME}
ENTRYPOINT ["java","-jar","demo-0.0.1.jar"]