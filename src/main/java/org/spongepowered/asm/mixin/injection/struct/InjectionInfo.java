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

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.tools.Diagnostic.Kind;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.extensibility.IActivityContext.IActivity;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.InjectionPoint;
import org.spongepowered.asm.mixin.injection.code.ISliceContext;
import org.spongepowered.asm.mixin.injection.code.Injector;
import org.spongepowered.asm.mixin.injection.code.InjectorTarget;
import org.spongepowered.asm.mixin.injection.code.MethodSlice;
import org.spongepowered.asm.mixin.injection.code.MethodSlices;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelector;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelectors;
import org.spongepowered.asm.mixin.injection.selectors.TargetSelectors.SelectedMethod;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorException;
import org.spongepowered.asm.mixin.injection.struct.InjectionNodes.InjectionNode;
import org.spongepowered.asm.mixin.injection.throwables.InjectionError;
import org.spongepowered.asm.mixin.injection.throwables.InvalidInjectionException;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.struct.AnnotatedMethodInfo;
import org.spongepowered.asm.mixin.struct.SpecialMethodInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.mixin.transformer.ActivityStack;
import org.spongepowered.asm.mixin.transformer.MixinTargetContext;
import org.spongepowered.asm.mixin.transformer.throwables.InvalidMixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.Bytecode;
import org.spongepowered.asm.util.asm.ASM;
import org.spongepowered.asm.util.asm.MethodNodeEx;
import org.spongepowered.asm.util.logging.MessageRouter;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;

/**
 * Contructs information about an injection from an {@link Inject} annotation
 * and allows the injection to be processed.
 */
public abstract class InjectionInfo extends SpecialMethodInfo implements ISliceContext {
    
