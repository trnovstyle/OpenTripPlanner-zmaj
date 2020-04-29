#!/bin/bash

FILE_TMP_PATH=/code/otpdata/malmo/Graph.obj

echo "Download Graph.obj to $FILE_TMP_PATH" > /var/log/otp.log
az storage blob download \
    --account-name tjpsageo \
    --container-name otp \
    --name Graph.obj \
    --file $FILE_TMP_PATH \
    --auth-mode key --account-key $TJPSAGEO_AZURE_BLOB_STORAGE_KEY

echo "$FILE_TMP_PATH downloaded " >> /var/log/otp.log

if [ -s $FILE_TMP_PATH ] ;
then
  echo "Downloaded $FILE_TMP_PATH"
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  exit 1
fi

echo "Start java OTP jar" >> /var/log/otp.log

exec "$@"
