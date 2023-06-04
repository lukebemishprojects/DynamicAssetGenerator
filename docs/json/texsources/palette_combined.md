# Combined Paletted Source

Source Type ID: `dynamic_asset_generator:palette_combined`

Format:

```json
{
    "type": "dynamic_asset_generator:palette_combined",
    "overlay": {   },
    "background": {   },
    "paletted": {   },
    "include_background": true, // optional, defaults to true
    "stretch_paletted": false, // optional, defaults to false
    "extend_palette_size": 0 // optional, defaults to 6
}
```

`overlay`, `paletted`, and `background` are texture sources. The Combined Paletted Image Source leverages some of the more powerful abilities of *Dynamic Asset Generator*. First, a palette of colors is extracted from `background`. If this palette contains fewer colors than `extend_palette_size`, it is grown to the correct size by adding lighter and darker colors to the ends of the palette. The `paletted` image is then used as a sort of "map" for this palette, with the average RGB value being used to select a color from the palette. If `stretch_paletted` is true, the values of this image will be "stretched" to fill the lightest and darkest values of the palette. Then, the images are stacked; if `include_background` is true, then the background goes at the back. Either way, the image colored with the palette goes in the middle and the overlay goes on top.
