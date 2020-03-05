#!/bin/bash

if [ "${KEYSTORE_URI}" != "" ]; then
    echo "Keystore URI found - downloading..."
    # we're using curl instead of wget because it will not
    # expose the sensitive uri in the build logs:
    curl -L -o "${GITHUB_WORKSPACE}/android-diplicity/signing.keystore" "${KEYSTORE_URI}"
else
    echo "Keystore uri not set. APK artifact will not be signed."
fi
