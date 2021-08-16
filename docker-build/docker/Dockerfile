FROM openjdk:11.0.12-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN apt-get -y install zip unzip

# install application insights
RUN curl -LO https://github.com/microsoft/ApplicationInsights-Java/releases/download/3.1.0/applicationinsights-agent-3.1.0.jar
RUN mkdir /code
RUN mv ./applicationinsights-agent-3.1.0.jar /code/applicationinsights-agent-3.1.0.jar

WORKDIR /code

COPY target/resesok-otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/ /code

RUN chmod a+x /code/*.sh

EXPOSE 8080

COPY docker-build/docker/scuttle/scuttle /bin/scuttle
ENV ENVOY_ADMIN_API="http://127.0.0.1:15000"
ENV ISTIO_QUIT_API="http://127.0.0.1:15020"
ENV APPLICATIONINSIGHTS_ROLE_NAME="OpenTripPlanner"
ENTRYPOINT ["scuttle", "bash", "/code/entrypoint.sh"]
