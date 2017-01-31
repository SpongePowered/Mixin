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
package org.spongepowered.tools.obfuscation.struct;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;

/**
 * Remap tracking object for injectors. When remapping an injector we will
 * generally want to raise an error if <tt>remap=true</tt> but we don't find
 * a mapping for the injector. However it may be the case that remap is true
 * because some of the &#064;At's need remapping. This state struct is used to
 * log the original error, but supress it if any &#064;At annotations are
 * remapped in the process. When {@link #dispatchPendingMessages} is called at
 * the end, if no &#064;At's have been remapped then we dispatch the error as
 * planned. 
 */
public class InjectorRemap {
    
    /**
     * True if remap=true
     */
    private final boolean remap;
    
    /**
     * Pending error message (if any)
     */
    private Message message;
    
    /**
     * Number of &#064;At annotations which were remapped 
     */
    private int remappedCount;

    public InjectorRemap(boolean remap) {
        this.remap = remap;
    }

    /**
     * Get whether <tt>remap=true</tt> on the injector
     */
    public boolean shouldRemap() {
        return this.remap;
    }
    
    /**
     * Callback from the parser to notify this injector that it has been
     * remapped
     */
    public void notifyRemapped() {
        this.remappedCount++;
        this.clearMessage();
    }

    /**
     * Add an error message on ths injector, the message will be suppressed if
     * a child {@link At} is remapped
     * 
     * @param kind message kind
     * @param msg message
     * @param element annotated element
     * @param annotation annotation
     */
    public void addMessage(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationHandle annotation) {
        this.message = new Message(kind, msg, element, annotation);
    }
    
    /**
     * Clear the current message (if any)
     */
    public void clearMessage() {
        this.message = null;
    }
    
    /**
     * Called after processing completes. Dispatches the queued message (if any)
     * if no child At annotations were remapped.
     * 
     * @param messager messager to push message into
     */
    public void dispatchPendingMessages(Messager messager) {
        if (this.remappedCount == 0 && this.message != null) {
            this.message.sendTo(messager);
        }
    }
}
