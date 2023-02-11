# Texture Generation

Generator ID: `dynamic_asset_generator:texture`

Format:

```json
{
    "type" : "dynamic_asset_generator:texture",
    "output_location" : "namespace:texture_path",
    "input" : {   }
}
```

The `output_location` is the location the output texture should be placed at. For instance, to override the default gold ingot texture, this would be `minecraft:items/gold_ingot`.

The `input` takes a texture source. These objects can take different forms, but all share the `type` field, which contains the id of the type of texture source described within.

## Example

A very simple working example of a texture configuration that replaces the gold ingot texture with the apple texture:

```json
{
    "type" : "dynamic_asset_generator:texture",
    "output_location" : "minecraft:item/gold_ingot",
    "input" : {
        "type" : "dynamic_asset_generator:texture",
        "path" : "minecraft:item/apple"
    }
}
```
