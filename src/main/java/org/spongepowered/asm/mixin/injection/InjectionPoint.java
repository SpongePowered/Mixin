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
package org.spongepowered.asm.mixin.injection;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.mixin.injection.points.BeforeFieldAccess;
import org.spongepowered.asm.mixin.injection.points.BeforeInvoke;
import org.spongepowered.asm.mixin.injection.points.BeforeNew;
import org.spongepowered.asm.mixin.injection.points.BeforeReturn;
import org.spongepowered.asm.mixin.injection.points.BeforeStringInvoke;
import org.spongepowered.asm.mixin.injection.points.JumpInsnPoint;
import org.spongepowered.asm.mixin.injection.points.MethodHead;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.ASMHelper;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * <p>Base class for injection point discovery classes. Each subclass describes
 * a strategy for locating code injection points within a method, with the
 * {@link #find} method populating a collection with insn nodes from the method
 * which satisfy its strategy.</p>
 * 
 * <p>This base class also contains composite strategy factory methods such as
 * {@link #and} and {@link #or} which allow strategies to be combined using
 * intersection (and) or union (or) relationships to allow multiple strategies
 * to be easily combined.</p>
 * 
 * <p>You are free to create your own injection point subclasses, but take note
 * that it <b>is allowed</b> for a single InjectionPoint instance to be used for
 * multiple injections and thus implementing classes MUST NOT cache the insn
 * list, event, or nodes instance passed to the {@link #find} method, as each
 * call to {@link #find} must be considered a separate functional contract and
 * the InjectionPoint's lifespan is not linked to the discovery lifespan,
 * therefore it is important that the InjectionPoint implementation is fully
 * <b>stateless</b>.</p>
 */
public abstract class InjectionPoint {

    /**
     * Find injection points in the supplied insn list
     * 
     * @param desc Method descriptor, supplied to allow return types and
     *      arguments etc. to be determined
     * @param insns Insn list to search in, the strategy MUST ONLY add nodes
     *      from this list to the {@code nodes} collection
     * @param nodes Collection of nodes to populate. Injectors should NOT make
     *      any assumptions about the state of this collection and should only
     *      call the <b>add()</b> method
     * @return true if one or more injection points were found
     */
    public abstract boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes);

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "InjectionPoint(" + this.getClass().getSimpleName() + ")";
    }

    /**
     * Composite injection point
     */
    abstract static class CompositeInjectionPoint extends InjectionPoint {

        protected final InjectionPoint[] components;

        protected CompositeInjectionPoint(InjectionPoint... components) {
            if (components == null || components.length < 2) {
                throw new IllegalArgumentException("Must supply two or more component injection points for composite point!");
            }

            this.components = components;
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.InjectionPoint#toString()
         */
        @Override
        public String toString() {
            return "CompositeInjectionPoint(" + this.getClass().getSimpleName() + ")[" + Joiner.on(',').join(this.components) + "]";
        }
    }

    /**
     * Intersection of several injection points, returns common nodes that
     * appear in all children
     */
    static final class Intersection extends InjectionPoint.CompositeInjectionPoint {

        public Intersection(InjectionPoint... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            boolean found = false;

            @SuppressWarnings({ "unchecked", "rawtypes" })
            ArrayList<AbstractInsnNode>[] allNodes = new ArrayList[this.components.length];

            for (int i = 0; i < this.components.length; i++) {
                allNodes[i] = new ArrayList<AbstractInsnNode>();
                this.components[i].find(desc, insns, allNodes[i]);
            }

            ArrayList<AbstractInsnNode> alpha = allNodes[0];
            for (int nodeIndex = 0; nodeIndex < alpha.size(); nodeIndex++) {
                AbstractInsnNode node = alpha.get(nodeIndex);
                boolean in = true;

                for (int b = 1; b < allNodes.length; b++) {
                    if (!allNodes[b].contains(node)) {
                        break;
                    }
                }

                if (!in) {
                    continue;
                }

                nodes.add(node);
                found = true;
            }

            return found;
        }
    }

    /**
     * Union of several injection points, returns all insns returned from all
     * injections
     */
    static final class Union extends InjectionPoint.CompositeInjectionPoint {

        public Union(InjectionPoint... points) {
            super(points);
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            LinkedHashSet<AbstractInsnNode> allNodes = new LinkedHashSet<AbstractInsnNode>();

            for (int i = 0; i < this.components.length; i++) {
                this.components[i].find(desc, insns, allNodes);
            }

            nodes.addAll(allNodes);

            return allNodes.size() > 0;
        }
    }

    /**
     * Shift injection point, takes an input injection point and shifts all
     * returned nodes by a fixed amount
     */
    static final class Shift extends InjectionPoint {

        private final InjectionPoint input;
        private final int shift;

        public Shift(InjectionPoint input, int shift) {
            if (input == null) {
                throw new IllegalArgumentException("Must supply an input injection point for SHIFT");
            }

            this.input = input;
            this.shift = shift;
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.mixin.injection.InjectionPoint#toString()
         */
        @Override
        public String toString() {
            return "InjectionPoint(" + this.getClass().getSimpleName() + ")[" + this.input + "]";
        }

        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            List<AbstractInsnNode> list = (nodes instanceof List) ? (List<AbstractInsnNode>) nodes : new ArrayList<AbstractInsnNode>(nodes);

            this.input.find(desc, insns, nodes);

            for (int i = 0; i < list.size(); i++) {
                list.set(i, insns.get(insns.indexOf(list.get(i)) + this.shift));
            }

            if (nodes != list) {
                nodes.clear();
                nodes.addAll(list);
            }

            return nodes.size() > 0;
        }
    }

    /**
     * Returns a composite injection point which returns the intersection of
     * nodes from all component injection points
     * 
     * @param operands injection points to perform intersection
     * @return adjusted InjectionPoint 
     */
    public static InjectionPoint and(InjectionPoint... operands) {
        return new InjectionPoint.Intersection(operands);
    }

    /**
     * Returns a composite injection point which returns the union of nodes from
     * all component injection points
     * 
     * @param operands injection points to perform union
     * @return adjusted InjectionPoint 
     */
    public static InjectionPoint or(InjectionPoint... operands) {
        return new InjectionPoint.Union(operands);
    }

    /**
     * Returns an injection point which returns all insns immediately following
     * insns from the supplied injection point
     * 
     * @param point injection points to perform shift
     * @return adjusted InjectionPoint 
     */
    public static InjectionPoint after(InjectionPoint point) {
        return new InjectionPoint.Shift(point, 1);
    }

    /**
     * Returns an injection point which returns all insns immediately prior to
     * insns from the supplied injection point
     * 
     * @param point injection points to perform shift
     * @return adjusted InjectionPoint 
     */
    public static InjectionPoint before(InjectionPoint point) {
        return new InjectionPoint.Shift(point, -1);
    }

    /**
     * Returns an injection point which returns all insns offset by the
     * specified "count" from insns from the supplied injection point
     * 
     * @param point injection points to perform shift
     * @param count amount to shift by
     * @return adjusted InjectionPoint 
     */
    public static InjectionPoint shift(InjectionPoint point, int count) {
        return new InjectionPoint.Shift(point, count);
    }

    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation
     * 
     * @param mixin Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param at {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(MixinTargetContext mixin, At at) {
        return InjectionPoint.parse(mixin, at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.ordinal(), at.opcode());
    }

    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation supplied
     * as an AnnotationNode instance
     * 
     * @param mixin Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param node {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(MixinTargetContext mixin, AnnotationNode node) {
        String at = ASMHelper.<String>getAnnotationValue(node, "value");
        List<String> args = ASMHelper.<List<String>>getAnnotationValue(node, "args");
        String target = ASMHelper.<String>getAnnotationValue(node, "target", "");
        At.Shift shift = ASMHelper.<At.Shift>getAnnotationValue(node, "shift", At.Shift.class, At.Shift.NONE);
        int by = ASMHelper.<Integer>getAnnotationValue(node, "by", Integer.valueOf(0));
        int ordinal = ASMHelper.<Integer>getAnnotationValue(node, "ordinal", Integer.valueOf(-1));
        int opcode = ASMHelper.<Integer>getAnnotationValue(node, "opcode", Integer.valueOf(0));

        if (args == null) {
            args = ImmutableList.<String>of();
        }

        return InjectionPoint.parse(mixin, at, shift, by, args, target, ordinal, opcode);
    }

    /**
     * Parse and instantiate an InjectionPoint from the supplied information.
     * Returns null if an InjectionPoint could not be created.
     * 
     * @param mixin Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param at Injection point specifier
     * @param shift Shift type to apply
     * @param by Amount of shift to apply for the BY shift type 
     * @param args Named parameters
     * @param target Target for supported injection points
     * @param ordinal Ordinal offset for supported injection points
     * @param opcode Bytecode opcode for supported injection points
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(MixinTargetContext mixin, String at, At.Shift shift, int by,
            List<String> args, String target, int ordinal, int opcode) {
        InjectionPointData data = new InjectionPointData(mixin, args, target, ordinal, opcode);
        InjectionPoint point = null;

        if (BeforeFieldAccess.CODE.equals(at)) {
            point = new BeforeFieldAccess(data);
        } else if (BeforeInvoke.CODE.equals(at)) {
            point = new BeforeInvoke(data);
        } else if (BeforeNew.CODE.equals(at)) {
            point = new BeforeNew(data);
        } else if (BeforeReturn.CODE.equals(at)) {
            point = new BeforeReturn(data);
        } else if (BeforeStringInvoke.CODE.equals(at)) {
            point = new BeforeStringInvoke(data);
        } else if (JumpInsnPoint.CODE.equals(at)) {
            point = new JumpInsnPoint(data);
        } else if (MethodHead.CODE.equals(at)) {
            point = new MethodHead(data);
        } else if (at.matches("^([A-Za-z_][A-Za-z0-9_]*\\.)+[A-Za-z_][A-Za-z0-9_]*$")) {
            try {
                @SuppressWarnings("unchecked") Class<? extends InjectionPoint> cls = (Class<? extends InjectionPoint>) Class.forName(at);
                Constructor<? extends InjectionPoint> ctor = cls.getDeclaredConstructor(InjectionPointData.class);
                ctor.setAccessible(true);
                point = ctor.newInstance(data);
            } catch (Exception ex) {
                throw new InvalidInjectionException(mixin, "The specified class " + at
                        + " could not be instanced or is not a valid InjectionPoint", ex);
            }
        } else {
            throw new InvalidInjectionException(mixin, at + " is not a valid injection point specifier");
        }

        if (point != null) {
            if (shift == At.Shift.BEFORE) {
                return InjectionPoint.before(point);
            } else if (shift == At.Shift.AFTER) {
                return InjectionPoint.after(point);
            } else if (shift == At.Shift.BY) {
                return InjectionPoint.shift(point, by);
            }
        }

        return point;
    }
}
