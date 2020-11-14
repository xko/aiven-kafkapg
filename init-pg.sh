#! /usr/bin/env bash
set -e
. src/main/bash/functions.sh

prereq

OUTFILE=pg.client.properties

echo -n "Will now download ca.pem from aiven " ; ask
avn service user-creds-download --username avnadmin pg-sink  || :
echo ".. ca.pem done. 'ERROR... user... does not have certificate and key' can be ignored"

echo
echo -n "Will generate $OUTFILE" ; ask
PG_SERVICE=$(avn service get pg-sink --json)

host=$(echo "$PG_SERVICE" | pyjson '["service_uri_params"]["host"]')
password=$(echo "$PG_SERVICE" | pyjson '["service_uri_params"]["password"]')
port=$(echo "$PG_SERVICE" | pyjson '["service_uri_params"]["port"]')
user=$(echo "$PG_SERVICE" | pyjson '["service_uri_params"]["user"]')
url="jdbc:postgresql://${host}:${port}/os-metrics-sink?ssl=true&sslrootcert=ca.pem&sslmode=verify-ca"


echo -n "Will use the url $url" ; ask
echo "url=$url" > $OUTFILE
echo "user=$user" >> $OUTFILE
echo "password=$password" >> $OUTFILE
echo "driver=org.postgresql.Driver" >> $OUTFILE
echo "..postgres properties done"
echo
