#!/bin/bash


APK_PATH="app/build/outputs/apk/release/app-release.apk"
AAPT_PATH="$(ls -1 $ANDROID_HOME/build-tools/*/aapt|tail -n 1)"
echo "Extracting release names from ${APK_PATH} using ${AAPT_PATH}."

APK_VERSION_LINE=$(${AAPT_PATH} dump badging ${APK_PATH})
APK_VERSION_CODE=$(echo ${APK_VERSION_LINE} | sed -e "s/.*versionCode='\([^']\+\)'.*/\1/")
APK_VERSION_NAME=$(echo ${APK_VERSION_LINE} | sed -e "s/.*versionName='\([^']\+\)'.*/\1/")

echo "Found version code ${APK_VERSION_CODE}."
echo "Found version name ${APK_VERSION_NAME}."

echo ${APK_VERSION_CODE} > ${GITHUB_WORKSPACE}/versionCode.txt
echo ${APK_VERSION_NAME} > ${GITHUB_WORKSPACE}/versionName.txt

