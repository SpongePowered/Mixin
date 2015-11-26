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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.ReferenceMapper;
import org.spongepowered.asm.util.ITokenProvider;
import org.spongepowered.tools.MirrorUtils;
import org.spongepowered.tools.obfuscation.IMixinValidator.ValidationPass;
import org.spongepowered.tools.obfuscation.struct.Message;
import org.spongepowered.tools.obfuscation.validation.ParentValidator;
import org.spongepowered.tools.obfuscation.validation.TargetValidator;

import com.google.common.collect.ImmutableList;

import net.minecraftforge.srg2source.rangeapplier.MethodData;
import net.minecraftforge.srg2source.rangeapplier.SrgContainer;


/**
 * Mixin info manager, stores all of the mixin info during processing and also
 * manages access to the srgs
 */
class AnnotatedMixins implements Messager, ITokenProvider, IOptionProvider {
    
    private static final String MAPID_SYSTEM_PROPERTY = "mixin.target.mapid";

    static enum CompilerEnvironment {
        /**
         * Default environment
         */
        JAVAC,
        
        /**
         * Eclipse 
         */
        JDT
    }
    
    /**
     * Singleton instances for each ProcessingEnvironment
     */
    private static Map<ProcessingEnvironment, AnnotatedMixins> instances = new HashMap<ProcessingEnvironment, AnnotatedMixins>();
    
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
     * Name of the resource to write generated srgs to
     */
    private final String outSrgFileName;
    
    /**
     * Name of the resource to write remapped refs to
     */
    private final String outRefMapFileName;
    
    /**
     * File containing the reobfd srgs
     */
    private final String reobfSrgFileName;
    
    /**
     * Reference mapper for reference mapping 
     */
    private final ReferenceMapper refMapper = new ReferenceMapper();
    
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
     * SRG container for mcp->srg mappings
     */
    private SrgContainer srgs;
    
    /**
     * True once we've tried to initialise the srgs, initially false so that we
     * can do srg init lazily
     */
    private boolean initDone;
    
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
        this.targets = this.initTargetMap();

        this.printMessage(Kind.NOTE, "SpongePowered Mixin Annotation Processor v" + MixinBootstrap.VERSION);
        
        this.reobfSrgFileName = this.getOption("reobfSrgFile");
        this.outSrgFileName = this.getOption("outSrgFile");
        this.outRefMapFileName = this.getOption("outRefMapFile"); 
        
        this.validators = ImmutableList.<IMixinValidator>of(
            new ParentValidator(processingEnv, this, this),
            new TargetValidator(processingEnv, this, this)
        );
        
