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
package org.spongepowered.tools.obfuscation;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.tools.obfuscation.interfaces.IJavadocProvider;
import org.spongepowered.tools.obfuscation.interfaces.IMixinAnnotationProcessor;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator;
import org.spongepowered.tools.obfuscation.interfaces.IMixinValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.interfaces.IObfuscationManager;
import org.spongepowered.tools.obfuscation.interfaces.ITypeHandleProvider;
import org.spongepowered.tools.obfuscation.mirror.AnnotationHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandle;
import org.spongepowered.tools.obfuscation.mirror.TypeHandleSimulated;
import org.spongepowered.tools.obfuscation.mirror.TypeReference;
import org.spongepowered.tools.obfuscation.struct.InjectorRemap;
import org.spongepowered.tools.obfuscation.validation.ParentValidator;
import org.spongepowered.tools.obfuscation.validation.TargetValidator;

import com.google.common.collect.ImmutableList;

/**
 * Mixin info manager, stores all of the mixin info during processing and also
 * manages access to the mappings
 */
final class AnnotatedMixins implements IMixinAnnotationProcessor, ITokenProvider, ITypeHandleProvider, IJavadocProvider {

    private static final String MAPID_SYSTEM_PROPERTY = "mixin.target.mapid";

    /**
     * Singleton instances for each ProcessingEnvironment
     */
    private static Map<ProcessingEnvironment, AnnotatedMixins> instances = new HashMap<ProcessingEnvironment, AnnotatedMixins>();

    /**
     * Detected compiler environment
     */
    private final CompilerEnvironment env;

    /**
     * Local processing environment
     */
    private final ProcessingEnvironment processingEnv;

    /**
     * Mixins during processing phase
     */
    private final Map<String, AnnotatedMixin> mixins = new HashMap<String, AnnotatedMixin>();

    /**
     * Mixins created during this AP pass
     */
    private final List<AnnotatedMixin> mixinsForPass = new ArrayList<AnnotatedMixin>();

    /**
     * Obfuscation manager
     */
    private final IObfuscationManager obf;

    /**
     * Rule validators
     */
    private final List<IMixinValidator> validators;

    /**
     * Resolved tokens for constraint validation
     */
    private final Map<String, Integer> tokenCache = new HashMap<String, Integer>();

    /**
     * Serialisable mixin target map
     */
    private final TargetMap targets;

    /**
     * Properties file used to specify options when AP options cannot be
     * configured via the build script (eg. when using AP with MCP)
     */
    private Properties properties;

    /**
     * Private constructor, get instances using {@link #getMixinsForEnvironment}
     */
    private AnnotatedMixins(ProcessingEnvironment processingEnv) {
        this.env = this.detectEnvironment(processingEnv);
        this.processingEnv = processingEnv;

        this.printMessage(Kind.NOTE, "SpongePowered MIXIN Annotation Processor Version=" + MixinBootstrap.VERSION);

        this.targets = this.initTargetMap();
        this.obf = new ObfuscationManager(this);
        this.obf.init();

        this.validators = ImmutableList.<IMixinValidator>of(
            new ParentValidator(this),
            new TargetValidator(this)
        );

        this.initTokenCache(this.getOption(SupportedOptions.TOKENS));
    }

    protected TargetMap initTargetMap() {
        TargetMap targets = TargetMap.create(System.getProperty(AnnotatedMixins.MAPID_SYSTEM_PROPERTY));
        System.setProperty(AnnotatedMixins.MAPID_SYSTEM_PROPERTY, targets.getSessionId());
        String targetsFileName = this.getOption(SupportedOptions.DEPENDENCY_TARGETS_FILE);
        if (targetsFileName != null) {
            try {
                targets.readImports(new File(targetsFileName));
            } catch (IOException ex) {
                this.printMessage(Kind.WARNING, "Could not read from specified imports file: " + targetsFileName);
            }
        }
        return targets;
    }

