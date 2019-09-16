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
package org.spongepowered.asm.service.modlauncher;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.jar.Manifest;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;

import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.TransformingClassLoader;

/**
 * Shim for accessing resources inside modlauncher that Mixin needs to operate.
 * Ideally in the long run facilities can be provided directly within
 * modlauncher to make this shim unnecessary.
 */
class Internals {

    /**
     * Temporary
     */
    static class MixinAppender extends AbstractAppender {

        private final List<Runnable> startupListeners = new ArrayList<Runnable>();

        MixinAppender() {
            super("MixinLogWatcherAppender", null, null);
            this.start();
        }
        
        void addListener(Runnable listener) {
            synchronized (this.startupListeners) {
                this.startupListeners.add(listener);
            }
        }

        @Override
        public void append(LogEvent event) {
            if (event.getLevel() != Level.INFO || !event.getMessage().getFormat().startsWith("Launching target '")) {
                return;
            }
            
            // transition to DEFAULT
            synchronized (this.startupListeners) {
                for (Runnable listener : this.startupListeners) {
                    listener.run();
                }
            }
        }
        
    }

    /**
     * Logger 
     */
    private static final Logger logger = LogManager.getLogger("mixin");
    
    private static MixinAppender appender;
    private static org.apache.logging.log4j.core.Logger launcherLogger;

    private static Internals instance;

    private static final String FD_CLASSLOADER = "classLoader";
    private static final String FD_CLASSTRANSFORMER = "classTransformer";
    
    private static final String FD_MANIFESTFINDER = "manifestFinder";
    private static final String FD_CLASSBYTESFINDER = "classBytesFinder";

    private static final String MD_TRANSFORM = "transform";

//    private static final String MD_LOADCLASS = "loadClass";
    
    private TransformingClassLoader transformingClassLoader;
    private ClassTransformer classTransformer;
    
    private Function<URLConnection, Manifest> manifestFinder;
    private Function<String, URL> classBytesFinder;
    
//    private Method mdLoadClass;
    private Method mdTransform;
    
    TransformingClassLoader getTransformingClassLoader() {
        if (this.transformingClassLoader == null) {
            this.transformingClassLoader = Internals.<Launcher, TransformingClassLoader>getField(Launcher.class, Launcher.INSTANCE,
                    Internals.FD_CLASSLOADER);
        }
        return this.transformingClassLoader;
    }
    
    ClassTransformer getClassTransformer() {
        if (this.classTransformer == null) {
            this.classTransformer = Internals.<TransformingClassLoader, ClassTransformer>getField(TransformingClassLoader.class,
                    this.getTransformingClassLoader(), Internals.FD_CLASSTRANSFORMER);
        }
        return this.classTransformer;
    }
    
    Function<URLConnection, Manifest> getManifestFinder() {
        if (this.manifestFinder == null) {
            this.manifestFinder = Internals.<TransformingClassLoader, Function<URLConnection, Manifest>>getField(TransformingClassLoader.class,
                    this.getTransformingClassLoader(), Internals.FD_MANIFESTFINDER);
        }
        return this.manifestFinder;
    }

    Function<String, URL> getClassBytesFinder() {
        if (this.classBytesFinder == null) {
            this.classBytesFinder = Internals.<TransformingClassLoader, Function<String, URL>>getField(TransformingClassLoader.class,
                    this.getTransformingClassLoader(), Internals.FD_CLASSBYTESFINDER);
        }
        return this.classBytesFinder;
    }

//    /**
//     * Invokes 'loadClass' on the Transforming classloader
//     * 
//     * @param name class name to load
//     * @param resolve If <tt>true</tt> then resolve the class
//     * @return loaded class
//     * @throws ClassNotFoundException propagated
//     */
//    public Class<?> findClass(String name, boolean resolve) throws ClassNotFoundException {
//        if (this.mdLoadClass == null) {
//            this.mdLoadClass = Internals.getMethod(TransformingClassLoader.class, Internals.MD_LOADCLASS);
//        }
//        if (this.mdLoadClass != null) {
//            try {
//                return (Class<?>)this.mdLoadClass.invoke(this.getTransformingClassLoader(), name, resolve);
//            } catch (ReflectiveOperationException ex) {
//                ex.printStackTrace();
//            }
//        }
//        return Class.forName("name");
//    }

