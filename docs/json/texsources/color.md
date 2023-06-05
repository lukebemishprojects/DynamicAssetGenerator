# Color Source

Source Type ID: `dynamic_asset_generator:color`

Format:

```json
{
    "type": "dynamic_asset_generator:color",
    "color": [
        "0xAAAAFF",
        "0xFFAAAA"
    ],
    "encoding": "RGB" // optional, defaults to ARGB
}
```

Forms a square image containing all the colors listed in `color`, encoded as the provided `encoding`. The colors may be provided as integers (`11184895`) or as strings (`"0xAAAAFF"`). The output texture will be square, with the width being the smallest power of two that can fit all the colors. The `encoding` may be one of `RGB`, `ARGB`, `ABGR`, or `BGR`; if `RGB` or `BGR`, the alpha channel is assumed to be opaque.
