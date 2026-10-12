#!/bin/sh
# Builds the mod and runs the GameTests. Output: build/libs/explosivearrows-<version>.jar
# Paths are relative to this file, so it works from any location.
cd "$(dirname "$0")" || exit 1
. ./scripts/find-java.sh || exit 1
if sh ./gradlew build; then
	echo
	echo "Done. Mod file:"
	ls build/libs/*.jar | grep -v sources
else
	echo
	echo "BUILD FAILED"
	exit 1
fi
