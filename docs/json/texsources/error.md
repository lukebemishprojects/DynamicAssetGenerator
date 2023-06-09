# Error Source

Source Type ID: `dynamic_asset_generator:palette_spread`

Format:

```json
{
    "type": "dynamic_asset_generator:error",
    "message": "Error message"
}
```

This source never works, and logs the given error message to the console. It is useful for debugging, or for more detailed error messages for specific sources with the help of [fallback sources](fallback.md).
