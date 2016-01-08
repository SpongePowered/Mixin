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
package org.spongepowered.asm.mixin.environment.phase;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.environment.IPhase;
import org.spongepowered.asm.mixin.environment.IPhaseTransition;

public abstract class AbstractPhaseTransition implements IPhaseTransition {
    
    static final class EmptyPhase implements IPhase {

        @Override
        public String getName() {
            return "INVALID PHASE";
        }

        @Override
        public void begin() {
            AbstractPhaseTransition.logger.error("Phase criterion matched but no phase was attached!");
        }
        
    }
    
    protected static final EmptyPhase EMPTY = new EmptyPhase();
    
    protected static final Logger logger = LogManager.getLogger("mixin");
    
    protected IPhase phase = AbstractPhaseTransition.EMPTY;

    @Override
    public void attach(IPhase phase) {
        this.phase = phase;
    }

    @Override
    public final void detach(IPhase phase) {
        this.phase = AbstractPhaseTransition.EMPTY;
    }
}
