import os
from rembg import remove
from PIL import Image

def process_image(img_path):
    print(f"Processing {img_path}...")
    try:
        input_image = Image.open(img_path)
        output_image = remove(input_image)
        output_image.save(img_path)
        print(f"Successfully processed {img_path}")
    except Exception as e:
        print(f"Error processing {img_path}: {e}")

assets_dir = r"d:\Idea\中国软件杯\smartmentor\smartmentor-web\src\assets"
images_to_process = [
    "desk_book.png",
    "desk_camera.png",
    "desk_coffee.png",
    "desk_tablet.png"
]

for img_name in images_to_process:
    img_path = os.path.join(assets_dir, img_name)
    if os.path.exists(img_path):
        process_image(img_path)
    else:
        print(f"File not found: {img_path}")
