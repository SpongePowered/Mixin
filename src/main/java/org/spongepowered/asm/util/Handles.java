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
package org.spongepowered.asm.util;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;

/**
 * Utility class for working with method and field handles
 */
public final class Handles {
    
    private static final int[] H_OPCODES = {
        0,                          // invalid
        Opcodes.GETFIELD,           // H_GETFIELD
        Opcodes.GETSTATIC,          // H_GETSTATIC
        Opcodes.PUTFIELD,           // H_PUTFIELD
        Opcodes.PUTSTATIC,          // H_PUTSTATIC
        Opcodes.INVOKEVIRTUAL,      // H_INVOKEVIRTUAL
        Opcodes.INVOKESTATIC,       // H_INVOKESTATIC
        Opcodes.INVOKESPECIAL,      // H_INVOKESPECIAL
        Opcodes.INVOKESPECIAL,      // H_NEWINVOKESPECIAL
        Opcodes.INVOKEINTERFACE     // H_INVOKEINTERFACE
    };

    private Handles() {
    }

    public static boolean isField(Handle handle) {
        switch (handle.getTag()) {
            case Opcodes.H_INVOKEVIRTUAL:
            case Opcodes.H_INVOKESTATIC:
            case Opcodes.H_INVOKEINTERFACE:
            case Opcodes.H_INVOKESPECIAL:
            case Opcodes.H_NEWINVOKESPECIAL:
                return false;
            case Opcodes.H_GETFIELD:
            case Opcodes.H_GETSTATIC:
            case Opcodes.H_PUTFIELD:
            case Opcodes.H_PUTSTATIC:
                return true;
            default:
                throw new IllegalArgumentException("Invalid tag " + handle.getTag() + " for method handle " + handle + ".");
        }
    }

    public static int opcodeFromTag(int tag) {
        return (tag >= 0 && tag < Handles.H_OPCODES.length) ? Handles.H_OPCODES[tag] : 0;
    }
    
    public static int tagFromOpcode(int opcode) {
        for (int tag = 1; tag < Handles.H_OPCODES.length; tag++) {
            if (Handles.H_OPCODES[tag] == opcode) {
                return tag;
            }
        }
        return 0;
    }

}
