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
package org.spongepowered.asm.mixin.injection.code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InjectionPoint.RestrictTargetLevel;
import org.spongepowered.asm.mixin.injection.invoke.RedirectInjector;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.struct.Target.Extension;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Traversal;
import org.spongepowered.asm.mixin.transformer.ClassInfo.TypeLookup;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.SignaturePrinter;
import org.spongepowered.asm.util.Bytecode.DelegateInitialiser;

import com.google.common.collect.ObjectArrays;

/**
 * Base class for bytecode injectors
 */
public abstract class Injector {

    /**
     * A nominated target node
     */
    public static final class TargetNode {
        
        final AbstractInsnNode insn;
        
        final Set<InjectionPoint> nominators = new HashSet<InjectionPoint>();

        TargetNode(AbstractInsnNode insn) {
            this.insn = insn;
        }
        
        public AbstractInsnNode getNode() {
            return this.insn;
        }
        
        public Set<InjectionPoint> getNominators() {
            return Collections.<InjectionPoint>unmodifiableSet(this.nominators);
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj == null || obj.getClass() != TargetNode.class) {
                return false;
            }
            
            return ((TargetNode)obj).insn == this.insn;
        }
        
        @Override
        public int hashCode() {
            return this.insn.hashCode();
        }
        
    }
    
    /**
     * Redirection data bundle base. No this isn't meant to be pretty, it's a
     * way of passing a bunch of state around the injector without having dumb
     * method signatures.
     */
    public static class InjectorData {

        /**
         * Redirect target 
         */
        public final Target target;
        
        /**
         * Mutable description. The bundle is be passed to different types of
         * handler and the handler decorates the bundle with a description of
         * the <em>type</em> of injection, purely for use in error messages.
         */
        public String description;
        
        /**
         * When passing through {@link RedirectInjector#validateParams} this
         * switch determines whether coercion is supported for both the primary
         * handler args and captured target args, or for target args only.
         */
        public boolean allowCoerceArgs;
        
        /**
         * Number of arguments to capture from the target, determined by the
         * number of extra args on the handler method 
         */
        public int captureTargetArgs = 0;
        
        /**
         * True if the method itself is decorated with {@link Coerce} and the
         * return type is coerced. Instructs the injector to add a CHECKCAST
         * following the handler.  
         */
        public boolean coerceReturnType = false;
        
        public InjectorData(Target target) {
            this(target, "handler");
        }
        
        public InjectorData(Target target, String description) {
            this(target, description, true);
        }
        
        public InjectorData(Target target, String description, boolean allowCoerceArgs) {
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
     * Log more things
     */
    protected static final Logger logger = LogManager.getLogger("mixin");

    /**
     * Injection info
     */
    protected InjectionInfo info;

    /**
     * Annotation type, for use in informational errors 
     */
    protected final String annotationType;
    
    /**
     * Class node
     */
    protected final ClassNode classNode;
    
    /**
     * Callback method 
     */
    protected final MethodNode methodNode;
    
    /**
     * Arguments of the handler method 
     */
    protected final Type[] methodArgs;
    
    /**
     * Return type of the handler method 
     */
    protected final Type returnType;

    /**
     * True if the callback method is static
     */
    protected final boolean isStatic;

    /**
     * Make a new CallbackInjector for the supplied InjectionInfo
     * 
     * @param info Information about this injection
     */
    public Injector(InjectionInfo info, String annotationType) {
        this.info = info;
        this.annotationType = annotationType;

        this.classNode = info.getClassNode();
        this.methodNode = info.getMethod();
        
        this.methodArgs = Type.getArgumentTypes(this.methodNode.desc);
        this.returnType = Type.getReturnType(this.methodNode.desc);
        this.isStatic = Bytecode.isStatic(this.methodNode);
    }
    
    @Override
    public String toString() {
        return String.format("%s::%s", this.classNode.name, this.info.getMethodName());
    }

    /**
     * ...
     * 
     * @param injectorTarget Target method to inject into
     * @param injectionPoints InjectionPoint instances which will identify
     *      target insns in the target method 
     * @return discovered injection points
     */
    public final List<InjectionNode> find(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        this.sanityCheck(injectorTarget.getTarget(), injectionPoints);

        List<InjectionNode> myNodes = new ArrayList<InjectionNode>();
        for (TargetNode node : this.findTargetNodes(injectorTarget, injectionPoints)) {
            this.addTargetNode(injectorTarget.getTarget(), myNodes, node.insn, node.nominators);
        }
        return myNodes;
    }

    protected void addTargetNode(Target target, List<InjectionNode> myNodes, AbstractInsnNode node, Set<InjectionPoint> nominators) {
        myNodes.add(target.addInjectionNode(node));
    }
    
    /**
     * Performs the injection on the specified target
     * 
     * @param target target to inject into
     * @param nodes selected nodes
     */
    public final void inject(Target target, List<InjectionNode> nodes) {
        for (InjectionNode node : nodes) {
            if (node.isRemoved()) {
                if (this.info.getContext().getOption(Option.DEBUG_VERBOSE)) {
                    Injector.logger.warn("Target node for {} was removed by a previous injector in {}", this.info, target);
                }
                continue;
            }
            this.inject(target, node);
        }
        
        for (InjectionNode node : nodes) {
            this.postInject(target, node);
        }
    }

    /**
     * Use the supplied InjectionPoints to find target insns in the target
     * method
     * 
     * @param injectorTarget Target method
     * @param injectionPoints List of injection points parsed from At
     *      annotations on the callback method
     * @return Target insn nodes in the target method
     */
    private Collection<TargetNode> findTargetNodes(InjectorTarget injectorTarget, List<InjectionPoint> injectionPoints) {
        IMixinContext mixin = this.info.getContext();
        MethodNode method = injectorTarget.getMethod();
        Map<Integer, TargetNode> targetNodes = new TreeMap<Integer, TargetNode>();
        Collection<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>(32);
        
        for (InjectionPoint injectionPoint : injectionPoints) {
            nodes.clear();
            
            if (injectorTarget.isMerged()
                    && !mixin.getClassName().equals(injectorTarget.getMergedBy())
                    && !injectionPoint.checkPriority(injectorTarget.getMergedPriority(), mixin.getPriority())) {
                throw new InvalidInjectionException(this.info, String.format(
                        "%s on %s with priority %d cannot inject into %s merged by %s with priority %d", injectionPoint, this, mixin.getPriority(),
                        injectorTarget, injectorTarget.getMergedBy(), injectorTarget.getMergedPriority()));
            }

            if (this.findTargetNodes(method, injectionPoint, injectorTarget.getSlice(injectionPoint), nodes)) {
                for (AbstractInsnNode insn : nodes) {
                    Integer key = method.instructions.indexOf(insn);
                    TargetNode targetNode = targetNodes.get(key);
                    if (targetNode == null) {
                        targetNode = new TargetNode(insn);
                        targetNodes.put(key, targetNode);
                    }
                    targetNode.nominators.add(injectionPoint);
                }
            }
        }
        
        return targetNodes.values();
    }

    protected boolean findTargetNodes(MethodNode into, InjectionPoint injectionPoint, InsnList insns, Collection<AbstractInsnNode> nodes) {
        return injectionPoint.find(into.desc, insns, nodes);
    }

    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        if (target.classNode != this.classNode) {
            throw new InvalidInjectionException(this.info, "Target class does not match injector class in " + this);
        }
    }

    /**
     * Check that the <tt>static</tt> modifier of the target method matches the
     * handler
     * 
     * @param target Target to check
     * @param exactMatch True if static must match, false to only check if an
     *      instance handler is targetting a static method
     */
    protected final void checkTargetModifiers(Target target, boolean exactMatch) {
        if (exactMatch && target.isStatic != this.isStatic) {
            throw new InvalidInjectionException(this.info, String.format("'static' modifier of handler method does not match target in %s", this));
        } else if (!exactMatch && !this.isStatic && target.isStatic) {
            throw new InvalidInjectionException(this.info,
                    String.format("non-static callback method %s targets a static method which is not supported", this));
        }
    }

    /**
     * The normal staticness check is not location-aware, in that it merely
     * enforces static modifiers of handlers to match their targets. For
     * injecting into constructors however (which are ostensibly instance
     * methods) calls which are injected <em>before</em> the call to <tt>
     * super()</tt> cannot access <tt>this</tt> and must therefore be declared
     * as static.
     * 
     * @param target Target method
     * @param node Injection location
     */
    protected void checkTargetForNode(Target target, InjectionNode node, RestrictTargetLevel targetLevel) {
        if (target.isCtor) {
            if (targetLevel == RestrictTargetLevel.METHODS_ONLY) {
                throw new InvalidInjectionException(this.info, String.format("Found %s targetting a constructor in injector %s",
                        this.annotationType, this));
            }
            
            DelegateInitialiser superCall = target.findDelegateInitNode();
            if (!superCall.isPresent) {
                throw new InjectionError(String.format("Delegate constructor lookup failed for %s target on %s", this.annotationType, this.info));
            }
            
            int superCallIndex = target.indexOf(superCall.insn);
            int targetIndex = target.indexOf(node.getCurrentTarget());
            if (targetIndex <= superCallIndex) {
                if (targetLevel == RestrictTargetLevel.CONSTRUCTORS_AFTER_DELEGATE) {
                    throw new InvalidInjectionException(this.info, String.format("Found %s targetting a constructor before %s() in injector %s",
                            this.annotationType, superCall, this));
                }
                
                if (!this.isStatic) {
                    throw new InvalidInjectionException(this.info, String.format("%s handler before %s() invocation must be static in injector %s",
                            this.annotationType, superCall, this));
                }
                return;
            }
        }
        
        this.checkTargetModifiers(target, true);
    }

    protected abstract void inject(Target target, InjectionNode node);

    protected void postInject(Target target, InjectionNode node) {
        // stub
    }

    /**
     * Invoke the handler method
     * 
     * @param insns Instruction list to inject into
     * @return injected insn node
     */
    protected AbstractInsnNode invokeHandler(InsnList insns) {
        return this.invokeHandler(insns, this.methodNode);
    }

    /**
     * Invoke a handler method
     * 
     * @param insns Instruction list to inject into
     * @param handler Actual method to invoke (may be different if using a
     *      surrogate)
     * @return injected insn node
     */
    protected AbstractInsnNode invokeHandler(InsnList insns, MethodNode handler) {
        boolean isPrivate = (handler.access & Opcodes.ACC_PRIVATE) != 0;
        int invokeOpcode = this.isStatic ? Opcodes.INVOKESTATIC : isPrivate ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL;
        MethodInsnNode insn = new MethodInsnNode(invokeOpcode, this.classNode.name, handler.name, handler.desc, false);
        insns.add(insn);
        this.info.addCallbackInvocation(handler);
        return insn;
    }

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

    /**
     * Throw an exception. The exception class must have a string which takes a
     * string argument
     * 
     * @param insns Insn list to inject into
     * @param exceptionType Type of exception to throw (binary name)
     * @param message Message to pass to the exception constructor
     */
    protected void throwException(InsnList insns, String exceptionType, String message) {
        insns.add(new TypeInsnNode(Opcodes.NEW, exceptionType));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new LdcInsnNode(message));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, exceptionType, "<init>", "(Ljava/lang/String;)V", false));
        insns.add(new InsnNode(Opcodes.ATHROW));
    }
    
    /**
     * Returns whether the <tt>from</tt> type can be coerced to the <tt>to</tt>
     * type.
     * 
     * @param from type to coerce from
     * @param to type to coerce to
     * @return true if <tt>from</tt> can be coerced to <tt>to</tt>
     */
    public static boolean canCoerce(Type from, Type to) {
        int fromSort = from.getSort();
        int toSort = to.getSort();
        if (fromSort >= Type.ARRAY && toSort >= Type.ARRAY && fromSort == toSort) {
            if (fromSort == Type.ARRAY && from.getDimensions() != to.getDimensions()) {
                return false;
            }
            return Injector.canCoerce(ClassInfo.forType(from, TypeLookup.ELEMENT_TYPE), ClassInfo.forType(to, TypeLookup.ELEMENT_TYPE));
        }
        
        return Injector.canCoerce(from.getDescriptor(), to.getDescriptor());
    }

    /**
     * Returns whether the <tt>from</tt> type can be coerced to the <tt>to</tt>
     * type.
     * 
     * @param from type to coerce from
     * @param to type to coerce to
     * @return true if <tt>from</tt> can be coerced to <tt>to</tt>
     */
    public static boolean canCoerce(String from, String to) {
        if (from.length() > 1 || to.length() > 1) {
            return false;
        }
        
        return Injector.canCoerce(from.charAt(0), to.charAt(0));
    }

    /**
     * Returns whether the <tt>from</tt> type can be coerced to the <tt>to</tt>
     * type.
     * 
     * @param from type to coerce from
     * @param to type to coerce to
     * @return true if <tt>from</tt> can be coerced to <tt>to</tt>
     */
    public static boolean canCoerce(char from, char to) {
        return to == 'I' && "IBSCZ".indexOf(from) > -1;
    }
    
    /**
     * Returns whether the <tt>from</tt> type can be coerced to the <tt>to</tt>
     * type. This is effectively a superclass check: the check suceeds if <tt>
     * to</tt> is a subclass of <tt>from</tt>.
     * 
     * @param from type to coerce from
     * @param to type to coerce to
     * @return true if <tt>from</tt> can be coerced to <tt>to</tt>
     */
    private static boolean canCoerce(ClassInfo from, ClassInfo to) {
        return from != null && to != null && (to == from || to.hasSuperClass(from, Traversal.ALL, true));
    }

}
