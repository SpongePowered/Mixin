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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.injection.modify.AfterStoreLocal;
import org.spongepowered.asm.mixin.injection.modify.BeforeLoadLocal;
import org.spongepowered.asm.mixin.injection.points.*;
import org.spongepowered.asm.mixin.injection.struct.InjectionPointData;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

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
     * Injection point code for {@link At} annotations to use
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface AtCode {
        
        /**
         * The string code used to specify the annotated injection point in At
         * annotations
         */
        public String value();
        
    }
    
    /**
     * Selector type for slice delmiters, ignored for normal injection points.
     * <tt>Selectors</tt> can be supplied in {@link At} annotations by including
     * a colon (<tt>:</tt>) character followed by the selector type
     * (case-sensitive), eg:
     * 
     * <blockquote><pre>&#064;At(value = "INVOKE:LAST", ... )</pre></blockquote>
     */
    public enum Selector {

        /**
         * Use the <em>first</em> instruction from the query result.
         */
        FIRST,
        
        /**
         * Use the <em>last</em> instruction from the query result.
         */
        LAST,
        
        /**
         * The query <b>must return exactly one</b> instruction, if it returns
         * more than one instruction this should be considered a fail-fast error
         * state and a runtime exception will be thrown.
         */
        ONE;
        
        /**
         * Default selector type used if no selector is explicitly specified.
         * <em>For internal use only. Currently {@link #FIRST}</em>
         */
        public static final Selector DEFAULT = Selector.FIRST;
        
    }
    
    /**
     * Behaviour for when the defined allowed value of {@link At#by} is exceeded
     */
    enum ShiftByViolationBehaviour {
        
        /**
         * Shift-by violations are ignored
         */
        IGNORE,
        
        /**
         * Shift-by violations cause a warning message 
         */
        WARN,
        
        /**
         * Shift-by violations throw an exception
         */
        ERROR
        
    }
    
    /**
     * Initial limit on the value of {@link At#by} which triggers warning/error
     * (based on environment)
     */
    public static final int DEFAULT_ALLOWED_SHIFT_BY = 0;
    
    /**
     * Hard limit on the value of {@link At#by} which triggers error
     */
    public static final int MAX_ALLOWED_SHIFT_BY = 5;

    /**
     * Available injection point types
     */
    private static Map<String, Class<? extends InjectionPoint>> types = new HashMap<String, Class<? extends InjectionPoint>>();
    
    static {
        // Standard Injection Points
        InjectionPoint.register(BeforeFieldAccess.class);
        InjectionPoint.register(BeforeInvoke.class);
        InjectionPoint.register(BeforeNew.class);
        InjectionPoint.register(BeforeReturn.class);
        InjectionPoint.register(BeforeStringInvoke.class);
        InjectionPoint.register(JumpInsnPoint.class);
        InjectionPoint.register(MethodHead.class);
        InjectionPoint.register(AfterInvoke.class);
        InjectionPoint.register(BeforeLoadLocal.class);
        InjectionPoint.register(AfterStoreLocal.class);
        InjectionPoint.register(BeforeFinalReturn.class);
        InjectionPoint.register(BeforeConstant.class);
    }
    
    private final String slice;
    private final Selector selector;
    private final String id;
    
    protected InjectionPoint() {
        this("", Selector.DEFAULT, null);
    }
    
    protected InjectionPoint(InjectionPointData data) {
        this(data.getSlice(), data.getSelector(), data.getId());
    }
    
    public InjectionPoint(String slice, Selector selector, String id) {
        this.slice = slice;
        this.selector = selector;
        this.id = id;
    }

    public String getSlice() {
        return this.slice;
    }
    
    public Selector getSelector() {
        return this.selector;
    }
    
    public String getId() {
        return this.id;
    }
    
    /**
     * Runs a priority check in the context of this injection point. A priority
     * check should return <tt>true</tt> if the injection point is allowed to
     * inject given the relative priorities of the <em>target</em> (a method
     * merged by another mixin with <tt>targetPriority</tt>) and the incoming
     * mixin with priority <tt>mixinPriority</tt>.
     * 
     * @param targetPriority Priority of the mixin which originally merged the
     *      target method in question
     * @param mixinPriority Priority of the mixin which owns the owning injector
     * @return true if the priority check succeeds
     */
    public boolean checkPriority(int targetPriority, int mixinPriority) {
        return targetPriority < mixinPriority;
    }

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
        return String.format("@At(\"%s\")", this.getAtCode());
    }
    
    /**
     * Get the insn immediately following the specified insn, or return the same
     * insn if the insn is the last insn in the list
     * 
     * @param insns Insn list to fetch from
     * @param insn Insn node
     * @return Next insn or the same insn if last in the list
     */
    protected static AbstractInsnNode nextNode(InsnList insns, AbstractInsnNode insn) {
        int index = insns.indexOf(insn) + 1;
        if (index > 0 && index < insns.size()) {
            return insns.get(index);
        }
        return insn;
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

        @SuppressWarnings("unchecked")
        @Override
        public boolean find(String desc, InsnList insns, Collection<AbstractInsnNode> nodes) {
            boolean found = false;

            ArrayList<AbstractInsnNode>[] allNodes = (ArrayList<AbstractInsnNode>[]) Array.newInstance(ArrayList.class, this.components.length);

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
     * Parse a collection of InjectionPoints from the supplied {@link At}
     * annotations
     * 
     * @param owner Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param ats {@link At} annotations to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static List<InjectionPoint> parse(IInjectionPointContext owner, List<AnnotationNode> ats) {
        return InjectionPoint.parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), ats);
    }
    
    /**
     * Parse a collection of InjectionPoints from the supplied {@link At}
     * annotations
     * 
     * @param context Data for the mixin containing the annotation, used to
     *      obtain the refmap, amongst other things
     * @param method The annotated handler method
     * @param parent The parent annotation which owns this {@link At} annotation
     * @param ats {@link At} annotations to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static List<InjectionPoint> parse(IMixinContext context, MethodNode method, AnnotationNode parent, List<AnnotationNode> ats) {
        Builder<InjectionPoint> injectionPoints = ImmutableList.<InjectionPoint>builder();
        for (AnnotationNode at : ats) {
            InjectionPoint injectionPoint = InjectionPoint.parse(context, method, parent, at);
            if (injectionPoint != null) {
                injectionPoints.add(injectionPoint);
            }
        }
        return injectionPoints.build();
    }
    
    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation
     * 
     * @param owner Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param at {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(IInjectionPointContext owner, At at) {
        return InjectionPoint.parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), at.value(), at.shift(), at.by(),
                Arrays.asList(at.args()), at.target(), at.slice(), at.ordinal(), at.opcode(), at.id());
    }

    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation
     * 
     * @param context Data for the mixin containing the annotation, used to
     *      obtain the refmap, amongst other things
     * @param method The annotated handler method
     * @param parent The parent annotation which owns this {@link At} annotation
     * @param at {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, At at) {
        return InjectionPoint.parse(context, method, parent, at.value(), at.shift(), at.by(), Arrays.asList(at.args()), at.target(), at.slice(),
                at.ordinal(), at.opcode(), at.id());
    }
    
    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation supplied
     * as an AnnotationNode instance
     * 
     * @param owner Data for the mixin containing the annotation, used to obtain
     *      the refmap, amongst other things
     * @param node {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(IInjectionPointContext owner, AnnotationNode node) {
        return InjectionPoint.parse(owner.getContext(), owner.getMethod(), owner.getAnnotation(), node);
    }

    /**
     * Parse an InjectionPoint from the supplied {@link At} annotation supplied
     * as an AnnotationNode instance
     * 
     * @param context Data for the mixin containing the annotation, used to
     *      obtain the refmap, amongst other things
     * @param method The annotated handler method
     * @param parent The parent annotation which owns this {@link At} annotation
     * @param node {@link At} annotation to parse information from
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, AnnotationNode node) {
        String at = Annotations.<String>getValue(node, "value");
        List<String> args = Annotations.<List<String>>getValue(node, "args");
        String target = Annotations.<String>getValue(node, "target", "");
        String slice = Annotations.<String>getValue(node, "slice", "");
        At.Shift shift = Annotations.<At.Shift>getValue(node, "shift", At.Shift.class, At.Shift.NONE);
        int by = Annotations.<Integer>getValue(node, "by", Integer.valueOf(0));
        int ordinal = Annotations.<Integer>getValue(node, "ordinal", Integer.valueOf(-1));
        int opcode = Annotations.<Integer>getValue(node, "opcode", Integer.valueOf(0));
        String id = Annotations.<String>getValue(node, "id");

        if (args == null) {
            args = ImmutableList.<String>of();
        }

        return InjectionPoint.parse(context, method, parent, at, shift, by, args, target, slice, ordinal, opcode, id);
    }

    /**
     * Parse and instantiate an InjectionPoint from the supplied information.
     * Returns null if an InjectionPoint could not be created.
     * 
     * @param context Data for the mixin containing the annotation, used to
     *      obtain the refmap, amongst other things
     * @param method The annotated handler method
     * @param parent The parent annotation which owns this {@link At} annotation
     * @param at Injection point specifier
     * @param shift Shift type to apply
     * @param by Amount of shift to apply for the BY shift type 
     * @param args Named parameters
     * @param target Target for supported injection points
     * @param slice Slice id for injectors which support multiple slices
     * @param ordinal Ordinal offset for supported injection points
     * @param opcode Bytecode opcode for supported injection points
     * @param id Injection point id from annotation
     * @return InjectionPoint parsed from the supplied data or null if parsing
     *      failed
     */
    public static InjectionPoint parse(IMixinContext context, MethodNode method, AnnotationNode parent, String at, At.Shift shift, int by,
            List<String> args, String target, String slice, int ordinal, int opcode, String id) {
        InjectionPointData data = new InjectionPointData(context, method, parent, at, args, target, slice, ordinal, opcode, id);
        Class<? extends InjectionPoint> ipClass = findClass(context, data);
        InjectionPoint point = InjectionPoint.create(context, data, ipClass);
        return InjectionPoint.shift(context, method, parent, point, shift, by);
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends InjectionPoint> findClass(IMixinContext context, InjectionPointData data) {
        String type = data.getType();
        Class<? extends InjectionPoint> ipClass = InjectionPoint.types.get(type);
        if (ipClass == null) {
            if (type.matches("^([A-Za-z_][A-Za-z0-9_]*\\.)+[A-Za-z_][A-Za-z0-9_]*$")) {
                try {
                    ipClass = (Class<? extends InjectionPoint>)Class.forName(type);
                    InjectionPoint.types.put(type, ipClass);
                } catch (Exception ex) {
                    throw new InvalidInjectionException(context, data + " could not be loaded or is not a valid InjectionPoint", ex);
                }
            } else {
                throw new InvalidInjectionException(context, data + " is not a valid injection point specifier");
            }
        }
        return ipClass;
    }
    
    private static InjectionPoint create(IMixinContext context, InjectionPointData data, Class<? extends InjectionPoint> ipClass) {
        Constructor<? extends InjectionPoint> ipCtor = null;
        try {
            ipCtor = ipClass.getDeclaredConstructor(InjectionPointData.class);
            ipCtor.setAccessible(true);
        } catch (NoSuchMethodException ex) {
            throw new InvalidInjectionException(context, ipClass.getName() + " must contain a constructor which accepts an InjectionPointData", ex);
        }

        InjectionPoint point = null;
        try {
            point = ipCtor.newInstance(data);
        } catch (Exception ex) {
            throw new InvalidInjectionException(context, "Error whilst instancing injection point " + ipClass.getName() + " for " + data.getAt(), ex);
        }
        
        return point;
    }

    private static InjectionPoint shift(IMixinContext context, MethodNode method, AnnotationNode parent, InjectionPoint point,
            At.Shift shift, int by) {
        
        if (point != null) {
            if (shift == At.Shift.BEFORE) {
                return InjectionPoint.before(point);
            } else if (shift == At.Shift.AFTER) {
                return InjectionPoint.after(point);
            } else if (shift == At.Shift.BY) {
                InjectionPoint.validateByValue(context, method, parent, point, by);
                return InjectionPoint.shift(point, by);
            }
        }

        return point;
    }

    private static void validateByValue(IMixinContext context, MethodNode method, AnnotationNode parent, InjectionPoint point, int by) {
        MixinEnvironment env = context.getMixin().getConfig().getEnvironment();
        ShiftByViolationBehaviour err = env.<ShiftByViolationBehaviour>getOption(Option.SHIFT_BY_VIOLATION_BEHAVIOUR, ShiftByViolationBehaviour.WARN);
        if (err == ShiftByViolationBehaviour.IGNORE) {
            return;
        }
        
        String limitBreached = "the maximum allowed value: ";
        String advice = "Increase the value of maxShiftBy to suppress this warning.";
        int allowed = InjectionPoint.DEFAULT_ALLOWED_SHIFT_BY;
        if (context instanceof MixinTargetContext) {
            allowed = ((MixinTargetContext)context).getMaxShiftByValue();
        }
        
        if (by <= allowed) {
            return;
        }
        
        if (by > InjectionPoint.MAX_ALLOWED_SHIFT_BY) {
            limitBreached = "MAX_ALLOWED_SHIFT_BY=";
            advice = "You must use an alternate query or a custom injection point.";
            allowed = InjectionPoint.MAX_ALLOWED_SHIFT_BY; 
        }
        
        String message = String.format("@%s(%s) Shift.BY=%d on %s::%s exceeds %s%d. %s", Bytecode.getSimpleName(parent), point,
                by, context, method.name, limitBreached, allowed, advice);
        
        if (err == ShiftByViolationBehaviour.WARN && allowed < InjectionPoint.MAX_ALLOWED_SHIFT_BY) {
            LogManager.getLogger("mixin").warn(message);
            return;
        }

        throw new InvalidInjectionException(context, message);
    }
    
    protected String getAtCode() {
        AtCode code = this.getClass().<AtCode>getAnnotation(AtCode.class);
        return code == null ? this.getClass().getName() : code.value(); 
    }

    /**
     * Register an injection point class. The supplied class must be decorated
     * with an {@link AtCode} annotation for registration purposes.
     * 
     * @param type injection point type to register
     */
    public static void register(Class<? extends InjectionPoint> type) {
        AtCode code = type.<AtCode>getAnnotation(AtCode.class);
        if (code == null) {
            throw new IllegalArgumentException("Injection point class " + type + " is not annotated with @AtCode");
        }
        
        Class<? extends InjectionPoint> existing = InjectionPoint.types.get(code.value());
        if (existing != null && !existing.equals(type)) {
            LogManager.getLogger("mixin").debug("Overriding InjectionPoint {} with {} (previously {})", code.value(), type.getName(),
                    existing.getName());
        }
        
        InjectionPoint.types.put(code.value(), type);
    }
}
