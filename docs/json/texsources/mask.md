# Mask Source

Source Type ID: `dynamic_asset_generator:mask`

Format:

```json
{
    "type": "dynamic_asset_generator:mask",
    "mask": {   },
    "input": {   }
}
```

* `mask` is a texture source used for the mask texture.
* `input` is a texture source used for the input texture.

The output texture will be identical to the input, with its alpha multiplied by that of the mask texture. Textures are scaled to fit the wider texture if necessary.