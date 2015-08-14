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
package org.spongepowered.asm.launch;

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;


/**
 * A collection of {@link IMixinLaunchAgent launch agents)
 */
public class MixinTweakContainer {

    public static final List<String> agentClasses = new ArrayList<String>();
    
    static {
        Launch.blackboard.put("mixin.agents", MixinTweakContainer.agentClasses);
        MixinTweakContainer.agentClasses.add("org.spongepowered.asm.launch.MixinLaunchAgentFML");
        MixinTweakContainer.agentClasses.add("org.spongepowered.asm.launch.MixinLaunchAgentDefault");
    }
    
    private final URI uri;
    
    private final List<IMixinLaunchAgent> agents = new ArrayList<IMixinLaunchAgent>();

    public MixinTweakContainer(URI uri) {
        this.uri = uri;
        
        for (String agentClass : MixinTweakContainer.agentClasses) {
            try {
                @SuppressWarnings("unchecked")
                Class<IMixinLaunchAgent> clazz = (Class<IMixinLaunchAgent>)Class.forName(agentClass);
                Constructor<IMixinLaunchAgent> ctor = clazz.getDeclaredConstructor(URI.class);
                IMixinLaunchAgent agent = ctor.newInstance(uri);
                this.agents.add(agent);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * 
     */
    public URI getURI() {
        return this.uri;
    }

    /**
     * 
     */
    public void prepare() {
        for (IMixinLaunchAgent agent : this.agents) {
            agent.prepare();
        }
    }

    /**
     * @param classLoader
     */
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        for (IMixinLaunchAgent agent : this.agents) {
            agent.injectIntoClassLoader(classLoader);
        }
    }

    public String getLaunchTarget() {
        for (IMixinLaunchAgent agent : this.agents) {
            String launchTarget = agent.getLaunchTarget();
            if (launchTarget != null) {
                return launchTarget;
            }
        }
        return null;
    }
    
}
