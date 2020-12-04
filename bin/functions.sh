function ask() {
    read -p "Continue?[Y/n]" && [[ $REPLY =~ ^[Yy]?$ ]] || exit 0
}

function pyjson() {
  python -c "import sys, json; print(json.load(sys.stdin)$1)"
}

function prerequisites() {
  local r=0
  if ! which python >/dev/null 2>&1; then
    echo "No python - need either 2.x or 3.x available on \$PATH as 'python'"
    r=1
  fi
  if [ -n "$TFVARS" ] && ! which terraform >/dev/null 2>&1; then
    echo "No terraform on \$PATH, see https://learn.hashicorp.com/tutorials/terraform/install-cli"
    r=1
  fi
  if ! which avn >/dev/null 2>&1; then
    echo "No aiven CLI on \$PATH, see https://github.com/aiven/aiven-client"
    r=1
  fi
  return $r
}

function login() {
   if ! avn user info >/dev/null; then
    avn user login || return 1
    echo
  fi
}