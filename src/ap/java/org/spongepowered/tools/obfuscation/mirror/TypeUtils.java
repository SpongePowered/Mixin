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
package org.spongepowered.tools.obfuscation.mirror;

import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.SignaturePrinter;

/**
 * Convenience functions for mirror types
 */
public abstract class TypeUtils {
    
    /**
     * Result type returned by {@link TypeUtils#isEquivalentType}
     */
    public enum Equivalency {
        
        /**
         * Types are not equivalent
         */
        NOT_EQUIVALENT,
        
        /**
         * Types are equivalent, but one type is raw 
         */
        EQUIVALENT_BUT_RAW,
        
        /**
         * Types are equivalent, but generic type parameters do not match 
         */
        BOUNDS_MISMATCH,
        
        /**
         * Types are equivalent, any generic type parameters are also equivalent
         * or both types are raw
         */
        EQUIVALENT
        
    }
    
    /**
     * Result bundle from a type equivalency check. See
     * {@link TypeUtils#isEquivalentType} for details
     */
    public static class EquivalencyResult {
        
        /**
         * Singleton for equivalent type results
         */
        static final EquivalencyResult EQUIVALENT = new EquivalencyResult(Equivalency.EQUIVALENT, "", 0);
        
        /**
         * The equivalency result type
         */
        public final Equivalency type;
        
        /**
         * Detail string for use in user-facing error messages describing the
         * nature of match failures
         */
        public final String detail;
        
        /**
         * For {@link Equivalency#EQUIVALENT_BUT_RAW} indicates which argument
         * was the raw one. This should only ever have the values 0 (for not
         * relevant), 1 or 2. It is up to the outer scope (the caller) to
         * determine whether to warn based on the rawness of the respective
         * "left" and "right" types.
         */
        public final int rawType;
        
        EquivalencyResult(Equivalency type, String detail, int rawType) {
            this.type = type;
            this.detail = detail;
            this.rawType = rawType;
        }
        
        @Override
        public String toString() {
            return this.detail;
        }
        
        static EquivalencyResult notEquivalent(String format, Object... args) {
            return new EquivalencyResult(Equivalency.NOT_EQUIVALENT, String.format(format, args), 0);
        }

        static EquivalencyResult boundsMismatch(String format, Object... args) {
            return new EquivalencyResult(Equivalency.BOUNDS_MISMATCH, String.format(format, args), 0);
        }
        
        static EquivalencyResult equivalentButRaw(int rawType) {
            return new EquivalencyResult(Equivalency.EQUIVALENT_BUT_RAW, String.format("Type %d is raw", rawType), rawType);
        }
        
    }
    
    /**
     * Number of times to recurse into TypeMirrors when trying to determine the
     * upper bound of a TYPEVAR 
     */
    private static final int MAX_GENERIC_RECURSION_DEPTH = 5;
    
    private static final String OBJECT_SIG = "java.lang.Object";

    // No instances for you
    private TypeUtils() {}

    /**
     * If the supplied type is a {@link DeclaredType}, return the package in
     * which it is declared
     * 
     * @param type type to find package for
     * @return package for supplied type or null
     */
    public static PackageElement getPackage(TypeMirror type) {
        if (!(type instanceof DeclaredType)) {
            return null;
        }
        return TypeUtils.getPackage((TypeElement)((DeclaredType)type).asElement());
    }
    
    /**
     * Return the package in which the specified type element is declared
     * @param type type to find package for
     * @return package for supplied type or null
     */
    public static PackageElement getPackage(TypeElement type) {
        Element parent = type.getEnclosingElement();
        while (parent != null && !(parent instanceof PackageElement)) {
            parent = parent.getEnclosingElement();
        }
        return (PackageElement)parent;
    }

    /**
     * Convenience method to convert element to string representation for error
     * messages
     * 
     * @param element Element to inspect
     * @return string representation of element name
     */
    public static String getElementType(Element element) {
        if (element instanceof TypeElement) {
            return "TypeElement";
        } else if (element instanceof ExecutableElement) {
            return "ExecutableElement";
        } else if (element instanceof VariableElement) {
            return "VariableElement";
        } else if (element instanceof PackageElement) {
            return "PackageElement";
        } else if (element instanceof TypeParameterElement) {
            return "TypeParameterElement";
        }
        
        return element.getClass().getSimpleName();
    }

