# Mask Sources

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

# Generating Masks

DynamicAssetGenerator has a number of texture sources meant for generating masks. These operations are all in the `dynamic_asset_generator:mask/` folder. These sources all do a given operation in the alpha channel, and make no guarantees about the content of other channels.