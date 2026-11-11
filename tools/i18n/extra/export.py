"""Ek diller icin kaynak metinleri disa aktarir.
values/strings*.xml (Ingilizce) -> src/NN.json parcalari (her biri ~120 metin).
Ceviriler <dil>/NN.json olarak ayni anahtarlarla yazilir, build.py ile res'e uretilir.
Kullanim: python3 export.py"""
import os, json, glob, xml.etree.ElementTree as ET

HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, "../../../app/src/main/res")
CHUNK = 120


def unesc(s):
    return s.replace("\\'", "'").replace('\\"', '"')


def main():
    items = []
    for path in sorted(glob.glob(os.path.join(RES, "values", "strings*.xml"))):
        fname = os.path.basename(path)
        for el in ET.parse(path).getroot():
            if el.tag != "string" or el.get("translatable") == "false":
                continue
            items.append((fname, el.get("name"), unesc(el.text or "")))
    out = os.path.join(HERE, "src")
    os.makedirs(out, exist_ok=True)
    for f in glob.glob(os.path.join(out, "*.json")):
        os.remove(f)
    index = {}
    for i in range(0, len(items), CHUNK):
        part = items[i:i + CHUNK]
        n = f"{i // CHUNK:02d}"
        with open(os.path.join(out, n + ".json"), "w", encoding="utf-8") as fh:
            json.dump({k: v for _, k, v in part}, fh, ensure_ascii=False, indent=0)
        for fname, k, _ in part:
            index[k] = fname
    with open(os.path.join(HERE, "index.json"), "w", encoding="utf-8") as fh:
        json.dump(index, fh, ensure_ascii=False, indent=0)
    print(len(items), "metin,", (len(items) + CHUNK - 1) // CHUNK, "parca")


if __name__ == "__main__":
    main()
