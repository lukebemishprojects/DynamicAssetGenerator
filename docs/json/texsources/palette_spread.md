# Palette Spread Source

Source Type ID: `dynamic_asset_generator:palette_spread`

Format:

```json
{
    "type": "dynamic_asset_generator:palette_spread",
    "source": {   },
    "palette_cutoff": 3.5, // optional, defaults to 3.5
}
```

This source converts the provided source, `source`, to a greyscale image by evenly spreading out the individual colors in the images palette so that distances between colors are even. This is useful if you are going to use the image as a palette providing texture, as it will make the colors more evenly distributed. `palette_cutoff` configures how sensitive the palette used is, with colors falling within the given distance of each other being considered the same color. The default, `3.5`, is usually a good value.
