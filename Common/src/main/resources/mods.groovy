/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[40,)'

    license = 'LGPL-3.0-or-later'
    issueTrackerUrl = 'https://github.com/lukebemish/DynamicAssetGenerator/issues'

    mod {
        modId = this.buildProperties['mod_id']
        displayName = this.buildProperties['mod_name']
        version = this.version
        group = this.group
        intermediate_mappings = 'net.fabricmc:intermediary'
        displayUrl = 'https://github.com/lukebemish/DynamicAssetGenerator'

        description = 'A tool for generating asset and data resources at runtime from existing resources.'
        authors = [this.buildProperties['mod_author'] as String]

        dependencies {
            minecraft = this.minecraftVersionRange

            forge {
                versionRange = ">=${this.forgeVersion}"
            }

            onQuilt {
                quiltLoader = ">=${this.quiltLoaderVersion}"
                quilt_base = ">=${this.libs.versions.qsl}"
            }
        }

        entrypoints {
            client_init = ['dev.lukebemish.dynamicassetgenerator.quilt.DynamicAssetGeneratorClientQuilt']
            init = ['dev.lukebemish.dynamicassetgenerator.quilt.DynamicAssetGeneratorQuilt']
        }
    }
    onQuilt {
        mixin = [
                'mixin.dynamic_asset_generator.json',
                'mixin.dynamic_asset_generator_quilt.json'
        ]
    }
}