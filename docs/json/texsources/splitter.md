# Animation Splitter

Source Type ID: `dynamic_asset_generator:animation_splitter` and `dynamic_asset_generator:frame_capture`

Format:

```json
{
    "type" : "dynamic_asset_generator:animation_splitter",
    "sources" : {
        "name1" : {   },
        "name2" : {   }
    },
    "generator" : {
        ...
        {
            "type" : "dynamic_asset_generator:frame_capture",
            "capture" : "name1"
        }
        ...
    }
}
```

This source allows animation images to be split up and processed frame-by-frame. A list of `sources` is specified; each of these is broken up into square frames. The total length of the output animation will be the least common multiple of the lengths of the input animations. The `generator` is calculated for each frame of the output animation; inside of this texture source, any other texture source can be used, in addition to the special `frame_capture` source, which is given a source name and captures a single frame of that source at a time.
