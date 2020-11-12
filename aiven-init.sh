#! /usr/bin/env bash
set -e

echo
echo Welcome!
echo We will init/update aiven cloud infrastructure needed to run this demo and generate the required local files.
echo
echo Prerequisites:
echo - python installed and available on \$PATH as \'python\'
echo - aiven CLI installed and available on \$PATH, see https://github.com/aiven/aiven-client
echo - user logged in, use \'avn user login\'
echo - terraform installed and available on \$PATH, see https://learn.hashicorp.com/tutorials/terraform/install-cli
echo

function ask() {
    read -p " Continue?[Y/n]" && [[ $REPLY =~ ^[Yy]?$ ]] || exit 0
}

PROJECT=$(avn project details --json | \
        python -c "import sys, json; print(json.load(sys.stdin)[0]['project_name'])")
echo -n "Will set up for currently selected aiven project $PROJECT." ; ask
echo

if [[ ! -f "terraform.tfvars" ]]; then
  echo -n "Will now create an access token 'automation' in your aiven account to authorize terraform" ; ask
  FULL_TOKEN=$(avn user access-token create --description automation --json | \
             python -c "import sys, json; print(json.load(sys.stdin)[0]['full_token'])")
  echo aiven_api_token = "\"$FULL_TOKEN\"" > terraform.tfvars
  echo aiven_project_name = "\"$PROJECT\"" >> terraform.tfvars
  echo ..token and project are written to terraform.tfvars
  echo
fi


echo "Will now initialize terraform and apply aiven.tf"
echo -n "WARNING: Terraform will destroy all the services and resources not specified in aiven.tf." ; ask

terraform init
terraform import aiven_project.xko-aiven-kafkapg "$PROJECT" 2>/dev/null || :
terraform apply || TRERR=", there where errors"
echo -n "..terraform done" ; echo $TRERR
if [ -n "$TRERR" ] ; then
  echo "- 'Error: error waiting for Aiven Kafka topic to be ACTIVE...' might be fixed by creating the missing topic in aiven web console"
  echo "- 'Error: 409: ... Service name is already in use in this project' means inconsistent terraform state"
  echo "  might be fixed by running this script against empty aiven project or using 'terraform import' to sync the state"
  echo "- 'Error: 5XX..' might be fixed bt re-running 'terraform apply'"
  echo "- 'Error: 401: ... Missing (expired) db token' can be fixed by deleting terraform.tfvars and re-running this script"
  ask
fi
echo

echo -n "Will now generate Java client certificate files and client.properties." ; ask
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
echo "..all done!"
echo "Cheers :)"
