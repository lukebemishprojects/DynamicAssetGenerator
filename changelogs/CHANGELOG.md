### Dynamic Asset Generator v1.0.0

- *Major* internal refactors and breaking API changes
- Transition ITexSources to codecs
- Hopefully make everything a bit easier to work with going forwards
- Update past newest Forge breaking changes

### Dynamic Asset Generator v0.7.1

- Internal refactors
- Hopefully make certain edge cases fail less terribly

### Dynamic Asset Generator v0.7.0

- Remove dependency on ARRP for Quilt (and hence remove dependency on fabric API)

### Dynamic Asset Generator v0.6.4

- Finish updating to 1.19
- Fix issues with getResources on 1.19

### Dynamic Asset Generator v0.6.3

- Fix major issue with mixins at runtime on Forge.

### Dynamic Asset Generator v0.6.2

- Remove log spam relating to fallback texture source.

### Dynamic Asset Generator v0.6.1

- Add new fallback texture source.
- Add datagen helpers for JSON texture sources.

### Dynamic Asset Generator v0.6.0

- *Major* internal restructuring of build system; now setup for Quilt support.
- Not backwards compatible if java classes are referenced, due to internal package name changes to match proper convention.

### Dynamic Asset Generator v0.5.2

- Fix potential issue with `fillHoles` by making palette containment checking a bit less sensitive, as all channels are
now checked individually. This *shouldn't* have other effects, but I am still uncertain what else might have been broken
by `fillHoles`.

### Dynamic Asset Generator v0.5.1

- Add `fillHoles` option for texture extraction.

### Dynamic Asset Generator v0.5.0

- Add new method for registering tags - this should allow multiple mods to use DynAssetGen to add to the same tags.

### Dynamic Asset Generator v0.4.9

- Allow runtime generated maps.
- Fix a lot of unclosed InputStreams or NativeImages

### Dynamic Asset Generator v0.4.7

- Switch from AWT BufferedImage to Mojang's NativeImage.
- Change logic on clustering extractor and hybrid pixels.

### Dynamic Asset Generator v0.4.6

- Add alternate clustering palette extractor as a fallback. Other uses for this will be added at some point.

### Dynamic Asset Generator v0.4.5

- Shrink default cutoff for palette colors even more, to prevent weirdness.

### Dynamic Asset Generator v0.4.4

- Shrink default cutoff for palette colors and add handling for empty palettes during extraction.

### Dynamic Asset Generator v0.4.3

- Fix data caching and generation.

### Dynamic Asset Generator v0.4.2

- Improve palette extractions.

### Dynamic Asset Generator v0.4.1

- Fix `ServerPrePackRepository` not working right.

### Dynamic Asset Generator v0.3.5

- Update to 1.18.2 - *This version is not backwards compatible*.

### Dynamic Asset Generator v0.3.2

- Fix crash with caching.

### Dynamic Asset Generator v0.3.1

- Fix crash caused by mixin weirdness.

### Dynamic Asset Generator v0.3.0

- Add JSON-controlled generation of textures; change backend to Forge API or ARRP (on Fabric).

### Dynamic Asset Generator v0.2.0

- Ability to cache data between runs to save load time. Disabled by default.

### Dynamic Asset Generator v0.1.7

- More palette extractor improvements, including the ability to change the closeness cutoff.

### Dynamic Asset Generator v0.1.6

- More improvements to extractors - added two new settings targeted at extracting ore textures.

### Dynamic Asset Generator v0.1.5

- General improvments to extracors
- Server data is wack. Currently only works for tags that already exists, but whatever. Try to avoid using where possible.

### Dynamic Asset Generator v0.1.4

- Add resetting suppliers.

### Dynamic Asset Generator v0.1.3

- Add server data features.

### Dynamic Asset Generator v0.1.2

- Fix out of bounds issue.

### Dynamic Asset Generator v0.1.1

- Remove debug stuff.

### Dynamic Asset Generator v0.1.0

- Initial version.
