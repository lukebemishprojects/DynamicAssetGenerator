# JSON Generators

Dynamic Asset Generator can be used to add or overwrite assets or data using JSON files. These files live in `assets/<namespace>/dynamic_asset_generator` or `data/<namespace>/dynamic_asset_generator`, and can be placed under any namespace and within subfolders. Their general format is as follows:

```json
{
    "type" : "<type identifier>",
    "args" : ...
}
```

Where the `type` defines what sort of generator it is and what it does; the other specified arguments will depend on the type of generator.

## Sprite Sources

Dynamic Asset Generator's texture sources can also be used as a sprite source in a texture atlas, similar to vanilla's paletted
permutations. To use them this way, a sprite source of type `dynamic_asset_generator:tex_sources` should be added to the texture
atlas JSON file you wish to generate textures for. This sprite source takes several parameters:
* `sources` - a map of the resource locations of textures to generate, to the texture source to use to generate them.
* `location` - optional; a path representing a folder to search for texture sources to generate.

For instance, if the following were placed at `assets/minecraft/atlases/blocks.json`:
```json
{
  "sources": [
    {
      "type": "dynamic_asset_generator:tex_sources",
      "sources": {
        "minecraft:block/clay": {
          "type" : "dynamic_asset_generator:texture",
          "path" : "minecraft:block/iron_block"
        }
      },
      "location": "namespace:path"
    }
  ]
}
```
Then the texture for clay would be replaced with the texture for iron, and a file placed at `assets/minecraft/namespace/path/block/end_stone.json`
containing a texture source would replace the texture for end stone. This sprite source takes the same types of texture sources
as [Texture Generation](texsources/category.md). If the texture source accesses other textures, the sprite source will attempt
to generate animation metadata from those textures.
