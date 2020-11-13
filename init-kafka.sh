#! /usr/bin/env bash
set -e
. src/main/bash/functions.sh

prereq

echo -n "Will now generate Java client certificate files for kafka and client.properties." ; ask
if [ -f client.truststore.jks ] || [ -f client.keystore.p12 ]; then
  echo -n "Either client.truststore.jks or client.keystore.p12 already exist, will overwrite." ; ask
  rm -f client.truststore.jks client.keystore.p12
fi
echo -n "Create the new password for client.truststore.jks and client.keystore.p12 (6+ chars):"
read -r CERT_PWD
avn service user-kafka-java-creds --username avnadmin kafka -p "$CERT_PWD"
sed -i '/ssl.keystore.location=/c\ssl.keystore.location=client.keystore.p12' client.properties
sed -i '/ssl.truststore.location=/c\ssl.truststore.location=client.truststore.jks' client.properties
echo "..certificates done"
echo

