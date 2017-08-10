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
package org.spongepowered.asm.util;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.LocalVariableNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;

import com.google.common.base.Strings;

/**
 * Generates callback signature for callback pretty-print
 */
public class SignaturePrinter {

    /**
     * Method name 
     */
    private final String name;
    
    /**
     * The return type of the <b>target</b> method (used to fill in the generic
     * bound on a CallbackInfoReturnable. 
     */
    private final Type returnType;
    
    /**
     * Argument type array (sparse, with no TOP entries) 
     */
    private final Type[] argTypes;
    
    /**
     * Argument names 
     */
    private final String[] argNames;
    
    /**
     * Method prefix
     */
    private String modifiers = "private void";
    
    /**
     * Fully qualify class names 
     */
    private boolean fullyQualified;
    
    public SignaturePrinter(MethodNode method) {
        this(method.name, Type.VOID_TYPE, Type.getArgumentTypes(method.desc));
        this.setModifiers(method);
    }
    
    public SignaturePrinter(MethodNode method, String[] argNames) {
        this(method.name, Type.VOID_TYPE, Type.getArgumentTypes(method.desc), argNames);
        this.setModifiers(method);
    }
    
    public SignaturePrinter(MemberInfo member) {
        this(member.name, member.desc);
    }

    public SignaturePrinter(String name, String desc) {
        this(name, Type.getReturnType(desc), Type.getArgumentTypes(desc));
    }

