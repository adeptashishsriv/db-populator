#!/usr/bin/env bash
# ==========================================================
#  DB Explorer Distribution Builder - Bundled JVM
#  Uses jlink to create a trimmed JRE (~50MB vs 300MB full JDK)
#  JVM: Eclipse Temurin / OpenJDK
#       GPL v2 + Classpath Exception - free to redistribute
#       https://adoptium.net/
# ==========================================================
set -e

# Read version from pom.xml automatically
APP_VERSION=$(grep -m1 '<version>[0-9]' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')
JAR_NAME="db-explorer-${APP_VERSION}.jar"
DIST_DIR="dist-with-jvm"
JRE_DIR="${DIST_DIR}/jre"

echo "=========================================="
echo "  DB Explorer Distribution Builder"
echo "  (Bundled JVM - users need no Java)"
echo "=========================================="
echo ""

for cmd in mvn jlink jdeps; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "Error: '$cmd' not found in PATH."
        [ "$cmd" != "mvn" ] && echo "Ensure JDK 17+ is installed: https://adoptium.net/"
        exit 1
    fi
done

echo "[1/5] Building project..."
mvn clean package -DskipTests

JAR_PATH="target/${JAR_NAME}"
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR not found at ${JAR_PATH}"
    exit 1
fi

echo ""
echo "[2/5] Detecting required JDK modules with jdeps..."
MODULES=$(jdeps \
    --ignore-missing-deps \
    --print-module-deps \
    --multi-release 17 \
    "$JAR_PATH" 2>/dev/null || true)

# Always include essential modules for Swing + JDBC
EXTRA="java.desktop,java.naming,java.security.jgss,jdk.crypto.ec"
MODULES="${MODULES:+${MODULES},}${EXTRA}"

# Deduplicate
MODULES=$(echo "$MODULES" | tr ',' '\n' | sort -u | paste -sd ',' -)
echo "Modules: ${MODULES}"

echo ""
echo "[3/5] Creating trimmed JRE with jlink..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

jlink \
    --add-modules "$MODULES" \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2 \
    --output "$JRE_DIR"

echo ""
echo "[4/5] Copying application files..."
cp "$JAR_PATH" "$DIST_DIR/"
[ -f USER_HANDBOOK.md ]      && cp USER_HANDBOOK.md      "$DIST_DIR/"
[ -f USER_HANDBOOK.pdf ]     && cp USER_HANDBOOK.pdf     "$DIST_DIR/"
[ -f RELEASE_NOTES.md ]      && cp RELEASE_NOTES.md      "$DIST_DIR/"
[ -f LICENSE_AGREEMENT.txt ] && cp LICENSE_AGREEMENT.txt "$DIST_DIR/"

JVM_LICENSE=$(find "${JRE_DIR}/legal" -name "LICENSE" 2>/dev/null | head -1)
[ -n "$JVM_LICENSE" ] && cp "$JVM_LICENSE" "${DIST_DIR}/JVM_LICENSE.txt"

echo ""
echo "[5/5] Creating launcher..."
cat > "${DIST_DIR}/db-explorer.sh" <<EOF
#!/usr/bin/env bash
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
"\${SCRIPT_DIR}/jre/bin/java" -jar "\${SCRIPT_DIR}/${JAR_NAME}" "\$@"
EOF
chmod +x "${DIST_DIR}/db-explorer.sh"

# macOS .app bundle
if [[ "$(uname)" == "Darwin" ]]; then
    APP_BUNDLE="${DIST_DIR}/DB Explorer.app"
    mkdir -p "${APP_BUNDLE}/Contents/MacOS"
    cat > "${APP_BUNDLE}/Contents/MacOS/db-explorer" <<APPEOF
#!/usr/bin/env bash
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
"\${SCRIPT_DIR}/../../jre/bin/java" -jar "\${SCRIPT_DIR}/../../${JAR_NAME}" "\$@"
APPEOF
    chmod +x "${APP_BUNDLE}/Contents/MacOS/db-explorer"
    cat > "${APP_BUNDLE}/Contents/Info.plist" <<PLISTEOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key><string>DB Explorer</string>
    <key>CFBundleExecutable</key><string>db-explorer</string>
    <key>CFBundleVersion</key><string>${APP_VERSION}</string>
    <key>CFBundlePackageType</key><string>APPL</string>
</dict>
</plist>
PLISTEOF
    echo "macOS .app bundle created."
fi

DIST_SIZE=$(du -sh "$DIST_DIR" 2>/dev/null | cut -f1)
echo ""
echo "=========================================="
echo "  Done! Output: ${DIST_DIR}/"
echo "  Size: ${DIST_SIZE}"
echo "=========================================="
echo ""
echo "Users do NOT need Java installed - JRE is bundled."
echo "To distribute: tar/zip the entire '${DIST_DIR}' folder."
echo ""
