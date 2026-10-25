#!/usr/bin/env bash
# Usage: run-logged.sh "<title>" <command...>
# Runs the command, prints its output as usual, and if it fails also publishes the key error
# lines as a GitHub annotation (visible through the checks API and on the run page).
title="$1"
shift
log="$(mktemp)"

"$@" >"$log" 2>&1
code=$?
cat "$log"

if [ "$code" -ne 0 ]; then
  {
    grep -E -A6 'error:|What went wrong|Could not|Execution failed|FAILED|Exception' "$log" | head -60
    echo '---- tail ----'
    tail -n 30 "$log"
  } | head -c 7000 | python3 -c '
import sys
s = sys.stdin.read()
s = s.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")
print("::error title=%s::%s" % (sys.argv[1], s))
' "$title"
fi

exit "$code"
