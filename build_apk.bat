@echo off
echo ===========================================
echo Membangun APK Spotify Launcher...
echo ===========================================
call gradlew.bat assembleDebug
echo ===========================================
echo Selesai! File APK ada di:
echo app\build\outputs\apk\debug\app-debug.apk
echo ===========================================
pause
