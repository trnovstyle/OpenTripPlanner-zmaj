#!/bin/bash
. /code/bash-files/graph-build.sh
. /code/bash-files/azure-utils.sh
. /code/bash-files/logging.sh

FILE_TMP_PATH=/code/otpdata/malmo/
FILE_ZIP_PATH=/code/otpdata/malmo/Graph.zip
FILE_NETEX_PATH=/code/otpdata/malmo
OTP_JAR_PATH=/code/otp-shaded.jar

GRAPH_CONTAINER="resesok-graph"
NETEX_FILENAME="ST_netex.zip"
OSM_FILENAME="sweden-filtered.osm.pbf"
SA_NAME="ressa$ENVIRONMENT"

log_info "Running Entrypoint.sh.."

if ! VERSION=$(java -jar /code/otp-shaded.jar --version|grep version|cut -d' ' -f2) ||
 ! GIT_HASH=$(java -jar /code/otp-shaded.jar --version|grep commit|cut -d' ' -f2); then
  log_error "Failed to get OTP version or hash"
  exit 1
fi

GRAPH_NAME=GRAPH-$VERSION-$GIT_HASH.zip

keyvault="RES-OTP-KV-${ENVIRONMENT^^}"

log_info "Logging into azure.."

if ! az login --identity; then
  log_error "Login to Azure using assigned identity failed"
  exit 1
fi

# Get value from KeyVault
if ! applicationInsightsKey=$(getKeyVaultValue $keyvault "applicationInsightsKey"); then
  # Warn about not finding this key, but don't stop the startup
  log_error "Error fetching value for KeyVault key $applicationInsightsKey in keyvault $keyvault"
  exit 1
fi

# Cut of " chars and set as environment variable
export applicationInsightsKey=${applicationInsightsKey:1:-1}

# Attempt to download graph from azure storage
downloadFromAzureStorage $SA_NAME $GRAPH_CONTAINER $GRAPH_NAME $FILE_ZIP_PATH

if [ -s $FILE_ZIP_PATH ] ;
then
  log_info "Found $FILE_ZIP_PATH, unzipping.."
  unzip $FILE_ZIP_PATH -d $FILE_TMP_PATH
else
  log_info "** WARNING: Downloaded file ($FILE_ZIP_PATH) is empty or not present**"
  # Download netex + OSM data from azure storage in background
  downloadNetexFiles $FILE_NETEX_PATH &
  downloadOSMFile $FILE_NETEX_PATH &
  # Wait for download of Netex/OSM to finish
  wait
  if [ -s $FILE_NETEX_PATH/$NETEX_FILENAME ] && [ -s $FILE_NETEX_PATH/$OSM_FILENAME ] ;
  then

    if ! buildGraphFromNetexData $OTP_JAR_PATH $FILE_NETEX_PATH $FILE_TMP_PATH; then
      log_error "Building graph failed"
      exit 1
    fi
    rm $FILE_NETEX_PATH/$NETEX_FILENAME $FILE_NETEX_PATH/$OSM_FILENAME
    zip -j /code/${GRAPH_NAME} ${FILE_TMP_PATH}Graph.obj
    uploadToAzureStorage $SA_NAME $GRAPH_CONTAINER /code/${GRAPH_NAME} $GRAPH_NAME
  else
    ls -al $FILE_NETEX_PATH
    log_error "ERROR: Empty netex or OSM file, quitting."
    exit 1
  fi
fi

cd /code/appInsights || exit 1

wget -q -P /code https://github.com/microsoft/ApplicationInsights-Java/releases/download/2.6.1/applicationinsights-agent-2.6.1.jar > /dev/null
wget -q https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-web-auto/2.6.1/applicationinsights-web-auto-2.6.1.jar > /dev/null
wget -q https://repo1.maven.org/maven2/com/microsoft/azure/applicationinsights-logging-logback/2.6.1/applicationinsights-logging-logback-2.6.1.jar > /dev/null

jar -xf applicationinsights-web-auto-2.6.1.jar > /dev/null && jar -xf applicationinsights-logging-logback-2.6.1.jar > /dev/null
jar -uf /code/otp-shaded.jar com/ > /dev/null
jar -uf /code/otp-shaded.jar ApplicationInsights.xml > /dev/null
jar -uf /code/otp-shaded.jar logback.xml > /dev/null

cd /code || exit 1
# Remove application insights temporary files
rm -rf appInsights

log_info "Start java OTP jar"

exec java -javaagent:/code/applicationinsights-agent-2.6.1.jar -DAPPINSIGHTS_INSTRUMENTATIONKEY=$applicationInsightsKey -Xms256m -Xmx6144m -jar $OTP_JAR_PATH --server --graphs /code/otpdata --router malmo
