#!/usr/bin/env bash
# ==========================================================
#  DB Explorer Distribution Builder - No bundled JVM
#  Requires users to have Java 17+ installed
# ==========================================================
set -e

# Read version from pom.xml automatically
APP_VERSION=$(grep -m1 '<version>[0-9]' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')
JAR_NAME="db-explorer-${APP_VERSION}.jar"
DIST_DIR="dist-no-jvm"

echo "=========================================="
echo "  DB Explorer Distribution Builder"
echo "  (No bundled JVM - requires Java 17+)"
echo "=========================================="
echo ""

if ! command -v mvn &>/dev/null; then
    echo "Error: Maven not found in PATH."
    exit 1
fi

echo "[1/3] Building project..."
mvn clean package -DskipTests

JAR_PATH="target/${JAR_NAME}"
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR not found at ${JAR_PATH}"
    exit 1
fi

echo ""
echo "[2/3] Creating distribution directory..."
rm -rf "$DIST_DIR"
mkdir -p "$DIST_DIR"

echo ""
echo "[3/3] Copying files..."
cp "$JAR_PATH" "$DIST_DIR/"
[ -f USER_HANDBOOK.md ]      && cp USER_HANDBOOK.md      "$DIST_DIR/"
[ -f USER_HANDBOOK.pdf ]     && cp USER_HANDBOOK.pdf     "$DIST_DIR/"
[ -f RELEASE_NOTES.md ]      && cp RELEASE_NOTES.md      "$DIST_DIR/"
[ -f LICENSE_AGREEMENT.txt ] && cp LICENSE_AGREEMENT.txt "$DIST_DIR/"

# Launcher using system Java
cat > "${DIST_DIR}/db-explorer.sh" <<EOF
#!/usr/bin/env bash
SCRIPT_DIR="\$(cd "\$(dirname "\${BASH_SOURCE[0]}")" && pwd)"
java -jar "\${SCRIPT_DIR}/${JAR_NAME}" "\$@"
EOF
chmod +x "${DIST_DIR}/db-explorer.sh"

DIST_SIZE=$(du -sh "$DIST_DIR" 2>/dev/null | cut -f1)
echo ""
echo "=========================================="
echo "  Done! Output: ${DIST_DIR}/"
echo "  Size: ${DIST_SIZE}"
echo "=========================================="
echo ""
echo "NOTE: Users must have Java 17+ installed to run this package."
echo ""
