# Add Mask Source

Source Type ID: `dynamic_asset_generator:mask/add`

Format:

```json
{
    "type": "dynamic_asset_generator:mask/add",
    "sources": [
        {   },
        {   }
    ]
}
```

Adds the alpha channels of the provided sources, then clamps to the range [0, 255].