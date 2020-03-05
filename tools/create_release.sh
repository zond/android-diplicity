#!/bin/bash

echo 'Will create release...'
pwd

APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAPT_PATH="$(ls -1 $ANDROID_HOME/build-tools/*/aapt|tail -n 1)"
echo ${AAPT_PATH}
APK_VERSION_LINE=$(${AAPT_PATH} dump badging ${APK_PATH})
APK_VERSION_CODE=$(echo ${APK_VERSION_LINE} | sed -e "s/.*versionCode='\([^']\+\)'.*/\1/")
APK_VERSION_NAME=$(echo ${APK_VERSION_LINE} | sed -e "s/.*versionName='\([^']\+\)'.*/\1/")

echo ${APK_VERSION_LINE}
echo ${APK_VERSION_CODE}
echo ${APK_VERSION_NAME}

go version
go run tools/create_release.go -apk=${APK_PATH} -build=${CIRCLE_BUILD_NUM} -tag=${APK_VERSION_CODE} -short_sha=${APK_VERSION_NAME}
