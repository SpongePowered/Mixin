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
    
    /**
     * Interface for object which supports printing to pretty printer
     */
    public interface IPrettyPrintable {
        
        /**
         * Append this objec to specified pretty printer
         * 
         * @param printer printer to append to
         */
        public abstract void print(PrettyPrinter printer);
        
    }
    
    /**
     * Interface for objects which need their width calculated prior to printing
     */
    interface IVariableWidthEntry {
        
        public abstract int getWidth();
        
    }
    
    /**
     * Interface for objects which control their own output format
     */
    interface ISpecialEntry {
        
    }
    
    /**
     * A key/value pair for convenient printing
     */
    class KeyValue implements PrettyPrinter.IVariableWidthEntry {
        
        private final String key;
        
        private final Object value;
        
        public KeyValue(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format(PrettyPrinter.this.kvFormat, this.key, this.value);
        }

        @Override
        public int getWidth() {
            return this.toString().length();
        }
        
    }
    
    /**
     * Horizontal rule
     */
    class HorizontalRule implements PrettyPrinter.ISpecialEntry {
        
        private final char[] hrChars;

        public HorizontalRule(char... hrChars) {
            this.hrChars = hrChars;
        }
        
        @Override
        public String toString() {
            return Strings.repeat(new String(this.hrChars), PrettyPrinter.this.width + 2);
        }
        
    }
    
    /**
     * Centred text
     */
    class CentredText {
        
        private final Object centred;

        public CentredText(Object centred) {
            this.centred = centred;
        }
        
        @Override
        public String toString() {
            String text = this.centred.toString();
            return String.format("%" + (((PrettyPrinter.this.width - (text.length())) / 2) + text.length()) + "s", text);
        }
        
    }
    
    private final HorizontalRule horizontalRule = new HorizontalRule('*');

    /**
     * Content lines
     */
    private final List<Object> lines = new ArrayList<Object>();
    
    private boolean recalcWidth = false;
    
    /**
     * Box with (adapts to contents)
     */
    protected int width = 100;
    
    /**
     *  Wrap width used when an explicit wrap width is not specified
     */
    protected int wrapWidth = 80;
    
    /**
     * Key/value key width
     */
    protected int kvKeyWidth = 10;
    
    protected String kvFormat = PrettyPrinter.makeKvFormat(this.kvKeyWidth); 
    
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
     * Add a formatted key/value pair to the output
     * 
     * @param key Key
     * @param format Value format
     * @param args Value args
     * @return fluent interface
     */
    public PrettyPrinter kv(String key, String format, Object... args) {
        return this.kv(key, String.format(format, args));
    }
    
    /**
     * Add a key/value pair to the output
     * 
     * @param key Key
     * @param value Value
     * @return fluent interface
     */
    public PrettyPrinter kv(String key, Object value) {
        this.lines.add(new KeyValue(key, value));
        return this.kvWidth(key.length());
    }
    
    /**
     * Set the minimum key display width
     * 
     * @param width width to set
     * @return fluent
     */
    public PrettyPrinter kvWidth(int width) {
        if (width > this.kvKeyWidth) {
            this.kvKeyWidth = width;
            this.kvFormat = PrettyPrinter.makeKvFormat(width);
        }
        this.recalcWidth = true;
        return this;
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
        this.lines.add(new HorizontalRule(ruleChar));
        return this;
    }
    
    /**
     * Centre the last line added
     * 
     * @return fluent interface
     */
    public PrettyPrinter centre() {
        if (!this.lines.isEmpty()) {
            Object lastLine = this.lines.get(this.lines.size() - 1);
            if (lastLine instanceof String) {
                this.lines.add(new CentredText(this.lines.remove(this.lines.size() - 1)));
            }
        }
        return this;
    }

    /**
     * Print this printer to the specified output
     * 
     * @param stream stream to print to
     */
    public void print(PrintStream stream) {
        this.updateWidth();
        this.printSpecial(stream, this.horizontalRule);
        for (Object line : this.lines) {
            if (line instanceof ISpecialEntry) {
                this.printSpecial(stream, (ISpecialEntry)line);
            } else {
                this.printString(stream, line.toString());
            }
        }
        this.printSpecial(stream, this.horizontalRule);
    }

    private void printSpecial(PrintStream stream, ISpecialEntry line) {
        stream.printf("/*%s*/\n", line.toString());
    }

    private void printString(PrintStream stream, String string) {
        stream.printf("/* %-" + this.width + "s */\n", string);
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
        this.updateWidth();
        this.logSpecial(logger, level, this.horizontalRule);
        for (Object line : this.lines) {
            if (line instanceof ISpecialEntry) {
                this.logSpecial(logger, level, (ISpecialEntry)line);
            } else {
                this.logString(logger, level, line.toString());
            }
        }
        this.logSpecial(logger, level, this.horizontalRule);
    }

    private void logSpecial(Logger logger, Level level, ISpecialEntry line) {
        logger.log(level, "/*{}*/\n", line.toString());
    }

    private void logString(Logger logger, Level level, String line) {
        logger.log(level, String.format("/* %-" + this.width + "s */", line));
    }
    
    private void updateWidth() {
        if (this.recalcWidth) {
            this.recalcWidth = false;
            for (Object line : this.lines) {
                if (line instanceof IVariableWidthEntry) {
                    this.width = Math.max(this.width, ((IVariableWidthEntry)line).getWidth());
                }
            }
        }
    }

    private static String makeKvFormat(int keyWidth) {
        return String.format("%%%ds : %%s", keyWidth);
    }

}
