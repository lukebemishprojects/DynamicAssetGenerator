/*
 * Copyright (C) 2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

import modsdotgroovy.Dependency

/*
 * Copyright (C) 2022-2023 Luke Bemish and contributors
 * SPDX-License-Identifier: LGPL-3.0-or-later
 */

ModsDotGroovy.make {
    modLoader = 'javafml'
    loaderVersion = '[1,)'

    license = 'LGPL-3.0-or-later'
    issueTrackerUrl = 'https://github.com/lukebemishprojects/DynamicAssetGenerator/issues'

    mod {
        modId = this.buildProperties['mod_id']
        displayName = this.buildProperties['mod_name']
        version = this.version
        displayUrl = 'https://github.com/lukebemishprojects/DynamicAssetGenerator'

        description = 'A tool for generating asset and data resources at runtime from existing resources.'
        authors = [this.buildProperties['mod_author'] as String]

        dependencies {
            mod 'minecraft', {
                def minor = this.libs.versions.minecraft.split(/\./)[1] as int
                versionRange = "[${this.libs.versions.minecraft},1.${minor+1}.0)"
            }

            onForge {
                neoforge = ">=${this.libs.versions.neoforge}"
            }

            onFabric {
                mod 'fabricloader', {
                    versionRange = ">=${this.libs.versions.fabric.loader}"
                }
                mod 'fabric-api', {
                    versionRange = ">=${this.libs.versions.fabric.api.split(/\+/)[0]}"
                }
            }
        }

        dependencies = dependencies.collect {dep ->
            new Dependency() {
                @Override
                Map asForgeMap() {
                    def map = dep.asForgeMap()
                    map.remove('mandatory')
                    map.put('type', this.mandatory ? 'required' : 'optional')
                    return map
                }
            }
        }

        entrypoints {
            onFabric {
                entrypoint 'client', ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.DynamicAssetGeneratorClientFabric']
                entrypoint 'main', ['dev.lukebemish.dynamicassetgenerator.impl.fabriquilt.fabric.DynamicAssetGeneratorFabric']
            }
        }
    }
    onFabric {
        mixin = [
            'mixin.dynamic_asset_generator.json',
            'mixin.dynamic_asset_generator_fabriquilt.json'
        ]
    }
    onForge {
        mixins = [
            ['config':'mixin.dynamic_asset_generator.json']
        ]
    }
}
