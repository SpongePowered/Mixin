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
package org.spongepowered.asm.mixin.injection.invoke;

import java.util.Arrays;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;

import com.google.common.collect.ObjectArrays;

/**
 * Base class for injectors which inject at method invokes
 */
public abstract class InvokeInjector extends Injector {
    
    /**
     * Redirection data bundle base. No this isn't meant to be pretty, it's a
     * way of passing a bunch of state around the injector without having dumb
     * method signatures.
     */
    static class InjectorData {

        /**
         * Redirect target 
         */
        final Target target;
        
        /**
         * Mutable description. The bundle is be passed to different types of
         * handler and the handler decorates the bundle with a description of
         * the <em>type</em> of injection, purely for use in error messages.
         */
        String description;
        
        /**
         * When passing through {@link RedirectInjector#validateParams} this
         * switch determines whether coercion is supported for both the primary
         * handler args and captured target args, or for target args only.
         */
        boolean allowCoerceArgs;
        
        /**
         * Number of arguments to capture from the target, determined by the
         * number of extra args on the handler method 
         */
        int captureTargetArgs = 0;
        
        /**
         * True if the method itself is decorated with {@link Coerce} and the
         * return type is coerced. Instructs the injector to add a CHECKCAST
         * following the handler.  
         */
        boolean coerceReturnType = false;
        
        InjectorData(Target target) {
            this(target, "handler");
        }
        
        InjectorData(Target target, String description) {
            this(target, description, true);
        }
        
        InjectorData(Target target, String description, boolean allowCoerceArgs) {
            this.target = target;
            this.description = description;
            this.allowCoerceArgs = allowCoerceArgs;
        }
        
        @Override
        public String toString() {
            return this.description;
        }

    }

    /**
     * @param info Information about this injection
     * @param annotationType Annotation type, used for error messages
     */
    public InvokeInjector(InjectionInfo info, String annotationType) {
        super(info, annotationType);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        this.checkTarget(target);
    }

    /**
     * Sanity checks on target
     * 
     * @param target target
     */
    protected void checkTarget(Target target) {
        this.checkTargetModifiers(target, true);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #inject(org.spongepowered.asm.mixin.injection.callback.Target,
     *      org.objectweb.asm.tree.AbstractInsnNode)
     */
    @Override
    protected void inject(Target target, InjectionNode node) {
        if (!(node.getCurrentTarget() instanceof MethodInsnNode)) {
            throw new InvalidInjectionException(this.info, String.format("%s annotation on is targetting a non-method insn in %s in %s",
                    this.annotationType, target, this));
        }
        
        this.injectAtInvoke(target, node);
    }
    
    /**
     * Perform a single injection
     * 
     * @param target Target to inject into
     * @param node Discovered instruction node 
     */
    protected abstract void injectAtInvoke(Target target, InjectionNode node);

