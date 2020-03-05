#!/bin/bash

if [ "${KEYSTORE_URL}" != "" ]; then
    echo "KEYSTORE_URL found - downloading..."
    # we're using curl instead of wget because it will not
    # expose the sensitive uri in the build logs:
    curl -L -o "${GITHUB_WORKSPACE}/app/signing.keystore" "${KEYSTORE_URL}"
else
    echo "KEYSTORE_URL not set. APK artifact will not be signed."
fi
