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

import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic.Kind;

import org.spongepowered.tools.obfuscation.SupportedOptions;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor.CompilerEnvironment;

/**
 * An extended {@link Messager} which supports messages with configurable
 * message levels based on the supplied {@link MessageType}.
 */
public interface IMessagerEx extends Messager {
    
    /**
     * Message types, centralised so that individual error/warning levels can be
     * controlled at runtime. 
     */
    public static enum MessageType {
        
        /**
         * Level for NOTE messages which are informational and can be suppressed
         * by quiet
         */
        INFO(Kind.NOTE),
        
        /** All other NOTE level messages */
        NOTE(Kind.NOTE),

        /** Uncategorised (usually unexpected or critical) ERROR messages */
        ERROR(Kind.ERROR),

        /** Uncategorised (usually unexpected or important) WARNING messages */
        WARNING(Kind.WARNING),
        
        /** &#64;Mixin annotation on a non class or interface, eg. package */
        MIXIN_ON_INVALID_TYPE(Kind.ERROR),
        
        /** Soft target for mixin not found */
        MIXIN_SOFT_TARGET_NOT_FOUND(Kind.ERROR),

        /** Soft target for mixin not found, but imaginary type was returned */
        MIXIN_SOFT_TARGET_NOT_RESOLVED(Kind.WARNING),
        
        /** Soft target for mixin is public */
        MIXIN_SOFT_TARGET_IS_PUBLIC(Kind.WARNING),
        
        /** &#64;Mixin annotation doesn't specify any targets */
        MIXIN_NO_TARGETS(Kind.ERROR),
        
        /** Errors raised by the parent validator */
        PARENT_VALIDATOR(Kind.ERROR),

        /** Errors raised by the target validator */
        TARGET_VALIDATOR(Kind.ERROR),
        
        /** Unexpected error attaching accessor/invoker */
        ACCESSOR_ATTACH_ERROR(Kind.ERROR),

        /** Accessor target could not be found */
        ACCESSOR_TARGET_NOT_FOUND(Kind.ERROR),

        /** The AP was unable to determine the accessor type for some reason */
        ACCESSOR_TYPE_UNSUPPORTED(Kind.WARNING),

        /**
         * The AP was unable to determine the accessor name either from the
         * annotation or using the inflector on the annoated method name 
         */
        ACCESSOR_NAME_UNRESOLVED(Kind.WARNING),
 
        /** Invoker uses a raw return type when the return type is generic */
        INVOKER_RAW_RETURN_TYPE(Kind.WARNING),
        
        /** Generic args of factory invoker incompatible with return type */
        FACTORY_INVOKER_GENERIC_ARGS(Kind.ERROR),

        /** Return type of factory invoker does not match target type */
        FACTORY_INVOKER_RETURN_TYPE(Kind.ERROR),
        
        /** Factory invoker is not declared as static */
        FACTORY_INVOKER_NONSTATIC(Kind.ERROR),
        
        /** Constraint violation with supplied tokens */
        CONSTRAINT_VIOLATION(Kind.ERROR),

        /** Constraint expression is not valid */
        INVALID_CONSTRAINT(Kind.WARNING),

        /** Mapping conflict encountered on accessor method */
        ACCESSOR_MAPPING_CONFLICT(Kind.ERROR),
        
        /** Mapping conflict encountered on injector method */
        INJECTOR_MAPPING_CONFLICT(Kind.ERROR),
        
        /** Mapping conflict encountered on overwrite method */
        OVERWRITE_MAPPING_CONFLICT(Kind.ERROR),
        
        /** Mapping conflict encountered on shadow method */
        SHADOW_MAPPING_CONFLICT(Kind.ERROR),
        
        /** Injector-annotated method encounterd in an interface */
        INJECTOR_IN_INTERFACE(Kind.ERROR),
        
        /** Injector annotation encountered on element which is not a method */
        INJECTOR_ON_NON_METHOD_ELEMENT(Kind.WARNING),

        /** Overwrite annotation encountered on element which is not a method */
        OVERWRITE_ON_NON_METHOD_ELEMENT(Kind.ERROR),

        /** Accessor annotation encountered on element which is not a method */
        ACCESSOR_ON_NON_METHOD_ELEMENT(Kind.ERROR),

        /** Shadow annotation encountered on non- field or method element */
        SHADOW_ON_INVALID_ELEMENT(Kind.ERROR),

        /** Injector annotation encountered on method which isn't in a mixin */
        INJECTOR_ON_NON_MIXIN_METHOD(Kind.ERROR),

        /** Overwrite annotation encountered on method which isn't in a mixin */
        OVERWRITE_ON_NON_MIXIN_METHOD(Kind.ERROR),

        /** Accessor annotation encountered on method which isn't in a mixin */
        ACCESSOR_ON_NON_MIXIN_METHOD(Kind.ERROR),

