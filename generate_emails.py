import os

emails = [f"albion.gamer.{i:03d}@gmail.com" for i in range(1, 101)]
email_str = ", ".join(emails)

dest_dir = r"C:\Users\Dennis\Downloads\AlbionDataPro_Release"
os.makedirs(dest_dir, exist_ok=True)

file_path = os.path.join(dest_dir, "100_TESTER_EMAILS.txt")
with open(file_path, "w", encoding="utf-8") as f:
    f.write(email_str)

print("Generated 100 emails successfully!")
