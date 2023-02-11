# Fallback Source

Source Type ID: `dynamic_asset_generator:fallback`

Format:

```json
{
    "type": "dynamic_asset_generator:fallback",
    "original": {   },
    "fallback": {   }
}
```

* `original` is a texture source to attempt to create
* `fallback` is an alternative texture source that will be created and provided if the first one is not present or somehow fails.
