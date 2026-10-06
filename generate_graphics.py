from PIL import Image, ImageDraw, ImageFont
import os

dest_dir = r"C:\Users\Dennis\Downloads\AlbionDataPro_Release"
os.makedirs(dest_dir, exist_ok=True)

# 1. Generate Feature Graphic (1024x500)
img_fg = Image.new("RGB", (1024, 500), color="#090d16")
draw = ImageDraw.Draw(img_fg)

for x in range(1024):
    r = int(9 + (30 - 9) * (x / 1024))
    g = int(13 + (41 - 13) * (x / 1024))
    b = int(22 + (59 - 22) * (x / 1024))
    draw.line([(x, 0), (x, 500)], fill=(r, g, b))

try:
    font_title = ImageFont.truetype("arial.ttf", 64)
    font_sub = ImageFont.truetype("arial.ttf", 26)
except:
    font_title = ImageFont.load_default()
    font_sub = ImageFont.load_default()

draw.text((340, 180), "AlbionDataPro", fill="#ffffff", font=font_title)
draw.text((340, 260), "Das ultimative In-Game Markt-Overlay", fill="#38bdf8", font=font_sub)
draw.text((340, 310), "Arbitrage Radar • Live-Preise • Silber Rechner • 24/7 Cloud", fill="#94a3b8", font=font_sub)

fg_path = os.path.join(dest_dir, "AlbionDataPro_FeatureGraphic_1024x500.png")
img_fg.save(fg_path)

# 2. Generate App Icon (512x512)
img_icon = Image.new("RGB", (512, 512), color="#1E293B")
draw_icon = ImageDraw.Draw(img_icon)
draw_icon.rectangle([20, 20, 492, 492], outline="#06B6D4", width=8)
try:
    font_icon = ImageFont.truetype("arial.ttf", 80)
except:
    font_icon = ImageFont.load_default()
draw_icon.text((120, 210), "ADP", fill="#F59E0B", font=font_icon)

icon_path = os.path.join(dest_dir, "AlbionDataPro_Logo_512x512.png")
img_icon.save(icon_path)

print("Successfully generated graphics!")
