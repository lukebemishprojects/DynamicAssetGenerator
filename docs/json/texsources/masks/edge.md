# Edge Mask Source

Source Type ID: `dynamic_asset_generator:mask/edge`

Format:

```json
{
    "type": "dynamic_asset_generator:mask/edge",
    "source": {   },
    "count_outside_frame": false, // optional, defaults to false
    "edges": [
        "north",
        "northeast",
        "east",
        "southeast",
        "south",
        "southwest",
        "west",
        "northwest"
    ], // optional, defaults to all directions
    "cutoff": 128, // optional, defaults to 128
}
```

Generates a mask which is solid everywhere the provided source has an edge, and transparent otherwise. The `edges` may be any subset of `north`, `northeast`, `east`, `southeast`, `south`, `southwest`, `west`, and `northwest`. The `cutoff` must be an integer between 0 and 255, inclusive. A pixel is considered to be an edge if it's alpha is greater than or equal to the cutoff and at least one of its neighbors, in the provided `edges`, is less than the cutoff. If `count_outside_frame` is true, then pixels outside the frame are considered to always be less than the cutoff.