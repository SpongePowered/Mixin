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
package org.spongepowered.asm.mixin.injection.selectors;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.spongepowered.asm.util.asm.ElementNode;

public class InvalidSelector implements ITargetSelector {
    
    private String input;
    
    private Throwable cause;
    
    public InvalidSelector(Throwable cause) {
        this(cause, null);
    }
    
    public InvalidSelector(String input) {
        this(null, input);
    }
    
    public InvalidSelector(Throwable cause, String input) {
        this.input = input;
        this.cause = cause;
    }

    @Override
    public ITargetSelector next() {
        return null;
    }

    @Override
    public ITargetSelector configure(String... args) {
        return this;
    }

    @Override
    public ITargetSelector validate() throws InvalidSelectorException {
        if (this.cause instanceof InvalidSelectorException) {
            throw (InvalidSelectorException)this.cause;
        }
        String message = "Error parsing target selector";
        if (this.input != null) {
            message += ", the input was in an unexpected format: " + this.input;
        }
        if (this.cause != null) {
            throw new InvalidSelectorException(message, this.cause);
        }
        throw new InvalidSelectorException(message);
    }

    @Override
    public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
        return this;
    }

    @Override
    public int getMatchCount() {
        return 0;
    }

    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        this.validate();
        return MatchResult.NONE;
    }

    @Override
    public MatchResult match(AbstractInsnNode insn) {
        this.validate();
        return MatchResult.NONE;
    }

}
