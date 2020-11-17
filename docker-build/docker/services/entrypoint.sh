#!/bin/bash
. /code/get-keyvault-value.sh


FILE_TMP_PATH=/code/otpdata/malmo/Graph.obj
SA_NAME="ressa$ENVIRONMENT"

keyvault="RES-OTP-KV-${ENVIRONMENT^^}"

echo "Download Graph.obj to $FILE_TMP_PATH" > /var/log/otp.log
echo "Download Graph.obj to $FILE_TMP_PATH"

if ! az login --identity; then
  echo "Login to Azure using assigned identity failed"
  exit 1
fi

if ! az storage blob download \
    --account-name $SA_NAME \
    --container-name resesok-graph \
    --name Graph.obj \
    --file $FILE_TMP_PATH \
    --auth-mode key; then

      echo "Failed to download for account name $SA_NAME"
      exit 1
fi

# Get value from KeyVault
if ! applicationInsightsKey=$(getKeyVaultValue $keyvault "applicationInsightsKey"); then
  # Warn about not finding this key, but don't stop the startup
  echo "Error fetching value for KeyVault key $applicationInsightsKey in keyvault $keyvault"
  exit 1
fi

# Cut of " chars and set as environment variable
export applicationInsightsKey=${applicationInsightsKey:1:-1}

echo "$FILE_TMP_PATH downloaded " >> /var/log/otp.log
echo "$FILE_TMP_PATH downloaded "

if [ -s $FILE_TMP_PATH ] ;
then
  echo "Downloaded $FILE_TMP_PATH"
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**" >> /var/log/otp.log
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  exit 1
fi

cd /code || exit 1

wget -q https://github.com/microsoft/ApplicationInsights-Java/releases/download/2.6.1/applicationinsights-agent-2.6.1.jar > /dev/null
wget -q https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-web-auto/2.6.1/applicationinsights-web-auto-2.6.1.jar > /dev/null
wget -q https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-logging-logback/2.6.1/applicationinsights-logging-logback-2.6.1.jar > /dev/null

jar -xf applicationinsights-web-auto-2.6.1.jar > /dev/null && jar -xf applicationinsights-logging-logback-2.6.1.jar > /dev/null
jar -uf /code/otp-shaded.jar com/ > /dev/null
jar -uf /code/otp-shaded.jar ApplicationInsights.xml > /dev/null
jar -uf /code/otp-shaded.jar logback.xml > /dev/null

echo "Start java OTP jar" >> /var/log/otp.log
echo "Start java OTP jar"

exec java -javaagent:/code/applicationinsights-agent-2.6.1.jar -DAPPINSIGHTS_INSTRUMENTATIONKEY=$applicationInsightsKey -Xms256m -Xmx6144m -jar /code/otp-shaded.jar --server --graphs /code/otpdata --router malmo