        this.initTokenCache(this.getOption("tokens"));
    }

    protected TargetMap initTargetMap() {
        TargetMap targets = TargetMap.create(System.getProperty(AnnotatedMixins.MAPID_SYSTEM_PROPERTY));
        System.setProperty(AnnotatedMixins.MAPID_SYSTEM_PROPERTY, targets.getSessionId());
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
     * Lazy initialisation for srgs, so that we only initialise the srgs if
     * they're actually required.
     */
    private boolean initSrgs() {
        if (!this.initDone) {
            this.initDone = true;
            
            if (this.reobfSrgFileName == null) {
                this.printMessage(Kind.ERROR, "The reobfSrgFile argument was not supplied, obfuscation processing will not occur");
                return false;
            }
            
            try {
                File reobfSrgFile = new File(this.reobfSrgFileName);
                this.printMessage(Kind.NOTE, "Loading SRGs from " + reobfSrgFile.getAbsolutePath());
                this.srgs = new SrgContainer().readSrg(reobfSrgFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                this.printMessage(Kind.ERROR, "The specified SRG file could not be read, processing cannot continue");
                this.srgs = null;
            }
        }
        
        return this.srgs != null;
    }

    /**
     * Write out generated srgs
     */
    public void writeSrgs() {
        if (this.outSrgFileName == null) {
            return;
        }
        
        Set<String> fieldMappings = new LinkedHashSet<String>();
        Set<String> methodMappings = new LinkedHashSet<String>();
        
        for (AnnotatedMixin mixin : this.mixins.values()) {
            fieldMappings.addAll(mixin.getFieldMappings());
            methodMappings.addAll(mixin.getMethodMappings());
        }
        
        PrintWriter writer = null;
        
        try {
            writer = this.openFileWriter(this.outSrgFileName, "output SRGs");
            for (String fieldMapping : fieldMappings) {
                writer.println(fieldMapping);
            }
            for (String methodMapping : methodMappings) {
                writer.println(methodMapping);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    // oh well
                }
            }
        }
    }

    /**
     * Open a writer for an output file
     */
    private PrintWriter openFileWriter(String fileName, String description) throws IOException {
        if (fileName.matches("^.*[\\\\/:].*$")) {
            File outSrgFile = new File(fileName);
            outSrgFile.getParentFile().mkdirs();
            this.printMessage(Kind.NOTE, "Writing " + description + " to " + outSrgFile.getAbsolutePath());
            return new PrintWriter(outSrgFile);
        }
        
        Filer filer = this.processingEnv.getFiler();
        FileObject outSrg = filer.createResource(StandardLocation.CLASS_OUTPUT, "", fileName);
        this.printMessage(Kind.NOTE, "Writing " + description + " to " + new File(outSrg.toUri()).getAbsolutePath());
        return new PrintWriter(outSrg.openWriter());
    }

    /**
     * Write out stored mappings
     */
    public void writeRefs() {
        if (this.outRefMapFileName == null) {
            return;
        }
        
        PrintWriter writer = null;
        
        try {
            writer = this.openFileWriter(this.outRefMapFileName, "refmap");
            this.refMapper.write(writer);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ex) {
                    // oh well
                }
            }
        }
        
    }

    /**
     * Get the reference mapper
     */
    public ReferenceMapper getReferenceMapper() {
        return this.refMapper;
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
        
        mixinClass.registerOverwrite(method, MirrorUtils.getAnnotation(method, Overwrite.class));
    }

    /**
     * Register a {@link org.spongepowered.asm.mixin.Shadow} field
     * 
     * @param mixinType Mixin class
     * @param method Shadow field
     * @param shadow {@link org.spongepowered.asm.mixin.Shadow} annotation
     */
    public void registerShadow(TypeElement mixinType, VariableElement field, AnnotationMirror shadow) {
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
    public void registerShadow(TypeElement mixinType, ExecutableElement method, AnnotationMirror shadow) {
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
    public void registerInjector(TypeElement mixinType, ExecutableElement method, AnnotationMirror inject) {
        AnnotatedMixin mixinClass = this.getMixin(mixinType);
        if (mixinClass == null) {
            String type = "@" + inject.getAnnotationType().asElement().getSimpleName() + "";
            this.printMessage(Kind.ERROR, "Found " + type + " annotation on a non-mixin method", method);
            return;
        }

        boolean remap = this.shouldRemap(mixinClass, inject);
        Message errorMessage = mixinClass.registerInjector(method, inject, remap);
        int remappedAts = 0;
        if (remap) {
            Object ats = MirrorUtils.getAnnotationValue(inject, "at");
            
            if (ats instanceof List) {
                List<?> annotationList = (List<?>)ats;
                for (Object at : annotationList) {
                    // Fix for JDT
                    if (at instanceof AnnotationValue) {
                        at = ((AnnotationValue)at).getValue();
                    }
                    if (at instanceof AnnotationMirror) {
                        remappedAts += mixinClass.registerInjectionPoint(method, inject, (AnnotationMirror)at);
                    } else {
                        this.printMessage(Kind.WARNING, "No annotation mirror on " + at.getClass().getName());
                    }
                }
            } else if (ats instanceof AnnotationMirror) {
                remappedAts += mixinClass.registerInjectionPoint(method, inject, (AnnotationMirror)ats);
            } else if (ats instanceof AnnotationValue) {
                // Fix for JDT
                Object mirror = ((AnnotationValue)ats).getValue();
                if (mirror instanceof AnnotationMirror) {
                    remappedAts += mixinClass.registerInjectionPoint(method, inject, (AnnotationMirror)mirror);
                }
            }
        }
        
        // The annotation was *not* marked as remap=false and failed so we
        // checked for remappable @At annotations, if that failed as well then
        // we raise the original error.
        if (remappedAts == 0 && errorMessage != null) {
            errorMessage.sendTo(this);
        }
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
    public void onPassCompleted() {
        if (!"true".equalsIgnoreCase(this.getOption("disableTargetExport"))) {
            this.targets.write(true);
        }
        
        for (AnnotatedMixin mixin : this.mixinsForPass) {
            mixin.runValidators(ValidationPass.LATE, this.validators);
        }
    }

    private boolean shouldRemap(AnnotatedMixin mixinClass, AnnotationMirror annotation) {
        return mixinClass.remap() && AnnotatedMixins.getRemapValue(annotation);
    }

    /**
     * Check whether we should remap the annotated member or skip it
     */
    public static boolean getRemapValue(AnnotationMirror annotation) {
        return MirrorUtils.<Boolean>getAnnotationValue(annotation, "remap", Boolean.TRUE).booleanValue();
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
     * Print a message to the AP messager
     */
    public void printMessage(Kind kind, CharSequence msg, AnnotatedMixin mixin) {
        this.processingEnv.getMessager().printMessage(kind, msg, mixin.getMixin(), mixin.getAnnotation());
    }
    
    /**
     * Get a TypeHandle representing another type in the current processing
     * environment
     */
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
     * Get an obfuscation mapping for a method
     */
    public MethodData getObfMethod(MemberInfo method) {
        MethodData obfd = this.getObfMethod(method.asMethodData());
        if (obfd != null || !method.isFullyQualified()) {
            return obfd;
        }
        
        // Get a type handle for the declared method owner
        TypeHandle type = this.getTypeHandle(method.owner);
        if (type.isImaginary()) {
            return null;
        }
        
        // See if we can get the superclass from the reference
        TypeMirror superClass = type.getElement().getSuperclass();
        if (superClass.getKind() != TypeKind.DECLARED) {
            return null;
        }
        
        // Well we found it, let's inflect the class name and recurse the search
        String superClassName = ((TypeElement)((DeclaredType)superClass).asElement()).getQualifiedName().toString();
        return this.getObfMethod(new MemberInfo(method.name, superClassName.replace('.', '/'), method.desc, method.matchAll));
    }

    /**
     * Get an obfuscation mapping for a method
     */
    public MethodData getObfMethod(MethodData method) {
        if (this.initSrgs()) {
            return this.srgs.methodMap.get(method);
        }
        return null;
    }

    /**
     * Get an obfuscation mapping for a field
     */
    public String getObfField(String field) {
        if (this.initSrgs()) {
            return this.srgs.fieldMap.get(field);
        }
        return null;
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
