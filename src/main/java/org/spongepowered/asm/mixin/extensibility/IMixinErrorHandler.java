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
package org.spongepowered.asm.mixin.extensibility;

import org.apache.logging.log4j.Level;

/**
 * Interface for objects which want to perform custom behaviour when fatal mixin
 * errors occur. For example displaying a user-friendly error message
 */
public interface IMixinErrorHandler {
    
    /**
     * Action to take when handling an error. By default, if a config is marked
     * as "required" then the default action will be {@link #ERROR}, and will be
     * {@link #WARN} otherwise.
     */
    public static enum ErrorAction {
        
        /**
         * Take no action, this should be treated as a non-critical error and
         * processing should continue 
         */
        NONE(Level.INFO),
        
        /**
         * Generate a warning but continue processing 
         */
        WARN(Level.WARN),
        
        /**
         * Throw a
         * {@link org.spongepowered.asm.mixin.throwables.MixinApplyError} to
         * halt further processing if possible
         */
        ERROR(Level.FATAL);
        
        /**
         * Logging level for the specified error action
         */
        public final Level logLevel;

        private ErrorAction(Level logLevel) {
            this.logLevel = logLevel;
        }
    }
    
    /**
     * Called when an error occurs whilst initialising a mixin config. This
     * allows the plugin to display more user-friendly error messages if
     * required.
     * 
     * <p>By default, when a critical error occurs the mixin processor will
     * raise a warning if the config is not marked as "required" and will throw
     * an {@link Error} if it is. This behaviour can be altered by returning
     * different values from this method.</p>
     * 
     * <p>The original throwable which was caught is passed in via the <code>
     * th</code> parameter and the default action is passed in to the <code>
     * action</code> parameter. A plugin can choose to output a friendly message
     * but leave the original behaviour intact (by returning <code>null</code>
     * or returning <code>action</code> directly. Alternatively it may throw a
     * different exception or error, or can reduce the severity of the error by
     * returning a different {@link ErrorAction}.</p>
     * 
     * @param config Config being prepared when the error occurred
     * @param th Throwable which was caught
     * @param mixin Mixin which was being applied at the time of the error
     * @param action Default action
     * @return null to perform the default action (or return action) or new
     *      action to take
     */
    public abstract ErrorAction onPrepareError(IMixinConfig config, Throwable th, IMixinInfo mixin, ErrorAction action);
    
    /**
     * Called when an error occurs applying a mixin. This allows
     * the plugin to display more user-friendly error messages if required.
     * 
     * <p>By default, when a critical error occurs the mixin processor will
     * raise a warning if the config is not marked as "required" and will throw
     * an {@link Error} if it is. This behaviour can be altered by returning
     * different values from this method.</p>
     * 
     * <p>The original throwable which was caught is passed in via the <code>
     * th</code> parameter and the default action is passed in to the <code>
     * action</code> parameter. A plugin can choose to output a friendly message
     * but leave the original behaviour intact (by returning <code>null</code>
     * or returning <code>action</code> directly. Alternatively it may throw a
     * different exception or error, or can reduce the severity of the error by
     * returning a different {@link ErrorAction}.</p>
     * 
     * @param targetClassName Class being transformed when the error occurred
     * @param th Throwable which was caught
     * @param mixin Mixin which was being applied at the time of the error
     * @param action Default action
     * @return null to perform the default action (or return action) or new
     *      action to take
     */
    public abstract ErrorAction onApplyError(String targetClassName, Throwable th, IMixinInfo mixin, ErrorAction action);
    
}
