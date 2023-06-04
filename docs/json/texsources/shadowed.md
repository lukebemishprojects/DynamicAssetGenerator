# Shadowed Source

Source Type ID: `dynamic_asset_generator:palette_spread`

Format:

```json
{
    "type": "dynamic_asset_generator:palette_spread",
    "background": {   },
    "foreground": {   },
    "extend_palette_size": 6, // optional, defaults to 6
    "highlight_strength": 72, // optional, defaults to 72
    "shadow_strength": 72, // optional, defaults to 72
    "uniformity": 1.0, // optional, defaults to 1.0
}
```

This source overlays the foreground texture on top of the background texture, and creates a directional shadow behind and around the foreground texture. Several other parameters can be configured:
* `extend_palette_size` extends the extracted palette to this size by adding colors.
* `highlight_strength` determines how much the highlight is emphasized. A higher number results in a brighter highlight.
* `shadow_strength` determines how much the shadow is emphasized. A higher number results in a darker shadow.
* `uniformity` determines how uniform the shadow and highlights are relative to the original background. A higher number results in a more uniform shadow.
