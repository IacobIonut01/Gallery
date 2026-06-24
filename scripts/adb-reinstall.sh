#!/bin/bash

# Configuration
PACKAGE_NAME="com.dot.gallery.debug"
MAIN_ACTIVITY="com.dot.gallery.feature_node.presentation.main.MainActivity"

# Use system ADB or fall back
ADB=$(command -v adb || echo "/home/tazztone/Android/Sdk/platform-tools/adb")

echo "⚠️ Uninstalling existing app to resolve signature/version conflicts..."
$ADB uninstall $PACKAGE_NAME

echo "🚀 Building and installing clean debug APK..."
./gradlew :app:installUniversalWithMLDebug

if [ $? -eq 0 ]; then
    echo "✅ Install successful. Launching $PACKAGE_NAME..."
    $ADB shell am start -n $PACKAGE_NAME/$MAIN_ACTIVITY
else
    echo "❌ Build or Install failed."
    exit 1
fi
