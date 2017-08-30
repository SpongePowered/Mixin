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
package org.spongepowered.asm.mixin.injection.struct;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.throwables.InjectionValidationException;
import org.spongepowered.asm.util.Annotations;

/**
 * Information store for injector groups
 */
public class InjectorGroupInfo {
    
    /**
     * Storage for injector groups
     */
    public static final class Map extends HashMap<String, InjectorGroupInfo> {
        
        private static final long serialVersionUID = 1L;
        
        private static final InjectorGroupInfo NO_GROUP = new InjectorGroupInfo("NONE", true);
        
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
            return this.parseGroup(Annotations.getInvisible(method, Group.class), defaultGroup);
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
            
            String name = Annotations.<String>getValue(annotation, "name");
            if (name == null || name.isEmpty()) {
                name = defaultGroup;
            }
            InjectorGroupInfo groupInfo = this.forName(name);
            
            Integer min = Annotations.<Integer>getValue(annotation, "min");
            if (min != null && min.intValue() != -1) {
                groupInfo.setMinRequired(min.intValue());
            }
            
            Integer max = Annotations.<Integer>getValue(annotation, "max");
            if (max != null && max.intValue() != -1) {
                groupInfo.setMaxAllowed(max.intValue());
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
     * True if this is the default group
     */
    private final boolean isDefault;
    
    /**
     * Number of callbacks we require injected across this group
     */
    private int minCallbackCount = -1;
    
    /**
     * Maximum number of callbacks allowed across this group 
     */
    private int maxCallbackCount = Integer.MAX_VALUE;

    public InjectorGroupInfo(String name) {
        this(name, false);
    }
    
    InjectorGroupInfo(String name, boolean flag) {
        this.name = name;
        this.isDefault = flag;
    }
    
    @Override
    public String toString() {
        return String.format("@Group(name=%s, min=%d, max=%d)", this.getName(), this.getMinRequired(), this.getMaxAllowed());
    }
    
    public boolean isDefault() {
        return this.isDefault;
    }
    
    public String getName() {
        return this.name;
    }
    
    public int getMinRequired() {
        return Math.max(this.minCallbackCount, 1);
    }
    
    public int getMaxAllowed() {
        return Math.min(this.maxCallbackCount, Integer.MAX_VALUE);
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
     * Set the required minimum value for this group. Since this is normally
     * done on the first {@link Group} annotation it is considered a
     * warning-level event if a later annotation sets a different value. The
     * highest value specified on all annotations is always used.
     * 
     * @param min new value for min required
     */
    public void setMinRequired(int min) {
        if (min < 1) {
            throw new IllegalArgumentException("Cannot set zero or negative value for injector group min count. Attempted to set min="
                    + min + " on " + this);
        }
        if (this.minCallbackCount > 0 && this.minCallbackCount != min) {
            LogManager.getLogger("mixin").warn("Conflicting min value '{}' on @Group({}), previously specified {}", min, this.name,
                    this.minCallbackCount);
        }
        this.minCallbackCount = Math.max(this.minCallbackCount, min);
    }
    
    /**
     * Set the required minimum value for this group. Since this is normally
     * done on the first {@link Group} annotation it is considered a
     * warning-level event if a later annotation sets a different value. The
     * highest value specified on all annotations is always used.
     * 
     * @param max new value for max allowed
     */
    public void setMaxAllowed(int max) {
        if (max < 1) {
            throw new IllegalArgumentException("Cannot set zero or negative value for injector group max count. Attempted to set max="
                    + max + " on " + this);
        }
        if (this.maxCallbackCount < Integer.MAX_VALUE && this.maxCallbackCount != max) {
            LogManager.getLogger("mixin").warn("Conflicting max value '{}' on @Group({}), previously specified {}", max, this.name,
                    this.maxCallbackCount);
        }
        this.maxCallbackCount = Math.min(this.maxCallbackCount, max);
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
        
        int min = this.getMinRequired();
        int max = this.getMaxAllowed();
        if (total < min) {
            throw new InjectionValidationException(this, String.format("expected %d invocation(s) but only %d succeeded", min, total));
        } else if (total > max) {
            throw new InjectionValidationException(this, String.format("maximum of %d invocation(s) allowed but %d succeeded", max, total));
        }
        
        return this;
    }
}
