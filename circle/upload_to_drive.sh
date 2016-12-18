#!/usr/bin/env bash

APK_PATH="app/build/outputs/apk/app-release.apk"
APK_VERSION_LINE=`/usr/local/android-sdk-linux/build-tools/25.1.0/aapt dump badging ${APK_PATH}`
APK_VERSION_CODE=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionCode='\([^']\+\)'.*/\1/"`
APK_VERSION_NAME=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionName='\([^']\+\)'.*/\1/"`
APK_NAME="build-${CIRCLE_BUILD_NUM}-${APK_VERSION_CODE}-${APK_VERSION_NAME}"

$HOME/.go_workspace/bin/gdrive --refresh-token ${GDRIVE_REFRESH_TOKEN} upload -p ${GDRIVE_PARENT_FOLDER} --name ${APK_NAME} ${APK_PATH}
