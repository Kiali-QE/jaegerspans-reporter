FROM openjdk:alpine

ENV APP_HOME /app/

COPY target/jaegerspans-reporter-*.jar $APP_HOME/jaegerspans-reporter.jar

WORKDIR $APP_HOME
CMD java -jar jaegerspans-reporter.jar