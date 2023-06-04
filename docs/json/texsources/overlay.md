# Overlay Source

Source Type ID: `dynamic_asset_generator:overlay`

Format:

```json
{
    "type": "dynamic_asset_generator:overlay",
    "sources": [
        {   },
        {   }
    ]
}
```

* `inputs` is a list of texture sources to overlay. These textures are layered one on top of another, with the first texture listed being placed at the back. The final texture will have the dimensions of the texture with the largest width, with textures being scaled up to fit.
