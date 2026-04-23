#!/bin/sh
set -e

java -jar /app/rest-api.jar &
REST_PID=$!

java -jar /app/gateway.jar &
GW_PID=$!

trap 'kill $REST_PID $GW_PID 2>/dev/null; wait' TERM INT

wait -n $REST_PID $GW_PID
EXIT_CODE=$?

kill $REST_PID $GW_PID 2>/dev/null
wait
exit $EXIT_CODE
