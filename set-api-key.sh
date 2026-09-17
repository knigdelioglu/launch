#!/bin/sh
set -e

printf "Gemini API anahtarınızı girin: "
read -r API_KEY

API_KEY=$(echo "$API_KEY" | tr -d '[:space:]')

if [ -z "$API_KEY" ]; then
    echo "Hata: API anahtarı boş bırakılamaz."
    exit 1
fi

LOCAL_PROPS="local.properties"

if [ -f "$LOCAL_PROPS" ]; then
    if grep -q "^GEMINI_API_KEY=" "$LOCAL_PROPS"; then
        python3 -c "
import re
with open('$LOCAL_PROPS', 'r') as f:
    content = f.read()
new_content = re.sub(r'^GEMINI_API_KEY=.*$', 'GEMINI_API_KEY=$API_KEY', content, flags=re.MULTILINE)
with open('$LOCAL_PROPS', 'w') as f:
    f.write(new_content)
"
    else
        printf "\nGEMINI_API_KEY=%s\n" "$API_KEY" >> "$LOCAL_PROPS"
    fi
else
    printf "GEMINI_API_KEY=%s\n" "$API_KEY" > "$LOCAL_PROPS"
fi

echo "Başarılı: Gemini API anahtarı local.properties dosyasına kaydedildi."
