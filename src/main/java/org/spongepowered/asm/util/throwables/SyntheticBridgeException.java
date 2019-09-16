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
package org.spongepowered.asm.util.throwables;

import java.util.Iterator;
import java.util.ListIterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.meta.MixinMerged;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.PrettyPrinter;

/**
 * Exception thrown when comparing two synthetic bridge methods fails, thrown
 * with the corresponding problem and functionality to support debugging when
 * verbosity is enabled.
 */
public class SyntheticBridgeException extends MixinException {

    private static final long serialVersionUID = 1L;

    /**
     * Problem types for synthetic bridge comparisons
     */
    public enum Problem {
        
        BAD_INSN("Conflicting opcodes %4$s and %5$s at offset %3$d in synthetic bridge method %1$s%2$s"),
        BAD_LOAD("Conflicting variable access at offset %3$d in synthetic bridge method %1$s%2$s"),
        BAD_CAST("Conflicting type cast at offset %3$d in synthetic bridge method %1$s%2$s"),
        BAD_INVOKE_NAME("Conflicting synthetic bridge target method name in synthetic bridge method %1$s%2$s Existing:%6$s Incoming:%7$s"),
        BAD_INVOKE_DESC("Conflicting synthetic bridge target method descriptor in synthetic bridge method %1$s%2$s Existing:%8$s Incoming:%9$s"),
        BAD_LENGTH("Mismatched bridge method length for synthetic bridge method %1$s%2$s unexpected extra opcode at offset %3$d");
        
        private final String message;

        private Problem(String message) {
            this.message = message;
        }
        
        String getMessage(String name, String desc, int index, AbstractInsnNode a, AbstractInsnNode b) {
            return String.format(this.message, name, desc, index, Bytecode.getOpcodeName(a), Bytecode.getOpcodeName(a),
                    Problem.getInsnName(a), Problem.getInsnName(b), Problem.getInsnDesc(a), Problem.getInsnDesc(b));
        }

        private static String getInsnName(AbstractInsnNode node) {
            return node instanceof MethodInsnNode ? ((MethodInsnNode)node).name : "";
        }

        private static String getInsnDesc(AbstractInsnNode node) {
            return node instanceof MethodInsnNode ? ((MethodInsnNode)node).desc : "";
        }
        
    }
    
    /**
     * Problem?
     */
    private final Problem problem;
    
    /**
     * Bridge method name and descriptor
     */
    private final String name, desc;
    
    /**
     * Opcode index where the mismatch occurred 
     */
    private final int index;
    
    /**
     * Mismatched nodes, a is from the target class, b is from the incoming
     * class 
     */
    private final AbstractInsnNode a, b;

    public SyntheticBridgeException(Problem problem, String name, String desc, int index, AbstractInsnNode a, AbstractInsnNode b) {
        super(problem.getMessage(name, desc, index, a, b));
        this.problem = problem;
        this.name = name;
        this.desc = desc;
        this.index = index;
        this.a = a;
        this.b = b;
    }
    
    /**
     * @param context Incoming mixin
     * @param mda Target method
     * @param mdb Incoming method
     */
    public void printAnalysis(IMixinContext context, MethodNode mda, MethodNode mdb) {
        PrettyPrinter printer = new PrettyPrinter();
        printer.addWrapped(100, this.getMessage()).hr();
        printer.add().kv("Method", this.name + this.desc).kv("Problem Type", this.problem).add().hr();
        String merged = Annotations.<String>getValue(Annotations.getVisible(mda, MixinMerged.class), "mixin");
        String owner = merged != null ? merged : context.getTargetClassRef().replace('/', '.');
        this.printMethod(printer.add("Existing method").add().kv("Owner", owner).add(), mda).hr();
        this.printMethod(printer.add("Incoming method").add().kv("Owner", context.getClassRef().replace('/', '.')).add(), mdb).hr();
        this.printProblem(printer, context, mda, mdb).print(System.err);
    }

    private PrettyPrinter printMethod(PrettyPrinter printer, MethodNode method) {
        int index = 0;
        for (Iterator<AbstractInsnNode> iter = method.instructions.iterator(); iter.hasNext(); index++) {
            printer.kv(index == this.index ? ">>>>" : "", Bytecode.describeNode(iter.next()));
        }
        return printer.add();
    }

