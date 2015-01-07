/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.transformer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.helpers.Booleans;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.InvalidMixinException;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.transformers.TreeTransformer;
import org.spongepowered.asm.util.ASMHelper;

/**
 * Transformer which applies Mixin classes to their declared target classes
 */
public class MixinTransformer extends TreeTransformer {
    
    private static final String CLINIT = "<clinit>";

    private static final boolean DEBUG_EXPORT = Booleans.parseBoolean(System.getProperty("mixin.debug.export"), false);

    /**
     * Log all the things
     */
    private final Logger logger = LogManager.getLogger("mixin");
    
    /**
     * Mixin configuration bundle
     */
    private final List<MixinConfig> configs = new ArrayList<MixinConfig>();

    /**
     * ctor 
     */
    public MixinTransformer() {
        // Go via blackboard to create FORWARD compatibility if Mixins get pulled into FML 
        Object globalMixinTransformer = MixinEnvironment.getCurrentEnvironment().getActiveTransformer();
        if (globalMixinTransformer instanceof IClassTransformer) {
            throw new RuntimeException("Terminating MixinTransformer instance " + this);
        }
        
        // I am a leaf on the wind
        MixinEnvironment.getCurrentEnvironment().setActiveTransformer(this);
        
        List<String> configs = MixinEnvironment.getCurrentEnvironment().getMixinConfigs();
        
        if (configs != null) {
            for (String configFile : configs) {
                try {
                    this.configs.add(MixinConfig.create(configFile));
                } catch (Exception ex) {
                    this.logger.warn(String.format("Failed to load mixin config: %s", configFile), ex);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see net.minecraft.launchwrapper.IClassTransformer#transform(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        for (MixinConfig config : this.configs) {
            if (transformedName != null && transformedName.startsWith(config.getMixinPackage())) {
                throw new RuntimeException(String.format("%s is a mixin class and cannot be referenced directly", transformedName));
            }
            
            if (config.hasMixinsFor(transformedName)) {
                try {
                    basicClass = this.applyMixins(config, transformedName, basicClass);
                } catch (InvalidMixinException th) {
                    this.logger.warn(String.format("Class mixin failed: %s %s", th.getClass().getName(), th.getMessage()), th);
                    th.printStackTrace();
                }
            }
        }

        return basicClass;
    }

    /**
     * Apply mixins for specified target class to the class described by the supplied byte array
     * 
     * @param config
     * @param transformedName 
     * @param basicClass
     * @return
     */
    private byte[] applyMixins(MixinConfig config, String transformedName, byte[] basicClass) {
        // Tree for target class
        ClassNode targetClass = this.readClass(basicClass, true);
        
        // Get and sort mixins for the class
        List<MixinInfo> mixins = config.getMixinsFor(transformedName);
        Collections.sort(mixins);
        
        for (MixinInfo mixin : mixins) {
            this.logger.info("Mixing {} into {}", mixin.getName(), transformedName);
            this.applyMixin(targetClass, mixin.getData());
        }
        
        // Extension point
        this.postTransform(transformedName, targetClass, mixins);
        
        // Collapse tree to bytes
        byte[] bytes = this.writeClass(targetClass);
        
        // Export transformed class for debugging purposes
        if (MixinTransformer.DEBUG_EXPORT) {
            try {
                FileUtils.writeByteArrayToFile(new File(".mixin.out/" + transformedName.replace('.', '/') + ".class"), bytes);
            } catch (IOException ex) {
                // don't care
            }
        }
        
        return bytes;
    }

    /**
     * @param transformedName
     * @param targetClass
     * @param mixins
     */
    protected void postTransform(String transformedName, ClassNode targetClass, List<MixinInfo> mixins) {
        // Stub for subclasses
    }

    /**
     * Apply the mixin described by mixin to the supplied classNode
     * 
     * @param targetClass
     * @param mixin
     */
    protected void applyMixin(ClassNode targetClass, MixinData mixin) {
        try {
            this.verifyClasses(targetClass, mixin);
            this.applyMixinInterfaces(targetClass, mixin);
            this.applyMixinAttributes(targetClass, mixin);
            this.applyMixinFields(targetClass, mixin);
            this.applyMixinMethods(targetClass, mixin);
            this.applyInjections(targetClass, mixin);
        } catch (Exception ex) {
            throw new InvalidMixinException("Unexpecteded error whilst applying the mixin class", ex);
        }
    }

    /**
     * Perform pre-flight checks on the mixin and target classes
     * 
     * @param targetClass
     * @param mixin
     * @throws IOException
     */
    protected void verifyClasses(ClassNode targetClass, MixinData mixin) throws IOException {
        String superName = mixin.getClassNode().superName;
        if ("java/lang/Object".equals(superName)) {
            return;
        }
        while (targetClass.superName != null && !targetClass.superName.equals(superName)) {
            targetClass = this.readClass(Launch.classLoader.getClassBytes(targetClass.superName));
        }
        if (targetClass.superName == null) {
            throw new InvalidMixinException("Mixin classes must have a superclass that is also a superclass of their target class");
        }
    }

    /**
     * Mixin interfaces implemented by the mixin class onto the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinInterfaces(ClassNode targetClass, MixinData mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (!targetClass.interfaces.contains(interfaceName)) {
                targetClass.interfaces.add(interfaceName);
            }
        }
    }

    /**
     * Mixin misc attributes from mixin class onto the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinAttributes(ClassNode targetClass, MixinData mixin) {
        if (mixin.shouldSetSourceFile()) {
            targetClass.sourceFile = mixin.getClassNode().sourceFile;
        }
    }

    /**
     * Mixin fields from mixin class into the target class. It is vital that this is done before mixinMethods because we need to compute renamed
     * fields so that transformMethod can rename field references in the method body
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinFields(ClassNode targetClass, MixinData mixin) {
        for (FieldNode field : mixin.getClassNode().fields) {
            // Public static fields will fall foul of early static binding in java, including them in a mixin is an error condition
            if (MixinTransformer.hasFlag(field, Opcodes.ACC_STATIC) && !MixinTransformer.hasFlag(field, Opcodes.ACC_PRIVATE)) {
                throw new InvalidMixinException(String.format("Mixin classes cannot contain visible static methods or fields, found %s", field.name));
            }

            FieldNode target = this.findTargetField(targetClass, field);
            if (target == null) {
                // If this field is a shadow field but is NOT found in the target class, that's bad, mmkay
                boolean isShadow = ASMHelper.getVisibleAnnotation(field, Shadow.class) != null;
                if (isShadow) {
                    throw new InvalidMixinException(String.format("Shadow field %s was not located in the target class", field.name));
                }
                
                // This is just a local field, so add it
                targetClass.fields.add(field);
            } else {
                // Check that the shadow field has a matching descriptor
                if (!target.desc.equals(field.desc)) {
                    throw new InvalidMixinException(String.format("The field %s in the target class has a conflicting signature", field.name));
                }
            }
        }
    }

    /**
     * Mixin methods from the mixin class into the target class
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyMixinMethods(ClassNode targetClass, MixinData mixin) {
        for (MethodNode mixinMethod : mixin.getClassNode().methods) {
            // Reparent all mixin methods into the target class
            this.transformMethod(mixinMethod, mixin.getClassNode().name, targetClass.name);

            boolean isShadow = ASMHelper.getVisibleAnnotation(mixinMethod, Shadow.class) != null;
            boolean isOverwrite = ASMHelper.getVisibleAnnotation(mixinMethod, Overwrite.class) != null;
            boolean isAbstract = MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_ABSTRACT);
            
            if (isShadow || isAbstract) {
                // For shadow (and abstract, which can be used as a shorthand for Shadow) methods, we just check they're present
                MethodNode target = this.findTargetMethod(targetClass, mixinMethod);
                if (target == null) {
                    throw new InvalidMixinException(String.format("Shadow method %s was not located in the target class", mixinMethod.name));
                }
            } else if (!mixinMethod.name.startsWith("<")) {
                if (MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_STATIC)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_PRIVATE)
                        && !MixinTransformer.hasFlag(mixinMethod, Opcodes.ACC_SYNTHETIC)
                        && !isOverwrite) {
                    throw new InvalidMixinException(
                            String.format("Mixin classes cannot contain visible static methods or fields, found %s", mixinMethod.name));
                }

                MethodNode target = this.findTargetMethod(targetClass, mixinMethod);
                if (target != null) {
                    targetClass.methods.remove(target);
                } else if (isOverwrite) {
                    throw new InvalidMixinException(String.format("Overwrite target %s was not located in the target class", mixinMethod.name));
                }
                targetClass.methods.add(mixinMethod);
            } else if (MixinTransformer.CLINIT.equals(mixinMethod.name)) {
                // Class initialiser insns get appended
                this.appendInsns(targetClass, mixinMethod.name, mixinMethod);
            }
        }
    }

    /**
     * Handles "re-parenting" the method supplied, changes all references to the mixin class to refer to the target class (for field accesses and
     * method invokations) and also renames fields accesses to their obfuscated versions
     * 
     * @param method
     * @param fromClass
     * @param toClass
     * @return
     */
    private void transformMethod(MethodNode method, String fromClass, String toClass) {
        Iterator<AbstractInsnNode> iter = method.instructions.iterator();
        while (iter.hasNext()) {
            AbstractInsnNode insn = iter.next();

            if (insn instanceof MethodInsnNode) {
                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                if (methodInsn.owner.equals(fromClass)) {
                    methodInsn.owner = toClass;
                }
            }
            if (insn instanceof FieldInsnNode) {
                FieldInsnNode fieldInsn = (FieldInsnNode) insn;
                if (fieldInsn.owner.equals(fromClass)) {
                    fieldInsn.owner = toClass;
                }
            }
        }
    }

    /**
     * Handles appending instructions from the source method to the target method
     * 
     * @param targetClass
     * @param targetMethodName
     * @param sourceMethod
     */
    private void appendInsns(ClassNode targetClass, String targetMethodName, MethodNode sourceMethod) {
        if (Type.getReturnType(sourceMethod.desc) != Type.VOID_TYPE) {
            throw new IllegalArgumentException("Attempted to merge insns into a method which does not return void");
        }

        if (targetMethodName == null || targetMethodName.length() == 0) {
            targetMethodName = sourceMethod.name;
        }

        for (MethodNode method : targetClass.methods) {
            if ((targetMethodName.equals(method.name)) && sourceMethod.desc.equals(method.desc)) {
                AbstractInsnNode returnNode = null;
                Iterator<AbstractInsnNode> findReturnIter = method.instructions.iterator();
                while (findReturnIter.hasNext()) {
                    AbstractInsnNode insn = findReturnIter.next();
                    if (insn.getOpcode() == Opcodes.RETURN) {
                        returnNode = insn;
                        break;
                    }
                }

                Iterator<AbstractInsnNode> injectIter = sourceMethod.instructions.iterator();
                while (injectIter.hasNext()) {
                    AbstractInsnNode insn = injectIter.next();
                    if (!(insn instanceof LineNumberNode) && insn.getOpcode() != Opcodes.RETURN) {
                        method.instructions.insertBefore(returnNode, insn);
                    }
                }
            }
        }
    }

    /**
     * Process {@link Inject} annotations and inject callbacks to annotated methods
     * 
     * @param targetClass
     * @param mixin
     */
    private void applyInjections(ClassNode targetClass, MixinData mixin) {
        for (MethodNode method : targetClass.methods) {
            AnnotationNode injectAnnotation = ASMHelper.getVisibleAnnotation(method, Inject.class);
            if (injectAnnotation == null) {
                continue;
            }
            
            InjectionInfo injectInfo = new InjectionInfo(targetClass, method, mixin, injectAnnotation);
            if (injectInfo.isValid()) {
                injectInfo.inject();
            }
            
            method.visibleAnnotations.remove(injectAnnotation);
        }
    }
    
    /**
     * Finds a method in the target class
     * 
     * @param targetClass
     * @param searchFor
     * @return
     */
    private MethodNode findTargetMethod(ClassNode targetClass, MethodNode searchFor) {
        for (MethodNode target : targetClass.methods) {
            if (target.name.equals(searchFor.name) && target.desc.equals(searchFor.desc)) {
                return target;
            }
        }
        
        return null;
    }

    /**
     * Finds a field in the target class
     * 
     * @param targetClass
     * @param searchFor
     * @return
     */
    private FieldNode findTargetField(ClassNode targetClass, FieldNode searchFor) {
        for (FieldNode target : targetClass.fields) {
            if (target.name.equals(searchFor.name)) {
                return target;
            }
        }

        return null;
    }
    
    /**
     * Check whether the specified flag is set on the specified method
     * 
     * @param method
     * @param flag 
     * @return
     */
    private static boolean hasFlag(MethodNode method, int flag) {
        return (method.access & flag) == flag;
    }
    
    /**
     * Check whether the specified flag is set on the specified field
     * 
     * @param field
     * @param flag 
     * @return
     */
    private static boolean hasFlag(FieldNode field, int flag) {
        return (field.access & flag) == flag;
    }
}
