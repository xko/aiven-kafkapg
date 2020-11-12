#! /usr/bin/env bash

FULL_TOKEN=$(avn user access-token create --description automation --json | \
  python -c "import sys, json; print(json.load(sys.stdin)[0]['full_token'])")
PROJECT=$(avn project details --json | \
  python -c "import sys, json; print(json.load(sys.stdin)[0]['project_name'])")

echo -n "Will set up for aiven project $PROJECT (Enter or Ctrl-C):"
read
echo aiven_api_token = "\"$FULL_TOKEN\"" > terraform.tfvars
echo aiven_project_name = "\"$PROJECT\"" >> terraform.tfvars
echo ..written terraform.tfvars
echo

echo -n "Will run terraform apply (Enter or Ctrl-C):"
read
terraform import aiven_project.xko-aiven-kafkapg $PROJECT
terraform apply
echo ..terraform done
echo

echo -n "Will delete certificate files before generating new (Enter or Ctrl-C):"
read
rm ca.pem client.keystore.p12 client.truststore.jks service.cert service.key
echo -n "New password for Java Certificates (6+ chars): "
read CERT_PWD
avn service user-kafka-java-creds --username avnadmin kafka -p $CERT_PWD
echo ..certificates done
echo -n "Will delete certificate intermediate files - ca.pem service.cert service.key (Enter or Ctrl-C):"
read
rm ca.pem service.cert service.key
echo
echo "..all done!"
echo "Cheers :)"
