#!/bin/bash

: ${GRAPH_FILE_TARGET_PATH="/code/otpdata/norway/Graph.obj"}
: ${FILE_TMP_PATH="/tmp/graph_obj_from_gcs"}
: ${ETCD_GRAPH_URL_ENDPOINT="http://etcd-client:2379/v2/keys/prod/otp/marduk.file?quorum=false&recursive=false&sorted=false"}
# Notice ending slash here, it is correct
: ${MARDUK_GCP_BASE="gs://marduk/"}

echo "GRAPH_FILE_TARGET_PATH: $GRAPH_FILE_TARGET_PATH"

FROM_ETCD=$( curl --silent -X GET "$ETCD_GRAPH_URL_ENDPOINT" )
echo "From etcd: $FROM_ETCD"
if [[ "x" != "x$FROM_ETCD" ]] ;
then
  FILENAME=$( echo $FROM_ETCD | jq '.node.value' | tr -d '"')
fi

if [[ "x" == "x$FILENAME" ]] ;
then
  FILENAME=$(wget --quiet http://hubot/hubot/marduk/filename -O -)
  echo "Reading value from hubot instead of etcd as etcd cannot be connected."
  echo "As hubot is backed by etcd, this is likely to fail..."
  echo "This would only work if the etcd value does not exist, but that etcd is up..."
  echo "From hubot: $FILENAME"
fi

echo "Activating marduk blobstore service account"
/code/google-cloud-sdk/bin/gcloud auth activate-service-account --key-file /etc/marduk/marduk-blobstore-credentials.json

DOWNLOAD="${MARDUK_GCP_BASE}${FILENAME}"
echo "Downloading $DOWNLOAD"
/code/google-cloud-sdk/bin/gsutil cp $DOWNLOAD $FILE_TMP_PATH

# Testing exists and has a size greater than zero
if [ -s $FILE_TMP_PATH ] ;
then
  echo "Overwriting $GRAPH_FILE_TARGET_PATH"
  mv $FILE_TMP_PATH $GRAPH_FILE_TARGET_PATH
else
  echo "** WARNING: Downloaded file ($FILE_TMP_PATH) is empty or not present**"
  echo "** Not overwriting $GRAPH_FILE_TARGET_PATH**"
  wget -q --header 'Content-Type: application/json' --post-data='{"source":"otp", "message":":no_entry: Downloaded file is empty or not present. This makes OTP fail! Please check logs"}' http://hubot/hubot/say/
  echo "Now sleeping 5m in the hope that this will be manually resolved in the mean time, and then restarting."
  sleep 5m
  exit 1
fi

exec "$@"
