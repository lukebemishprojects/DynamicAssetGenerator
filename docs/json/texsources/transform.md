# Transform Source

Source Type ID: `dynamic_asset_generator:transform`

Format:

```json
{
    "type": "dynamic_asset_generator:transform",
    "input": {   },
    "rotate": 0,
    "flip": false
}
```

`input` is a texture source to use as an input. The image will first be rotated clockwise by 90 degrees `rotate` times; then, if `flip` is true, it will be flipped horizontally.