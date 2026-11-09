"""Ada Patronu metinleri: s*.py (EN/TR kaynak) + t_*.py (diger 8 dil) -> res/values*/strings_island.xml
Kullanim: python3 build.py   (eksik ceviriler raporlanir; eksik dilde Ingilizce kullanilir)"""
import os, re, glob, importlib.util, xml.dom.minidom
from xml.sax.saxutils import escape
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "../../../app/src/main/res")
LANGS = ["de", "es", "fr", "it", "ja", "ru", "da", "hu"]

def load(pattern):
    out = {}
    for f in sorted(glob.glob(os.path.join(HERE, pattern))):
        spec = importlib.util.spec_from_file_location("m", f); m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
        out.update(m.S)
    return out

def esc(v):
    x = escape(v); x = re.sub(r"(?<!\\)'", r"\\'", x); return re.sub(r'(?<!\\)"', r'\\"', x)
def ph(s): return sorted(re.findall(r"%\d+\$[sd]", s))

src = load("s*.py")
tr = load("t_*.py")

def write(d, texts, root_attr=""):
    lines = []
    for k, v in texts.items():
        en = src[k][0]
        fmt = ' formatted="false"' if ('%' in v + en and not ph(en)) else ''
        lines.append(f'    <string name="{k}"{fmt}>{esc(v)}</string>')
    p = os.path.join(RES, d, "strings_island.xml")
    os.makedirs(os.path.dirname(p), exist_ok=True)
    open(p, "w", encoding="utf-8").write(f'<?xml version="1.0" encoding="utf-8"?>\n<resources{root_attr}>\n' + "\n".join(lines) + "\n</resources>\n")
    xml.dom.minidom.parse(p)

for k, v in src.items(): assert ph(v[0]) == ph(v[1]), k
write("values", {k: v[0] for k, v in src.items()}, ' xmlns:tools="http://schemas.android.com/tools" tools:ignore="MissingTranslation"')
write("values-tr", {k: v[1] for k, v in src.items()})
for i, lang in enumerate(LANGS):
    texts = {}
    for k in src:
        if k in tr:
            v = tr[k][i]
            assert ph(v) == ph(src[k][0]), (lang, k, v)
            texts[k] = v
    if texts: write(f"values-{lang}", texts)
    print(lang, len(texts), "/", len(src))
extra = [k for k in tr if k not in src]
if extra: print("FAZLA:", extra)
