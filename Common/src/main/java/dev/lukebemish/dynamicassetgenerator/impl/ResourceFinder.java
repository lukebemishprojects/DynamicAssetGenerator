package dev.lukebemish.dynamicassetgenerator.impl;

import net.minecraft.server.packs.PackResources;

import java.util.List;

@FunctionalInterface
public interface ResourceFinder {
    ResourceFinder[] INSTANCES = new ResourceFinder[2];

    List<PackResources> getPacks();


}
