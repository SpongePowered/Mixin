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

import java.lang.annotation.Annotation;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Field;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.meta.MixinRenamed;
import org.spongepowered.asm.util.ASMHelper;
import org.spongepowered.asm.util.Constants;


/**
 * <p>Mixin bytecode pre-processor. This class is responsible for bytecode pre-
 * processing tasks required to be performed on mixin bytecode before the mixin
 * can be applied. In previous versions the duties performed by this class were
 * performed by {@link MixinInfo}.</p>
 * 
 * <p>Before a mixin can be applied to the target class, it is necessary to
 * convert certain aspects of the mixin bytecode into the intended final form of
 * the mixin, this involves for example stripping the prefix from shadow and
 * soft-implemented methods. This preparation is done in two stages: first the
 * target-context-insensitive transformations are applied (this also acts as a
 * validation pass when the mixin is first loaded) and then transformations
 * which depend on the target class are applied in a second stage.</p>
 * 
 * <p>The validation pass propagates method renames into the metadata tree and
 * thus changes made during this phase are visible to all other mixins. The
 * target-context-sensitive pass on the other hand can only operate on private
 * class members for obvious reasons.</p>  
 */
class MixinPreProcessor {

    /**
     * The mixin
     */
    private final MixinInfo mixin;
    
    /**
     * Mixin class node
     */
    private final ClassNode classNode;
    
    private boolean prepared, attached;

    MixinPreProcessor(MixinInfo mixin, ClassNode classNode) {
        this.mixin = mixin;
        this.classNode = classNode;
    }

    /**
     * Run the first pass. Propagates changes into the metadata tree.
     * 
     * @return Prepared classnode
     */
    ClassNode prepare() {
        if (!this.prepared) {
            this.prepared = true;
            
            for (MethodNode mixinMethod : this.classNode.methods) {
                Method method = this.mixin.getClassInfo().findMethod(mixinMethod);
                this.prepareShadow(mixinMethod, method);
                this.prepareSoftImplements(mixinMethod, method);
            }
        }
        
        return this.classNode;
    }

    private void prepareShadow(MethodNode mixinMethod, Method method) {
        AnnotationNode shadowAnnotation = ASMHelper.getVisibleAnnotation(mixinMethod, Shadow.class);
        if (shadowAnnotation == null) {
            return;
        }
        
        String prefix = ASMHelper.<String>getAnnotationValue(shadowAnnotation, "prefix", Shadow.class);
        if (mixinMethod.name.startsWith(prefix)) {
            ASMHelper.setVisibleAnnotation(mixinMethod, MixinRenamed.class, "originalName", mixinMethod.name);
            String newName = mixinMethod.name.substring(prefix.length());
            method.renameTo(newName);
            mixinMethod.name = newName;
        }
    }

    private void prepareSoftImplements(MethodNode mixinMethod, Method method) {
        for (InterfaceInfo iface : this.mixin.getSoftImplements()) {
            if (iface.renameMethod(mixinMethod)) {
                method.renameTo(mixinMethod.name);
            }
        }
    }

    MixinTargetContext createContextFor(ClassNode target) {
        this.prepare();
        MixinTargetContext context = new MixinTargetContext(this.mixin, this.classNode, target);
        this.attach(context);
        return context;
    }

    /**
     * Run the second pass, attach to the specified context
     * 
     * @param context
     */
    void attach(MixinTargetContext context) {
        if (this.attached) {
            throw new IllegalStateException("Preprocessor was already attached");
        }
        
        this.attached = true;
        
        // Perform context-sensitive attachment phase
        this.attachMethods(context);
        this.attachFields(context);
        
        // Apply transformations to the mixin bytecode
        this.transform(context);
    }

    private void attachMethods(MixinTargetContext context) {
        for (Iterator<MethodNode> iter = this.classNode.methods.iterator(); iter.hasNext();) {
            MethodNode mixinMethod = iter.next();
            
            if (this.processMethod(context, mixinMethod, Shadow.class, true, true)) {
                iter.remove();
                context.addShadowMethod(mixinMethod);
                continue;
            }

            this.processMethod(context, mixinMethod, Overwrite.class, false, false);
        }
    }

    private boolean processMethod(MixinTargetContext context, MethodNode mixinMethod, Class<? extends Annotation> annotationType,
            boolean mustExist, boolean mustBePrivate) {
        AnnotationNode annotation = ASMHelper.getVisibleAnnotation(mixinMethod, annotationType);
        if (annotation == null) {
            return false;
        }
        
        Method method = this.mixin.getClassInfo().findMethod(mixinMethod, true);
        MethodNode target = MixinPreProcessor.findMethod(context.getTargetClass(), mixinMethod, annotation);
        
        if (target == null) {
            if (!mustExist) {
                return false;
            }
            
            throw new InvalidMixinException(this.mixin, annotationType.getSimpleName() + " method " + mixinMethod.name
                    + " was not located in the target class");
        }
        
        if (Constants.INIT.equals(target.name)) {
            throw new InvalidMixinException(this.mixin, "Nice try! Cannot alias a constructor!");
        }
        
        if (!target.name.equals(mixinMethod.name)) {
            if (mustBePrivate && (target.access & Opcodes.ACC_PRIVATE) == 0) {
                throw new InvalidMixinException(this.mixin, "Non-private method cannot be aliased. Found " + target.name);
            }
            
            mixinMethod.name = target.name;
            method.renameTo(target.name);
        }
        
        return true;
    }

