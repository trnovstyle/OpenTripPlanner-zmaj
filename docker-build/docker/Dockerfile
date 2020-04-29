FROM openjdk:11.0.1-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN mkdir -p /code/otpdata/malmo

WORKDIR /code

COPY ./services/otp-*-shaded.jar /code/otp-shaded.jar
COPY services/entrypoint.sh /code/download-graph.sh
RUN chmod a+x /code/download-graph.sh

EXPOSE 8080

ENTRYPOINT ["/code/entrypoint.sh"]
CMD ["java", "-Xms256m", "-Xmx6144m", "-jar", "/code/otp-shaded.jar", "--server", "--graphs","/code/otpdata", "--router", "malmo"]
