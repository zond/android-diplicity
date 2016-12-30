#!/usr/bin/env bash

APK_PATH="app/build/outputs/apk/app-release.apk"
APK_VERSION_LINE=`/usr/local/android-sdk-linux/build-tools/25.0.2/aapt dump badging ${APK_PATH}`
APK_VERSION_CODE=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionCode='\([^']\+\)'.*/\1/"`
APK_VERSION_NAME=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionName='\([^']\+\)'.*/\1/"`

go get github.com/google/go-github/github golang.org/x/oauth2 google.golang.org/api/drive/v3
go run circle/create_release.go -apk=${APK_PATH} -build=${CIRCLE_BUILD_NUM} -tag=${APK_VERSION_CODE} -short_sha=${APK_VERSION_NAME}
