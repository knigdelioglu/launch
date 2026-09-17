#!/bin/sh
set -e

printf "API-Football (api-sports.io) anahtarınızı girin: "
read -r API_KEY

API_KEY=$(echo "$API_KEY" | tr -d '[:space:]')

if [ -z "$API_KEY" ]; then
    echo "Hata: API anahtarı boş bırakılamaz."
    exit 1
fi

LOCAL_PROPS="local.properties"

if [ -f "$LOCAL_PROPS" ]; then
    if grep -q "^FOOTBALL_API_KEY=" "$LOCAL_PROPS"; then
        python3 -c "
import re
with open('$LOCAL_PROPS', 'r') as f:
    content = f.read()
new_content = re.sub(r'^FOOTBALL_API_KEY=.*$', 'FOOTBALL_API_KEY=$API_KEY', content, flags=re.MULTILINE)
with open('$LOCAL_PROPS', 'w') as f:
    f.write(new_content)
"
    else
        printf "\nFOOTBALL_API_KEY=%s\n" "$API_KEY" >> "$LOCAL_PROPS"
    fi
else
    printf "FOOTBALL_API_KEY=%s\n" "$API_KEY" > "$LOCAL_PROPS"
fi

echo "Başarılı: API-Football anahtarı local.properties dosyasına kaydedildi."
