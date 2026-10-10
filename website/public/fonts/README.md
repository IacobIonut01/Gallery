# fonts/

The site uses **Google Sans**, the same family the app bundles in
`app/core/ui` compose resources (`font/google_sans_*.ttf`).

The woff2 files here were converted from those TTFs with fontTools:

```sh
python3 - <<'EOF'
from fontTools.ttLib import TTFont
import glob, os
for f in glob.glob("app/core/ui/src/commonMain/composeResources/font/*.ttf"):
    font = TTFont(f); font.flavor = "woff2"
    font.save(f"website/public/fonts/{os.path.basename(f).replace('.ttf','.woff2')}")
EOF
```

They are checked in so clean checkouts and CI deploys render Google Sans
instead of falling back to the system stack in `--font-sans`.
