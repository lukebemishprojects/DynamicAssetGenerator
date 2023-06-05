# Cutoff Mask Source

Source Type ID: `dynamic_asset_generator:mask/cutoff`

Format:

```json
{
    "type": "dynamic_asset_generator:mask/cutoff",
    "source": {   },
    "cutoff": 128, // optional, defaults to 128
    "channel": "alpha" // optional, defaults to "alpha"
}
```

Generates a mask which is solid everywhere the provided channel of the provided source is greater than the cutoff, and transparent otherwise. The `channel` may be one of `red`, `green`, `blue`, `alpha`, `cielab_lightness`, `cielab_a`, `cielab_b`, `hsl_hue`, `hsl_lightness`, or `hsl_saturation`. The `cutoff` must be an integer between 0 and 255, inclusive.