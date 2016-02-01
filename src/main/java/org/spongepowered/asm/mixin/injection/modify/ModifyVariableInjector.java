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
package org.spongepowered.asm.mixin.injection.modify;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.InvalidInjectionException;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.modify.ModifyVariableInjector.Context.Local;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * A bytecode injector which allows a single argument of a chosen method call to
 * be altered.
 */
public class ModifyVariableInjector extends Injector {
    
    class Context extends InsnList {
        
        class Local {
            
            int ord = 0;
            String name;
            Type type;

            public Local(String name, Type type) {
                this.name = name;
                this.type = type;
            }
            
            @Override
            public String toString() {
                return String.format("Local[ordinal=%d, name=%s, type=%s]", this.ord, this.name, this.type);
            }
            
        }
        
        final Target target;
        final AbstractInsnNode node;
        final int baseArgIndex;
        final Local[] locals;
        
        private final ClassNode targetClass;
        private final boolean isStaticHandler;

        public Context(ClassNode targetClass, boolean isStaticHandler, Target target, AbstractInsnNode node, boolean argsOnly) {
            this.targetClass = targetClass;
            this.isStaticHandler = isStaticHandler;
            this.target = target;
            this.node = node;
            this.baseArgIndex = this.isStaticHandler ? 0 : 1;
            this.locals = this.initLocals(target, node, argsOnly);
            this.initOrdinals();
        }

        private Local[] initLocals(Target target, AbstractInsnNode node, boolean argsOnly) {
            if (!argsOnly) {
                LocalVariableNode[] locals = Locals.getLocalsAt(this.targetClass, target.method, node);
                if (locals != null) {
                    Local[] lvt = new Local[locals.length];
                    for (int l = 0; l < locals.length; l++) {
                        if (locals[l] != null) {
                            lvt[l] = new Local(locals[l].name, Type.getType(locals[l].desc));
                        }
                    }
                    return lvt;
                }
            }
            
            Local[] lvt = new Local[this.baseArgIndex + target.arguments.length];
            if (!this.isStaticHandler) {
                lvt[0] = new Local("this", Type.getType(this.targetClass.name));
            }
            for (int local = this.baseArgIndex; local < lvt.length; local++) {
                Type arg = target.arguments[local - this.baseArgIndex];
                lvt[local] = new Local("arg" + local, arg);
            }
            return lvt;
        }
        
        private void initOrdinals() {
            Map<Type, Integer> ordinalMap = new HashMap<Type, Integer>();
            for (int l = 0; l < this.locals.length; l++) {
                Integer ordinal = Integer.valueOf(0);
                if (this.locals[l] != null) {
                    ordinal = ordinalMap.get(this.locals[l].type);
                    ordinalMap.put(this.locals[l].type, ordinal = Integer.valueOf(ordinal == null ? 0 : ordinal.intValue() + 1));
                    this.locals[l].ord = ordinal.intValue();
                }
            }
        }

    }

    /**
     * Print LVT 
     */
    private final boolean print;
    
    /**
     * True to consider only method args
     */
    private final boolean argsOnly;
    
    /**
     * Ordinal of the target variable or -1 to fail over to {@link index}
     */
    private final int ordinal;

    /**
     * Ordinal of the target variable or -1 to fail over to {@link names}
     */
    private final int index;
    
    /**
     * Candidate names for the local variable, if empty fails over to matching
     * single local by type
     */
    private final Set<String> names;

    /**
     * @param info Injection info
     * @param print
     * @param argsOnly 
     * @param ordinal target variable ordinal
     * @param index target variable index
     * @param names target variable names
     */
    public ModifyVariableInjector(InjectionInfo info, boolean print, boolean argsOnly, int ordinal, int index, Set<String> names) {
        super(info);
        
        this.print = print;
        this.argsOnly = argsOnly;
        this.ordinal = ordinal;
        this.index = index;
        this.names = names;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.callback.BytecodeInjector
     *      #sanityCheck(org.spongepowered.asm.mixin.injection.callback.Target,
     *      java.util.List)
     */
    @Override
    protected void sanityCheck(Target target, List<InjectionPoint> injectionPoints) {
        super.sanityCheck(target, injectionPoints);
        
        if (target.isStatic != this.isStatic) {
            throw new InvalidInjectionException(this.info, "'static' of variable modifier method does not match target in " + this.methodNode.name);
        }
        
        if (this.ordinal < -1) {
            throw new InvalidInjectionException(this.info, "Invalid ordinal " + this.ordinal + " specified in " + this.methodNode.name);
        }
        
        if (this.index == 0 && !this.isStatic) {
            throw new InvalidInjectionException(this.info, "Invalid index 0 specified in non-static variable modifier " + this.methodNode.name);
        }
    }
    
