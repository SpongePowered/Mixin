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
package org.spongepowered.asm.service.modlauncher;

import java.util.function.BiConsumer;

import org.spongepowered.asm.service.IMixinAuditTrail;

/**
 * Audit trail adapter for ModLauncher
 */
public class ModLauncherAuditTrail implements IMixinAuditTrail {
    
    private static final String APPLY_MIXIN_ACTIVITY = "apply";
    private static final String POST_PROCESS_ACTIVITY = "decorate";
    
    /**
     * Audit trail consumer
     */
    private BiConsumer<String, String[]> auditTrailConsumer;

    /**
     * @param consumer audit trail consumer which sinks audit trail actions
     */
    public void setConsumer(BiConsumer<String, String[]> consumer) {
        if (this.auditTrailConsumer != null) {
            throw new IllegalStateException("Audit trail consumer was already set.");
        }
        this.auditTrailConsumer = consumer;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinAuditTrail#onApply(
     *      java.lang.String, java.lang.String)
     */
    @Override
    public void onApply(String className, String mixinName) {
        this.writeActivity(className, ModLauncherAuditTrail.APPLY_MIXIN_ACTIVITY, mixinName);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinAuditTrail#onPostProcess(
     *      java.lang.String)
     */
    @Override
    public void onPostProcess(String className) {
        this.writeActivity(className, ModLauncherAuditTrail.POST_PROCESS_ACTIVITY);
    }

    private void writeActivity(String className, String... activity) {
        if (this.auditTrailConsumer != null) {
            this.auditTrailConsumer.accept(className, activity);
        }
    }

}
