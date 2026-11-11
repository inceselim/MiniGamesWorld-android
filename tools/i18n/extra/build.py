"""Ek dillerin cevirilerini res/values-<q>/strings*.xml dosyalarina yazar.
<dil>/NN.json (anahtar -> ceviri) okunur; eksik anahtarlar yazilmaz (Android Ingilizceye duser).
Yer tutucular (%1$s, %d, \\n) kaynakla karsilastirilir, uyusmayan ceviri atlanip raporlanir.
Kullanim: python3 build.py [dil ...]"""
import os, re, sys, json, glob
from xml.sax.saxutils import escape

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "../../../app/src/main/res")
# dil klasoru -> Android kaynak niteleyicisi
LANGS = {
    "el": "el", "pt": "pt", "pl": "pl", "nl": "nl",
    "sv": "sv", "nb": "nb", "fi": "fi",
    "cs": "cs", "ro": "ro", "uk": "uk", "bg": "bg",
    "hr": "hr", "sk": "sk", "sl": "sl", "sr": "sr",
}


def esc(v):
    x = escape(v)
    x = re.sub(r"(?<!\\)'", r"\\'", x)
    x = re.sub(r'(?<!\\)"', r'\\"', x)
    if x.startswith("@") or x.startswith("?"):
        x = "\\" + x
    return x


def marks(s):
    return sorted(re.findall(r"%\d+\$[sdf]|%[sdf]|\\n", s)), s.count("%%")


def load_dir(d):
    out = {}
    for f in sorted(glob.glob(os.path.join(d, "*.json"))):
        with open(f, encoding="utf-8") as fh:
            out.update(json.load(fh))
    return out


def main(langs):
    src = load_dir(os.path.join(HERE, "src"))
    with open(os.path.join(HERE, "index.json"), encoding="utf-8") as fh:
        index = json.load(fh)
    files = sorted(set(index.values()))
    for lang in langs:
        tr = load_dir(os.path.join(HERE, lang))
        if not tr:
            continue
        bad, done = [], 0
        per_file = {f: [] for f in files}
        for k, en in src.items():
            v = tr.get(k)
            if v is None:
                continue
            if marks(v) != marks(en):
                bad.append(k)
                continue
            fmt = ' formatted="false"' if ("%" in en and not re.search(r"%\d+\$[sdf]|%[sdf]", en)) else ""
            per_file[index[k]].append(f'    <string name="{k}"{fmt}>{esc(v)}</string>')
            done += 1
        q = LANGS[lang]
        for f, lines in per_file.items():
            if not lines:
                continue
            p = os.path.join(RES, f"values-{q}", f)
            os.makedirs(os.path.dirname(p), exist_ok=True)
            with open(p, "w", encoding="utf-8") as fh:
                fh.write('<?xml version="1.0" encoding="utf-8"?>\n<resources>\n' + "\n".join(lines) + "\n</resources>\n")
        print(f"{lang}: {done} / {len(src)}" + (f"  YER TUTUCU HATASI: {', '.join(bad)}" if bad else ""))


if __name__ == "__main__":
    main(sys.argv[1:] or list(LANGS))
