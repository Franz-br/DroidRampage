#!/bin/sh
# ─────────────────────────────────────────────────────────────────────────────
#  Droid Rampage – Starter for macOS and Linux
#
#  1. Uses Java 17 or newer if it is already installed.
#  2. Otherwise downloads a private Java runtime (Eclipse Temurin, ~50 MB) into
#     the "java-runtime" folder next to this script. Nothing gets installed on
#     the system; delete the folder to remove it again.
# ─────────────────────────────────────────────────────────────────────────────

cd "$(dirname "$0")" || exit 1

JAR="DroidRampage.jar"
RUNTIME_DIR="java-runtime"
MIN_VERSION=17
JAVA_FEATURE=21

fail() {
    echo ""
    echo "ERROR: $1"
    echo "You can also install Java $MIN_VERSION+ yourself (https://adoptium.net) and run:  java -jar $JAR"
    printf "Press Enter to close ... "
    read -r _
    exit 1
}

# Prints the major version of the given java binary (e.g. 21), or nothing.
java_major() {
    "$1" -version 2>&1 | awk -F'"' '/version/ { split($2, v, "."); if (v[1] == "1") print v[2] + 0; else print v[1] + 0; exit }'
}

usable() {
    [ -n "$1" ] && [ -x "$1" ] || return 1
    major=$(java_major "$1")
    [ -n "$major" ] && [ "$major" -ge "$MIN_VERSION" ]
}

find_local_runtime() {
    [ -d "$RUNTIME_DIR" ] && find "$RUNTIME_DIR" -type f -path "*/bin/java" 2>/dev/null | head -n 1
}

[ -f "$JAR" ] || fail "$JAR not found. Keep this script in the same folder as $JAR."

OS=$(uname -s)
JAVA=""

# 1) Previously downloaded runtime
candidate=$(find_local_runtime)
usable "$candidate" && JAVA="$candidate"

# 2) Installed Java
if [ -z "$JAVA" ] && [ "$OS" = "Darwin" ]; then
    # /usr/bin/java on macOS is only a stub that opens an "install Java" dialog – ask java_home instead
    home=$(/usr/libexec/java_home -v "$MIN_VERSION+" 2>/dev/null)
    [ -n "$home" ] && usable "$home/bin/java" && JAVA="$home/bin/java"
fi
if [ -z "$JAVA" ] && [ -n "$JAVA_HOME" ] && usable "$JAVA_HOME/bin/java"; then
    JAVA="$JAVA_HOME/bin/java"
fi
if [ -z "$JAVA" ] && [ "$OS" != "Darwin" ] && command -v java >/dev/null 2>&1; then
    candidate=$(command -v java)
    usable "$candidate" && JAVA="$candidate"
fi

# 3) Download a private runtime
if [ -z "$JAVA" ]; then
    case "$OS" in
        Darwin) os=mac ;;
        Linux)  os=linux ;;
        *)      fail "Unsupported system: $OS" ;;
    esac
    case "$(uname -m)" in
        x86_64|amd64)  arch=x64 ;;
        arm64|aarch64) arch=aarch64 ;;
        *)             fail "Unsupported processor: $(uname -m)" ;;
    esac

    url="https://api.adoptium.net/v3/binary/latest/$JAVA_FEATURE/ga/$os/$arch/jre/hotspot/normal/eclipse"
    archive="java-runtime-download.tar.gz"
    echo "Java $MIN_VERSION+ was not found."
    echo "Downloading a private Java runtime (about 50 MB, only needed once) ..."
    if command -v curl >/dev/null 2>&1; then
        curl -fL --progress-bar -o "$archive" "$url" || { rm -f "$archive"; fail "Download failed. Check your internet connection."; }
    elif command -v wget >/dev/null 2>&1; then
        wget -q --show-progress -O "$archive" "$url" || { rm -f "$archive"; fail "Download failed. Check your internet connection."; }
    else
        fail "Neither curl nor wget is available to download Java."
    fi

    rm -rf "$RUNTIME_DIR"
    mkdir -p "$RUNTIME_DIR"
    tar -xzf "$archive" -C "$RUNTIME_DIR" || { rm -f "$archive"; fail "Could not unpack the Java runtime."; }
    rm -f "$archive"

    candidate=$(find_local_runtime)
    usable "$candidate" || fail "The downloaded Java runtime does not work."
    JAVA="$candidate"
    echo "Java is ready."
fi

echo "Starting Droid Rampage with $("$JAVA" -version 2>&1 | head -n 1) ..."
exec "$JAVA" -jar "$JAR" "$@"