    /**
     * @param args handler arguments
     * @param insns InsnList to inject insns into
     * @param argMap Mapping of args to local variables
     * @return injected insn node
     */
    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap) {
        return this.invokeHandlerWithArgs(args, insns, argMap, 0, args.length);
    }
    
    /**
     * @param args handler arguments
     * @param insns InsnList to inject insns into
     * @param argMap Mapping of args to local variables
     * @param startArg Starting arg to consume
     * @param endArg Ending arg to consume
     * @return injected insn node
     */
    protected AbstractInsnNode invokeHandlerWithArgs(Type[] args, InsnList insns, int[] argMap, int startArg, int endArg) {
        if (!this.isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        this.pushArgs(args, insns, argMap, startArg, endArg);
        return this.invokeHandler(insns);
    }

    /**
     * Store args on the stack starting at the end and working back to position
     * specified by start, return the generated argMap
     * 
     * @param target target method
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param start Starting index
     * @return the generated argmap
     */
    protected int[] storeArgs(Target target, Type[] args, InsnList insns, int start) {
        int[] argMap = target.generateArgMap(args, start);
        this.storeArgs(args, insns, argMap, start, args.length);
        return argMap;
    }

    /**
     * Store args on the stack to their positions allocated based on argMap
     * 
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param argMap generated argmap containing local indices for all args
     * @param start Starting index
     * @param end Ending index
     */
    protected void storeArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        for (int arg = end - 1; arg >= start; arg--) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ISTORE), argMap[arg]));
        }
    }

    /**
     * Load args onto the stack from their positions allocated in argMap
     * 
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param argMap generated argmap containing local indices for all args
     * @param start Starting index
     * @param end Ending index
     */
    protected void pushArgs(Type[] args, InsnList insns, int[] argMap, int start, int end) {
        this.pushArgs(args, insns, argMap, start, end, null);
    }
    
    /**
     * Load args onto the stack from their positions allocated in argMap
     * 
     * @param args argument types
     * @param insns instruction list to generate insns into
     * @param argMap generated argmap containing local indices for all args
     * @param start Starting index
     * @param end Ending index
     */
    protected void pushArgs(Type[] args, InsnList insns, int[] argMap, int start, int end, Extension extension) {
        for (int arg = start; arg < end && arg < args.length; arg++) {
            insns.add(new VarInsnNode(args[arg].getOpcode(Opcodes.ILOAD), argMap[arg]));
            if (extension != null) {
                extension.add(args[arg].getSize());
            }
        }
    }

    /**
     * Collects all the logic from old validateParams/checkDescriptor so that we
     * can consistently apply coercion logic to method params, and also provide
     * more detailed errors when something doesn't line up.
     * 
     * <p>The supplied return type and argument list will be verified first. Any
     * arguments on the handler beyond the base arguments consume arguments from
     * the target. The flag <tt>allowCoerceArgs</tt> on the <tt>redirect</tt>
     * instance determines whether coercion is supported for the base args and
     * return type, coercion is always allowed for captured target args.</p>
     * 
     * <p>Following validation, the <tt>captureTargetArgs</tt> and
     * <tt>coerceReturnType</tt> values will be set on the bundle and the
     * calling injector function should adjust itself accordingly.</p>
     * 
     * @param injector Data bundle for the injector
     * @param returnType Return type for the handler, must not be null
     * @param args Array of handler args, must not be null
     */
    protected final void validateParams(InjectorData injector, Type returnType, Type... args) {
        String description = String.format("%s %s method %s from %s", this.annotationType, injector, this, this.info.getContext());
        int argIndex = 0;
        try {
            injector.coerceReturnType = this.checkCoerce(-1, returnType, description, injector.allowCoerceArgs);
            
            for (Type arg : args) {
                if (arg != null) {
                    this.checkCoerce(argIndex, arg, description, injector.allowCoerceArgs);
                    argIndex++;
                }
            }
            
            if (argIndex == this.methodArgs.length) {
                return;
            }
            
            for (int targetArg = 0; targetArg < injector.target.arguments.length && argIndex < this.methodArgs.length; targetArg++, argIndex++) {
                this.checkCoerce(argIndex, injector.target.arguments[targetArg], description, true);
                injector.captureTargetArgs++;
            }
        } catch (InvalidInjectionException ex) {
            String expected = this.methodArgs.length > args.length
                    ? Bytecode.generateDescriptor(returnType, ObjectArrays.concat(args, injector.target.arguments, Type.class))
                    : Bytecode.generateDescriptor(returnType, args);
            throw new InvalidInjectionException(this.info, String.format("%s. Handler signature: %s Expected signature: %s", ex.getMessage(),
                    this.methodNode.desc, expected));
        }
        
        if (argIndex < this.methodArgs.length) {
            Type[] extraArgs = Arrays.copyOfRange(this.methodArgs, argIndex, this.methodArgs.length);
            throw new InvalidInjectionException(this.info, String.format(
                    "%s has an invalid signature. Found %d unexpected additional method arguments: %s",
                    description, this.methodArgs.length - argIndex, new SignaturePrinter(extraArgs).getFormattedArgs()));
        }
    }
    
    /**
     * Called inside {@link #validateParams} but can also be used directly. This
     * method checks whether the supplied type is compatible with the specified
     * handler argument, apply coercion logic where necessary.
     * 
     * @param index Handler argument index, pass in a negative value (by
     *      convention -1) to specify handler return type
     * @param toType Desired type based on the injector contract
     * @param description human-readable description of the handler method, used
     *      in raised exception
     * @param allowCoercion True if coercion logic can be applied to this
     *      argument, false to only allow a precise match
     * @return <tt>false</tt> if the argument matched perfectly, <tt>true</tt>
     *      if coercion is required for the argument
     */
    protected final boolean checkCoerce(int index, Type toType, String description, boolean allowCoercion) {
        Type fromType = index < 0 ? this.returnType : this.methodArgs[index];
        if (index >= this.methodArgs.length) {
            throw new InvalidInjectionException(this.info, String.format(
                    "%s has an invalid signature. Not enough arguments: expected argument type %s at index %d",
                    description, SignaturePrinter.getTypeName(toType), index));
        }
        
        AnnotationNode coerce = Annotations.getInvisibleParameter(this.methodNode, Coerce.class, index);
        boolean isReturn = index < 0;
        String argType = isReturn ? "return" : "argument";
        Object argIndex = isReturn ? "" : " at index " + index;
        
        if (fromType.equals(toType)) {
            if (coerce != null && this.info.getContext().getOption(Option.DEBUG_VERBOSE)) {
                Injector.logger.info("Possibly-redundant @Coerce on {} {} type{}, {} is identical to {}", description, argType, argIndex,
                        SignaturePrinter.getTypeName(toType), SignaturePrinter.getTypeName(fromType));
            }
            return false;
        }
        
        if (coerce == null || !allowCoercion) {
            String coerceWarning = coerce != null ? ". @Coerce not allowed here" : "";
            throw new InvalidInjectionException(this.info, String.format(
                    "%s has an invalid signature. Found unexpected %s type %s%s, expected %s%s", description, argType,
                    SignaturePrinter.getTypeName(fromType), argIndex, SignaturePrinter.getTypeName(toType), coerceWarning));
        }
        
        boolean canCoerce = Injector.canCoerce(fromType, toType);
        if (!canCoerce) {
            throw new InvalidInjectionException(this.info, String.format(
                    "%s has an invalid signature. Cannot @Coerce %s type %s%s to %s", description, argType,
                    SignaturePrinter.getTypeName(toType), argIndex, SignaturePrinter.getTypeName(fromType)));
        }
        
        return true;
    }

}
