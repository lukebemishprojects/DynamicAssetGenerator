# JSON Generators

Dynamic Asset Generator can be used to add or overwrite assets or data using JSON files. These files live in `assets/<namespace>/dynamic_asset_generator` or `data/<namespace>/dynamic_asset_generator`, and can be placed under any namespace and within subfolders. Their general format is as follows:

```json
{
    "type" : "<type identifier>",
    "args" : ...
}
```

Where the `type` defines what sort of generator it is and what it does; the other specified arguments will depend on the type of generator.