        /** Shadow annotation encountered on element which isn't in a mixin */
        SHADOW_ON_NON_MIXIN_ELEMENT(Kind.ERROR),
        
        /** &#64;Implements annotation on a non- class or interface element */
        SOFT_IMPLEMENTS_ON_INVALID_TYPE(Kind.ERROR),

        /** &#64;Implements annotation on a non-mixin class or interface */
        SOFT_IMPLEMENTS_ON_NON_MIXIN(Kind.ERROR),
        
        /** &#64;Implements annotation has no values */
        SOFT_IMPLEMENTS_EMPTY(Kind.WARNING),

        /** A target selector failed validation */
        TARGET_SELECTOR_VALIDATION(Kind.ERROR),
        
        /** An injection point target is missing owner, descriptor, or both */
        INJECTOR_TARGET_NOT_FULLY_QUALIFIED(Kind.ERROR),
        
        /**
         * A descriptor is missing on an injector target in a multi-target mixin
         */
        MISSING_INJECTOR_DESC_MULTITARGET(Kind.ERROR),
        
        /**
         * A descriptor is missing on an injector target in a single-target
         * mixin but enclosing type information is unavailable (imaginary) or
         * could not be determined from the target class.
         */
        MISSING_INJECTOR_DESC_SINGLETARGET(Kind.WARNING),
        
        /**
         * A descriptor is missing on an injector target in an &#64;Pseudo mixin
         */
        MISSING_INJECTOR_DESC_SIMULATED(Kind.OTHER),
        
        /**
         * The target element of an injection could not be located in the target
         */
        TARGET_ELEMENT_NOT_FOUND(Kind.WARNING),
        
        /** Mismatched visibility of overwrite with resolved target, warn */
        METHOD_VISIBILITY(Kind.WARNING),
        
        /** Obf provider did not return a result for the accessor target */
        NO_OBFDATA_FOR_ACCESSOR(Kind.WARNING),

        /** Obf provider did not return a result for the specified class */
        NO_OBFDATA_FOR_CLASS(Kind.WARNING),
        
        /**
         * Obf provider did not return a result for the injector target and the
         * target is a normal method  
         */
        NO_OBFDATA_FOR_TARGET(Kind.ERROR),
        
        /**
         * Obf provider did not return a result for the injector target but the
         * target is a constructor, warn
         */
        NO_OBFDATA_FOR_CTOR(Kind.WARNING),

        /**
         * Obf provider did not return a result for the specified overwrite and
         * the overwrite target is an instance method
         */
        NO_OBFDATA_FOR_OVERWRITE(Kind.ERROR),

        /**
         * Obf provider did not return a result for the specified overwrite but
         * the overwrite target is static, warn
         */
        NO_OBFDATA_FOR_STATIC_OVERWRITE(Kind.WARNING),
        
        /** Obf provider did not return a result for the specified field */
        NO_OBFDATA_FOR_FIELD(Kind.WARNING),

        /** Obf provider did not return a result for the specified method */
        NO_OBFDATA_FOR_METHOD(Kind.WARNING),
        
        /** Obf provider did not return a result for the shadow target */
        NO_OBFDATA_FOR_SHADOW(Kind.WARNING),

        /**
         * Obf provider did not return a result for the shadow target but the
         * enclosing type is simulated, warn
         */
        NO_OBFDATA_FOR_SIMULATED_SHADOW(Kind.WARNING),
        
        /**
         * Obf provider did not return a result for the specified
         * soft-implemented method
         */
        NO_OBFDATA_FOR_SOFT_IMPLEMENTS(Kind.ERROR),
                
        /**
         * A method in a multi-target mixin has conflicting descriptors so the
         * AP is storing the bare reference (name only) in the refmap, which has
         * resolvability issues.
         */
        BARE_REFERENCE(Kind.WARNING),
        
        /**
         * All warnings related to overwrite javadoc contents, can be upgrade to
         * error by {@link SupportedOptions#OVERWRITE_ERROR_LEVEL}
         */
        OVERWRITE_DOCS(Kind.WARNING);
        
        /**
         * Prefix applied to supported commandline options for message levels,
         * eg. <tt>INVALID_CONSTRAINT</tt> becomes
         * <tt>MSG_INVALID_CONSTRAINT</tt>
         */
        private static final String OPTION_PREFIX = "MSG_";

        /**
         * Development option, enable decoration of messages to make it easier
         * to determine message origins.
         */
        private static boolean decorate = false;
        
        /**
         * Global option, prefix for all annotation processor messages to make
         * them easier to differentiate from other messages 
         */
        private static String prefix = "";

        /**
         * The original message kind 
         */
        private final Kind originalKind;
        
        /**
         * The current message kind
         */
        private Kind kind;
        
        /**
         * Whether this message is globally enabled or disabled
         */
        private boolean enabled = true;
        
        /**
         * Whether this message kind has been specified manually by the user
         */
        private boolean setByUser = false;
        
        private MessageType(Kind kind) {
            this.originalKind = this.kind = kind;
        }
        
