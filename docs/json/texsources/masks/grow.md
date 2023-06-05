# Grow Mask Source

Source Type ID: `dynamic_asset_generator:mask/grow`

Format:

```json
{
    "type": "dynamic_asset_generator:mask/grow",
    "source": {   },
    "growth": 0.0625, // optional, defaults to 0.0625
    "cutoff": 128 // optional, defaults to 128
}
```

Grows or shrinks a mask by the provided `growth` factor, while binning pixel values as either fully opaque or transparent based on whether they are more than or less than the provided `cutoff`.