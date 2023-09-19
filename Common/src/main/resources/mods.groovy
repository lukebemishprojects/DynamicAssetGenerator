/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[40,)'

    license = 'LGPL-3.0-or-later'
    issueTrackerUrl = 'https://github.com/lukebemishprojects/DynamicAssetGenerator/issues'

    mod {
        modId = this.buildProperties['mod_id']
        displayName = this.buildProperties['mod_name']
        version = this.version
        group = this.group
        intermediate_mappings = 'net.fabricmc:intermediary'
        displayUrl = 'https://github.com/lukebemishprojects/DynamicAssetGenerator'

        description = 'A tool for generating asset and data resources at runtime from existing resources.'
        authors = [this.buildProperties['mod_author'] as String]

        dependencies {
            minecraft = this.minecraftVersionRange

            forge {
                versionRange = ">=${this.forgeVersion}"
            }

            onQuilt {
                quilt_loader = ">=${this.libs.versions.quilt.loader}"
                quilted_fabric_api = ">=${this.libs.versions.qfapi}"            }

            onFabric {
                mod 'fabric-api', {
                    versionRange = ">=${this.libs.versions.qfapi.split('-')[0].split(/\+/)[1]}"
                }
            }
        }

        entrypoints {
            onQuilt {
                client_init = ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt.DynamicAssetGeneratorClientQuilt']
                init = ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.quilt.DynamicAssetGeneratorQuilt']
            }
            onFabric {
                client = ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.DynamicAssetGeneratorClientFabric']
                main = ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.DynamicAssetGeneratorFabric']
            }
        }
    }
    onQuilt {
        mixin = [
            'mixin.dynamic_asset_generator.json',
            'mixin.dynamic_asset_generator_fabriquilt.json'
        ]
    }
    onFabric {
        mixin = [
            'mixin.dynamic_asset_generator.json',
            'mixin.dynamic_asset_generator_fabriquilt.json',
            'mixin.dynamic_asset_generator_fabric.json'
        ]
    }
}