    /**
     * Decoration for subclasses which indicates the injector annotation that
     * the subclass handles
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.TYPE)
    public @interface AnnotationType {
        
        /**
         * The injector annotation this InjectionInfo can handle
         */
        public Class<? extends Annotation> value();
        
    }
    
    /**
     * Decoration for subclasses which specifies the prefix to use when
     * conforming annotated handler methods
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.TYPE)
    public @interface HandlerPrefix {
        
        /**
         * Default conform prefix for handler methods 
         */
        public static final String DEFAULT = "handler";

        /**
         * String prefix for conforming handler methods
         */
        public String value();
        
    }
    
    /**
     * Decoration for subclasses which specifies the order (phase) in which the
     * injector should be applied relative to other injectors. Built-in
     * injectors except for redirectors all run at DEFAULT unless specified in
     * the injector annotation.
     * 
     * <p>Injectors in the same order are sorted by mixin priority and
     * declaration order within the mixin as always.</p>
     */
    @Retention(RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target(ElementType.TYPE)
    public @interface InjectorOrder {

        /**
         * An early injector, run before most injectors
         */
        public static final int EARLY = 0;
        
        /**
         * Default order, all injectors except redirect run here unless manually
         * adjusted
         */
        public static final int DEFAULT = 1000;

        /**
         * Late injector, runs after most injectors but before redirects 
         */
        public static final int LATE = 2000;
        
        /**
         * Built-in order for Redirect injectors
         */
        public static final int REDIRECT = 10000;
        
        /**
         * Injector which should run after redirect injector 
         */
        public static final int AFTER_REDIRECT = 20000;
        
        /**
         * String prefix for conforming handler methods
         */
        public int value() default InjectorOrder.DEFAULT;
        
    }

    /**
     * An injector registration entry
     */
    static class InjectorEntry {
        
        final Class<? extends Annotation> annotationType;
        
        final Class<? extends InjectionInfo> injectorType;
        
        final Constructor<? extends InjectionInfo> ctor;
        
        final String annotationDesc;
        
        final String prefix;

        InjectorEntry(Class<? extends Annotation> annotationType, Class<? extends InjectionInfo> type) throws NoSuchMethodException {
            this.annotationType = annotationType;
            this.injectorType = type;
            this.ctor = type.getDeclaredConstructor(MixinTargetContext.class, MethodNode.class, AnnotationNode.class);
            this.annotationDesc = Type.getDescriptor(annotationType);
            
            HandlerPrefix handlerPrefix = type.<HandlerPrefix>getAnnotation(HandlerPrefix.class);
            this.prefix = handlerPrefix != null ? handlerPrefix.value() : HandlerPrefix.DEFAULT;
        }
        
        InjectionInfo create(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
            try {
                return this.ctor.newInstance(mixin, method, annotation);
            } catch (InvocationTargetException itex) {
                Throwable cause = itex.getCause();
                if (cause instanceof MixinException) {
                    throw (MixinException)cause;
                }
                Throwable ex = cause != null ? cause : itex;
                throw new MixinError("Error initialising injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
            } catch (ReflectiveOperationException ex) {
                throw new MixinError("Failed to instantiate injector metaclass [" + this.injectorType + "] for annotation " + annotation.desc, ex);
            }
        }
    }
    
    /**
     * Registry of subclasses
     */
    private static Map<String, InjectorEntry> registry = new LinkedHashMap<String, InjectorEntry>();
    
    /**
     * Registered annotations, baked and used to call
     * Annotations::getSingleVisible efficiently 
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static Class<? extends Annotation>[] registeredAnnotations = new Class[0];
    
    static {
        // Standard injectors
        InjectionInfo.register(CallbackInjectionInfo.class);        // @Inject
        InjectionInfo.register(ModifyArgInjectionInfo.class);       // @ModifyArg
        InjectionInfo.register(ModifyArgsInjectionInfo.class);      // @ModifyArgs
        InjectionInfo.register(RedirectInjectionInfo.class);        // @Redirect
        InjectionInfo.register(ModifyVariableInjectionInfo.class);  // @ModifyVariable
        InjectionInfo.register(ModifyConstantInjectionInfo.class);  // @ModifyConstant
    }
    
    /**
     * Activity tracker
     */
    protected final ActivityStack activities = new ActivityStack(null);

    /**
     * Annotated method is static 
     */
    protected final boolean isStatic;
    
    /**
     * Targets
     */
    protected final TargetSelectors targets;
    
    /**
     * Method slice descriptors parsed from the annotation
     */
    protected final MethodSlices slices;
    
    /**
     * The key into the annotation which contains the injection points
     */
    protected final String atKey;
    
    protected final List<AnnotationNode> injectionPointAnnotations = new ArrayList<AnnotationNode>();

    /**
     * Injection points parsed from
     * {@link org.spongepowered.asm.mixin.injection.At} annotations
     */
    protected final List<InjectionPoint> injectionPoints = new ArrayList<InjectionPoint>();
    
    /**
     * Map of lists of nodes enumerated by calling {@link #prepare}
     */
    protected final Map<Target, List<InjectionNode>> targetNodes = new LinkedHashMap<Target, List<InjectionNode>>();
    
    /**
     * Number of target methods identified by the injection points 
     */
    protected int targetCount = 0;

    /**
     * Bytecode injector
     */
    protected Injector injector;
    
    /**
     * Injection group
     */
    protected InjectorGroupInfo group;
    
    /**
     * Methods injected by injectors 
     */
    private final List<MethodNode> injectedMethods = new ArrayList<MethodNode>(0);
    
    /**
     * Number of callbacks we expect to inject into targets 
     */
    private int expectedCallbackCount = 1;
    
    /**
     * Number of callbacks we require injected 
     */
    private int requiredCallbackCount = 0;
    
    /**
     * Maximum number of callbacks allowed to be injected 
     */
    private int maxCallbackCount = Integer.MAX_VALUE;

    /**
     * Actual number of injected callbacks
     */
    private int injectedCallbackCount = 0;
    
    /**
     * Injection messages which are not severe enough to log when they occur,
     * but may be of interest if a <tt>require</tt> clause fails in order to
     * diagnose why no injections were performed.
     */
    private List<String> messages;
    
    /**
     * Injector order, parsed from either the injector annotation or uses the
     * default for this injection type 
     */
    private int order = InjectorOrder.DEFAULT;
    
    /**
     * ctor
     * 
     * @param mixin Mixin data
     * @param method Injector method
     * @param annotation Annotation to parse
     */
    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation) {
        this(mixin, method, annotation, "at");
    }
    
    protected InjectionInfo(MixinTargetContext mixin, MethodNode method, AnnotationNode annotation, String atKey) {
        super(mixin, method, annotation);
        this.isStatic = Bytecode.isStatic(method);
        this.targets = new TargetSelectors(this, mixin.getTargetClassNode());
        this.slices = MethodSlices.parse(this);
        this.atKey = atKey;
        this.readAnnotation();
    }

    /**
     * Parse the info from the supplied annotation
     */
    protected void readAnnotation() {
        if (this.annotation == null) {
            return;
        }

        this.activities.clear();
        try {
            // When remapping refmap is enabled this implies we are in a development environment. In
            // certain circumstances including the descriptor for the method may actually fail, so we
            // will do a second "permissive" pass without the descriptor if this happens.
            this.targets.setPermissivePass(this.mixin.getOption(Option.REFMAP_REMAP));
            
            IActivity activity = this.activities.begin("Read Injection Points");
            this.readInjectionPoints();
            activity.next("Parse Requirements");
            this.parseRequirements();
            activity.next("Parse Order");
            this.parseOrder();
            activity.next("Parse Selectors");
            this.parseSelectors();
            activity.next("Find Targets");
            this.targets.find();
            activity.next("Validate Targets");
            this.targets.validate(this.expectedCallbackCount, this.requiredCallbackCount);
            activity.next("Parse Injection Points");
            this.parseInjectionPoints(this.injectionPointAnnotations);
            activity.next("Parse Injector");
            this.injector = this.parseInjector(this.annotation);
            activity.end();
        } catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this.mixin, "Unexpected " + ex.getClass().getSimpleName() + " parsing "
                    + this.getElementDescription(), ex, this.activities);
        }
    }

    protected void readInjectionPoints() {
        List<AnnotationNode> ats = Annotations.<AnnotationNode>getValue(this.annotation, this.atKey, false);
        if (ats == null) {
            throw new InvalidInjectionException(this, String.format("%s is missing '%s' value(s)", this.getElementDescription(), this.atKey));
        }
        this.injectionPointAnnotations.addAll(ats);
    }

    protected void parseRequirements() {
        this.group = this.mixin.getInjectorGroups().parseGroup(this.method, this.mixin.getDefaultInjectorGroup()).add(this);
        
        Integer expect = Annotations.<Integer>getValue(this.annotation, "expect");
        if (expect != null) {
            this.expectedCallbackCount = expect.intValue();
        }

        Integer require = Annotations.<Integer>getValue(this.annotation, "require");
        if (require != null && require.intValue() > -1) {
            this.requiredCallbackCount = require.intValue();
        } else if (this.group.isDefault()) {
            this.requiredCallbackCount = this.mixin.getDefaultRequiredInjections();
        }
        
        Integer allow = Annotations.<Integer>getValue(this.annotation, "allow");
        if (allow != null) {
            this.maxCallbackCount = Math.max(Math.max(this.requiredCallbackCount, 1), allow);
        }
    }
    
    protected void parseOrder() {
        Integer userOrder = Annotations.<Integer>getValue(this.annotation, "order");
        if (userOrder != null) {
            this.order = userOrder.intValue();
            return;
        }

        InjectorOrder injectorDefault = this.getClass().<InjectorOrder>getAnnotation(InjectorOrder.class);
        this.order = injectorDefault != null ? injectorDefault.value() : InjectorOrder.DEFAULT;
    }

    protected void parseSelectors() {
        Set<ITargetSelector> selectors = new LinkedHashSet<ITargetSelector>();
        TargetSelector.parse(Annotations.<String>getValue(this.annotation, "method", false), this, selectors);
        TargetSelector.parse(Annotations.<AnnotationNode>getValue(this.annotation, "target", false), this, selectors);
        
        // Raise an error if we have no selectors
        if (selectors.size() == 0) {
            throw new InvalidInjectionException(this, String.format("%s is missing 'method' or 'target' to specify targets",
                    this.getElementDescription()));
        }
        
        this.targets.parse(selectors);
    }

    protected void parseInjectionPoints(List<AnnotationNode> ats) {
        this.injectionPoints.addAll(InjectionPoint.parse(this, ats));
    }

    // stub
    protected abstract Injector parseInjector(AnnotationNode injectAnnotation);
    
    /**
     * Get whether there is enough valid information in this info to actually
     * perform an injection.
     * 
     * @return true if this InjectionInfo was successfully parsed
     */
    public boolean isValid() {
        return this.targets.size() > 0 && this.injectionPoints.size() > 0;
    }
    
    /**
     * Get the application order for this injector type
     */
    public int getOrder() {
        return this.order;
    }
    
    /**
     * Discover injection points
     */
    public void prepare() {
        this.activities.clear();
        try {
            this.targetNodes.clear();
            IActivity activity = this.activities.begin("?");
            for (SelectedMethod targetMethod : this.targets) {
                activity.next("{ target: %s }", targetMethod);
                Target target = this.mixin.getTargetMethod(targetMethod.getMethod());
                InjectorTarget injectorTarget = new InjectorTarget(this, target, targetMethod);
                try {
                    this.targetNodes.put(target, this.injector.find(injectorTarget, this.injectionPoints));
                } catch (SelectorException ex) {
                    throw new InvalidInjectionException(this, String.format("Injection validation failed: %s: %s. %s%s",
                            this.getElementDescription(), ex.getMessage(), this.mixin.getReferenceMapper().getStatus(),
                            AnnotatedMethodInfo.getDynamicInfo(this.method)));
                } finally {
                    injectorTarget.dispose();
                }
            }
            activity.end();
        } catch (InvalidMixinException ex) {
            ex.prepend(this.activities);
            throw ex;
        } catch (Exception ex) {
            throw new InvalidMixinException(this.mixin, "Unexpecteded " + ex.getClass().getSimpleName() + " preparing "
                    + this.getElementDescription(), ex, this.activities);
        }
    }
    
    /**
     * Perform pre-injection checks and tasks
     */
    public void preInject() {
        for (Entry<Target, List<InjectionNode>> entry : this.targetNodes.entrySet()) {
            this.injector.preInject(entry.getKey(), entry.getValue());
        }
    }
    
    /**
     * Perform injections
     */
    public void inject() {
        for (Entry<Target, List<InjectionNode>> entry : this.targetNodes.entrySet()) {
            this.injector.inject(entry.getKey(), entry.getValue());
        }
        this.targets.clear();
    }
    
    /**
     * Perform cleanup and post-injection tasks 
     */
    public void postInject() {
        for (MethodNode method : this.injectedMethods) {
            this.classNode.methods.add(method);
        }
        
        String description = this.getDescription();
        String refMapStatus = this.mixin.getReferenceMapper().getStatus();
        String extraInfo = AnnotatedMethodInfo.getDynamicInfo(this.method) + this.getMessages();
        if ((this.mixin.getOption(Option.DEBUG_INJECTORS) && this.injectedCallbackCount < this.expectedCallbackCount)) {
            throw new InvalidInjectionException(this,
                    String.format("Injection validation failed: %s %s%s in %s expected %d invocation(s) but %d succeeded. Scanned %d target(s). %s%s",
                            description, this.methodName, this.method.desc, this.mixin, this.expectedCallbackCount, this.injectedCallbackCount,
                            this.targetCount, refMapStatus, extraInfo));
        } else if (this.injectedCallbackCount < this.requiredCallbackCount) {
            throw new InjectionError(
                    String.format("Critical injection failure: %s %s%s in %s failed injection check, (%d/%d) succeeded. Scanned %d target(s). %s%s",
                            description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.requiredCallbackCount,
                            this.targetCount, refMapStatus, extraInfo));
        } else if (this.injectedCallbackCount > this.maxCallbackCount) {
            throw new InjectionError(
                    String.format("Critical injection failure: %s %s%s in %s failed injection check, %d succeeded of %d allowed.%s",
                    description, this.methodName, this.method.desc, this.mixin, this.injectedCallbackCount, this.maxCallbackCount, extraInfo));
        }
        
        this.slices.postInject();
    }
    
    /**
     * Callback from injector which notifies us that a callback was injected. No
     * longer used.
     * 
     * @param target target into which the injector injected
     */
    public void notifyInjected(Target target) {
//        this.targets.remove(target.method);
    }

    protected String getDescription() {
        return "Callback method";
    }

    @Override
    public String toString() {
        return InjectionInfo.describeInjector(this.mixin, this.annotation, this.method);
    }
    
    /**
     * Get number of methods being injected into
     * 
     * @return count of methods being injected into
     */
    public int getTargetCount() {
        return this.targets.size();
    }
    
    /**
     * Get the slice descriptors
     */
    @Override
    public MethodSlice getSlice(String id) {
        return this.slices.get(this.getSliceId(id));
    }
    
    /**
     * Return the mapped slice id for the specified ID. Injectors which only
     * support use of a single slice will always return the default id (an empty
     * string)
     * 
     * @param id slice id
     * @return mapped id
     */
    public String getSliceId(String id) {
        return "";
    }

    /**
     * Get the injected callback count
     * 
     * @return the injected callback count
     */
    public int getInjectedCallbackCount() {
        return this.injectedCallbackCount;
    }

    /**
     * Inject a method into the target class
     * 
     * @param access Method access flags, synthetic will be automatically added
     * @param name Method name
     * @param desc Method descriptor
     * 
     * @return new method
     */
    public MethodNode addMethod(int access, String name, String desc) {
        MethodNode method = new MethodNode(ASM.API_VERSION, access | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
        this.injectedMethods.add(method);
        return method;
    }
    
    /**
     * Notify method, called by injector when adding a callback into a target
     * 
     * @param handler callback handler being invoked
     */
    public void addCallbackInvocation(MethodNode handler) {
        this.injectedCallbackCount++;
    }
    
    /**
     * Notify method, called by injector or injection point when a notable but
     * non-fatal failures occur, for example allows injection points to add
     * notes when they return no results.
     * 
     * @param format Message format
     * @param args Format args
     */
    @Override
    public void addMessage(String format, Object... args) {
        super.addMessage(format, args);
        if (this.messages == null) {
            this.messages = new ArrayList<String>();
        }
        String message = String.format(format, args);
        this.messages.add(message);
    }
    
    protected String getMessages() {
        return this.messages != null ? " Messages: { " + Joiner.on(" ").join(this.messages) + "}" : "";
    }

    /**
     * Parse an injector from the specified method (if an injector annotation is
     * present). If no injector annotation is present then <tt>null</tt> is
     * returned.
     * 
     * @param mixin context
     * @param method mixin method
     * @return parsed InjectionInfo or null
     */
    public static InjectionInfo parse(MixinTargetContext mixin, MethodNode method) {
        AnnotationNode annotation = InjectionInfo.getInjectorAnnotation(mixin.getMixin(), method);
        
        if (annotation == null) {
            return null;
        }
        
        for (InjectorEntry injector : InjectionInfo.registry.values()) {
            if (annotation.desc.equals(injector.annotationDesc)) {
                return injector.create(mixin, method, annotation);
            }
        }
        
        return null;
    }

    /**
     * Returns any injector annotation found on the specified method. If
     * multiple matching annotations are found then an exception is thrown. If
     * no annotations are present then <tt>null</tt> is returned.
     * 
     * @param mixin context
     * @param method mixin method
     * @return annotation or null
     */
    public static AnnotationNode getInjectorAnnotation(IMixinInfo mixin, MethodNode method) {
        AnnotationNode annotation = null;
        try {
            annotation = Annotations.getSingleVisible(method, InjectionInfo.registeredAnnotations);
        } catch (IllegalArgumentException ex) {
            throw new InvalidMixinException(mixin, String.format("Error parsing annotations on %s in %s: %s", method.name, mixin.getClassName(),
                    ex.getMessage()));
        }
        return annotation;
    }

    /**
     * Get the conform prefix for an injector handler by type
     * 
     * @param annotation Annotation to inspect
     * @return conform prefix
     */
    public static String getInjectorPrefix(AnnotationNode annotation) {
        if (annotation == null) {
            return HandlerPrefix.DEFAULT;
        }
        
        for (InjectorEntry injector : InjectionInfo.registry.values()) {
            if (annotation.desc.endsWith(injector.annotationDesc)) {
                return injector.prefix;
            }
        }
        
        return HandlerPrefix.DEFAULT;
    }

    static String describeInjector(IMixinContext mixin, AnnotationNode annotation, MethodNode method) {
        return String.format("%s->@%s::%s%s", mixin.toString(), Annotations.getSimpleName(annotation), MethodNodeEx.getName(method), method.desc);
    }

    /**
     * Register an injector info class. The supplied class must be decorated
     * with an {@link AnnotationType} annotation for registration purposes.
     * 
     * @param type injection info subclass to register
     */
    public static void register(Class<? extends InjectionInfo> type) {
        AnnotationType annotationType = type.<AnnotationType>getAnnotation(AnnotationType.class);
        if (annotationType == null) {
            throw new IllegalArgumentException("Injection info class " + type + " is not annotated with @AnnotationType");
        }
        
        InjectorEntry entry;
        try {
            entry = new InjectorEntry(annotationType.value(), type);
        } catch (NoSuchMethodException ex) {
            throw new MixinError("InjectionInfo class " + type.getName() + " is missing a valid constructor");
        }
        InjectorEntry existing = InjectionInfo.registry.get(entry.annotationDesc);
        if (existing != null) { // && !existing.type.equals(type)) {
            MessageRouter.getMessager().printMessage(Kind.WARNING, String.format("Overriding InjectionInfo for @%s with %s (previously %s)",
                    annotationType.value().getSimpleName(), type.getName(), existing.injectorType.getName()));
        } else {
            MessageRouter.getMessager().printMessage(Kind.OTHER, String.format("Registering new injector for @%s with %s",
                    annotationType.value().getSimpleName(), type.getName()));
        }
        
        InjectionInfo.registry.put(entry.annotationDesc, entry);
        
        ArrayList<Class<? extends Annotation>> annotations = new ArrayList<Class<? extends Annotation>>();
        for (InjectorEntry injector : InjectionInfo.registry.values()) {
            annotations.add(injector.annotationType);
        }
        InjectionInfo.registeredAnnotations = annotations.toArray(InjectionInfo.registeredAnnotations);
    }
    
    public static Set<Class<? extends Annotation>> getRegisteredAnnotations() {
        return ImmutableSet.<Class<? extends Annotation>>copyOf(InjectionInfo.registeredAnnotations);
    }

}
