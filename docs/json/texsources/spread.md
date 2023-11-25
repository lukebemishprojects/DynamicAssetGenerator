# Spread Source

Source Type ID: `dynamic_asset_generator:spread`

Format:

```json
{
    "type": "dynamic_asset_generator:spread",
    "source": {   },
    "range": [
        [0, 50],
        [100, 255]
    ] // optional, defaults to [[0, 255]]
}
```

This source converts the provided source, `source`, to a greyscale image by spreading out or squishing the colors in an image to fit in a given range. The `range` configures the range of values to use for the output image; the members of the palette will correspond to evenly spaced values within this range.
