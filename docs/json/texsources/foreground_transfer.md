# Foreground Transfer Source

Source Type ID: `dynamic_asset_generator:foreground_transfer`

Format:

```json
{
    "type": "dynamic_asset_generator:foreground_transfer",
    "background": {   },
    "full": {   },
    "new_background": {   },
    "trim_trailing": true, // optional, default true
    "force_neighbors": true, // optional, default true
    "fill_holes": true, // optional, default true
    "extend_palette_size": 0, // optional, default 6
    "close_cutoff": 2 // optional, default 2
}
```

`background`, `full`, and `new_background` are texture sources. The Foreground Transfer Source is similar to the Combined Paletted Image Source. First, an overlay image and image storing palette changes are extracted from the `background` and `full`. Then, this same set of palette changes and overlay is applied to the `new_background` image. Several other parameters can be configured:
* `extend_palette_size` extends the extracted palette to this size by adding colors.
* `trim_trailing` removes transparent overlay pixels or palette change pixels not connected to a solid overlay pixel. Defaults to `false`.
* `force_neighbors` records the palette state for any pixel next to a solid overlay pixel, regardless of whether it changes between the two textures. Defaults to `false`.
* `fill_holes` attempts to fill holes in the texture, possibly with some success. Defaults to `false`.
* `close_cutoff` determines where the line is drawn between pixels not in the palette and pixels that are formed by a semi-transparent overlay on a palette color. Increasing this value makes it more lenient. Defaults to `2`, which is usually a good value.
