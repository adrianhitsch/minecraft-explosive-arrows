#!/bin/sh
# Starts Minecraft with the mod (development client, world is stored under run/)
cd "$(dirname "$0")" || exit 1
. ./scripts/find-java.sh || exit 1
exec sh ./gradlew runClient
