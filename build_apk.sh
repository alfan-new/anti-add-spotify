#!/bin/bash
echo "==========================================="
echo "Membangun APK Spotify Launcher..."
echo "==========================================="
./gradlew assembleDebug
echo "==========================================="
echo "Selesai! File APK ada di:"
echo "app/build/outputs/apk/debug/app-debug.apk"
echo "==========================================="
