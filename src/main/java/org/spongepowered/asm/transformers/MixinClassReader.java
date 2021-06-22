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
package org.spongepowered.asm.transformers;

import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.util.asm.ASM;

/**
 * A ClassReader which returns a more verbose exception message when the
 * incoming class major version is higher than the version supported by the
 * active ASM.
 */
public class MixinClassReader extends ClassReader {

    public MixinClassReader(final byte[] classFile, final String name) {
        super(MixinClassReader.checkClassVersion(classFile, name));
    }

    private static byte[] checkClassVersion(final byte[] classFile, final String name) {
        short majorClassVersion = (short)(((classFile[6] & 0xFF) << 8) | (classFile[7] & 0xFF));
        if (majorClassVersion > ASM.getMaxSupportedClassVersionMajor()) {
            throw new IllegalArgumentException(String.format(
                    "Class file major version %d is not supported by active ASM (version %d.%d supports class version %d), reading %s",
                    majorClassVersion, ASM.getApiVersionMajor(), ASM.getApiVersionMinor(), ASM.getMaxSupportedClassVersionMajor(), name));
        }
        return classFile;
    }

}
