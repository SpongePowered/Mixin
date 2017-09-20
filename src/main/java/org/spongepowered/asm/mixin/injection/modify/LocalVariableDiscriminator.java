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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.mixin.injection.modify.LocalVariableDiscriminator.Context.Local;
import org.spongepowered.asm.mixin.injection.struct.Target;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Locals;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * Encapsulates logic for identifying a local variable in a target method using
 * 3 criteria: <em>ordinal</em>, <em>index</em> and <em>name</em>. This is used
 * by the {@link ModifyVariableInjector} and its associated injection points.
 */
public class LocalVariableDiscriminator {
    
    /**
     * Discriminator context information, wraps all relevant information about
     * a target location for use when performing discrimination
     */
    public static class Context implements org.spongepowered.asm.util.PrettyPrinter.IPrettyPrintable {
        
        /**
         * Information about a local variable in the LVT, used during
         * discrimination
         */
        public class Local {
            
            /**
             * Ordinal value of this local variable type 
             */
            int ord = 0;
            
            /**
             * Local variable name 
             */
            String name;
            
            /**
             * Local variable type 
             */
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
        
        /**
         * Target method for this context
         */
        final Target target;
        
        /**
         * The return type of the handler in question, also the type of the
         * local variable that we care about 
         */
        final Type returnType;
        
        /**
         * Injection point 
         */
        final AbstractInsnNode node;
        
        /**
         * Base argument index, for static methods this is 0, for instance
         * methods this is 1
         */
        final int baseArgIndex;
        
        /**
         * Enumerated locals in this context
         */
        final Local[] locals;
        
        /**
         * True if the handler (and target) are static 
         */
        private final boolean isStatic;

        public Context(Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
            this.isStatic = Bytecode.methodIsStatic(target.method);
            this.returnType = returnType;
            this.target = target;
            this.node = node;
            this.baseArgIndex = this.isStatic ? 0 : 1;
            this.locals = this.initLocals(target, argsOnly, node);
            this.initOrdinals();
        }

