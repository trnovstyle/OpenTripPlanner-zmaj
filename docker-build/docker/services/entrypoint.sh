#!/bin/bash

FILE_TMP_PATH=/code/otpdata/malmo/Graph.obj

echo "Download Graph.obj to $FILE_TMP_PATH" > /var/log/otp.log
echo "Download Graph.obj to $FILE_TMP_PATH"
az storage blob download \
    --account-name tjpsageo \
    --container-name $AZ_CONTAINER \
    --name Graph.obj \
    --file $FILE_TMP_PATH \
    --auth-mode key --account-key $TJPSAGEO_AZURE_BLOB_STORAGE_KEY

echo "$FILE_TMP_PATH downloaded from AZ_CONTAINER $AZ_CONTAINER" >> /var/log/otp.log
echo "$FILE_TMP_PATH downloaded from AZ_CONTAINER $AZ_CONTAINER"

if [ -s $FILE_TMP_PATH ] ;
then
  echo "Downloaded $FILE_TMP_PATH"
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**" >> /var/log/otp.log
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  exit 1
fi

cd /code
wget https://github.com/microsoft/ApplicationInsights-Java/releases/download/2.6.1/applicationinsights-agent-2.6.1.jar
wget https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-web-auto/2.6.1/applicationinsights-web-auto-2.6.1.jar
wget https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-logging-logback/2.6.1/applicationinsights-logging-logback-2.6.1.jar

jar -xf applicationinsights-web-auto-2.6.1.jar && jar -xf applicationinsights-logging-logback-2.6.1.jar
jar -uf /code/otp-shaded.jar com/
jar -uf /code/otp-shaded.jar ApplicationInsights.xml
jar -uf /code/otp-shaded.jar logback.xml

echo "Start java OTP jar" >> /var/log/otp.log
echo "Start java OTP jar"

exec java -javaagent:/code/applicationinsights-agent-2.6.1.jar -DAPPINSIGHTS_INSTRUMENTATIONKEY=$applicationInsightsKey -Xms256m -Xmx6144m -jar /code/otp-shaded.jar --server --graphs /code/otpdata --router malmo
