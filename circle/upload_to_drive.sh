#!/usr/bin/env 

APK_NAME="build-${CIRCLE_BUILD_NUM}-`git rev-parse --short HEAD`.apk"

gdrive --refresh-token ${GDRIVE_REFRESH_TOKEN} upload -p ${GDRIVE_PARENT_FOLDER} app/build/outputs/apk/app-release.apk
