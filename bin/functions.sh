function ask() {
    read -p "Continue?[Y/n]" && [[ $REPLY =~ ^[Yy]?$ ]] || exit 0
}

function pyjson() {
  python -c "import sys, json; print(json.load(sys.stdin)$1)"
}

function prerequisites() {
  [ -z "$PREREQ_DONE" ] || return 0
  echo - python installed and available on \$PATH as \'python\'
  echo - aiven CLI installed and available on \$PATH, see https://github.com/aiven/aiven-client
  echo - user logged in, use \'avn user login\'
  PREREQ_DONE=true
}