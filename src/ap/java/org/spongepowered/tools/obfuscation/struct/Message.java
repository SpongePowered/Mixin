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
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;

/**
 * Wrapper for Annotation Processor messages, used to enable messages to be
 * easily queued and manipulated
 */
public class Message {

    private Diagnostic.Kind kind;
    private CharSequence msg;
    private final Element element;
    private final AnnotationMirror annotation;
    private final AnnotationValue value;

    public Message(Diagnostic.Kind kind, CharSequence msg) {
        this(kind, msg, null, (AnnotationMirror)null, null);
    }
    
    public Message(Diagnostic.Kind kind, CharSequence msg, Element element) {
        this(kind, msg, element, (AnnotationMirror)null, null);
    }
    
    public Message(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationHandle annotation) {
        this(kind, msg, element, annotation.asMirror(), null);
    }
    
    public Message(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation) {
        this(kind, msg, element, annotation, null);
    }
    
    public Message(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationHandle annotation, AnnotationValue value) {
        this(kind, msg, element, annotation.asMirror(), value);
    }
    
    public Message(Diagnostic.Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
        this.kind = kind;
        this.msg = msg;
        this.element = element;
        this.annotation = annotation;
        this.value = value;
    }

    /**
     * Send this message to the supplied message
     * 
     * @param messager messager to send to
     * @return fluent interface
     */
    public Message sendTo(Messager messager) {
        if (this.value != null) {
            messager.printMessage(this.kind, this.msg, this.element, this.annotation, this.value);
        } else if (this.annotation != null) {
            messager.printMessage(this.kind, this.msg, this.element, this.annotation);
        } else if (this.element != null) {
            messager.printMessage(this.kind, this.msg, this.element);
        } else {
            messager.printMessage(this.kind, this.msg);
        }
        return this;
    }

    /**
     * Get the message kind
     */
    public Diagnostic.Kind getKind() {
        return this.kind;
    }
    
    /**
     * Set the message kind
     * 
     * @param kind message kind
     * @return fluent interface
     */
    public Message setKind(Diagnostic.Kind kind) {
        this.kind = kind;
        return this;
    }
    
    /**
     * Get the message text
     * 
     * @return fluent interface
     */
    public CharSequence getMsg() {
        return this.msg;
    }
    
    /**
     * Set the message text
     * 
     * @param msg message text
     * @return fluent interface
     */
    public Message setMsg(CharSequence msg) {
        this.msg = msg;
        return this;
    }
    
    /**
     * Get the target element
     */
    public Element getElement() {
        return this.element;
    }
    
    /**
     * Get the target annotation
     */
    public AnnotationMirror getAnnotation() {
        return this.annotation;
    }
    
    /**
     * Get the target annotation value
     */
    public AnnotationValue getValue() {
        return this.value;
    }

}