    private void attachFields(MixinTargetContext context) {
        for (Iterator<FieldNode> iter = this.classNode.fields.iterator(); iter.hasNext();) {
            FieldNode mixinField = iter.next();
            AnnotationNode shadow = ASMHelper.getVisibleAnnotation(mixinField, Shadow.class);
            if (!this.validateField(context, mixinField, shadow)) {
                iter.remove();
                continue;
            }
            
            context.transformDescriptor(mixinField);
            
            Field field = this.mixin.getClassInfo().findField(mixinField);
            FieldNode target = this.findField(context.getTargetClass(), mixinField, shadow);
            if (target == null) {
                // If this field is a shadow field but is NOT found in the target class, that's bad, mmkay
                if (shadow != null) {
                    throw new InvalidMixinException(this.mixin, "Shadow field " + mixinField.name + " was not located in the target class");
                }
            } else {
                // Check that the shadow field has a matching descriptor
                if (!target.desc.equals(mixinField.desc)) {
                    throw new InvalidMixinException(this.mixin, "The field " + mixinField.name + " in the target class has a conflicting signature");
                }
                
                if (!target.name.equals(mixinField.name)) {
                    if ((target.access & Opcodes.ACC_PRIVATE) == 0 && (target.access & Opcodes.ACC_SYNTHETIC) == 0) {
                        throw new InvalidMixinException(this.mixin, "Non-private field cannot be aliased. Found " + target.name);
                    }
                    
                    mixinField.name = target.name;
                    field.renameTo(target.name);
                }
                
                // Shadow fields get stripped from the mixin class
                iter.remove();
                
                if (shadow != null) {
                    context.addShadowField(mixinField);
                }
            }
        }
    }

    private boolean validateField(MixinTargetContext context, FieldNode field, AnnotationNode shadow) {
        // Public static fields will fall foul of early static binding in java, including them in a mixin is an error condition
        if (MixinApplicator.hasFlag(field, Opcodes.ACC_STATIC)
                && !MixinApplicator.hasFlag(field, Opcodes.ACC_PRIVATE)
                && !MixinApplicator.hasFlag(field, Opcodes.ACC_SYNTHETIC)) {
            throw new InvalidMixinException(context, String.format("Mixin classes cannot contain visible static methods or fields, found %s",
                    field.name));
        }

        // Shadow fields can't have prefixes, it's meaningless for them anyway
        String prefix = ASMHelper.<String>getAnnotationValue(shadow, "prefix", Shadow.class);
        if (field.name.startsWith(prefix)) {
            throw new InvalidMixinException(context, String.format("Shadow field %s in %s has a shadow prefix. This is not allowed.",
                    field.name, context));
        }
        
        // Imaginary super fields get stripped from the class, but first we validate them
        if (Constants.IMAGINARY_SUPER.equals(field.name)) {
            if (field.access != Opcodes.ACC_PRIVATE) {
                throw new InvalidMixinException(this.mixin, "Imaginary super field " + field.name + " must be private and non-final");
            }
            if (!field.desc.equals("L" + this.mixin.getClassRef() + ";")) {
                throw new InvalidMixinException(this.mixin, "Imaginary super field " + field.name + " must have the same type as the parent mixin");
            }
            return false;
        }
        
        return true;
    }

    /**
     * Apply discovered method and field renames to method invocations and field
     * accesses in the mixin
     */
    private void transform(MixinTargetContext context) {
        for (MethodNode mixinMethod : this.classNode.methods) {
            for (Iterator<AbstractInsnNode> iter = mixinMethod.instructions.iterator(); iter.hasNext();) {
                AbstractInsnNode insn = iter.next();
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodNode = (MethodInsnNode)insn;
                    Method method = this.mixin.getClassInfo().findMethodInHierarchy(methodNode, true, true);
                    if (method != null && method.isRenamed()) {
                        methodNode.name = method.getName();
                    }
                } else if (insn instanceof FieldInsnNode) {
                    FieldInsnNode fieldNode = (FieldInsnNode)insn;
                    Field field = this.mixin.getClassInfo().findField(fieldNode, true);
                    if (field != null && field.isRenamed()) {
                        fieldNode.name = field.getName();
                    }
                }
            }
        }
    }

    private static MethodNode findMethod(ClassNode classNode, MethodNode method, AnnotationNode annotation) {
        Deque<String> aliases = new LinkedList<String>();
        aliases.add(method.name);
        if (annotation != null) {
            List<String> aka = ASMHelper.<List<String>>getAnnotationValue(annotation, "aliases");
            if (aka != null) {
                aliases.addAll(aka);
            }
        }
        
        return MixinPreProcessor.findMethod(classNode, aliases, method.desc);
    }

    private static MethodNode findMethod(ClassNode classNode, Deque<String> aliases, String desc) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        
        for (MethodNode target : classNode.methods) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
                return target;
            }
        }

        return MixinPreProcessor.findMethod(classNode, aliases, desc);
    }

    private FieldNode findField(ClassNode classNode, FieldNode field, AnnotationNode shadow) {
        Deque<String> aliases = new LinkedList<String>();
        aliases.add(field.name);
        if (shadow != null) {
            List<String> aka = ASMHelper.<List<String>>getAnnotationValue(shadow, "aliases");
            if (aka != null) {
                aliases.addAll(aka);
            }
        }
        
        return this.findField(classNode, aliases, field.desc);
    }

    /**
     * Finds a field in the target class
     * 
     * @param aliases 
     * @param desc
     * @return Target field  or null if not found
     */
    private FieldNode findField(ClassNode classNode, Deque<String> aliases, String desc) {
        String alias = aliases.poll();
        if (alias == null) {
            return null;
        }
        
        for (FieldNode target : classNode.fields) {
            if (target.name.equals(alias) && target.desc.equals(desc)) {
                return target;
            }
        }

        return this.findField(classNode, aliases, desc);
    }
}
