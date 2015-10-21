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
package org.spongepowered.tools.agent;

import net.minecraft.launchwrapper.Launch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.mixin.transformer.debug.IHotSwap;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * An agent that re-transforms a mixin's target classes if the mixin has been
 * redefined. Basically this agent enables hot-swapping of mixins.
 */
public class MixinAgent implements IHotSwap {

    /**
     * Class file transformer that re-transforms mixin's target classes if the
     * mixin has been redefined.
     */
    private class Transformer implements ClassFileTransformer {

        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) throws IllegalClassFormatException {
            if (classBeingRedefined == null) {
                return null;
            }
            byte[] mixinBytecode = MixinAgent.classLoader.getFakeMixinBytecode(classBeingRedefined);
            if (mixinBytecode != null) {
                MixinAgent.logger.info("Redefining mixin " + className);
                List<String> targets;
                try {
                    targets = MixinAgent.this.classTransformer.reload(className.replace('/', '.'), classfileBuffer);
                } catch (Throwable th) {
                    // catch everything as otherwise it is ignored
                    MixinAgent.logger.error("Error while finding targets for mixin " + className, th);
                    return mixinBytecode;
                }
                for (String target : targets) {
                    String targetName = target.replace('/', '.');
                    MixinAgent.logger.debug("Re-transforming target class " + target);
                    try {
                        Class<?> targetClass = Launch.classLoader.findClass(targetName);
                        byte[] targetBytecode = MixinAgent.classLoader.getOriginalTargetBytecode(targetName);
                        if (targetBytecode == null) {
                            MixinAgent.logger.error("Target class " + targetName + " hasn't registered its bytecode");
                            continue;
                        }
                        targetBytecode = MixinAgent.this.classTransformer.transform(null, targetName, targetBytecode);
                        instrumentation.redefineClasses(new ClassDefinition(targetClass, targetBytecode));
                    } catch (Throwable th) {
                        MixinAgent.logger.error("Error while re-transforming target class " + target, th);
                    }
                }
                return mixinBytecode;
            }
            try {
                MixinAgent.logger.info("Redefining class " + className);
                return MixinAgent.this.classTransformer.transform(null, className, classfileBuffer);
            } catch (Throwable th) {
                MixinAgent.logger.error("Error while re-transforming class " + className, th);
                return null;
            }
        }
    }

    /**
     * Class loader used to load mixin classes
     */
    private static final MixinClassLoader classLoader = new MixinClassLoader();

    private static final Logger logger = LogManager.getLogger("mixin.agent");

    /**
     * Instance used to register the transformer
     */
    private static Instrumentation instrumentation = null;

    /**
     * Instances of all agents
     */
    private static List<MixinAgent> agents = new ArrayList<MixinAgent>();

    /**
     * MixinTransformer instance to use to transform the mixin's target classes
     */
    private final MixinTransformer classTransformer;

    /**
     * Constructs an agent from a class transformer in which it will use to
     * transform mixin's target class.
     *
     * @param classTransformer Class transformer that will transform a mixin's
     *                         target class
     */
    public MixinAgent(MixinTransformer classTransformer) {
        this.classTransformer = classTransformer;
        MixinAgent.agents.add(this);
        if (MixinAgent.instrumentation != null) {
            this.initTransformer();
        }
    }

    private void initTransformer() {
        MixinAgent.instrumentation.addTransformer(new Transformer(), true);
    }

    @Override
    public void registerMixinClass(String name) {
        MixinAgent.classLoader.addMixinClass(name);
    }

    @Override
    public void registerTargetClass(String name, byte[] bytecode) {
        MixinAgent.classLoader.addTargetClass(name, bytecode);
    }

    /**
     * Sets the instrumentation instance so that the mixin agents can redefine
     * mixins.
     *
     * @param instrumentation Instance to use to redefine the mixins
     */
    public static void init(Instrumentation instrumentation) {
        MixinAgent.instrumentation = instrumentation;
        if (!MixinAgent.instrumentation.isRedefineClassesSupported()) {
            MixinAgent.logger.error("The instrumentation doesn't support re-definition of classes");
        }
        for (MixinAgent agent:agents) {
            agent.initTransformer();
        }
    }

    /**
     * Initialize the java agent
     *
     * This will be called automatically if the jar is in a -javaagent java
     * command line argument
     *
     * @param arg Ignored
     * @param instrumentation Instance to use to transform the mixins
     */
    public static void premain(String arg, Instrumentation instrumentation) {
        init(instrumentation);
    }

    /**
     * Initialize the java agent
     *
     * This will be called automatically if the java agent is loaded after jvm
     * startup
     *
     * @param arg Ignored
     * @param instrumentation Instance to use to re-define the mixins
     */
    public static void agentmain(String arg, Instrumentation instrumentation) {
        init(instrumentation);
    }
}