    public SignaturePrinter(String name, Type returnType, Type[] args) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = new Type[args.length];
        this.argNames = new String[args.length];
        for (int l = 0, v = 0; l < args.length; l++) {
            if (args[l] != null) {
                this.argTypes[l] = args[l];
                this.argNames[l] = "var" + v++; 
            }
        }
    }

    public SignaturePrinter(String name, Type returnType, LocalVariableNode[] args) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = new Type[args.length];
        this.argNames = new String[args.length];
        for (int l = 0; l < args.length; l++) {
            if (args[l] != null) {
                this.argTypes[l] = Type.getType(args[l].desc);
                this.argNames[l] = args[l].name;
            }
        }
    }
    
    public SignaturePrinter(String name, Type returnType, Type[] argTypes, String[] argNames) {
        this.name = name;
        this.returnType = returnType;
        this.argTypes = argTypes;
        this.argNames = argNames;
        if (this.argTypes.length > this.argNames.length) {
            throw new IllegalArgumentException(String.format("Types array length must not exceed names array length! (names=%d, types=%d)",
                    this.argNames.length, this.argTypes.length));
        }
    }
    
    /**
     * Return only the arguments portion of this signature as a Java-style block
     */
    public String getFormattedArgs() {
        return this.appendArgs(new StringBuilder(), true, true).toString();
    }
    
    /**
     * Get string representation of this signature's return type
     */
    public String getReturnType() {
        return SignaturePrinter.getTypeName(this.returnType, false, this.fullyQualified);
    }

    /**
     * Set modifiers on this signature using the supplied method node
     * 
     * @param method method node to read modifiers from
     */
    public void setModifiers(MethodNode method) {
        String returnType = SignaturePrinter.getTypeName(Type.getReturnType(method.desc), false, this.fullyQualified);
        if ((method.access & Opcodes.ACC_PUBLIC) != 0) {
            this.setModifiers("public " + returnType);
        } else if ((method.access & Opcodes.ACC_PROTECTED) != 0) {
            this.setModifiers("protected " + returnType);
        } else if ((method.access & Opcodes.ACC_PRIVATE) != 0) {
            this.setModifiers("private " + returnType);
        } else {
            this.setModifiers(returnType);
        }
    }
    
    /**
     * Set modifiers on this signature explicitly. Use the special token
     * <tt>${returnType}</tt> to insert the return type into the modifier
     * string.
     * 
     * @param modifiers modifiers to prepend
     * @return fluent interface
     */
    public SignaturePrinter setModifiers(String modifiers) {
        this.modifiers = modifiers.replace("${returnType}", this.getReturnType());
        return this;
    }
    
    /**
     * Set whether this signature generates fully-qualified class output, mainly
     * used when generating signatures for Mirror
     * 
     * @param fullyQualified new value for fully-qualified
     * @return fluent interface
     */
    public SignaturePrinter setFullyQualified(boolean fullyQualified) {
        this.fullyQualified = fullyQualified;
        return this;
    }
    
    /**
     * Get whether this printer will fully-qualify class names in generated
     * signatures
     */
    public boolean isFullyQualified() {
        return this.fullyQualified;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.appendArgs(new StringBuilder().append(this.modifiers).append(" ").append(this.name), false, true).toString();
    }
    
    /**
     * Return this signature in descriptor format (return type after args)
     */
    public String toDescriptor() {
        StringBuilder args = this.appendArgs(new StringBuilder(), true, false);
        return args.append(SignaturePrinter.getTypeName(this.returnType, false, this.fullyQualified)).toString();
    }
    
    private StringBuilder appendArgs(StringBuilder sb, boolean typesOnly, boolean pretty) {
        sb.append('(');
        for (int var = 0; var < this.argTypes.length; var++) {
            if (this.argTypes[var] == null) {
                continue;
            } else if (var > 0) {
                sb.append(',');
                if (pretty) {
                    sb.append(' ');
                }
            }
            try {
                String name = typesOnly ? null : Strings.isNullOrEmpty(this.argNames[var]) ? "unnamed" + var : this.argNames[var];
                this.appendType(sb, this.argTypes[var], name);
            } catch (Exception ex) {
//                System.err.printf("\n\n>>> argTypes=%d, argNames=%d\n\n", this.argTypes.length, this.argNames.length);
                throw new RuntimeException(ex);
            }
        }
        return sb.append(")");
    }
    
    private StringBuilder appendType(StringBuilder sb, Type type, String name) {
        switch (type.getSort()) {
            case Type.ARRAY:
                return SignaturePrinter.appendArraySuffix(this.appendType(sb, type.getElementType(), name), type);
            case Type.OBJECT:
                return this.appendType(sb, type.getClassName(), name);
            default:
                sb.append(SignaturePrinter.getTypeName(type, false, this.fullyQualified));
                if (name != null) {
                    sb.append(' ').append(name);
                }
                return sb;
        }
    }

    private StringBuilder appendType(StringBuilder sb, String typeName, String name) {
        if (!this.fullyQualified) {
            typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
        }
        sb.append(typeName);
        if (typeName.endsWith("CallbackInfoReturnable")) {
            sb.append('<').append(SignaturePrinter.getTypeName(this.returnType, true, this.fullyQualified)).append('>');
        }
        if (name != null) {
            sb.append(' ').append(name);
        }
        return sb;
    }

    /**
     * Get the source code name for the specified type
     * 
     * @param type Type to generate a friendly name for
     * @param box True to return the equivalent boxing type for primitives 
     * @return String representation of the specified type, eg "int" for an
     *         integer primitive or "String" for java.lang.String
     */
    public static String getTypeName(Type type, boolean box) {
        return SignaturePrinter.getTypeName(type, box, false);
    }

    /**
     * Get the source code name for the specified type
     * 
     * @param type Type to generate a friendly name for
     * @param box True to return the equivalent boxing type for primitives
     * @param fullyQualified fully-qualify class names 
     * @return String representation of the specified type, eg "int" for an
     *         integer primitive or "String" for java.lang.String
     */
    public static String getTypeName(Type type, boolean box, boolean fullyQualified) {
        switch (type.getSort()) {
            case Type.VOID:    return box ? "Void"      : "void";
            case Type.BOOLEAN: return box ? "Boolean"   : "boolean";
            case Type.CHAR:    return box ? "Character" : "char";
            case Type.BYTE:    return box ? "Byte"      : "byte";
            case Type.SHORT:   return box ? "Short"     : "short";
            case Type.INT:     return box ? "Integer"   : "int";
            case Type.FLOAT:   return box ? "Float"     : "float";
            case Type.LONG:    return box ? "Long"      : "long";
            case Type.DOUBLE:  return box ? "Double"    : "double";
            case Type.ARRAY:   return SignaturePrinter.getTypeName(type.getElementType(), box, fullyQualified) + SignaturePrinter.arraySuffix(type);
            case Type.OBJECT:
                String typeName = type.getClassName();
                if (!fullyQualified) {
                    typeName = typeName.substring(typeName.lastIndexOf('.') + 1);
                }
                return typeName;
            default:
                return "Object";
        }
    }
    
    private static String arraySuffix(Type type) {
        return Strings.repeat("[]", type.getDimensions());
    }
    
    
    private static StringBuilder appendArraySuffix(StringBuilder sb, Type type) {
        for (int i = 0; i < type.getDimensions(); i++) {
            sb.append("[]");
        }
        return sb;
    }
}