    private void initTokenCache(String tokens) {
        if (tokens != null) {
            Pattern tokenPattern = Pattern.compile("^([A-Z0-9\\-_\\.]+)=([0-9]+)$");

            String[] tokenValues = tokens.replaceAll("\\s", "").toUpperCase().split("[;,]");
            for (String tokenValue : tokenValues) {
                Matcher tokenMatcher = tokenPattern.matcher(tokenValue);
                if (tokenMatcher.matches()) {
                    this.tokenCache.put(tokenMatcher.group(1), Integer.parseInt(tokenMatcher.group(2)));
                }
            }
        }
    }

    @Override
    public ITypeHandleProvider getTypeProvider() {
        return this;
    }

    @Override
    public ITokenProvider getTokenProvider() {
        return this;
    }

    @Override
    public IObfuscationManager getObfuscationManager() {
        return this.obf;
    }

    @Override
    public IJavadocProvider getJavadocProvider() {
        return this;
    }

    @Override
    public ProcessingEnvironment getProcessingEnvironment() {
        return this.processingEnv;
    }

    @Override
    public CompilerEnvironment getCompilerEnvironment() {
        return this.env;
    }

    @Override
    public Integer getToken(String token) {
        if (this.tokenCache.containsKey(token)) {
            return this.tokenCache.get(token);
        }

        String option = this.getOption(token);
        Integer value = null;
        try {
            value = Integer.valueOf(Integer.parseInt(option));
        } catch (Exception ex) {
            // npe or number format exception
        }

        this.tokenCache.put(token, value);
        return value;
    }

    @Override
    public String getOption(String option) {
        if (option == null) {
            return null;
        }

        String value = this.processingEnv.getOptions().get(option);
        if (value != null) {
            return value;
        }

        return this.getProperties().getProperty(option);
    }

    public Properties getProperties() {
        if (this.properties == null) {
            this.properties = new Properties();

            try {
                Filer filer = this.processingEnv.getFiler();
                FileObject propertyFile = filer.getResource(StandardLocation.SOURCE_PATH, "", "mixin.properties");
                if (propertyFile != null) {
                    InputStream inputStream = propertyFile.openInputStream();
                    this.properties.load(inputStream);
                    inputStream.close();
                }
            } catch (Exception ex) {
                // ignore
            }
        }

        return this.properties;
    }

    private CompilerEnvironment detectEnvironment(ProcessingEnvironment processingEnv) {
        if (processingEnv.getClass().getName().contains("jdt")) {
            return CompilerEnvironment.JDT;
        }

        return CompilerEnvironment.JAVAC;
    }

    /**
     * Write out generated mappings
     */
    public void writeMappings() {
        this.obf.writeMappings();
    }

    /**
     * Write out stored references
     */
    public void writeReferences() {
        this.obf.writeReferences();
    }

    /**
     * Clear all registered mixins
     */
    public void clear() {
        this.mixins.clear();
    }

    /**
     * Register a new mixin class
     */
    public void registerMixin(TypeElement mixinType) {
        String name = mixinType.getQualifiedName().toString();

        if (!this.mixins.containsKey(name)) {
            AnnotatedMixin mixin = new AnnotatedMixin(this, mixinType);
            this.targets.registerTargets(mixin);
            mixin.runValidators(ValidationPass.EARLY, this.validators);
            this.mixins.put(name, mixin);
            this.mixinsForPass.add(mixin);
        }
    }

    /**
     * Get a registered mixin
     */
    public AnnotatedMixin getMixin(TypeElement mixinType) {
        return this.getMixin(mixinType.getQualifiedName().toString());
    }

    /**
     * Get a registered mixin
     */
    public AnnotatedMixin getMixin(String mixinType) {
        return this.mixins.get(mixinType);
    }

    public Collection<TypeMirror> getMixinsTargeting(TypeMirror targetType) {
        return this.getMixinsTargeting((TypeElement)((DeclaredType)targetType).asElement());
    }

