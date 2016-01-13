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
package org.spongepowered.asm.mixin.injection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.struct.InjectionInfo;
import org.spongepowered.asm.util.ASMHelper;

/**
 * Information store for injector groups
 */
public class InjectorGroupInfo {
    
    /**
     * Storage for injector groups
     */
    public static final class Map extends HashMap<String, InjectorGroupInfo> {
        
        private static final long serialVersionUID = 1L;
        
        private static final InjectorGroupInfo NO_GROUP = new InjectorGroupInfo("NONE");
        
        @Override
        public InjectorGroupInfo get(Object key) {
            return this.forName(key.toString());
        }

        /**
         * Get group for the specified name, creates the group in this map if
         * it does not already exist
         * 
         * @param name Name of group to fetch
         * @return Existing group or new group if none was previously declared
         */
        public InjectorGroupInfo forName(String name) {
            InjectorGroupInfo value = super.get(name);
            if (value == null) {
                value = new InjectorGroupInfo(name);
                this.put(name, value);
            }
            return value;
        }
        
        /**
         * Parse a group from the specified method, use the default group name
         * if no group name is specified on the annotation
         * 
         * @param method (Possibly) annotated method
         * @param defaultGroup Default group name to use
         * @return Group or NO_GROUP if no group
         */
        public InjectorGroupInfo parseGroup(MethodNode method, String defaultGroup) {
            return this.parseGroup(ASMHelper.getInvisibleAnnotation(method, Group.class), defaultGroup);
        }
        
        /**
         * Parse a group from the specified annotation, use the default group
         * name if no group name is specified on the annotation
         * 
         * @param annotation Annotation or null
         * @param defaultGroup Default group name to use
         * @return Group or NO_GROUP if no group
         */
        public InjectorGroupInfo parseGroup(AnnotationNode annotation, String defaultGroup) {
            if (annotation == null) {
                return InjectorGroupInfo.Map.NO_GROUP;
            }
            
            String name = ASMHelper.<String>getAnnotationValue(annotation, "name");
            if (name == null || name.isEmpty()) {
                name = defaultGroup;
            }
            InjectorGroupInfo groupInfo = this.forName(name);
            
            Integer require = ASMHelper.<Integer>getAnnotationValue(annotation, "require");
            if (require != null) {
                if (require.intValue() != -1) {
                    groupInfo.setRequired(require.intValue());
                }
            }
            
            return groupInfo;
        }
        
        /**
         * Validate all groups in this collection
         * 
         * @throws InjectionValidationException if validation fails
         */
        public void validateAll() throws InjectionValidationException {
            for (InjectorGroupInfo group : this.values()) {
                group.validate();
            }
        }
        
    }

    /**
     * Group name
     */
    private final String name;
    
    /**
     * Members of this group
     */
    private final List<InjectionInfo> members = new ArrayList<InjectionInfo>();
    
    /**
     * Number of callbacks we require injected across this group
     */
    private int requiredCallbackCount = -1;
    
    public InjectorGroupInfo(String name) {
        this.name = name;
    }
    
    @Override
    public String toString() {
        return String.format("@Group(name=%s, require=%d)", this.getName(), this.getRequired());
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getRequired() {
        return Math.max(this.requiredCallbackCount, 1);
    }
    
    /**
     * Get all members of this group as a read-only collection
     * 
     * @return read-only view of group members
     */
    public Collection<InjectionInfo> getMembers() {
        return Collections.unmodifiableCollection(this.members);
    }
    
    /**
     * Set the required value for this group. Since this is normally done on the
     * first {@link Group} annotation it is considered a warning-level event if
     * a later annotation sets a different value. The highest value specified on
     * all annotations is always used
     * 
     * @param required new value for required
     */
    public void setRequired(int required) {
        if (required < 1) {
            throw new IllegalArgumentException("Cannot set zero or negative value for injector group required count. Attempted to set required="
                    + required + " on " + this + "");
        }
        if (this.requiredCallbackCount > 0 && this.requiredCallbackCount != required) {
            LogManager.getLogger("mixin").warn("Conflicting require value '{}' on @Group({}), previously specified {}", required, this.name,
                    this.requiredCallbackCount);
        }
        this.requiredCallbackCount = Math.max(this.requiredCallbackCount, required);
    }
    
    /**
     * Add a new member to this group
     * 
     * @param member injector to add
     * @return fluent interface
     */
    public InjectorGroupInfo add(InjectionInfo member) {
        this.members.add(member);
        return this;
    }
    
    /**
     * Validate all members in this group
     * 
     * @return fluent interface
     * @throws InjectionValidationException if validation fails
     */
    public InjectorGroupInfo validate() throws InjectionValidationException {
        if (this.members.size() == 0) {
            // I have no idea how we got here, but it's not an error :/
            return this;
        }
        
        int total = 0;
        for (InjectionInfo member : this.members) {
            total += member.getInjectedCallbackCount();
        }
        
        int required = this.getRequired();
        if (total < required) {
            throw new InjectionValidationException(this, String.format("expected %d invocation(s) but only %d succeeded", required, total));
        }
        
        return this;
    }
}