        private Local[] initLocals(Target target, boolean argsOnly, AbstractInsnNode node) {
            if (!argsOnly) {
                LocalVariableNode[] locals = Locals.getLocalsAt(target.classNode, target.method, node);
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
            if (!this.isStatic) {
                lvt[0] = new Local("this", Type.getType(target.classNode.name));
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

        @Override
        public void print(PrettyPrinter printer) {
            printer.add("%5s  %7s  %30s  %-50s  %s", "INDEX", "ORDINAL", "TYPE", "NAME", "CANDIDATE");
            for (int l = this.baseArgIndex; l < this.locals.length; l++) {
                Local local = this.locals[l];
                if (local != null) {
                    Type localType = local.type;
                    String localName = local.name;
                    int ordinal = local.ord;
                    String candidate = this.returnType.equals(localType) ? "YES" : "-";
                    printer.add("[%3d]    [%3d]  %30s  %-50s  %s", l, ordinal, SignaturePrinter.getTypeName(localType, false), localName, candidate);
                } else if (l > 0) {
                    Local prevLocal = this.locals[l - 1];
                    boolean isTop = prevLocal != null && prevLocal.type != null && prevLocal.type.getSize() > 1;
                    printer.add("[%3d]           %30s", l, isTop ? "<top>" : "-");
                }
            }
        }

    }
        
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
     * True to request print of the LVT 
     */
    private final boolean print;

    /**
     * @param argsOnly true to only search within the method arguments
     * @param ordinal target variable ordinal
     * @param index target variable index
     * @param names target variable names
     * @param print true to print lvt
     */
    public LocalVariableDiscriminator(boolean argsOnly, int ordinal, int index, Set<String> names, boolean print) {
        this.argsOnly = argsOnly;
        this.ordinal = ordinal;
        this.index = index;
        this.names = Collections.<String>unmodifiableSet(names);
        this.print = print;
    }
    
    /**
     * True if this discriminator will examine only the target method args and
     * won't consider the rest of the LVT at the target location
     */
    public boolean isArgsOnly() {
        return this.argsOnly;
    }
    
    /**
     * Get the local variable ordinal (nth variable of type)
     */
    public int getOrdinal() {
        return this.ordinal;
    }
    
    /**
     * Get the local variable absolute index
     */
    public int getIndex() {
        return this.index;
    }
    
    /**
     * Get valid names for consideration
     */
    public Set<String> getNames() {
        return this.names;
    }

    /**
     * Returns true if names is not empty
     */
    public boolean hasNames() {
        return !this.names.isEmpty();
    }

    /**
     * True if the injector should print the LVT
     */
    public boolean printLVT() {
        return this.print;
    }

    /**
     * If the user specifies no values for <tt>ordinal</tt>, <tt>index</tt> or 
     * <tt>names</tt> then we are considered to be operating in "implicit mode"
     * where only a single local variable of the specified type is expected to
     * exist.
     * 
     * @param context Target context
     * @return true if operating in implicit mode
     */
    protected boolean isImplicit(final Context context) {
        return this.ordinal < 0 && this.index < context.baseArgIndex && this.names.isEmpty();
    }

    /**
     * Find a matching local variable in the specified target 
     * 
     * @param returnType variable tyoe
     * @param argsOnly only match in the method args
     * @param target target method
     * @param node current instruction
     * @return index of local or -1 if not matched
     */
    public int findLocal(Type returnType, boolean argsOnly, Target target, AbstractInsnNode node) {
        try {
            return this.findLocal(new Context(returnType, argsOnly, target, node));
        } catch (InvalidImplicitDiscriminatorException ex) {
            return -2;
        }
    }
    
    /**
     * Find a local variable for the specified context
     * 
     * @param context search context
     * @return index of local or -1 if not found
     */
    public int findLocal(Context context) {
        if (this.isImplicit(context)) {
            return this.findImplicitLocal(context);
        }
        return this.findExplicitLocal(context);
    }

    /**
     * Find an implicit local, this means that we expect exactly 1 variable with
     * the specified type in scope, if more than one is found then we throw an
     * {@link InvalidImplicitDiscriminatorException}
     * 
     * @param context search context
     * @return local variable index
     */
    private int findImplicitLocal(final Context context) {
        int found = 0;
        int count = 0;
        for (int index = context.baseArgIndex; index < context.locals.length; index++) {
            Local local = context.locals[index];
            if (local == null || !local.type.equals(context.returnType)) {
                continue;
            }
            count++;
            found = index;
        }
        
        if (count == 1) {
            return found;
        }

        throw new InvalidImplicitDiscriminatorException("Found " + count + " candidate variables but exactly 1 is required.");
    }

    /**
     * Find an explicit local variable in the local variable table. Returns -1
     * if no variables match the discriminator
     * 
     * @param context search context
     * @return variable index or -1 if not found
     */
    private int findExplicitLocal(final Context context) {
        for (int index = context.baseArgIndex; index < context.locals.length; index++) {
            Local local = context.locals[index];
            if (local == null || !local.type.equals(context.returnType)) {
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
    
    /**
     * Parse a local variable discriminator from the supplied annotation
     * 
     * @param annotation annotation to parse
     * @return discriminator configured using values from the annoation
     */
    public static LocalVariableDiscriminator parse(AnnotationNode annotation) {
        boolean argsOnly = Annotations.<Boolean>getValue(annotation, "argsOnly", Boolean.FALSE).booleanValue();
        int ordinal = Annotations.<Integer>getValue(annotation, "ordinal", -1);
        int index = Annotations.<Integer>getValue(annotation, "index", -1);
        boolean print = Annotations.<Boolean>getValue(annotation, "print", Boolean.FALSE).booleanValue();
        
        Set<String> names = new HashSet<String>();
        List<String> namesList = Annotations.<List<String>>getValue(annotation, "name", (List<String>)null);
        if (namesList != null) {
            names.addAll(namesList);
        }
        
        return new LocalVariableDiscriminator(argsOnly, ordinal, index, names, print);
    }

}