    public Collection<TypeMirror> getMixinsTargeting(TypeElement targetType) {
        List<TypeMirror> minions = new ArrayList<TypeMirror>();

        for (TypeReference mixin : this.targets.getMixinsTargeting(targetType)) {
            TypeHandle handle = mixin.getHandle(this.processingEnv);
            if (handle != null) {
                minions.add(handle.getType());
            }
        }

        return minions;
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.gen.Accessor} method
     *
     * @param mixinType Mixin class
     * @param method Accessor method
     */
    public void registerAccessor(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Accessor annotation on a non-mixin method", method);
            return;
        }

        AnnotationHandle accessor = AnnotationHandle.of(method, Accessor.class);
        mixinClass.registerAccessor(method, accessor, this.shouldRemap(mixinClass, accessor));
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.gen.Accessor} method
     *
     * @param mixinType Mixin class
     * @param method Accessor method
     */
    public void registerInvoker(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Accessor annotation on a non-mixin method", method);
            return;
        }

        AnnotationHandle invoker = AnnotationHandle.of(method, Invoker.class);
        mixinClass.registerInvoker(method, invoker, this.shouldRemap(mixinClass, invoker));
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.Overwrite} method
     *
     * @param mixinType Mixin class
     * @param method Overwrite method
     */
    public void registerOverwrite(TypeElement mixinType, ExecutableElement method) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Overwrite annotation on a non-mixin method", method);
            return;
        }

        AnnotationHandle overwrite = AnnotationHandle.of(method, Overwrite.class);
        mixinClass.registerOverwrite(method, overwrite, this.shouldRemap(mixinClass, overwrite));
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} field
     *
     * @param mixinType Mixin class
     * @param field Shadow field
     * @param shadow {@link org.spongepowered.asm.mixin.Shadow} annotation
     */
    public void registerShadow(TypeElement mixinType, VariableElement field, AnnotationHandle shadow) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Shadow annotation on a non-mixin field", field);
            return;
        }

        mixinClass.registerShadow(field, shadow, this.shouldRemap(mixinClass, shadow));
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} method
     *
     * @param mixinType Mixin class
     * @param method Shadow method
     * @param shadow {@link org.spongepowered.asm.mixin.Shadow} annotation
     */
    public void registerShadow(TypeElement mixinType, ExecutableElement method, AnnotationHandle shadow) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Shadow annotation on a non-mixin method", method);
            return;
        }

        mixinClass.registerShadow(method, shadow, this.shouldRemap(mixinClass, shadow));
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.injection.Inject} method
     *
     * @param mixinType Mixin class
     * @param method Injector method
     * @param inject {@link org.spongepowered.asm.mixin.injection.Inject}
     *      annotation
     */
    public void registerInjector(TypeElement mixinType, ExecutableElement method, AnnotationHandle inject) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found " + inject + " annotation on a non-mixin method", method);
            return;
        }

        InjectorRemap remap = new InjectorRemap(this.shouldRemap(mixinClass, inject));
        mixinClass.registerInjector(method, inject, remap);
        remap.dispatchPendingMessages(this);
    }

    /**
     * Register an {@link org.spongepowered.asm.mixin.Implements} declaration on
     * a mixin class
     *
     * @param mixin Annotated mixin
     * @param implementsAnnotation
     *      {@link org.spongepowered.asm.mixin.Implements} annotation
     */
    public void registerSoftImplements(TypeElement mixin, AnnotationHandle implementsAnnotation) {
        AnnotatedMixin mixinClass = this.getMixin(mixin);
        if (mixinClass == null) {
            this.printMessage(Kind.ERROR, "Found @Implements annotation on a non-mixin class");
            return;
        }

        mixinClass.registerSoftImplements(implementsAnnotation);
    }

    /**
     * Called from each AP class to notify the environment that a new pass is
     * starting
     */
    public void onPassStarted() {
        this.mixinsForPass.clear();
    }

    /**
     * Called from each AP when a pass is completed
     */
    public void onPassCompleted(RoundEnvironment roundEnv) {
        if (!"true".equalsIgnoreCase(this.getOption(SupportedOptions.DISABLE_TARGET_EXPORT))) {
            this.targets.write(true);
        }
        
        for (AnnotatedMixin mixin : roundEnv.processingOver() ? this.mixins.values() : this.mixinsForPass) {
            mixin.runValidators(roundEnv.processingOver() ? ValidationPass.FINAL : ValidationPass.LATE, this.validators);
        }
    }

    private boolean shouldRemap(AnnotatedMixin mixinClass, AnnotationHandle annotation) {
        return annotation.getBoolean("remap", mixinClass.remap());
    }

    /**
     * Print a message to the AP messager
     */
    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg) {
        if (this.env == CompilerEnvironment.JAVAC || kind != Kind.NOTE) {
            this.processingEnv.getMessager().printMessage(kind, msg);
        }
    }

    /**
     * Print a message to the AP messager
     */
    @Override
    public void printMessage(Diagnostic.Kind kind, CharSequence msg, Element element) {
        this.processingEnv.getMessager().printMessage(kind, msg, element);
    }

    /**
     * Print a message to the AP messager
     */
    @Override
    public void printMessage(Kind kind, CharSequence msg, Element element, AnnotationMirror annotation) {
        this.processingEnv.getMessager().printMessage(kind, msg, element, annotation);
    }

    /**
     * Print a message to the AP messager
     */
    @Override
    public void printMessage(Kind kind, CharSequence msg, Element element, AnnotationMirror annotation, AnnotationValue value) {
        this.processingEnv.getMessager().printMessage(kind, msg, element, annotation, value);
    }

    /**
     * Get a TypeHandle representing another type in the current processing
     * environment
     */
    @Override
    public TypeHandle getTypeHandle(String name) {
        name = name.replace('/', '.');

        Elements elements = this.processingEnv.getElementUtils();
        TypeElement element = elements.getTypeElement(name);
        if (element != null) {
            try {
                return new TypeHandle(element);
            } catch (NullPointerException ex) {
                // probably bad package
            }
        }

        int lastDotPos = name.lastIndexOf('.');
        if (lastDotPos > -1) {
            String pkg = name.substring(0, lastDotPos);
            PackageElement packageElement = elements.getPackageElement(pkg);
            if (packageElement != null) {
                return new TypeHandle(packageElement, name);
            }
        }

        return null;
    }

    /**
     * Returns a simulated TypeHandle for a non-existing type
     */
    @Override
    public TypeHandle getSimulatedHandle(String name, TypeMirror simulatedTarget) {
        name = name.replace('/', '.');
        int lastDotPos = name.lastIndexOf('.');
        if (lastDotPos > -1) {
            String pkg = name.substring(0, lastDotPos);
            PackageElement packageElement = this.processingEnv.getElementUtils().getPackageElement(pkg);
            if (packageElement != null) {
                return new TypeHandleSimulated(packageElement, name, simulatedTarget);
            }
        }

        return new TypeHandleSimulated(name, simulatedTarget);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.tools.obfuscation.IJavadocProvider
     *      #getJavadoc(javax.lang.model.element.Element)
     */
    @Override
    public String getJavadoc(Element element) {
        Elements elements = this.processingEnv.getElementUtils();
        return elements.getDocComment(element);
    }

    /**
     * Get the mixin manager instance for this environment
     */
    public static AnnotatedMixins getMixinsForEnvironment(ProcessingEnvironment processingEnv) {
        AnnotatedMixins mixins = AnnotatedMixins.instances.get(processingEnv);
        if (mixins == null) {
            mixins = new AnnotatedMixins(processingEnv);
            AnnotatedMixins.instances.put(processingEnv, mixins);
        }
        return mixins;
    }
}
