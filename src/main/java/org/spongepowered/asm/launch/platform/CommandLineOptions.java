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
package org.spongepowered.asm.launch.platform;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Options passed in to Mixin via the command line
 */
public final class CommandLineOptions {
    
    private List<String> configs = new ArrayList<String>();
    
    private CommandLineOptions() {
    }
    
    public List<String> getConfigs() {
        return Collections.<String>unmodifiableList(this.configs);
    }

    /**
     * Read and parse command-line arguments
     * 
     * @param args command-line arguments
     */
    private void parseArgs(List<String> args) {
        boolean captureNext = false;
        for (String arg : args) {
            if (captureNext) {
                this.configs.add(arg);
            }
            captureNext = "--mixin".equals(arg) || "--mixin.config".equals(arg);
        }
    }
    
    /**
     * Create a CommandLineOptions with default args (read from system property)
     * 
     * @return CommandLineOptions instance
     */
    public static CommandLineOptions defaultArgs() {
        return CommandLineOptions.ofArgs(null);
    }
    
    /**
     * Create a CommandLineOptions using the supplied unparsed argument list.
     * Uses args from system property if 
     * 
     * @param args Argument list to parse, can be null
     * @return CommandLineOptions instance
     */
    public static CommandLineOptions ofArgs(List<String> args) {
        CommandLineOptions options = new CommandLineOptions();
        if (args == null) {
            String argv = System.getProperty("sun.java.command");
            if (argv != null) {
                args = Arrays.asList(argv.split(" "));
            }            
        }
        if (args != null) {
            options.parseArgs(args);
        }
        return options;
    }

    /**
     * Create a CommandLineOptions using the supplied list of pre-parsed configs
     * 
     * @param configs List of configs
     * @return CommandLineOptions instance
     */
    public static CommandLineOptions of(List<String> configs) {
        CommandLineOptions options = new CommandLineOptions();
        options.configs.addAll(configs);
        return options;
    }

}
