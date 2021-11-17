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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.StandardLocation;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * The specs of JSR 269 mean that mirror will only give us things visible down
 * to the method level and no further. This means that classes declared inside
 * method bodies (anonymous classes) cannot be accessed via mirror. However for
 * classpath classes we can at least retrieve the class bytecode via the AP
 * Filer, so this class is used to wrap an ASM {@link ClassNode} up as a
 * {@link TypeHandle} so that the mixin AP can use it to resolve things. It
 * delegates back to {@link ITypeHandleProvider} where appropriate so that
 * related classes (superclasses, interfaces, etc.) will be backed by mirror
 * TypeElements wherever possible.
 */
public class TypeHandleASM extends TypeHandle {
    
    /**
     * Cache ASM type handles to avoid unnecessary churn
     */
    private static final Map<String, TypeHandleASM> cache = new HashMap<String, TypeHandleASM>();
    
    /**
     * ClassNode used for accessing information when not accessible via mirror
     */
    private final ClassNode classNode;
    
    /**
     * Type handle provider, we need this since we have to resolve type handles
     * for related classes (eg. superclass) without using mirror
     */
    private final ITypeHandleProvider typeProvider;

    /**
     * Ctor for imaginary elements, require the enclosing package and the FQ
     * name
     * 
     * @param pkg Package
     * @param name FQ class name
     */
    protected TypeHandleASM(PackageElement pkg, String name, ClassNode classNode, ITypeHandleProvider typeProvider) {
        super(pkg, name);
        this.classNode = classNode;
        this.typeProvider = typeProvider;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getAnnotation(java.lang.Class)
     */
    @Override
    public IAnnotationHandle getAnnotation(Class<? extends Annotation> annotationClass) {
        AnnotationNode visibleAnnotation = Annotations.getVisible(this.classNode, annotationClass);
        if (visibleAnnotation != null) {
            return Annotations.handleOf(visibleAnnotation);
        }
        AnnotationNode invisibleAnnotation = Annotations.getInvisible(this.classNode, annotationClass);
        if (invisibleAnnotation != null) {
            return Annotations.handleOf(invisibleAnnotation);
        }
        return AnnotationHandle.of(null);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getEnclosedElements(javax.lang.model.element.ElementKind[])
     */
    @Override
    public <T extends Element> List<T> getEnclosedElements(ElementKind... kind) {
        return super.getEnclosedElements(kind);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #hasTypeMirror()
     */
    @Override
    public boolean hasTypeMirror() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getTypeMirror()
     */
    @Override
    public TypeMirror getTypeMirror() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getSuperclass()
     */
    @Override
    public TypeHandle getSuperclass() {
        TypeHandle superClass = this.typeProvider.getTypeHandle(this.classNode.superName);
        return superClass;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle
     *      #getInterfaces()
     */
    @Override
    public List<TypeHandle> getInterfaces() {
        Builder<TypeHandle> list = ImmutableList.<TypeHandle>builder();
        for (String ifaceName : this.classNode.interfaces) {
            TypeHandle iface = this.typeProvider.getTypeHandle(ifaceName);
            if (iface != null) {
                list.add(iface);
            }
        }
        return list.build();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#getMethods()
     */
    @Override
    public List<MethodHandle> getMethods() {
        Builder<MethodHandle> methods = ImmutableList.<MethodHandle>builder();
        for (MethodNode method : this.classNode.methods) {
            if (!method.name.startsWith("<") && (method.access & Opcodes.ACC_SYNTHETIC) == 0) {
                methods.add(new MethodHandleASM(this, method));
            }
        }
        return methods.build();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#isPublic()
     */
    @Override
    public boolean isPublic() {
        return (this.classNode.access & Opcodes.ACC_PUBLIC) != 0;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#isImaginary()
     */
    @Override
    public boolean isImaginary() {
        return false;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#findDescriptor
     *  (org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorByName)
     */
    @Override
    public String findDescriptor(ITargetSelectorByName selector) {
        String desc = selector.getDesc();
        if (desc == null) {
            for (MethodNode method : this.classNode.methods) {
                if (method.name.equals(selector.getName())) {
                    desc = method.desc;
                    break;
                }
            }
        }
        return desc;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#findField(
     *      java.lang.String, java.lang.String, boolean)
     */
    @Override
    public FieldHandle findField(String name, String type, boolean matchCase) {
        for (FieldNode field : this.classNode.fields) {
            if (TypeHandleASM.compareElement(field.name, TypeUtils.getJavaSignature(field.desc), name, type, matchCase)) {
                return new FieldHandleASM(this, field);
            }                
        }
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.mirror.TypeHandle#findMethod(
     *      java.lang.String, java.lang.String, boolean)
     */
    @Override
    public MethodHandle findMethod(String name, String signature, boolean matchCase) {
        for (MethodNode method : this.classNode.methods) {
            if (TypeHandleASM.compareElement(method.name, TypeUtils.getJavaSignature(method.desc), name, signature, matchCase)) {
                return new MethodHandleASM(this, method);
            }
        }
        return null;
    }

    protected static boolean compareElement(String elementName, String elementType, String name, String type, boolean matchCase) {
        try {
            boolean compared = matchCase ? name.equals(elementName) : name.equalsIgnoreCase(elementName);
            return compared && (type.length() == 0 || type.equals(elementType));
        } catch (NullPointerException ex) {
            return false;
        }
    }

    /**
     * Attempts to read a class from the compile classpath using ASM. This at
     * least lets us determine whether the class exists and inspect the methods
     * and fields it contains. Returns <tt>null</tt> if the class resource could
     * not be resolved via the compile classpath.
     * 
     * @param pkg Containing package
     * @param name Class name
     * @param ap Annotation Processor, used to access Filer and
     *      TypeHandleProvider
     * @return ASM TypeHandle or null if the class could not be read
     */
    public static TypeHandle of(PackageElement pkg, String name, IMixinAnnotationProcessor ap) {
        String fqName = pkg.getQualifiedName() + "." + name;
        if (TypeHandleASM.cache.containsKey(fqName)) {
            return TypeHandleASM.cache.get(fqName);
        }
        
        InputStream is = null;
        try {
            Filer filer = ap.getProcessingEnvironment().getFiler();
            is = filer.getResource(StandardLocation.CLASS_PATH, pkg.getQualifiedName(), name + ".class").openInputStream();
            ClassNode classNode = new ClassNode();
            new ClassReader(is).accept(classNode, 0);
            TypeHandleASM typeHandle = new TypeHandleASM(pkg, fqName, classNode, ap.getTypeProvider());
            TypeHandleASM.cache.put(fqName, typeHandle);
            return typeHandle;
        } catch (FileNotFoundException fnfe) {
            // This is expected if the resource doesn't exist
            TypeHandleASM.cache.put(fqName, null);
        } catch (Exception ex) {
            // This isn't expected but there's not a lot we can do about it, so just give up
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return null;
    }

}
