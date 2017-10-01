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
package org.spongepowered.asm.service.mojang;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.Blackboard;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

import com.google.common.collect.ImmutableList;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;

/**
 * Mixin service for launchwrapper
 */
public class MixinServiceLaunchWrapper implements IMixinService {

    // Consts
    private static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";
    private static final String MIXIN_UTIL_PACKAGE = "org.spongepowered.asm.util.";
    private static final String ASM_PACKAGE = "org.spongepowered.asm.lib.";
    private static final String STATE_TWEAKER = MixinServiceLaunchWrapper.MIXIN_PACKAGE + "EnvironmentStateTweaker";
    private static final String TRANSFORMER_PROXY_CLASS = MixinServiceLaunchWrapper.MIXIN_PACKAGE + "transformer.Proxy";

    /**
     * Logger 
     */
    private static final Logger logger = LogManager.getLogger("mixin");

    private final LaunchClassLoaderUtil classLoaderUtil = LaunchClassLoaderUtil.forClassLoader(Launch.classLoader);
    
    private final ReEntranceLock lock = new ReEntranceLock(1);
    
    /**
     * Class name transformer (if present)
     */
    private IClassNameTransformer nameTransformer;
    
    @Override
    public String getName() {
        return "LaunchWrapper";
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            // Detect launchwrapper
            Launch.classLoader.hashCode();
        } catch (Throwable ex) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#prepare()
     */
    @Override
    public void prepare() {
        // The important ones
        Launch.classLoader.addClassLoaderExclusion(MixinServiceLaunchWrapper.ASM_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(MixinServiceLaunchWrapper.MIXIN_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(MixinServiceLaunchWrapper.MIXIN_UTIL_PACKAGE);
        
        // Only needed in dev, in production this would be handled by the tweaker
        Launch.classLoader.addClassLoaderExclusion(MixinServiceLaunchWrapper.LAUNCH_PACKAGE);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#init()
     */
    @Override
    public void init() {
        List<String> tweakClasses = Blackboard.<List<String>>get(Blackboard.Keys.TWEAKCLASSES);
        if (tweakClasses != null) {
            tweakClasses.add(MixinServiceLaunchWrapper.STATE_TWEAKER);
        }
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getReEntranceLock()
     */
    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPlatformAgents()
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.<String>of(
            "org.spongepowered.asm.launch.platform.MixinPlatformAgentFML"
        );
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassLoader()
     */
    @Override
    public ClassLoader getClassLoader() {
        return Launch.classLoader;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getApplicationClassLoader()
     */
    @Override
    public ClassLoader getApplicationClassLoader() {
        return Launch.class.getClassLoader();
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#beginPhase()
     */
    @Override
    public void beginPhase() {
        Launch.classLoader.registerTransformer(MixinServiceLaunchWrapper.TRANSFORMER_PROXY_CLASS);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return Launch.classLoader.getResourceAsStream(name);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#findClass(
     *      java.lang.String)
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return Launch.classLoader.findClass(name);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#registerInvalidClass(
     *      java.lang.String)
     */
    @Override
    public void registerInvalidClass(String className) {
        this.classLoaderUtil.registerInvalidClass(className);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isClassLoaded(
     *      java.lang.String)
     */
    @Override
    public boolean isClassLoaded(String className) {
        return this.classLoaderUtil.isClassLoaded(className);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isClassExcluded(
     *      java.lang.String, java.lang.String)
     */
    @Override
    public boolean isClassExcluded(String name, String transformedName) {
        return this.classLoaderUtil.isClassExcluded(name, transformedName);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassPath()
     */
    @Override
    public URL[] getClassPath() {
        return Launch.classLoader.getSources().toArray(new URL[0]);
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformers()
     */
    @Override
    public Collection<ITransformer> getTransformers() {
        List<IClassTransformer> transformers = Launch.classLoader.getTransformers();
        List<ITransformer> wrapped = new ArrayList<ITransformer>(transformers.size());
        for (IClassTransformer transformer : transformers) {
            if (transformer instanceof ITransformer) {
                wrapped.add((ITransformer)transformer);
            } else {
                wrapped.add(new LegacyTransformerHandle(transformer));
            }
            
            if (transformer instanceof IClassNameTransformer) {
                MixinServiceLaunchWrapper.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                this.nameTransformer = (IClassNameTransformer)transformer;
            }

        }
        return wrapped;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassBytes(
     *      java.lang.String, java.lang.String)
     */
    @Override
    public byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] classBytes = Launch.classLoader.getClassBytes(name);
        if (classBytes != null) {
            return classBytes;
        }
        
        URLClassLoader appClassLoader = (URLClassLoader)Launch.class.getClassLoader();
        
        InputStream classStream = null;
        try {
            final String resourcePath = transformedName.replace('.', '/').concat(".class");
            classStream = appClassLoader.getResourceAsStream(resourcePath);
            return IOUtils.toByteArray(classStream);
        } catch (Exception ex) {
            return null;
        } finally {
            IOUtils.closeQuietly(classStream);
        }
    }
    
    /**
     * Loads class bytecode from the classpath
     * 
     * @param className Name of the class to load
     * @param runTransformers True to run the loaded bytecode through the
     *      delegate transformer chain
     * @return Transformed class bytecode for the specified class
     * @throws ClassNotFoundException if the specified class could not be loaded
     * @throws IOException if an error occurs whilst reading the specified class
     */
    @Override
    public byte[] getClassBytes(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String transformedName = className.replace('/', '.');
        String name = this.unmapClassName(transformedName);
        
        Profiler profiler = MixinEnvironment.getProfiler();
        Section loadTime = profiler.begin(Profiler.ROOT, "class.load");
        byte[] classBytes = this.getClassBytes(name, transformedName);
        loadTime.end();

        if (runTransformers) {
            Section transformTime = profiler.begin(Profiler.ROOT, "class.transform");
            classBytes = this.applyTransformers(name, transformedName, classBytes, profiler);
            transformTime.end();
        }

        if (classBytes == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName));
        }

        return classBytes;
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply
     * the transformers ourself
     * 
     * @param name class name
     * @param transformedName transformed class name
     * @param basicClass input class bytes
     * @return class bytecode after processing by all registered transformers
     *      except the excluded transformers
     */
    private byte[] applyTransformers(String name, String transformedName, byte[] basicClass, Profiler profiler) {
        if (this.isClassExcluded(name, transformedName)) {
            return basicClass;
        }

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        
        for (ILegacyClassTransformer transformer : environment.getTransformers()) {
            // Clear the re-entrance semaphore
            this.lock.clear();
            
            int pos = transformer.getName().lastIndexOf('.');
            String simpleName = transformer.getName().substring(pos + 1);
            Section transformTime = profiler.begin(Profiler.FINE, simpleName.toLowerCase());
            transformTime.setInfo(transformer.getName());
            basicClass = transformer.transformClassBytes(name, transformedName, basicClass);
            transformTime.end();
            
            if (this.lock.isSet()) {
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                environment.addTransformerExclusion(transformer.getName());
                
                this.lock.clear();
                MixinServiceLaunchWrapper.logger.info("A re-entrant transformer '{}' was detected and will no longer process meta class data",
                        transformer.getName());
            }
        }

        return basicClass;
    }

    private String unmapClassName(String className) {
        if (this.nameTransformer == null) {
            this.findNameTransformer();
        }
        
        if (this.nameTransformer != null) {
            return this.nameTransformer.unmapClassName(className);
        }
        
        return className;
    }

    private void findNameTransformer() {
        List<IClassTransformer> transformers = Launch.classLoader.getTransformers();
        for (IClassTransformer transformer : transformers) {
            if (transformer instanceof IClassNameTransformer) {
                MixinServiceLaunchWrapper.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                this.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassNode(
     *      java.lang.String)
     */
    @Override
    public ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return this.getClassNode(this.getClassBytes(className, true), 0);
    }

    /**
     * Gets an ASM Tree for the supplied class bytecode
     * 
     * @param classBytes Class bytecode
     * @param flags ClassReader flags
     * @return ASM Tree view of the specified class 
     */
    private ClassNode getClassNode(byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getSideName()
     */
    @Override
    public final String getSideName() {
        // Using this method first prevents us from accidentally loading FML classes
        // too early when using the tweaker in dev
        for (ITweaker tweaker : this.<List<ITweaker>>getGlobalProperty(Blackboard.Keys.TWEAKS)) {
            if (tweaker.getClass().getName().endsWith(".common.launcher.FMLServerTweaker")) {
                return "SERVER";
            } else if (tweaker.getClass().getName().endsWith(".common.launcher.FMLTweaker")) {
                return "CLIENT";
            }
        }
        
        String name = this.getSideName("net.minecraftforge.fml.relauncher.FMLLaunchHandler", "side");
        if (name != null) {
            return name;
        }
        
        name = this.getSideName("cpw.mods.fml.relauncher.FMLLaunchHandler", "side");
        if (name != null) {
            return name;
        }
        
        name = this.getSideName("com.mumfrey.liteloader.launch.LiteLoaderTweaker", "getEnvironmentType");
        if (name != null) {
            return name;
        }
        
        return "UNKNOWN";
    }

    private String getSideName(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className, false, Launch.classLoader);
            Method method = clazz.getDeclaredMethod(methodName);
            return ((Enum<?>)method.invoke(null)).name();
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Get a value from the blackboard and duck-type it to the specified type
     * 
     * @param key blackboard key
     * @return value
     * @param <T> duck type
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getGlobalProperty(String key) {
        return (T)Launch.blackboard.get(key);
    }

    /**
     * Put the specified value onto the blackboard
     * 
     * @param key blackboard key
     * @param value new value
     */
    @Override
    public final void setGlobalProperty(String key, Object value) {
        Launch.blackboard.put(key, value);
    }
    
    /**
     * Get the value from the blackboard but return <tt>defaultValue</tt> if the
     * specified key is not set.
     * 
     * @param key blackboard key
     * @param defaultValue value to return if the key is not set or is null
     * @return value from blackboard or default value
     * @param <T> duck type
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> T getGlobalProperty(String key, T defaultValue) {
        Object value = Launch.blackboard.get(key);
        return value != null ? (T)value : defaultValue;
    }
    
    /**
     * Get a string from the blackboard, returns default value if not set or
     * null.
     * 
     * @param key blackboard key
     * @param defaultValue default value to return if the specified key is not
     *      set or is null
     * @return value from blackboard or default
     */
    @Override
    public final String getGlobalPropertyString(String key, String defaultValue) {
        Object value = Launch.blackboard.get(key);
        return value != null ? value.toString() : defaultValue;
    }

}
