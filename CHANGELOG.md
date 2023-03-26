## Dynamic Asset Generator 3.0.5

### Fixes
- Fix Overlay sources with non-square images
- Crop source logs inconsistently

## Dynamic Asset Generator 3.0.4

### Fixes
- Stop NativeImageHelper error on non-square images

## Dynamic Asset Generator 3.0.3

### Fixes
- Update to 1.19.4

## Dynamic Asset Generator 3.0.2

### Fixes
- no longer errors on uncachable sources
- Fix build to avoid loom 1.1 issues
- More cache system changes

## Dynamic Asset Generator 3.0.1

### Fixes
- Fix JSON-loaded generators not working and properly mark caching API as experimental

## Dynamic Asset Generator 3.0.0

### Features
- **BREAKING** Caching system allows and expects texture sources to define their own cacheable states in some cases
### Fixes
- **BREAKING** Added a fancy new caching system that should hopefully make working with large textures far faster, at the cost of breaking some API surfaces
- Add proper package-info files marking impl packages as internal
### Other
- Update README.md

## Dynamic Asset Generator 2.0.0

### Features
- **BREAKING** Begin breaking changes for 1.19.3
- **BREAKING** Switch ResourceCache system to use mod-registered cache instances
### Fixes
- Hide generated resources from pack selector
### Other
- Update build system to use new maven
- Update all old generators to new API
- Update mixins to 1.19.3
- Change base jar name
- Change approach to adding resource packs on Quilt
- Change testing system

## Dynamic Asset Generator 1.2.0

### Features
- New tag queueing system, where tag locations do not have to be known at initial queueing.
- Make new tag queueing system functional through rewriting of old system and adding of reset listener system.
- Add InvisibleResourceProvider system and switch around pack metadata solver
- Initial work on new template system; added template codec and class for tag files.
### Fixes
- More changes to tag queueing system
- Publish correct jar to modrinth
- Make provided server resources properly load before tags
- Hopefully fix #23 by adding cutoffs where the clustering algorithm should be preferred for extraction
### Other
- Add common code to exported platform-specific source/javadoc jars
- Add system to override publishing version for local testing

## Dynamic Asset Generator 1.1.0

### Features
- Dummy commit for new changelog/tag system
### Fixes
- Fix issues caused by attempting to get default scale values before they are present in TextureMetaGenerator
- Actually fix issues with animation metas where only some source files have an animation
### Other
- Preliminary work on new sources
- Default masking texture sources
- Remove fabric-specific subproject
- Deprecate datagen for removal; it's broken anyway.
- Refactor build logic.
- Further fixes to new build system

## Dynamic Asset Generator 1.0.0

- *Major* internal refactors and breaking API changes
- Transition ITexSources to codecs
- Hopefully make everything a bit easier to work with going forwards
- Update past newest Forge breaking changes

## Dynamic Asset Generator 0.7.1

- Internal refactors
- Hopefully make certain edge cases fail less terribly

## Dynamic Asset Generator 0.7.0

- Remove dependency on ARRP for Quilt (and hence remove dependency on fabric API)

## Dynamic Asset Generator 0.6.4

- Finish updating to 1.19
- Fix issues with getResources on 1.19

## Dynamic Asset Generator 0.6.3

- Fix major issue with mixins at runtime on Forge.

## Dynamic Asset Generator 0.6.2

- Remove log spam relating to fallback texture source.

## Dynamic Asset Generator 0.6.1

- Add new fallback texture source.
- Add datagen helpers for JSON texture sources.

## Dynamic Asset Generator 0.6.0

- *Major* internal restructuring of build system; now setup for Quilt support.
- Not backwards compatible if java classes are referenced, due to internal package name changes to match proper convention.

## Dynamic Asset Generator 0.5.2

- Fix potential issue with `fillHoles` by making palette containment checking a bit less sensitive, as all channels are
now checked individually. This *shouldn't* have other effects, but I am still uncertain what else might have been broken
by `fillHoles`.

## Dynamic Asset Generator 0.5.1

- Add `fillHoles` option for texture extraction.

## Dynamic Asset Generator 0.5.0

- Add new method for registering tags - this should allow multiple mods to use DynAssetGen to add to the same tags.

## Dynamic Asset Generator 0.4.9

- Allow runtime generated maps.
- Fix a lot of unclosed InputStreams or NativeImages

## Dynamic Asset Generator 0.4.7

- Switch from AWT BufferedImage to Mojang's NativeImage.
- Change logic on clustering extractor and hybrid pixels.

## Dynamic Asset Generator 0.4.6

- Add alternate clustering palette extractor as a fallback. Other uses for this will be added at some point.

## Dynamic Asset Generator 0.4.5

- Shrink default cutoff for palette colors even more, to prevent weirdness.

## Dynamic Asset Generator 0.4.4

- Shrink default cutoff for palette colors and add handling for empty palettes during extraction.

## Dynamic Asset Generator 0.4.3

- Fix data caching and generation.

## Dynamic Asset Generator 0.4.2

- Improve palette extractions.

## Dynamic Asset Generator 0.4.1

- Fix `ServerPrePackRepository` not working right.

## Dynamic Asset Generator 0.3.5

- Update to 1.18.2 - *This version is not backwards compatible*.

## Dynamic Asset Generator 0.3.2

- Fix crash with caching.

## Dynamic Asset Generator 0.3.1

- Fix crash caused by mixin weirdness.

## Dynamic Asset Generator 0.3.0

- Add JSON-controlled generation of textures; change backend to Forge API or ARRP (on Fabric).

## Dynamic Asset Generator 0.2.0

- Ability to cache data between runs to save load time. Disabled by default.

## Dynamic Asset Generator 0.1.7

- More palette extractor improvements, including the ability to change the closeness cutoff.

## Dynamic Asset Generator 0.1.6

- More improvements to extractors - added two new settings targeted at extracting ore textures.

## Dynamic Asset Generator 0.1.5

- General improvments to extracors
- Server data is wack. Currently only works for tags that already exists, but whatever. Try to avoid using where possible.

## Dynamic Asset Generator 0.1.4

- Add resetting suppliers.

## Dynamic Asset Generator 0.1.3

- Add server data features.

## Dynamic Asset Generator 0.1.2

- Fix out of bounds issue.

## Dynamic Asset Generator 0.1.1

- Remove debug stuff.

## Dynamic Asset Generator 0.1.0

- Initial version.