    private PrettyPrinter printProblem(PrettyPrinter printer, IMixinContext context, MethodNode mda, MethodNode mdb) {
        Type target = Type.getObjectType(context.getTargetClassRef());
        
        printer.add("Analysis").add();
        switch (this.problem) {
            case BAD_INSN:
                printer.add("The bridge methods are not compatible because they contain incompatible opcodes");
                printer.add("at index " + this.index + ":").add();
                printer.kv("Existing opcode: %s", Bytecode.getOpcodeName(this.a));
                printer.kv("Incoming opcode: %s", Bytecode.getOpcodeName(this.b)).add();
                printer.add("This implies that the bridge methods are from different interfaces. This problem");
                printer.add("may not be resolvable without changing the base interfaces.").add();
                break;
                
            case BAD_LOAD:
                printer.add("The bridge methods are not compatible because they contain different variables at");
                printer.add("opcode index " + this.index + ".").add();
                
                ListIterator<AbstractInsnNode> ia = mda.instructions.iterator();
                ListIterator<AbstractInsnNode> ib = mdb.instructions.iterator();
                
                Type[] argsa = Type.getArgumentTypes(mda.desc);
                Type[] argsb = Type.getArgumentTypes(mdb.desc);
                for (int index = 0; ia.hasNext() && ib.hasNext(); index++) {
                    AbstractInsnNode na = ia.next();
                    AbstractInsnNode nb = ib.next();
                    if (na instanceof VarInsnNode && nb instanceof VarInsnNode) {
                        VarInsnNode va = (VarInsnNode)na;
                        VarInsnNode vb = (VarInsnNode)nb;
                        
                        Type ta = va.var > 0 ? argsa[va.var - 1] : target;
                        Type tb = vb.var > 0 ? argsb[vb.var - 1] : target;
                        printer.kv("Target " + index, "%8s %-2d %s", Bytecode.getOpcodeName(va), va.var, ta);
                        printer.kv("Incoming " + index, "%8s %-2d %s", Bytecode.getOpcodeName(vb), vb.var, tb);
                        
                        if (ta.equals(tb)) {
                            printer.kv("", "Types match: %s", ta);
                        } else if (ta.getSort() != tb.getSort()) {
                            printer.kv("", "Types are incompatible");
                        } else if (ta.getSort() == Type.OBJECT) {
                            ClassInfo superClass = ClassInfo.getCommonSuperClassOrInterface(ta, tb);
                            printer.kv("", "Common supertype: %s", superClass);
                        }
                        
                        printer.add();
                    }
                }
                
                printer.add("Since this probably means that the methods come from different interfaces, you");
                printer.add("may have a \"multiple inheritance\" problem, it may not be possible to implement");
                printer.add("both root interfaces");
                break;
                
            case BAD_CAST:
                printer.add("Incompatible CHECKCAST encountered at opcode " + this.index + ", this could indicate that the bridge");
                printer.add("is casting down for contravariant generic types. It may be possible to coalesce the");
                printer.add("bridges by adjusting the types in the target method.").add();

                Type ta = Type.getObjectType(((TypeInsnNode)this.a).desc);
                Type tb = Type.getObjectType(((TypeInsnNode)this.b).desc);
                printer.kv("Target type", ta);
                printer.kv("Incoming type", tb);
                printer.kv("Common supertype", ClassInfo.getCommonSuperClassOrInterface(ta, tb)).add();
                break;
                
            case BAD_INVOKE_NAME:
                printer.add("Incompatible invocation targets in synthetic bridge. This is extremely unusual");
                printer.add("and implies that a remapping transformer has incorrectly remapped a method. This");
                printer.add("is an unrecoverable error.");
                break;
                
            case BAD_INVOKE_DESC:
                MethodInsnNode mdna = (MethodInsnNode)this.a;
                MethodInsnNode mdnb = (MethodInsnNode)this.b;
                
                Type[] arga = Type.getArgumentTypes(mdna.desc);
                Type[] argb = Type.getArgumentTypes(mdnb.desc);
                
                if (arga.length != argb.length) {
                    int argCount = Type.getArgumentTypes(mda.desc).length;
                    String winner = arga.length == argCount ? "The TARGET" : (argb.length == argCount ? " The INCOMING" : "NEITHER");
                    
                    printer.add("Mismatched invocation descriptors in synthetic bridge implies that a remapping");
                    printer.add("transformer has incorrectly coalesced a bridge method with a conflicting name.");
                    printer.add("Overlapping bridge methods should always have the same number of arguments, yet");
                    printer.add("the target method has %d arguments, the incoming method has %d. This is an", arga.length, argb.length);
                    printer.add("unrecoverable error. %s method has the expected arg count of %d", winner, argCount);
                    break;
                }
                
                Type rta = Type.getReturnType(mdna.desc);
                Type rtb = Type.getReturnType(mdnb.desc);

                printer.add("Incompatible invocation descriptors in synthetic bridge implies that generified");
                printer.add("types are incompatible over one or more generic superclasses or interfaces. It may");
                printer.add("be possible to adjust the generic types on implemented members to rectify this");
                printer.add("problem by coalescing the appropriate generic types.").add();
                
                this.printTypeComparison(printer, "return type", rta, rtb);
                for (int i = 0; i < arga.length; i++) {
                    this.printTypeComparison(printer, "arg " + i, arga[i], argb[i]);
                }
                
                break;
                
            case BAD_LENGTH:
                printer.add("Mismatched bridge method length implies the bridge methods are incompatible");
                printer.add("and may originate from different superinterfaces. This is an unrecoverable");
                printer.add("error.").add();
                break;
                
            default :
                break;
            
        }
        return printer;
    }

    private PrettyPrinter printTypeComparison(PrettyPrinter printer, String index, Type tpa, Type tpb) {
        printer.kv("Target " + index, "%s", tpa);
        printer.kv("Incoming " + index, "%s", tpb);
        
        if (tpa.equals(tpb)) {
            printer.kv("Analysis", "Types match: %s", tpa);
        } else if (tpa.getSort() != tpb.getSort()) {
            printer.kv("Analysis", "Types are incompatible");
        } else if (tpa.getSort() == Type.OBJECT) {
            ClassInfo superClass = ClassInfo.getCommonSuperClassOrInterface(tpa, tpb);
            printer.kv("Analysis", "Common supertype: L%s;", superClass);
        }
        
        return printer.add();
    }

}
