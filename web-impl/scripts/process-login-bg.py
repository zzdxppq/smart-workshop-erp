"""去除登录背景图水印并输出到 public/images/login-bg.png"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
SRC = Path(
    r"C:\Users\admin\.cursor\projects\e-claude-smart-workshop-erp\assets"
    r"\c__Users_admin_AppData_Roaming_Cursor_User_workspaceStorage_266cc1c96b4022266ffb2668c0b00414_images"
    r"_baitaisheng_erp_bg-a82b5ecb-48bb-48b6-9e20-2a8db72b58c9.png"
)
OUT = ROOT / "public" / "images" / "login-bg.png"


def remove_watermark_tl(im: Image.Image) -> Image.Image:
    w, h = im.size
    tl_w, tl_h = 148, 58
    patch = (
        im.crop((tl_w + 8, 4, tl_w + 8 + tl_w, 4 + tl_h))
        .resize((tl_w, tl_h), Image.Resampling.LANCZOS)
        .filter(ImageFilter.GaussianBlur(radius=1.2))
    )
    im.paste(patch, (0, 0))
    return im


def remove_watermark_br(im: Image.Image) -> Image.Image:
    w, h = im.size
    br_w, br_h = 130, 52
    patch = (
        im.crop((w - br_w - 200, h - br_h - 40, w - 200, h - 40))
        .resize((br_w, br_h), Image.Resampling.LANCZOS)
        .filter(ImageFilter.GaussianBlur(radius=1.0))
    )
    im.paste(patch, (w - br_w, h - br_h))
    return im


def soften_edges(im: Image.Image) -> Image.Image:
    overlay = Image.new("RGBA", im.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    w, h = im.size
    tl_w, tl_h = 148, 58
    br_w, br_h = 130, 52
    for i in range(12):
        alpha = int(40 * (1 - i / 12))
        draw.line([(0, tl_h - 12 + i), (tl_w, tl_h - 12 + i)], fill=(20, 28, 40, alpha))
    for i in range(10):
        alpha = int(35 * (1 - i / 10))
        y = h - br_h - 10 + i
        draw.line([(w - br_w, y), (w, y)], fill=(25, 22, 18, alpha))
    return Image.alpha_composite(im.convert("RGBA"), overlay).convert("RGB")


def main() -> None:
    OUT.parent.mkdir(parents=True, exist_ok=True)
    im = Image.open(SRC).convert("RGB")
    im = remove_watermark_tl(im)
    im = remove_watermark_br(im)
    im = soften_edges(im)
    im.save(OUT, optimize=True)
    print(f"OK {OUT} {im.size} {OUT.stat().st_size} bytes")


if __name__ == "__main__":
    main()
