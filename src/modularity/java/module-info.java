/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Mixin module declaration
 */
@SuppressWarnings("module") // Suppress the warnings about gson and gson below
module org.spongepowered.mixin {

    //
    // Actual modules we depend on
    //
    requires transitive cpw.mods.modlauncher;
    requires cpw.mods.securejarhandler;
    requires transitive java.compiler;
    requires transitive java.instrument;
    requires transitive org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires transitive org.objectweb.asm;
    requires transitive org.objectweb.asm.commons;
    requires transitive org.objectweb.asm.tree;
    requires transitive org.objectweb.asm.tree.analysis;
    requires transitive org.objectweb.asm.util;
    
    //
    // Automatic modules we depend on
    //
    requires jopt.simple;
    requires guava;
    requires gson;

    //
    // Exports
    //
    exports org.spongepowered.asm.launch;
    exports org.spongepowered.asm.launch.platform;
    exports org.spongepowered.asm.launch.platform.container;
    exports org.spongepowered.asm.lib;
    exports org.spongepowered.asm.lib.tree;
    exports org.spongepowered.asm.mixin;
    exports org.spongepowered.asm.mixin.connect;
    exports org.spongepowered.asm.mixin.extensibility;
    exports org.spongepowered.asm.mixin.gen;
    exports org.spongepowered.asm.mixin.gen.throwables;
    exports org.spongepowered.asm.mixin.injection;
    exports org.spongepowered.asm.mixin.injection.callback;
    exports org.spongepowered.asm.mixin.injection.code;
    exports org.spongepowered.asm.mixin.injection.invoke.arg;
    exports org.spongepowered.asm.mixin.injection.points;
    exports org.spongepowered.asm.mixin.injection.selectors;
    exports org.spongepowered.asm.mixin.injection.selectors.dynamic;
    exports org.spongepowered.asm.mixin.injection.selectors.throwables;
    exports org.spongepowered.asm.mixin.injection.struct;
    exports org.spongepowered.asm.mixin.injection.throwables;
    exports org.spongepowered.asm.mixin.refmap;
    exports org.spongepowered.asm.mixin.throwables;
    exports org.spongepowered.asm.mixin.transformer.ext;
    exports org.spongepowered.asm.mixin.transformer.throwables;
    exports org.spongepowered.asm.obfuscation;
    exports org.spongepowered.asm.obfuscation.mapping;
    exports org.spongepowered.asm.obfuscation.mapping.common;
    exports org.spongepowered.asm.obfuscation.mapping.mcp;
    exports org.spongepowered.asm.service;
    exports org.spongepowered.asm.service.modlauncher;
    exports org.spongepowered.asm.util;
    exports org.spongepowered.asm.util.asm;
    exports org.spongepowered.asm.util.perf;
    exports org.spongepowered.tools.agent;
    exports org.spongepowered.tools.obfuscation;
    exports org.spongepowered.tools.obfuscation.ext;
    exports org.spongepowered.tools.obfuscation.fg3;
    exports org.spongepowered.tools.obfuscation.interfaces;
    exports org.spongepowered.tools.obfuscation.mapping;
    exports org.spongepowered.tools.obfuscation.mapping.common;
    exports org.spongepowered.tools.obfuscation.mapping.fg3;
    exports org.spongepowered.tools.obfuscation.mapping.mcp;
    exports org.spongepowered.tools.obfuscation.mcp;
    exports org.spongepowered.tools.obfuscation.mirror;
    exports org.spongepowered.tools.obfuscation.service;
    
    // one of these won't exist, the SuppressWarnings above stops the compiler complaining
    opens org.spongepowered.asm.mixin.transformer
        to com.google.gson, gson;
    
    //
    // Service wiring
    //
    uses org.spongepowered.asm.service.IMixinServiceBootstrap;
    provides org.spongepowered.asm.service.IMixinServiceBootstrap
        with org.spongepowered.asm.service.modlauncher.MixinServiceModLauncherBootstrap;

    uses org.spongepowered.asm.service.IMixinService;
    provides org.spongepowered.asm.service.IMixinService
        with org.spongepowered.asm.service.modlauncher.MixinServiceModLauncher;

    uses org.spongepowered.asm.service.IGlobalPropertyService;
    provides org.spongepowered.asm.service.IGlobalPropertyService
        with org.spongepowered.asm.service.modlauncher.Blackboard;

    uses cpw.mods.modlauncher.api.ITransformationService;
    provides cpw.mods.modlauncher.api.ITransformationService
        with org.spongepowered.asm.launch.MixinTransformationService;

    uses cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
    provides cpw.mods.modlauncher.serviceapi.ILaunchPluginService
        with org.spongepowered.asm.launch.MixinLaunchPlugin;
    
    uses javax.annotation.processing.Processor;
    provides javax.annotation.processing.Processor
        with org.spongepowered.tools.obfuscation.MixinObfuscationProcessorInjection,
             org.spongepowered.tools.obfuscation.MixinObfuscationProcessorTargets;

    uses org.spongepowered.tools.obfuscation.service.IObfuscationService;
    provides org.spongepowered.tools.obfuscation.service.IObfuscationService
        with org.spongepowered.tools.obfuscation.mcp.ObfuscationServiceMCP,
             org.spongepowered.tools.obfuscation.fg3.ObfuscationServiceFG3;
}
