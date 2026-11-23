#!/usr/bin/env python3
"""Builds app/src/main/assets/kanji.json from KANJIDIC2 (EDRDG) + JLPT N-levels.

Sources (see ATTRIBUTION.md):
  - KANJIDIC2, EDRDG, CC BY-SA 4.0: https://www.edrdg.org/kanjidic/kanjidic2.xml.gz
  - JLPT N5-N1 lists by Jonathan Waller (tanos.co.uk), taken from the `jlpt_new`
    field of https://github.com/davidluzgouveia/kanji-data (MIT). No WaniKani data is used.

Usage: python3 tools/build_kanji_data.py <kanjidic2.xml> <kanji-data.json> <out.json>
Re-run with the newest KANJIDIC2 before each release (EDRDG licence, section 4).
"""
import json
import sys
import xml.etree.ElementTree as ET

KANA = {
    "きゃ": "kya", "きゅ": "kyu", "きょ": "kyo", "しゃ": "sha", "しゅ": "shu", "しょ": "sho",
    "ちゃ": "cha", "ちゅ": "chu", "ちょ": "cho", "にゃ": "nya", "にゅ": "nyu", "にょ": "nyo",
    "ひゃ": "hya", "ひゅ": "hyu", "ひょ": "hyo", "みゃ": "mya", "みゅ": "myu", "みょ": "myo",
    "りゃ": "rya", "りゅ": "ryu", "りょ": "ryo", "ぎゃ": "gya", "ぎゅ": "gyu", "ぎょ": "gyo",
    "じゃ": "ja", "じゅ": "ju", "じょ": "jo", "びゃ": "bya", "びゅ": "byu", "びょ": "byo",
    "ぴゃ": "pya", "ぴゅ": "pyu", "ぴょ": "pyo",
    "あ": "a", "い": "i", "う": "u", "え": "e", "お": "o", "か": "ka", "き": "ki", "く": "ku",
    "け": "ke", "こ": "ko", "さ": "sa", "し": "shi", "す": "su", "せ": "se", "そ": "so",
    "た": "ta", "ち": "chi", "つ": "tsu", "て": "te", "と": "to", "な": "na", "に": "ni",
    "ぬ": "nu", "ね": "ne", "の": "no", "は": "ha", "ひ": "hi", "ふ": "fu", "へ": "he",
    "ほ": "ho", "ま": "ma", "み": "mi", "む": "mu", "め": "me", "も": "mo", "や": "ya",
    "ゆ": "yu", "よ": "yo", "ら": "ra", "り": "ri", "る": "ru", "れ": "re", "ろ": "ro",
    "わ": "wa", "ゐ": "i", "ゑ": "e", "を": "o", "ん": "n", "が": "ga", "ぎ": "gi", "ぐ": "gu",
    "げ": "ge", "ご": "go", "ざ": "za", "じ": "ji", "ず": "zu", "ぜ": "ze", "ぞ": "zo",
    "だ": "da", "ぢ": "ji", "づ": "zu", "で": "de", "ど": "do", "ば": "ba", "び": "bi",
    "ぶ": "bu", "べ": "be", "ぼ": "bo", "ぱ": "pa", "ぴ": "pi", "ぷ": "pu", "ぺ": "pe",
    "ぽ": "po", "ぁ": "a", "ぃ": "i", "ぅ": "u", "ぇ": "e", "ぉ": "o", "ゃ": "ya", "ゅ": "yu",
    "ょ": "yo", "ー": "-",
}


def kata_to_hira(s):
    return "".join(chr(ord(c) - 0x60) if "ァ" <= c <= "ヶ" else c for c in s)


def romaji(kana):
    s = kata_to_hira(kana)
    out, i = [], 0
    while i < len(s):
        if s[i] == "っ" and i + 1 < len(s):
            nxt = KANA.get(s[i + 1:i + 3]) or KANA.get(s[i + 1], "")
            out.append(nxt[:1])
            i += 1
            continue
        if s[i:i + 2] in KANA:
            out.append(KANA[s[i:i + 2]])
            i += 2
            continue
        out.append(KANA.get(s[i], s[i]))  # keeps "." and "-" markers
        i += 1
    return "".join(out)


def okurigana(r):
    """KANJIDIC marks okurigana with a dot: まな.ぶ -> まな(ぶ)."""
    if "." in r:
        stem, tail = r.split(".", 1)
        return f"{stem}({tail})"
    return r


def main(kd2_path, levels_path, out_path):
    levels = {k: v["jlpt_new"] for k, v in json.load(open(levels_path, encoding="utf-8")).items()
              if v.get("jlpt_new")}
    root = ET.parse(kd2_path).getroot()
    header = root.find("header")
    version = {
        "file_version": header.findtext("file_version"),
        "database_version": header.findtext("database_version"),
        "date_of_creation": header.findtext("date_of_creation"),
    }
    out = []
    for ch in root.iter("character"):
        lit = ch.findtext("literal")
        if lit not in levels:
            continue
        misc = ch.find("misc")
        rm = ch.find("reading_meaning")
        on, kun, meanings = [], [], []
        if rm is not None:
            for grp in rm.iter("rmgroup"):
                for r in grp.findall("reading"):
                    if r.get("r_type") == "ja_on":
                        on.append(r.text)
                    elif r.get("r_type") == "ja_kun":
                        kun.append(r.text)
                for m in grp.findall("meaning"):
                    if m.get("m_lang") is None:
                        meanings.append(m.text)
        if not meanings:
            continue
        out.append({
            "k": lit,
            "n": levels[lit],
            "s": int(misc.findtext("stroke_count") or 0),
            "f": int(misc.findtext("freq") or 9999),
            "on": on,
            "onr": [romaji(x) for x in on],
            "kun": [okurigana(x) for x in kun],
            "kunr": [okurigana(romaji(x)) for x in kun],
            "m": meanings,
        })
    # Study order: easiest level first (N5), then by frequency.
    out.sort(key=lambda e: (-e["n"], e["f"], e["s"]))
    json.dump({"source": version, "kanji": out}, open(out_path, "w", encoding="utf-8"),
              ensure_ascii=False, separators=(",", ":"))
    by_level = {n: sum(1 for e in out if e["n"] == n) for n in (5, 4, 3, 2, 1)}
    print(f"wrote {len(out)} kanji {by_level} from KANJIDIC2 {version}")


if __name__ == "__main__":
    main(*sys.argv[1:4])