        /**
         * Get whether the message is treated as an error
         */
        public boolean isError() {
            return this.kind == Kind.ERROR;
        }
        
        /**
         * Get the current message kind
         */
        public Kind getKind() {
            return this.kind;
        }
        
        /**
         * Set the current kind
         * 
         * @param kind kind to set
         */
        public void setKind(Kind kind) {
            this.kind = kind;
            this.setByUser = true;
        }
        
        /**
         * Set the current kind to a lower kind, but only if the level has not
         * been set manually by the user. Used when an IDE is detected to
         * selectively lower the severity of some messages but we don't want to
         * override anything manually specified by the user. 
         * 
         * @param kind kind to set
         */
        public void quench(Kind kind) {
            if (!this.setByUser && kind.ordinal() > this.kind.ordinal()) {
                this.kind = kind;
            }
        }
        
        /**
         * Get whether this message type is enabled
         */
        public boolean isEnabled() {
            return this.enabled;
        }
        
        /**
         * Set whether this message type is enabled
         * 
         * @param enabled true to enable the message
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
            this.setByUser = true;
        }
        
        /**
         * Reset the message kind to the original value and re-enable the
         * message if it was disabled
         */
        public void reset() {
            this.kind = this.originalKind;
            this.enabled = true;
            this.setByUser = false;
        }
        
        /**
         * Called when the message is passed to a real Messager, allows the
         * message to be decorated in development and other special situations
         * such as inside an IDE 
         * 
         * @param msg Message to decorate
         * @return Decorated message
         */
        public CharSequence decorate(CharSequence msg) {
            return MessageType.decorate ? String.format("%s[%s] %s", MessageType.prefix, this.name(), msg) : MessageType.prefix + msg;
        }
        
        /**
         * Set whether development decoration of messages is enabled
         * 
         * @param enabled true to enable dev decorations
         */
        public static void setDecoration(boolean enabled) {
            MessageType.decorate = enabled;
        }
        
        /**
         * Set the prefix for all messages, used to distinguish mixin AP
         * messages from other compiler messages in IDE for example
         * 
         * @param prefix Prefix to apply to all messages (include trailing
         *      space)
         */
        public static void setPrefix(String prefix) {
            MessageType.prefix = prefix;
        }

        /**
         * Get all the available options for configuring message levels
         */
        public static Set<String> getSupportedOptions() {
            Set<String> supportedOptions = new HashSet<String>();
            for (MessageType type : MessageType.values()) {
                supportedOptions.add(MessageType.OPTION_PREFIX + type.name());
            }
            return supportedOptions;
        }
        
        /**
         * Apply options supplied to the AP which configure individual message
         * levels
         * 
         * @param env Compiler environment
         * @param options Options to apply
         */
        public static void applyOptions(CompilerEnvironment env, IOptionProvider options) {
            // Enable decorations if the option is specified
            MessageType.setDecoration("true".equalsIgnoreCase(options.getOption(SupportedOptions.SHOW_MESSAGE_TYPES)));
            
            // Apply the "quiet" option if specified, or if in dev env
            MessageType.INFO.setEnabled(!(env.isDevelopmentEnvironment() || "true".equalsIgnoreCase(options.getOption(SupportedOptions.QUIET))));
            
            // Legacy option
            if ("error".equalsIgnoreCase(options.getOption(SupportedOptions.OVERWRITE_ERROR_LEVEL))) {
                MessageType.OVERWRITE_DOCS.setKind(Kind.ERROR);
            }

            // Apply all other custom message levels
            for (MessageType type : MessageType.values()) {
                String option = options.getOption(MessageType.OPTION_PREFIX + type.name());
                if (option == null) {
                    continue;
                }
                
                if ("note".equalsIgnoreCase(option)) {
                    type.setKind(Kind.NOTE);
                } else if ("warning".equalsIgnoreCase(option)) {
                    type.setKind(Kind.WARNING);
                } else if ("error".equalsIgnoreCase(option)) {
                    type.setKind(Kind.ERROR);
                } else if ("disabled".equalsIgnoreCase(option)) {
                    type.setEnabled(false);
                }
            }
        }
        
    }

    /**
     * Prints a message of the specified kind.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     */
    void printMessage(MessageType type, CharSequence msg);

    /**
     * Prints a message of the specified kind at the location of the
     * element.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     * @param e the element to use as a position hint
     */
    void printMessage(MessageType type, CharSequence msg, Element e);

    /**
     * Prints a message of the specified kind at the location of the
     * annotation mirror of the annotated element.
     *
     * @param type the message type
     * @param msg the message, or an empty string if none
     * @param e the annotated element
     * @param a the annotation to use as a position hint
     */
    void printMessage(MessageType type, CharSequence msg, Element e, AnnotationMirror a);

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
     */
    void printMessage(MessageType type, CharSequence msg, Element e, AnnotationMirror a, AnnotationValue v);

}
