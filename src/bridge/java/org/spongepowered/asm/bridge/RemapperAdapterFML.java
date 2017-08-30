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
package org.spongepowered.asm.bridge;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.extensibility.IRemapper;

/**
 * Remapper adapter which remaps using FML's deobfuscating remapper
 */
public final class RemapperAdapterFML extends RemapperAdapter {
    
    private static final String DEOBFUSCATING_REMAPPER_CLASS = "fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper";
    private static final String DEOBFUSCATING_REMAPPER_CLASS_FORGE = "net.minecraftforge." + RemapperAdapterFML.DEOBFUSCATING_REMAPPER_CLASS;
    private static final String DEOBFUSCATING_REMAPPER_CLASS_LEGACY = "cpw.mods." + RemapperAdapterFML.DEOBFUSCATING_REMAPPER_CLASS;
    private static final String INSTANCE_FIELD = "INSTANCE";
    private static final String UNMAP_METHOD = "unmap";
    
    private final Method mdUnmap;
    
    private RemapperAdapterFML(org.objectweb.asm.commons.Remapper remapper, Method mdUnmap) {
        super(remapper);
        this.logger.info("Initialised Mixin FML Remapper Adapter with {}", remapper);
        this.mdUnmap = mdUnmap;
    }

    @Override
    public String unmap(String typeName) {
        try {
            return this.mdUnmap.invoke(this.remapper, typeName).toString();
        } catch (Exception ex) {
            return typeName;
        }
    }
    
    /**
     * Factory method
     */
    public static IRemapper create() {
        try {
            Class<?> clDeobfRemapper = RemapperAdapterFML.getFMLDeobfuscatingRemapper();
            Field singletonField = clDeobfRemapper.getDeclaredField(RemapperAdapterFML.INSTANCE_FIELD);
            Method mdUnmap = clDeobfRemapper.getDeclaredMethod(RemapperAdapterFML.UNMAP_METHOD, String.class);
            org.objectweb.asm.commons.Remapper remapper = (org.objectweb.asm.commons.Remapper)singletonField.get(null);
            return new RemapperAdapterFML(remapper, mdUnmap);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Attempt to get the FML Deobfuscating Remapper, tries the post-1.8
     * namespace first and falls back to 1.7.10 if class lookup fails
     */
    private static Class<?> getFMLDeobfuscatingRemapper() throws ClassNotFoundException {
        try {
            return Class.forName(RemapperAdapterFML.DEOBFUSCATING_REMAPPER_CLASS_FORGE);
        } catch (ClassNotFoundException ex) {
            return Class.forName(RemapperAdapterFML.DEOBFUSCATING_REMAPPER_CLASS_LEGACY);
        }
    }

}
