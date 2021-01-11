FROM openjdk:11.0.1-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN apt-get -y install zip unzip

WORKDIR /code

COPY target/resesok-otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/ /code

RUN chmod a+x /code/*.sh

EXPOSE 8080

COPY docker-build/docker/scuttle/scuttle /bin/scuttle
ENV ENVOY_ADMIN_API="http://127.0.0.1:15000"
ENV ISTIO_QUIT_API="http://127.0.0.1:15020"
ENTRYPOINT ["scuttle", "bash", "/code/entrypoint.sh"]
