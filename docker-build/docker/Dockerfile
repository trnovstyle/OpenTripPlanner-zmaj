FROM openjdk:11.0.1-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN mkdir -p /code/otpdata/malmo

WORKDIR /code

COPY target/resesok-otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/entrypoint.sh /code/entrypoint.sh
RUN chmod a+x /code/entrypoint.sh

COPY docker-build/docker/services/logback.xml /code/logback.xml
COPY docker-build/docker/services/ApplicationInsights.xml /code/ApplicationInsights.xml
COPY docker-build/docker/services/AI_Agent.xml /code/AI_Agent.xml

EXPOSE 8080

ENTRYPOINT ["/code/entrypoint.sh"]