    /**
     * If the user specifies no values for {@link ordinal}, {@link index} or 
     * {@link names} then we are considered to be operating in "implicit mode"
     * where only a single local variable of the specified type is expected to
     * exist.
     * 
     * @param context Target context
     * @return true if operating in implicit mode
     */
    protected boolean isImplicitMode(final Context context) {
        return this.ordinal < 0 && this.index < context.baseArgIndex && this.names.isEmpty();
    }
    
    /**
     * Do the injection
     */
    @Override
    protected void inject(Target target, AbstractInsnNode node) {
        Context context = new Context(this.classNode, this.isStatic, target, node, this.argsOnly);
        
        if (this.print) {
            this.printLocals(context);
        }

        int local = this.findLocal(context);
        if (local > -1) {
            this.inject(context, local);
        }
        
        target.insns.insertBefore(node, context);
        target.addToStack(this.isStatic ? 1 : 2);
    }

    /**
     * Pretty-print local variable information to stderr
     */
    private void printLocals(final Context context) {
        SignaturePrinter handlerSig = new SignaturePrinter(this.methodNode.name, this.returnType, this.methodArgs, new String[] { "var" });
        handlerSig.setModifiers(this.methodNode);

        PrettyPrinter printer = new PrettyPrinter();
        printer.add("%20s : %s", "Target Class", this.classNode.name.replace('/', '.'));
        printer.add("%20s : %s", "Target Method", context.target.method.name);
        printer.add("%20s : %s", "Callback Name", this.methodNode.name);
        printer.add("%20s : %s", "Capture Type", SignaturePrinter.getTypeName(this.returnType, false));
        printer.add("%20s : %s %s", "Instruction", context.node.getClass().getSimpleName(), ASMHelper.getOpcodeName(context.node.getOpcode())).hr();
        printer.add("%20s : %s", "Match mode", this.isImplicitMode(context) ? "IMPLICIT (match single)" : "EXPLICIT (match by criteria)");
        printer.add("%20s : %s", "Match ordinal", this.ordinal < 0 ? "any" : this.ordinal);
        printer.add("%20s : %s", "Match index", this.index < context.baseArgIndex ? "any" : this.index);
        printer.add("%20s : %s", "Match name(s)", this.names.isEmpty() ? "any" : this.names);
        printer.add("%20s : %s", "Args only", this.argsOnly).hr();
        printer.add("%5s  %7s  %20s  %-50s  %s", "INDEX", "ORDINAL", "TYPE", "NAME", "CANDIDATE");
        for (int l = context.baseArgIndex; l < context.locals.length; l++) {
            Local local = context.locals[l];
            if (local != null) {
                Type localType = local.type;
                String localName = local.name;
                int ordinal = local.ord;
                String candidate = this.returnType.equals(localType) ? "YES" : "-";
                printer.add("[%3d]    [%3d]  %20s  %-50s  %s", l, ordinal, SignaturePrinter.getTypeName(localType, false), localName, candidate);
            } else if (l > 0) {
                Local prevLocal = context.locals[l - 1];
                boolean isTop = prevLocal != null && prevLocal.type != null && prevLocal.type.getSize() > 1;
                printer.add("[%3d]           %20s", l, isTop ? "<top>" : "-");
            }
        }
        printer.print(System.err);
    }
    
    private void inject(final Context context, final int local) {
        if (!this.isStatic) {
            context.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        
        context.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ILOAD), local));
        this.invokeHandler(context);
        context.add(new VarInsnNode(this.returnType.getOpcode(Opcodes.ISTORE), local));
    }

    private int findLocal(Context context) {
        if (this.isImplicitMode(context)) {
            return this.findImplicitLocal(context);
        }
        return this.findExplicitLocal(context);
    }

    private int findImplicitLocal(final Context context) {
        int found = 0;
        int count = 0;
        for (int index = context.baseArgIndex; index < context.locals.length; index++) {
            Local local = context.locals[index];
            if (local == null || !local.type.equals(this.returnType)) {
                continue;
            }
            count++;
            found = index;
        }
        
        if (count == 1) {
            return found;
        }

        throw new InvalidInjectionException(this.info, "Implicit variable modifier injection failed in " + this.methodNode.name + ", found "
                + count + " candidate variables but exactly 1 is required.");
    }

    private int findExplicitLocal(final Context context) {
        for (int index = context.baseArgIndex; index < context.locals.length; index++) {
            Local local = context.locals[index];
            if (local == null || !local.type.equals(this.returnType)) {
                continue;
            }
            if (this.ordinal > -1) {
                if (this.ordinal == local.ord) {
                    return index;
                }
                continue;
            }
            if (this.index >= context.baseArgIndex) {
                if (this.index == index) {
                    return index;
                }
                continue;
            }
            if (this.names.contains(local.name)) {
                return index;
            }
        }
        
        return -1;
    }

}
