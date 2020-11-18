#!/bin/bash

function getKeyVaultValue {
  vault=$1
  key=$2

  # Get value from KeyVault
  if ! applicationInsightsKey=$(az keyvault secret show \
    --vault-name "$vault" \
    --name "$key" \
    --query value); then

    echo "Error fetching value for KeyVault key $key in keyvault $vault"
    exit 1
  fi

  echo $applicationInsightsKey
}