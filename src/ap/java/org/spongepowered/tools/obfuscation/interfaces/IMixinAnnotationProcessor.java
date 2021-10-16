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
package org.spongepowered.tools.obfuscation.interfaces;

import javax.annotation.processing.ProcessingEnvironment;

import org.spongepowered.asm.util.ITokenProvider;

/**
 * Interface for annotation processor core
 */
public interface IMixinAnnotationProcessor extends IMessagerSuppressible, IOptionProvider {

    /**
     * Detected compiler argument, specifies the behaviour of some operations
     * for compatibility purposes.
     */
    public static enum CompilerEnvironment {
        
        /**
         * Default environment
         */
        JAVAC(false, "Java Compiler"),
        
        /**
         * Eclipse 
         */
        JDT(true, "Eclipse (JDT)") {

            @Override
            protected boolean isDetected(ProcessingEnvironment processingEnv) {
                return processingEnv.getClass().getName().contains("jdt");
            }

        },
        
        /**
         * IntelliJ IDEA
         */
        IDEA(true, "IntelliJ IDEA") {

            @Override
            protected boolean isDetected(ProcessingEnvironment processingEnv) {
                for (String ideaSystemProperty : new String[] {
                        "idea.plugins.path",
                        "idea.config.path",
                        "idea.home.path",
                        "idea.paths.selector"
                    }) {
                    if (System.getProperty(ideaSystemProperty) != null) {
                        return true;
                    }
                }
                return false;
            }

        };

        /**
         * True if this compiler environment is an IDE
         */
        private final boolean isDevelopmentEnvironment;
        
        /**
         * Display name
         */
        private final String friendlyName;

        private CompilerEnvironment(boolean isDevelopmentEnvironment, String friendlyName) {
            this.isDevelopmentEnvironment = isDevelopmentEnvironment;
            this.friendlyName = friendlyName;
        }
        
        /**
         * True if this compiler environment is not an IDE
         */
        public boolean isCompiler() {
            return !this.isDevelopmentEnvironment;
        }
        
        /**
         * True if this compiler environment is an IDE
         */
        public boolean isDevelopmentEnvironment() {
            return this.isDevelopmentEnvironment;
        }
        
        /**
         * Get the human-readable name of this environment
         */
        public String getFriendlyName() {
            return this.friendlyName;
        }
        
        /**
         * Stub for each compiler environment to implement heuristics to detect
         * the presence of the environment
         * 
         * @param processingEnv Current processing environment to detect
         *      compiler environment from
         * @return true if this environment is detected
         */
        protected boolean isDetected(ProcessingEnvironment processingEnv) {
            return false;
        }

        /**
         * Detect compiler environments which require special handling (for the
         * time being, the only special environments we care about are IDEs)
         * using heuristic checks such as looking for specific class names or
         * system properties.
         * 
         * @param processingEnv Current processing environment to detect
         *      compiler environment from
         * @return detected compiler environment, defaults to {@link #JAVAC} if
         *      no special environment is detected
         */
        public static CompilerEnvironment detect(ProcessingEnvironment processingEnv) {
            for (CompilerEnvironment environment : CompilerEnvironment.values()) {
                if (environment.isDetected(processingEnv)) {
                    return environment;
                }
            }
            return CompilerEnvironment.JAVAC;
        }
        
    }
    
    /**
     * Get the detected compiler environment
     */
    public abstract CompilerEnvironment getCompilerEnvironment();

    /**
     * Get the underlying processing environment
     */
    public abstract ProcessingEnvironment getProcessingEnvironment();

    /**
     * Get the obfuscation manager
     */
    public abstract IObfuscationManager getObfuscationManager();
    
    /**
     * Get the token provider
     */
    public abstract ITokenProvider getTokenProvider();
    
    /**
     * Get the type handle provider
     */
    public abstract ITypeHandleProvider getTypeProvider();

    /**
     * Get the javadoc provider
     */
    public abstract IJavadocProvider getJavadocProvider();

}