    /**
     * Strip generic arguments from the supplied type descriptor
     * 
     * @param type type descriptor
     * @return type descriptor with generic args removed
     */
    public static String stripGenerics(String type) {
        StringBuilder sb = new StringBuilder();
        for (int pos = 0, depth = 0; pos < type.length(); pos++) {
            char c = type.charAt(pos);
            if (c == '<') {
                depth++;
            }
            if (depth == 0) {
                sb.append(c);
            } else if (c == '>') {
                depth--;
            }
        }
        return sb.toString();
    }
    
    /**
     * Get the name of the specified field
     * 
     * @param field field element
     * @return field name
     */
    public static String getName(VariableElement field) {
        return field != null ? field.getSimpleName().toString() : null;
    }
    
    /**
     * Get the name of the specified method
     * 
     * @param method method element
     * @return method name
     */
    public static String getName(ExecutableElement method) {
        return method != null ? method.getSimpleName().toString() : null;
    }

    /**
     * Get a java-style signature for the specified element (return type follows
     * args) eg:
     * 
     * <pre>(int,int)boolean</pre>
     * 
     * @param element element to generate java signature for
     * @return java signature
     */
    public static String getJavaSignature(Element element) {
        if (element instanceof ExecutableElement) {
            ExecutableElement method = (ExecutableElement)element;
            StringBuilder desc = new StringBuilder().append("(");
            boolean extra = false;
            for (VariableElement arg : method.getParameters()) {
                if (extra) {
                    desc.append(',');
                }
                desc.append(TypeUtils.getTypeName(arg.asType()));
                extra = true;
            }
            desc.append(')').append(TypeUtils.getTypeName(method.getReturnType()));
            return desc.toString();
        }
        return TypeUtils.getTypeName(element.asType());
    }
    
    /**
     * Get a java-style signature from the specified bytecode descriptor
     * 
     * @param descriptor descriptor to convert to java signature
     * @return java signature
     */
    public static String getJavaSignature(String descriptor) {
        return new SignaturePrinter("", descriptor).setFullyQualified(true).toDescriptor();
    }

    /**
     * Get the simple type name for the specified type
     * 
     * @param type type mirror
     * @return type name
     */
    public static String getSimpleName(TypeMirror type) {
        String name = TypeUtils.getTypeName(type);
        int pos = name.lastIndexOf('.');
        return pos > 0 ? name.substring(pos + 1) : name;
    }

