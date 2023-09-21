# Channel Mask Source

Source Type ID: `dynamic_asset_generator:mask/channel`

Format:

```json
{
    "type": "dynamic_asset_generator:mask/channel",
    "source": {   },
    "channel": "cielab_a"
}
```

Generates a mask whose alpha channel is the specified channel of the source texture. The `channel` may be one of `red`, `green`, `blue`, `alpha`, `cielab_lightness`, `cielab_a`, `cielab_b`, `hsl_hue`, `hsl_lightness`, `hsl_saturation`, `hsv_hue`, `hsv_value`, or `hsv_saturation`.