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
package org.spongepowered.tools.obfuscation.interfaces;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import org.spongepowered.tools.obfuscation.SuppressedBy;

/**
 * An extended {@link Messager} which supports messages that can be suppressed
 * using the standard Java {@link SuppressWarnings} annotation.
 */
public interface IMessagerSuppressible extends IMessagerEx {

    /**
     * Prints a message of the specified kind at the location of the
     * element.
     *
     * @param kind the kind of message
     * @param msg the message, or an empty string if none
     * @param e the element to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, SuppressedBy suppressedBy);

    /**
     * Prints a message of the specified kind at the location of the
     * element.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     * @param e the element to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(MessageType type, CharSequence msg, Element e, SuppressedBy suppressedBy);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation mirror of the annotated element.
     *
     * @param kind the kind of message
     * @param msg the message, or an empty string if none
     * @param e the annotated element
     * @param a the annotation to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, SuppressedBy suppressedBy);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation mirror of the annotated element.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     * @param e the annotated element
     * @param a the annotation to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(MessageType type, CharSequence msg, Element e, AnnotationMirror a, SuppressedBy suppressedBy);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation value inside the annotation mirror of the annotated
     * element.
     *
     * @param kind the kind of message
     * @param msg the message, or an empty string if none
     * @param e the annotated element
     * @param a the annotation containing the annotation value
     * @param v the annotation value to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(Diagnostic.Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v, SuppressedBy suppressedBy);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation value inside the annotation mirror of the annotated
     * element.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     * @param e the annotated element
     * @param a the annotation containing the annotation value
     * @param v the annotation value to use as a position hint
     * @param suppressedBy the {@link SuppressWarnings} value which will silence
     *      this message
     */
    void printMessage(MessageType type, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v, SuppressedBy suppressedBy);
    
}
