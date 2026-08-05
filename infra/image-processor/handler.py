import io
import os

import boto3
from PIL import Image, ImageOps


S3_BUCKET = os.environ["IMAGE_BUCKET"]
CONTENT_MAX_WIDTH = int(os.environ.get("CONTENT_MAX_WIDTH", "1280"))
THUMBNAIL_MAX_WIDTH = int(os.environ.get("THUMBNAIL_MAX_WIDTH", "320"))
CONTENT_QUALITY = int(os.environ.get("CONTENT_QUALITY", "82"))
THUMBNAIL_QUALITY = int(os.environ.get("THUMBNAIL_QUALITY", "75"))

s3 = boto3.client("s3")


def handle(event, _context):
    source = s3.get_object(Bucket=S3_BUCKET, Key=event["stagingKey"])["Body"].read()
    with Image.open(io.BytesIO(source)) as opened:
        image = ImageOps.exif_transpose(opened).convert("RGB")
        content = resize(image, CONTENT_MAX_WIDTH)
        thumbnail = resize(image, THUMBNAIL_MAX_WIDTH)

        content_metadata = save(content, event["contentKey"], CONTENT_QUALITY)
        thumbnail_metadata = save(thumbnail, event["thumbnailKey"], THUMBNAIL_QUALITY)

    return {
        "displayOrder": event["displayOrder"],
        "content": content_metadata,
        "thumbnail": thumbnail_metadata,
    }


def resize(image, max_width):
    if image.width <= max_width:
        return image.copy()
    height = max(1, round(image.height * max_width / image.width))
    return image.resize((max_width, height), Image.Resampling.LANCZOS)


def save(image, object_key, quality):
    output = io.BytesIO()
    image.save(output, format="JPEG", quality=quality, optimize=True, progressive=True)
    body = output.getvalue()
    s3.put_object(
        Bucket=S3_BUCKET,
        Key=object_key,
        Body=body,
        ContentType="image/jpeg",
        CacheControl="public,max-age=31536000,immutable",
    )
    return {
        "objectKey": object_key,
        "contentType": "image/jpeg",
        "width": image.width,
        "height": image.height,
        "bytes": len(body),
    }
