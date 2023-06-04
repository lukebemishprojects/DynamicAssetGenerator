# Palette Spread Source

Source Type ID: `dynamic_asset_generator:palette_spread`

Format:

```json
{
    "type": "dynamic_asset_generator:palette_spread",
    "source": {   },
    "palette_cutoff": 3.5, // optional, defaults to 3.5
    "range": [
        [0, 50],
        [100, 255]
    ] // optional, defaults to [[0, 255]]
}
```

This source converts the provided source, `source`, to a greyscale image by evenly spreading out the individual colors in the images palette so that distances between colors are even. This is useful if you are going to use the image as a palette providing texture, as it will make the colors more evenly distributed. `palette_cutoff` configures how sensitive the palette used is, with colors falling within the given distance of each other being considered the same color. The default, `3.5`, is usually a good value. The `range` configures the range of values to use for the output image; the members of the palette will correspond to evenly spaced values within this range. For instance, if the range is `[[0, 70], [225, 255]]`, and 4 colors are provided, the colors will be mapped to values 25 apart within the range, at `0`, `25`, `50`, and `230`.