    /**
     * Get the type name for the specified type
     * 
     * @param type type mirror
     * @return type name
     */
    public static String getTypeName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:    return TypeUtils.getTypeName(((ArrayType)type).getComponentType()) + "[]";
            case DECLARED: return TypeUtils.getTypeName((DeclaredType)type);
            case TYPEVAR:  return TypeUtils.getTypeName(TypeUtils.getUpperBound(type));
            case ERROR:    return TypeUtils.OBJECT_SIG;
            default:       return type.toString();
        }
    }

    /**
     * Get the type name for the specified type
     * 
     * @param type type mirror
     * @return type name
     */
    public static String getTypeName(DeclaredType type) {
        if (type == null) {
            return TypeUtils.OBJECT_SIG;
        }
        return TypeUtils.getInternalName((TypeElement)type.asElement()).replace('/', '.');
    }

    /**
     * Get a bytecode-style descriptor for the specified element 
     * 
     * @param element element to generate descriptor for
     * @return descriptor
     */
    public static String getDescriptor(Element element) {
        if (element instanceof ExecutableElement) {
            return TypeUtils.getDescriptor((ExecutableElement)element);
        } else if (element instanceof VariableElement) {
            return TypeUtils.getInternalName((VariableElement)element);
        }
        
        return TypeUtils.getInternalName(element.asType());
    }

    /**
     * Get a bytecode-style descriptor for the specified method 
     * 
     * @param method method to generate descriptor for
     * @return descriptor
     */
    public static String getDescriptor(ExecutableElement method) {
        if (method == null) {
            return null;
        }
        
        StringBuilder signature = new StringBuilder();
        
        for (VariableElement var : method.getParameters()) {
            signature.append(TypeUtils.getInternalName(var));
        }
        
        String returnType = TypeUtils.getInternalName(method.getReturnType());
        return String.format("(%s)%s", signature, returnType);
    }
    
    /**
     * Get a bytecode-style descriptor for the specified field 
     * 
     * @param field field to generate descriptor for
     * @return descriptor
     */
    public static String getInternalName(VariableElement field) {
        if (field == null) {
            return null;
        }
        return TypeUtils.getInternalName(field.asType());
    }
    
    /**
     * Get a bytecode-style descriptor for the specified type 
     * 
     * @param type type to generate descriptor for
     * @return descriptor
     */
    public static String getInternalName(TypeMirror type) {
        switch (type.getKind()) {
            case ARRAY:    return "[" + TypeUtils.getInternalName(((ArrayType)type).getComponentType());
            case DECLARED: return "L" + TypeUtils.getInternalName((DeclaredType)type) + ";";
            case TYPEVAR:  return "L" + TypeUtils.getInternalName(TypeUtils.getUpperBound(type)) + ";";
            case BOOLEAN:  return "Z";
            case BYTE:     return "B";
            case CHAR:     return "C";
            case DOUBLE:   return "D";
            case FLOAT:    return "F";
            case INT:      return "I";
            case LONG:     return "J";
            case SHORT:    return "S";
            case VOID:     return "V";
            // TODO figure out a better way to not crash when we get here
            case ERROR:    return Constants.OBJECT_DESC;
            default:
        }

        throw new IllegalArgumentException("Unable to parse type symbol " + type + " with " + type.getKind() + " to equivalent bytecode type");
    }

    /**
     * Get a bytecode-style name for the specified type
     * 
     * @param type type to get name for
     * @return bytecode-style name
     */
    public static String getInternalName(DeclaredType type) {
        if (type == null) {
            return Constants.OBJECT;
        }
        return TypeUtils.getInternalName((TypeElement)type.asElement());
    }

    /**
     * Get a bytecode-style name for the specified type element
     * 
     * @param element type element to get name for
     * @return bytecode-style name
     */
    public static String getInternalName(TypeElement element) {
        if (element == null) {
            return null;
        }
        StringBuilder reference = new StringBuilder();
        reference.append(element.getSimpleName());
        Element parent = element.getEnclosingElement();
        while (parent != null) {
            if (parent instanceof TypeElement) {
                reference.insert(0, "$").insert(0, parent.getSimpleName());
            } else if (parent instanceof PackageElement) {
                reference.insert(0, "/").insert(0, ((PackageElement)parent).getQualifiedName().toString().replace('.', '/'));
            }
            parent = parent.getEnclosingElement();
        }
        return reference.toString();
    }
    
    private static DeclaredType getUpperBound(TypeMirror type) {
        try {
            return TypeUtils.getUpperBound0(type, TypeUtils.MAX_GENERIC_RECURSION_DEPTH);
        } catch (IllegalStateException ex) {
            throw new IllegalArgumentException("Type symbol \"" + type + "\" is too complex", ex);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type, ex);
        }
    }

    private static DeclaredType getUpperBound0(TypeMirror type, int depth) {
        if (depth == 0) {
            throw new IllegalStateException("Generic symbol \"" + type + "\" is too complex, exceeded "
                    + TypeUtils.MAX_GENERIC_RECURSION_DEPTH + " iterations attempting to determine upper bound");
        }
        if (type instanceof DeclaredType) {
            return (DeclaredType)type;
        }
        if (type instanceof TypeVariable) {
            try {
                TypeMirror upper = ((TypeVariable)type).getUpperBound();
                return TypeUtils.getUpperBound0(upper, --depth);
            } catch (IllegalStateException ex) {
                throw ex;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                throw new IllegalArgumentException("Unable to compute upper bound of type symbol " + type);
            }
        }
        return null;
    }
    
    private static String describeGenericBound(TypeMirror type) {
        if (type instanceof TypeVariable) {
            StringBuilder description = new StringBuilder("<");
            TypeVariable typeVar = (TypeVariable)type;
            description.append(typeVar.toString());
            TypeMirror lowerBound = typeVar.getLowerBound();
            if (lowerBound.getKind() != TypeKind.NULL) {
                description.append(" super ").append(lowerBound);
            }
            TypeMirror upperBound = typeVar.getUpperBound();
            if (upperBound.getKind() != TypeKind.NULL) {
                description.append(" extends ").append(upperBound);
            }
            return description.append(">").toString();
        }
        
        return type.toString();
    }

    /**
     * Get whether the target type is assignable to the specified superclass
     * 
     * @param processingEnv processing environment
     * @param targetType target type to check
     * @param superClass superclass type to check
     * @return true if targetType is assignable to superClass
     */
    public static boolean isAssignable(ProcessingEnvironment processingEnv, TypeMirror targetType, TypeMirror superClass) {
        boolean assignable = processingEnv.getTypeUtils().isAssignable(targetType, superClass);
        if (!assignable && targetType instanceof DeclaredType && superClass instanceof DeclaredType) {
            TypeMirror rawTargetType = TypeUtils.toRawType(processingEnv, (DeclaredType)targetType);
            TypeMirror rawSuperType = TypeUtils.toRawType(processingEnv, (DeclaredType)superClass);
            return processingEnv.getTypeUtils().isAssignable(rawTargetType, rawSuperType);
        }
        
        return assignable;
    }
    
    /**
     * Get whether the two supplied type mirrors represent the same type. For 
     * generic types the type arguments must also be equivalent in order to
     * fully satisfy the equivalence condition. If one of the supplied types is
     * a raw type then a relevant result indicated by <tt>EQUIVALENT_BUT_RAW
     * </tt> will be returned and the caller can inspect which argument (t1 or
     * t2) was raw by querying the <tt>rawType</tt> member.
     * 
     * @param processingEnv processing environment
     * @param t1 first type for comparison
     * @param t2 second type for comparison
     * @return true if the supplied types are equivalent
     */
    public static EquivalencyResult isEquivalentType(ProcessingEnvironment processingEnv, TypeMirror t1, TypeMirror t2) {
        if (t1 == null || t2 == null) {
            return EquivalencyResult.notEquivalent("Invalid types supplied: %s, %s", t1, t2);
        }
        
        if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
            return EquivalencyResult.EQUIVALENT;
        }
        
        if (t1 instanceof TypeVariable && t2 instanceof TypeVariable) {
            t1 = TypeUtils.getUpperBound(t1);
            t2 = TypeUtils.getUpperBound(t2);
            if (processingEnv.getTypeUtils().isSameType(t1, t2)) {
                return EquivalencyResult.EQUIVALENT;
            }
        }
        
        if (t1 instanceof DeclaredType && t2 instanceof DeclaredType) {
            DeclaredType dtT1 = (DeclaredType)t1;
            DeclaredType dtT2 = (DeclaredType)t2;
            TypeMirror rawT1 = TypeUtils.toRawType(processingEnv, dtT1);
            TypeMirror rawT2 = TypeUtils.toRawType(processingEnv, dtT2);
            if (!processingEnv.getTypeUtils().isSameType(rawT1, rawT2)) {
                return EquivalencyResult.notEquivalent("Base types %s and %s are not compatible", rawT1, rawT2);
            }
            List<? extends TypeMirror> argsT1 = dtT1.getTypeArguments();
            List<? extends TypeMirror> argsT2 = dtT2.getTypeArguments();
            if (argsT1.size() != argsT2.size()) {
                if (argsT1.size() == 0) {
                    return EquivalencyResult.equivalentButRaw(1);
                }
                if (argsT2.size() == 0) {
                    return EquivalencyResult.equivalentButRaw(2);
                }
                return EquivalencyResult.notEquivalent("Mismatched generic argument counts %s<[%d]> and %s<[%d]>", rawT1, argsT1.size(), rawT2, argsT2.size());
            }

            for (int arg = 0; arg < argsT1.size(); arg++) {
                TypeMirror argT1 = argsT1.get(arg);
                TypeMirror argT2 = argsT2.get(arg);
                if (TypeUtils.isEquivalentType(processingEnv, argT1, argT2).type != Equivalency.EQUIVALENT) {
                    return EquivalencyResult.boundsMismatch("Generic bounds mismatch between %s and %s",
                            TypeUtils.describeGenericBound(argT1), TypeUtils.describeGenericBound(argT2));
                }
            }
            
            return EquivalencyResult.EQUIVALENT;
        }
        
        return EquivalencyResult.notEquivalent("%s and %s do not match", t1, t2);
    }

    private static TypeMirror toRawType(ProcessingEnvironment processingEnv, DeclaredType targetType) {
        if (targetType.getKind() == TypeKind.INTERSECTION) {
            return targetType;
        }
        Name qualifiedName = ((TypeElement)targetType.asElement()).getQualifiedName();
        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(qualifiedName);
        return typeElement != null ? typeElement.asType() : targetType;
    }
    
    /**
     * Get the ordinal visibility for the specified element
     * 
     * @param element element to inspect
     * @return visibility level or null if element is null
     */
    public static Visibility getVisibility(Element element) {
        if (element == null) {
            return null;
        }
        
        for (Modifier modifier : element.getModifiers()) {
            switch (modifier) {
                case PUBLIC: return Visibility.PUBLIC;
                case PROTECTED: return Visibility.PROTECTED;
                case PRIVATE: return Visibility.PRIVATE;
                default: break;
            }
        }
        
        return Visibility.PACKAGE;
    }
    
}
