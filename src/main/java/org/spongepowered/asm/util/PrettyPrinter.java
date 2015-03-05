/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;


/**
 * Prints information in a pretty box
 */
public class PrettyPrinter {
    
    /**
     * "Horizontal rule" marker
     */
    private static final String HR = "---";
    
    /**
     * Box with (adapts to contents)
     */
    private int width = 100;
    
    /**
     * Content lines
     */
    private final List<String> lines = new ArrayList<String>();
    
    /**
     * Adds a blank line to the output
     * 
     * @return fluent interface
     */
    public PrettyPrinter add() {
        this.lines.add("");
        return this;
    }

    /**
     * Adds a formatted line to the output
     * 
     * @param format format string
     * @param args arguments
     * 
     * @return fluent interface
     */
    public PrettyPrinter add(String format, Object... args) {
        String line = String.format(format, args);
        this.lines.add(line);
        this.width = Math.max(this.width, line.length());
        return this;
    }
    
    /**
     * Adds a horizontal rule to the output
     * 
     * @return fluent interface
     */
    public PrettyPrinter hr() {
        this.lines.add(PrettyPrinter.HR);
        return this;
    }

    /**
     * Print this printer to the specified output
     * 
     * @param stream stream to print to
     */
    public void print(PrintStream stream) {
        this.printHr(stream);
        for (String line : this.lines) {
            if (line == PrettyPrinter.HR) {
                this.printHr(stream);
            } else {
                stream.printf("/* %-" + this.width + "s */\n", line);
            }
        }
        this.printHr(stream);
    }

    private void printHr(PrintStream stream) {
        stream.printf("/*%s*/\n", Strings.repeat("*", this.width + 2));
    }
}
