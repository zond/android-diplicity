#!/usr/bin/env bash

APK_NAME="build-${CIRCLE_BUILD_NUM}-`git log | grep '^commit' | wc -l`-`git rev-parse --short HEAD`.apk"

$HOME/.go_workspace/bin/gdrive --refresh-token ${GDRIVE_REFRESH_TOKEN} upload -p ${GDRIVE_PARENT_FOLDER} --name ${APK_NAME} app/build/outputs/apk/app-release.apk
