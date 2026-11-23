#!/usr/bin/env python3
"""Builds the Google Play listing images in store/en-US/ from the Robolectric renders in
app/build/store/raw (run: ./gradlew testDebugUnitTest --tests '*StoreAssetsTest').
Fonts: Inter (system, /usr/share/fonts/opentype/inter) for captions, the app's Noto Sans JP for kanji."""
import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

RAW = "app/build/store/raw"
OUT = "store/en-US"
INTER = "/usr/share/fonts/opentype/inter/Inter-{}.otf"
KANJI = "app/src/main/res/font/noto_sans_jp_bold.otf"
PAPER, INK, MUTED, RED, NIGHT, CREAM = "#F7F3EA", "#1C1B19", "#5E5850", "#C8402F", "#141519", "#F4F1EA"


def font(weight, size):
    return ImageFont.truetype(INTER.format(weight), size)


def rounded(im, r):
    mask = Image.new("L", im.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width, im.height), r, fill=255)
    out = Image.new("RGBA", im.size)
    out.paste(im.convert("RGBA"), (0, 0), mask)
    return out


def caption(draw, text, y, color, width=1080, size=66):
    f = font("Bold", size)
    words, lines, line = text.split(), [], ""
    for w in words:
        t = (line + " " + w).strip()
        if draw.textlength(t, font=f) > width - 160 and line:
            lines.append(line)
            line = w
        else:
            line = t
    lines.append(line)
    for i, l in enumerate(lines):
        draw.text((width / 2, y + i * (size + 14)), l, font=f, fill=color, anchor="mt")


def framed(screen, title, bg=PAPER, fg=INK):
    canvas = Image.new("RGB", (1080, 1920), bg)
    d = ImageDraw.Draw(canvas)
    caption(d, title, 110, fg)
    shot = Image.open(screen).convert("RGB").resize((840, 1493), Image.LANCZOS)
    shot = rounded(shot, 48)
    shadow = Image.new("RGBA", (840 + 80, 1493 + 80), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle((40, 40, 880, 1533), 48, fill=(0, 0, 0, 60))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas.paste(shadow, (120 - 40, 380 - 30), shadow)
    canvas.paste(shot, (120, 380), shot)
    return canvas


def lock_shot():
    im = Image.open(f"{RAW}/lock.png").convert("RGB")
    d = ImageDraw.Draw(im)
    d.text((540, 300), "Monday 23 November", font=font("Medium", 40), fill="#A9A59D", anchor="mm")
    d.text((540, 420), "08:41", font=font("Light", 170), fill=CREAM, anchor="mm")
    path = f"{RAW}/lock-with-clock.png"
    im.save(path)
    return path


def widgets_shot():
    im = Image.new("RGB", (1080, 1920), "#2E3440")
    small = Image.open(f"{RAW}/widget-small.png").convert("RGBA")
    wide = Image.open(f"{RAW}/widget-wide.png").convert("RGBA")
    im.paste(wide, ((1080 - wide.width) // 2, 360), wide)
    im.paste(small, (45, 360 + wide.height + 60), small)
    d = ImageDraw.Draw(im)
    for i, c in enumerate(["#D9DDD3", RED, "#BFD3E6", "#F1E3B6"]):
        x = 135 + i * 270
        d.ellipse((x - 80, 1560, x + 80, 1720), fill=c)
    kf = ImageFont.truetype(KANJI, 96)
    d.text((135 + 270, 1640), "字", font=kf, fill=PAPER, anchor="mm")
    path = f"{RAW}/widgets-home.png"
    im.save(path)
    return path


def feature_graphic():
    im = Image.new("RGB", (1024, 500), NIGHT)
    d = ImageDraw.Draw(im)
    icon = rounded(Image.open(f"{RAW}/icon-512.png").resize((112, 112), Image.LANCZOS), 28)
    im.paste(icon, (72, 96), icon)
    d.text((72, 240), "Kanjilink", font=font("Bold", 72), fill=CREAM)
    d.text((72, 336), "Learn kanji every time", font=font("Medium", 34), fill="#A9A59D")
    d.text((72, 380), "you look at your phone", font=font("Medium", 34), fill="#A9A59D")
    d.text((790, 92), "JLPT N5", font=font("Bold", 26), fill="#E86A56", anchor="mt")
    d.text((790, 270), "学", font=ImageFont.truetype(KANJI, 260), fill=CREAM, anchor="mm")
    d.text((790, 430), "study, learning", font=font("Bold", 30), fill=CREAM, anchor="mt")
    return im


def main():
    os.makedirs(f"{OUT}/phoneScreenshots", exist_ok=True)
    Image.open(f"{RAW}/icon-512.png").convert("RGB").save(f"{OUT}/icon.png")
    feature_graphic().save(f"{OUT}/featureGraphic.png")
    shots = [
        (lock_shot(), "A new kanji on your lock screen", NIGHT, CREAM),
        (f"{RAW}/home.png", "Meaning and readings at a glance", PAPER, INK),
        (f"{RAW}/self-test.png", "Test yourself, then reveal", PAPER, INK),
        (widgets_shot(), "Widgets for your home screen", "#E9E3D6", INK),
        (f"{RAW}/settings.png", "Notifications on your schedule", PAPER, INK),
        (f"{RAW}/paywall.png", "JLPT N5 free. All 2,211 kanji with Pro", PAPER, INK),
    ]
    for i, (src, title, bg, fg) in enumerate(shots, 1):
        framed(src, title, bg, fg).save(f"{OUT}/phoneScreenshots/{i}.png")
    print("wrote", OUT)


if __name__ == "__main__":
    main()
