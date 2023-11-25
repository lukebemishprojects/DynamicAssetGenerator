# Channel Route Source

Source Type ID: `dynamic_asset_generator:channel_route`

Format:

```json
{
    "type": "dynamic_asset_generator:channel_route",
    "sources": {
        "source": {   },
        "another_source": {   },
        "...": {   },
    },
    "red": {
        "source": "another_source",
        "channel": "red"
    }, // optional
    "green": {   }, // optional
    "blue": {   }, // optional
    "alpha": {   } // optional
}
```

This source routes channels from provided inputs to the channels of an output image. The `sources` field takes a map of identifier
names to texture sources; the fields for each channel (`red`, `green`, `blue`, and `alpha`) refer to these provided sources, and
specify a channel (`red`, `green`, `blue`, `alpha`, `cielab_lightness`, `cielab_a`, `cielab_b`, `hsl_hue`, `hsl_lightness`, `hsl_saturation`, `hsv_hue`, 
`hsv_value`, or `hsv_saturation`) to use for the given channel; if a channel is not provided, it is assumed to always be 0 (`red`, `green`, or `blue`)
or 255 (`alpha`).

