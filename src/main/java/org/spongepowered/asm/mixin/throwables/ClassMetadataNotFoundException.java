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
package org.spongepowered.asm.mixin.throwables;

import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

/**
 * Since it's inconvenient to throw the (checked) {@link ClassNotFoundException}
 * this exception can be selectively thrown when {@link ClassInfo#forName}
 * returns <tt>null</tt> (indicating class couldn't be loaded or was not found)
 * in situations where we actually care about it failing and want to throw
 * something.
 */
public class ClassMetadataNotFoundException extends MixinException {

    private static final long serialVersionUID = 1L;

    public ClassMetadataNotFoundException(String message) {
        super(message);
    }

    public ClassMetadataNotFoundException(String message, ActivityStack context) {
        super(message, context);
    }

    public ClassMetadataNotFoundException(Throwable cause) {
        super(cause);
    }

    public ClassMetadataNotFoundException(Throwable cause, ActivityStack context) {
        super(cause, context);
    }

    public ClassMetadataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClassMetadataNotFoundException(String message, Throwable cause, ActivityStack context) {
        super(message, cause, context);
    }

}
