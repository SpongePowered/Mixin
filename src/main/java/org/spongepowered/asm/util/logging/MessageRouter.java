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
package org.spongepowered.asm.util.logging;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Logging router for objects which may need to log messages during standard
 * runtime or during Annotation Processor sessions. Provides a single interface
 * to obtain a {@link Messager} to write log entries in either environment.
 */
public final class MessageRouter {
    
    /**
     * Implementation of Messager which writes to a logger
     */
    static class LoggingMessager implements Messager {
        
        /**
         * Logger 
         */
        private static final Logger logger = LogManager.getLogger("mixin");

        @Override
        public void printMessage(Kind kind, CharSequence msg) {
            LoggingMessager.logger.log(LoggingMessager.messageKindToLoggingLevel(kind), msg);
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e) {
            LoggingMessager.logger.log(LoggingMessager.messageKindToLoggingLevel(kind), msg);
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
            LoggingMessager.logger.log(LoggingMessager.messageKindToLoggingLevel(kind), msg);
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
            LoggingMessager.logger.log(LoggingMessager.messageKindToLoggingLevel(kind), msg);
        }

        private static Level messageKindToLoggingLevel(Kind kind) {
            switch (kind) {
                case ERROR:
                    return Level.ERROR;
                case WARNING:
                case MANDATORY_WARNING:
                    return Level.WARN;
                case NOTE:
                    return Level.INFO;
                case OTHER:
                default:
                    return Level.DEBUG;
            }
        }
        
    }
    
    /**
     * Debug-level messages are expressed as {@link Kind#OTHER} but we don't
     * want to emit them. This wrapper just intercepts debug messages so they
     * don't spam the compiler output.
     */
    static class DebugInterceptingMessager implements Messager {
        
        private final Messager wrapped;
        
        DebugInterceptingMessager(Messager messager) {
            this.wrapped = messager;
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg) {
            if (kind != Kind.OTHER) {
                this.wrapped.printMessage(kind, msg);
            }
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e) {
            if (kind != Kind.OTHER) {
                this.wrapped.printMessage(kind, msg, e);
            }
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a) {
            if (kind != Kind.OTHER) {
                this.wrapped.printMessage(kind, msg, e, a);
            }
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v) {
            if (kind != Kind.OTHER) {
                this.wrapped.printMessage(kind, msg, e, a, v);
            }
        }
        
    }

    /**
     * Current message sink
     */
    private static Messager messager;
    
    /**
     * Utility class
     */
    private MessageRouter() {}
    
    /**
     * Get the current messager. If no messager is available the returned
     * Messager sinks messages to a log4j2 logger
     */
    public static Messager getMessager() {
        if (MessageRouter.messager == null) {
            MessageRouter.messager = new LoggingMessager();
        }
        return MessageRouter.messager;
    }

    /**
     * Set the messager to use, this should only be called by the AP
     * 
     * @param messager new messager to sink messages, can be null to revert to
     *      log4j2 logging
     */
    public static void setMessager(Messager messager) {
        MessageRouter.messager = messager == null ? null : new DebugInterceptingMessager(messager);
    }

}