    byte[] simulateClassLoad(String name, boolean runTransformers) throws ClassNotFoundException {
        Function<String, URL> classBytesFinder = this.getClassBytesFinder();
        
        final String path = name.replace('.', '/').concat(".class");
        final URL classResource = classBytesFinder.apply(path);
        byte[] classBytes;
        if (classResource != null) {
            try (AutoURLConnection urlConnection = new AutoURLConnection(classResource, this.getManifestFinder())) {
                final int length = urlConnection.getContentLength();
                final InputStream is = urlConnection.getInputStream();
                classBytes = new byte[length];
                int pos = 0, remain = length, read;
                while ((read = is.read(classBytes, pos, remain)) != -1 && remain > 0) {
                    pos += read;
                    remain -= read;
                }
                @SuppressWarnings("unused")
                Manifest jarManifest = urlConnection.getJarManifest();
            } catch (IOException e) {
                throw new ClassNotFoundException("Failed to find class bytes for " + name, e);
            }
        } else {
            classBytes = new byte[0];
        }
        return this.transform(classBytes, name);
    }

    /**
     * Copied directly from ModLauncher, ideally won't be necessary in the long
     * run
     */
    static class AutoURLConnection implements AutoCloseable {
        private final URLConnection urlConnection;
        private final InputStream inputStream;
        private final Function<URLConnection, Manifest> manifestFinder;

        AutoURLConnection(URL url, Function<URLConnection, Manifest> manifestFinder) throws IOException {
            this.urlConnection = url.openConnection();
            this.inputStream = this.urlConnection.getInputStream();
            this.manifestFinder = manifestFinder;
        }

        @Override
        public void close() throws IOException {
            this.inputStream.close();
        }

        int getContentLength() {
            return this.urlConnection.getContentLength();
        }

        InputStream getInputStream() {
            return this.inputStream;
        }

        Manifest getJarManifest() {
            return this.manifestFinder.apply(this.urlConnection);
        }
    }

    private byte[] transform(byte[] inputClass, String className) {
        if (this.mdTransform == null) {
            this.mdTransform = Internals.getMethod(ClassTransformer.class, Internals.MD_TRANSFORM, byte[].class, String.class);
        }
        if (this.mdTransform == null) {
            return inputClass;
        }
        try {
            return (byte[])this.mdTransform.invoke(this.getClassTransformer(), inputClass, className);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw new RuntimeException(cause);
        } catch (ReflectiveOperationException ex) {
            Internals.logger.catching(ex);
        }
        return inputClass;
    }

    @SuppressWarnings("unchecked")
    private static <C, T> T getField(Class<C> clazz, C instance, String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T)field.get(instance);
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        return null;
    }

    private static Method getMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        try {
            Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
        return null;
    }
    
    void registerStartupListener(Runnable listener) {
        if (Internals.appender == null) {
            Logger launcherLog = LogManager.getLogger("cpw.mods.modlauncher.LaunchServiceHandler");
            if (!(launcherLog instanceof org.apache.logging.log4j.core.Logger)) {
                return;
            }
            
            Internals.appender = new MixinAppender();
            Internals.launcherLogger = (org.apache.logging.log4j.core.Logger)launcherLog;
            Internals.launcherLogger.addAppender(Internals.appender);
        }
        
        Internals.appender.addListener(listener);
    }
    
    void destroyLogWatcher() {
        if (Internals.launcherLogger != null) {
            Internals.launcherLogger.removeAppender(Internals.appender);
        }
    }

    static synchronized Internals getInstance() {
        if (Internals.instance == null) {
            Internals.instance = new Internals();
        }
        return Internals.instance;
    }

}
