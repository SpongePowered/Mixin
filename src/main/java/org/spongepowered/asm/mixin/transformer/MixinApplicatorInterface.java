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
package org.spongepowered.asm.mixin.transformer;

import java.util.Map.Entry;

import org.spongepowered.asm.lib.tree.FieldNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Field;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidInterfaceMixinException;

/**
 * Applicator for interface mixins, mainly just disables things which aren't
 * supported for interface mixins
 */
class MixinApplicatorInterface extends MixinApplicatorStandard {

    /**
     * ctor
     * 
     * @param context applier context
     */
    MixinApplicatorInterface(TargetClassContext context) {
        super(context);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinApplicator
     *      #applyInterfaces(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext)
     */
    @Override
    protected void applyInterfaces(MixinTargetContext mixin) {
        for (String interfaceName : mixin.getInterfaces()) {
            if (!this.targetClass.name.equals(interfaceName) && !this.targetClass.interfaces.contains(interfaceName)) {
                this.targetClass.interfaces.add(interfaceName);
                mixin.getTargetClassInfo().addInterface(interfaceName);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinApplicator
     *      #applyFields(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext)
     */
    @Override
    protected void applyFields(MixinTargetContext mixin) {
        // shadow fields have no meaning for interfaces, just spam the dev with messages
        for (Entry<FieldNode, Field> entry : mixin.getShadowFields()) {
            FieldNode shadow = entry.getKey();
            this.logger.error("Ignoring redundant @Shadow field {}:{} in {}", shadow.name, shadow.desc, mixin);
        }
        
        this.mergeNewFields(mixin);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinApplicator
     *      #applyInitialisers(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext)
     */
    @Override
    protected void applyInitialisers(MixinTargetContext mixin) {
        // disabled for interface mixins
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinApplicator
     *      #prepareInjections(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext)
     */
    @Override
    protected void prepareInjections(MixinTargetContext mixin) {
        // disabled for interface mixins
        for (MethodNode method : this.targetClass.methods) {
            try {
                InjectionInfo injectInfo = InjectionInfo.parse(mixin, method);
                if (injectInfo != null) {
                    throw new InvalidInterfaceMixinException(mixin, injectInfo + " is not supported on interface mixin method " + method.name);
                }
            } catch (InvalidInjectionException ex) {
                String description = ex.getInjectionInfo() != null ? ex.getInjectionInfo().toString() : "Injection";
                throw new InvalidInterfaceMixinException(mixin, description + " is not supported in interface mixin");
            }
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.MixinApplicator
     *      #applyInjections(
     *      org.spongepowered.asm.mixin.transformer.MixinTargetContext)
     */
    @Override
    protected void applyInjections(MixinTargetContext mixin) {
        // Do nothing
    }

}
