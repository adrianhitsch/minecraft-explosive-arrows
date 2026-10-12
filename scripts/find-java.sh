# Sourced by build.sh/client.sh. Finds a JDK for Gradle:
# JAVA_HOME, on macOS JDK 25 via /usr/libexec/java_home, otherwise java on the PATH.
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	:
elif [ -x /usr/libexec/java_home ] && JAVA_HOME=$(/usr/libexec/java_home -v 25 2>/dev/null); then
	export JAVA_HOME
elif command -v java >/dev/null 2>&1; then
	unset JAVA_HOME
else
	echo "No Java found. Please install JDK 25, e.g. from https://adoptium.net"
	echo "(on Linux also via your package manager, on macOS e.g. with 'brew install --cask temurin')."
	return 1 2>/dev/null || exit 1
fi
