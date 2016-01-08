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
package org.spongepowered.asm.mixin.environment.phase;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class OnBeginGame extends AbstractPhaseTransition {
    
    static OnBeginGame instance;
    
    private OnBeginGame() {
        @SuppressWarnings("unchecked")
        List<String> tweakClasses = (List<String>)Launch.blackboard.get("TweakClasses");
        if (tweakClasses != null) {
            tweakClasses.add(OnBeginGame.class.getName() + "$StateTweaker");
        }
    }
    
    void begin() {
        this.phase.begin();
    }
    
    public static OnBeginGame onBeginGame() {
        if (OnBeginGame.instance == null) {
            OnBeginGame.instance = new OnBeginGame();
        }
        return OnBeginGame.instance;
    }
    
    public static class StateTweaker implements ITweaker { 
        
        @Override
        public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        }
    
        @Override
        public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        }
    
        @Override
        public String getLaunchTarget() {
            return "";
        }
    
        @Override
        public String[] getLaunchArguments() {
            OnBeginGame.instance.begin();
            return new String[0];
        }
    }

}
