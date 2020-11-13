function ask() {
    read -p " Continue?[Y/n]" && [[ $REPLY =~ ^[Yy]?$ ]] || exit 0
}

function pyjson() {
  python -c "import sys, json; print(json.load(sys.stdin)$1)"
}

function prereq() {
  [ -z "$PREREQ_DONE" ] || return 0
  echo Prerequisites:
  echo - python installed and available on \$PATH as \'python\'
  echo - aiven CLI installed and available on \$PATH, see https://github.com/aiven/aiven-client
  echo - user logged in, use \'avn user login\'
  echo - terraform installed and available on \$PATH, see https://learn.hashicorp.com/tutorials/terraform/install-cli
  echo
  PREREQ_DONE=true
}