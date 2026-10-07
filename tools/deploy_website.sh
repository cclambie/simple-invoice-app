#!/bin/bash
# Uploads website/ to the bunny.net storage zone behind apps.craiglambie.com and purges the CDN.
# Needs the bunny.net account API key in the keyring: secret-tool store --label=Bunny.net service bunny.net_api
set -euo pipefail
STORAGE_ZONE_ID=1973931
STORAGE_ZONE=appscraiglambie
STORAGE_HOST=syd.storage.bunnycdn.com
PULL_ZONE_ID=6755349

cd "$(dirname "$0")/../website"
KEY=$(secret-tool lookup service bunny.net_api)
PASSWORD=$(curl -sf -H "AccessKey: $KEY" "https://api.bunny.net/storagezone/$STORAGE_ZONE_ID" |
  python3 -I -c "import json,sys; print(json.load(sys.stdin)['Password'])")

failed=0
while IFS= read -r f; do
  f=${f#./}
  case "$f" in
    *.html) ct="text/html; charset=utf-8" ;; *.css) ct="text/css" ;; *.js) ct="application/javascript" ;;
    *.png) ct="image/png" ;; *.webp) ct="image/webp" ;; *.xml) ct="application/xml" ;; *.txt) ct="text/plain" ;;
    *) ct="application/octet-stream" ;;
  esac
  code=$(curl -s -o /dev/null -w "%{http_code}" -X PUT -H "AccessKey: $PASSWORD" -H "Content-Type: $ct" \
    --data-binary @"$f" "https://$STORAGE_HOST/$STORAGE_ZONE/$f")
  if [ "$code" = 201 ]; then echo "uploaded $f"; else echo "FAILED ($code) $f"; failed=1; fi
done < <(find . -type f | sort)

curl -sf -o /dev/null -X POST -H "AccessKey: $KEY" "https://api.bunny.net/pullzone/$PULL_ZONE_ID/purgeCache"
echo "CDN cache purged"
exit $failed
