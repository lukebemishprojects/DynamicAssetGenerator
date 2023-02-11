---
sidebar_position: 2
---

# Texture Metadata Generation

Generator ID: `dynamic_asset_generator:texture_meta`

Format:

```json
{
    "type" : "dynamic_asset_generator:texture_meta",
    "sources" : [
        "namespace:texture_path1",
        "namespace:texture_path2"
    ]
    "output_location" : "namespace:texture_path",
    "animation" : {
        "frametime" : 300,
        "interpolate" : true,
        "pattern_source" : "namespace:texture_path1",
        "scales" : [
            1,
            2
        ]
    },
    "villager" : {
        "hat" : "partial"
    },
    "texture" : {
        "blur" : true,
        "clamp" : false
    }
}
```

This generator generates a `.png.mcmeta` file for a texture at `output_location` based on a series of other textures specified in `sources`.

`animation`, `villager`, and `texture`, along with all their arguments, are optional; if not provided, they will either be excluded if appropriate of inherited from the source textures if present.

`animation`:
* `frametime` and `interpolate` are the same as the corresponding arguments in a `.png.mcmeta` file.
* `pattern_source` determines which of the sources to take the general ordering of the frames from.
* `scales` can be used to specify that one or more of the source textures has been scaled in time when added to the output texture; see [Animation Splitter](texsources/splitter).

The arguments of `villager` and `texture` allow the corresponding arguments in the `.png.mcmeta` to be overridden instead of inherited.

