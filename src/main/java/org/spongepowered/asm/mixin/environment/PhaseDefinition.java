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
package org.spongepowered.asm.mixin.environment;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PhaseDefinition {
    
    private final String name;
    
    private final Set<String> before = new HashSet<String>();
    
    private final Set<String> after = new HashSet<String>(); 

    private final List<IPhaseTransition> criteria = new ArrayList<IPhaseTransition>();

    private PhaseDefinition(String name) {
        checkArgument(!name.isEmpty(), "name cannot be empty");
        this.name = name.toUpperCase();
    }
    
    public PhaseDefinition before(String phase) {
        phase = checkNotNull(phase, "before phase is null").toUpperCase();
        checkArgument(!phase.equals(this.name), this.name + " cannot preceed itself");
        checkArgument(!phase.isEmpty(), "phase cannot be empty");
        this.before.add(phase);
        return this;
    }
    
    public PhaseDefinition after(String phase) {
        phase = checkNotNull(phase, "after phase is null").toUpperCase();
        checkArgument(!phase.equals(this.name), this.name + " cannot succeed itself");
        checkArgument(!phase.isEmpty(), "phase cannot be empty");
        this.after.add(phase);
        return this;
    }
    
    public PhaseDefinition when(IPhaseTransition transition) {
        if (!this.criteria.contains(checkNotNull(transition, "criterion is null"))) {
            this.criteria.add(transition);
        }
        return this;
    }
    
    public String getName() {
        return this.name;
    }
    
    public Set<String> getAfter() {
        return this.after;
    }
    
    public Set<String> getBefore() {
        return this.before;
    }
    
    public List<IPhaseTransition> getCriteria() {
        return this.criteria;
    }
    
    public static PhaseDefinition named(String name) {
        return new PhaseDefinition(name);
    }
    
}
