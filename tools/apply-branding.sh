#!/usr/bin/env bash
# يحوّل branding/logo.png إلى أيقونة التطبيق وشعار الواجهة. إن لم يوجد الملف يبقى الشعار المبدئي.
set -euo pipefail
SRC="branding/logo.png"
if [ ! -f "$SRC" ]; then
  echo "branding/logo.png not found — keeping the default logo"
  exit 0
fi
IM="$(command -v magick || command -v convert || true)"
if [ -z "$IM" ]; then echo "ImageMagick not found"; exit 1; fi
RES="app/src/main/res"
mkdir -p "$RES/drawable-nodpi"
# أيقونة Adaptive (طبقة أمامية 432x432 والشعار داخل المنطقة الآمنة)
"$IM" "$SRC" -trim +repage -resize 250x250 -background none -gravity center -extent 432x432 "$RES/drawable-nodpi/ic_launcher_foreground.png"
rm -f "$RES/drawable/ic_launcher_foreground.xml"
# شعار داخل التطبيق
"$IM" "$SRC" -trim +repage -resize 512x512 "$RES/drawable-nodpi/bw_logo.png"
rm -f "$RES/drawable/bw_logo.xml"
echo "Branding applied from $SRC"
