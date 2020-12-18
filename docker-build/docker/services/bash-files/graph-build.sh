#!/bin/bash

. logging.sh

GRAPH_CONTAINER="resesok-graph"
NETEX_CONTAINER="resesok-netex"
NETEX_FILE_NAME="ST_netex.zip"
OSM_FILE_NAME="sweden-filtered.osm.pbf"

function downloadNetexFiles {
  file_path=$1
  log_info "Downloading netex files"
  downloadFromAzureStorage $SA_NAME $NETEX_CONTAINER $NETEX_FILE_NAME $file_path/$NETEX_FILE_NAME
}

function downloadOSMFile {
  file_path=$1
  log_info "Downloading OSM file"
  downloadFromAzureStorage $SA_NAME $GRAPH_CONTAINER $OSM_FILE_NAME $file_path/$OSM_FILE_NAME
}

function buildGraphFromNetexData {
  otp_jar_path=$1
  file_path=$2
  graph_path=$3
  log_info "Building Graph"
  if ! java -Xmx6G -jar $otp_jar_path --basePath $file_path --build $graph_path; then
    log_error "Build OpenTripPlanner Graph database failed in bash $0"
    return 1
  fi

}
