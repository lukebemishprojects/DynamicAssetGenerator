# Crop Source

Source Type ID: `dynamic_asset_generator:crop`

Format:

```json
{
    "type": "dynamic_asset_generator:crop",
    "input": {   },
    "total_width": 16,
    "start_x": 0,
    "start_y": 0,
    "size_x": 8,
    "size_y": 8
}
```

* `input` a texture source used as an input.
* `total_width` is the expected width of the entire original image. This is used for scaling, and consistency across different resolution resource packs.
* `start_x` and `start_y` are the starting x and y pixels for the output image, counting from the top left. These can be negative.
* `size_x` and `size_y` are the dimensions of the output image in the x and y directions.

All the start and size parameters are relative to the `total_width`. In other words, if the `total_width` is the same as the width of the input image, then the output will be `size_x` by `size_y`, starting at `start_x` and `start_y`; if the image is twice the `total_width`, then the output will be `2*size_x` by `2_size_y`, starting at `2*start_x` and `2*start_y`; etc.
