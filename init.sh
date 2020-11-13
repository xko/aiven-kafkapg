#! /usr/bin/env bash
set -e
. src/main/bash/functions.sh

echo
echo Welcome!
echo We will init/update aiven cloud infrastructure needed to run this demo and generate the required local files.
echo

. init-aiven.sh
. init-kafka.sh

echo
echo ".. all done"
echo "Cheers :)"