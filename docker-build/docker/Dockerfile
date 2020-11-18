FROM openjdk:11.0.1-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN mkdir -p /code/otpdata/malmo

WORKDIR /code

COPY target/resesok-otp-*-shaded.jar /code/otp-shaded.jar
COPY docker-build/docker/services/ /code

RUN chmod a+x /code/*.sh

EXPOSE 8080

ENTRYPOINT ["/code/entrypoint.sh"]
