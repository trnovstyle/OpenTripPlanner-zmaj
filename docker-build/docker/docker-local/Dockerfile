FROM openjdk:11.0.1-jdk

RUN curl -sL https://aka.ms/InstallAzureCLIDeb | bash
RUN mkdir -p /code/otpdata/malmo

WORKDIR /code

COPY ./services/data/otp-*-shaded.jar /code/otp-shaded.jar
COPY ./services/entrypoint.sh /code/entrypoint.sh
RUN chmod a+x /code/entrypoint.sh

COPY ./services/logback.xml /code/logback.xml
COPY ./services/ApplicationInsights.xml /code/ApplicationInsights.xml
COPY ./services/AI_Agent.xml /code/AI_Agent.xml

EXPOSE 8080

ENTRYPOINT ["/code/entrypoint.sh"]
CMD ["java", "-Xms256m", "-Xmx6144m", "-jar", "/code/otp-shaded.jar", "--server", "--graphs","/code/otpdata", "--router", "malmo"]
