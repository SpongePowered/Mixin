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
package org.spongepowered.asm.util;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;

/**
 * Prints information in a pretty box
 */
public class PrettyPrinter {
    
    public interface IPrettyPrintable {
        
        public abstract void print(PrettyPrinter printer);
        
    }
    
    /**
     * "Horizontal rule" marker
     */
    private static final char HR = '\253';

    /**
     * If specified as the first character on a line, centres the line
     */
    private static final char CENTRE = '\252';
    
    /**
     * Box with (adapts to contents)
     */
    private int width = 100;
    
    /**
     *  Wrap width used when an explicit wrap width is not specified
     */
    private int wrapWidth = 80;
    
    /**
     * Content lines
     */
    private final List<String> lines = new ArrayList<String>();
    
    public PrettyPrinter() {
        this(100);
    }
    
    public PrettyPrinter(int width) {
        this.width = width;
    }
    
    public PrettyPrinter wrapTo(int wrapWidth) {
        this.wrapWidth = wrapWidth;
        return this;
    }
    
    public int wrapTo() {
        return this.wrapWidth;
    }

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
     * Add elements of the array to the output, one per line
     * 
     * @param array Array of objects to print 
     * @return fluent interface
     */
    public PrettyPrinter add(Object[] array) {
        return this.add(array, "%s");
    }
    
    /**
     * Add elements of the array to the output, one per line
     * 
     * @param array Array of objects to print
     * @param format Format for each row
     * @return fluent interface
     */
    public PrettyPrinter add(Object[] array, String format) {
        for (Object element : array) {
            this.add(format, element);
        }
        
        return this;
    }
    
    public PrettyPrinter add(IPrettyPrintable printable) {
        if (printable != null) {
            printable.print(this);
        }
        return this;
    }

    public PrettyPrinter addWrapped(String format, Object... args) {
        return this.addWrapped(this.wrapWidth, format, args);
    }

    /**
     * Adds a formatted line to the output, and attempts to wrap the line
     * content to the specified width
     *
     * @param width wrap width to use for this content
     * @param format format string
     * @param args arguments
     * 
     * @return fluent interface
     */
    public PrettyPrinter addWrapped(int width, String format, Object... args) {
        String indent = "";
        String line = String.format(format, args).replace("\t", "    ");
        Matcher indentMatcher = Pattern.compile("^(\\s+)(.*)$").matcher(line);
        if (indentMatcher.matches()) {
            indent = indentMatcher.group(1);
        }
        
        try {
            for (String wrappedLine : this.getWrapped(width, line, indent)) {
                this.lines.add(wrappedLine);
            }
        } catch (Exception ex) {
            this.add(line);
        }
        return this;
    }

    private List<String> getWrapped(int width, String line, String indent) {
        List<String> lines = new ArrayList<String>();
        
        while (line.length() > width) {
            int wrapPoint = line.lastIndexOf(' ', width);
            if (wrapPoint < 10) {
                wrapPoint = width;
            }
            String head = line.substring(0, wrapPoint);
            lines.add(head);
            line = indent + line.substring(wrapPoint + 1);
        }
        
        if (line.length() > 0) {
            lines.add(line);
        }
        
        return lines;
    }
    
    /**
     * Adds a horizontal rule to the output
     * 
     * @return fluent interface
     */
    public PrettyPrinter hr() {
        return this.hr('*');
    }
    
    
    /**
     * Adds a horizontal rule of the specified char to the output
     * 
     * @param ruleChar character to use for the horizontal rule
     * @return fluent interface
     */
    public PrettyPrinter hr(char ruleChar) {
        this.lines.add("" + PrettyPrinter.HR + ruleChar);
        return this;
    }
    
    /**
     * Centre the last line added
     * 
     * @return fluent interface
     */
    public PrettyPrinter centre() {
        if (!this.lines.isEmpty()) {
            this.lines.add(PrettyPrinter.CENTRE + this.lines.remove(this.lines.size() - 1));
        }
        return this;
    }

    /**
     * Print this printer to the specified output
     * 
     * @param stream stream to print to
     */
    public void print(PrintStream stream) {
        this.printHr(stream, '*');
        for (String line : this.lines) {
            int len = line.length();
            if (len > 1 && line.charAt(0) == PrettyPrinter.HR) {
                this.printHr(stream, line.charAt(1));
            } else {
                if (len > 0 && line.charAt(0) == PrettyPrinter.CENTRE) {
                    String text = line.substring(1);
                    line = String.format("%" + (((this.width - (text.length())) / 2) + text.length()) + "s", text);
                }
                stream.printf("/* %-" + this.width + "s */\n", line);
            }
        }
        this.printHr(stream, '*');
    }

    private void printHr(PrintStream stream, char... hrChars) {
        stream.printf("/*%s*/\n", Strings.repeat(new String(hrChars), this.width + 2));
    }
    
    public void log(Logger logger) {
        this.log(logger, Level.INFO);
    }
    
    /**
     * Write this printer to the specified logger
     * 
     * @param logger logger to log to
     * @param level log level
     */
    public void log(Logger logger, Level level) {
        this.logHr(logger, level, '*');
        for (String line : this.lines) {
            int len = line.length();
            if (len > 1 && line.charAt(0) == PrettyPrinter.HR) {
                this.logHr(logger, level, line.charAt(1));
            } else {
                if (len > 0 && line.charAt(0) == PrettyPrinter.CENTRE) {
                    String text = line.substring(1);
                    line = String.format("%" + (((this.width - (text.length())) / 2) + text.length()) + "s", text);
                }
                logger.log(level, String.format("/* %-" + this.width + "s */", line));
            }
        }
        this.logHr(logger, level, '*');
    }
    
    private void logHr(Logger logger, Level level, char... hrChars) {
        logger.log(level, String.format("/*%s*/", Strings.repeat(new String(hrChars), this.width + 2)));
    }
}
