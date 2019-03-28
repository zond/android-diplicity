#!/usr/bin/env bash

echo 'Will create release...'
pwd

APK_PATH="app/build/outputs/apk/release/app-release.apk"
APK_VERSION_LINE=`/opt/android/sdk/build-tools/27.0.3/aapt dump badging ${APK_PATH}`
APK_VERSION_CODE=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionCode='\([^']\+\)'.*/\1/"`
APK_VERSION_NAME=`echo ${APK_VERSION_LINE} | sed -e "s/.*versionName='\([^']\+\)'.*/\1/"`

echo ${APK_VERSION_LINE}
echo ${APK_VERSION_CODE}
echo ${APK_VERSION_NAME}

sudo apt-get install golang-1.9
export GOPATH="${HOME}/go"
/usr/lib/go-1.8/bin/go get github.com/google/go-github/github golang.org/x/oauth2 google.golang.org/api/drive/v3
/usr/lib/go-1.8/bin/go run circle/create_release.go -apk=${APK_PATH} -build=${CIRCLE_BUILD_NUM} -tag=${APK_VERSION_CODE} -short_sha=${APK_VERSION_NAME}
