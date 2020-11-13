#! /usr/bin/env bash
set -e
. src/main/bash/functions.sh

prereq

PROJECT=$(avn project details --json | pyjson '[0]["project_name"]')
echo -n "Will run terraform for currently selected aiven project $PROJECT." ; ask
echo

if [[ ! -f "terraform.tfvars" ]]; then
  echo -n "Will now create an access token 'automation' in your aiven account to authorize terraform" ; ask
  FULL_TOKEN=$(avn user access-token create --description automation --json | \
             python -c "import sys, json; print(json.load(sys.stdin)[0]['full_token'])")
  echo "aiven_api_token=\"$FULL_TOKEN\"" > terraform.tfvars
  echo "aiven_project_name=\"$PROJECT\"" >> terraform.tfvars
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


