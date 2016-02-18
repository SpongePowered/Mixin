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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
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
    
    /**
     * Set the wrap width (default 80 columns)
     * 
     * @param wrapWidth new width (in characters) to wrap to
     * @return fluent interface
     */
    public PrettyPrinter wrapTo(int wrapWidth) {
        this.wrapWidth = wrapWidth;
        return this;
    }
    
    /**
     * Get the current wrap width
     * 
     * @return the current wrap width
     */
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
     * Adds a string line to the output
     * 
     * @param string format string
     * @return fluent interface
     */
    public PrettyPrinter add(String string) {
        this.lines.add(string);
        this.width = Math.max(this.width, string.length());
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
    
    /**
     * Add elements of the array to the output, one per line, with array indices
     * 
     * @param array Array of objects to print
     * @return fluent interface
     */
    public PrettyPrinter addIndexed(Object[] array) {
        int indexWidth = String.valueOf(array.length - 1).length();
        String format = "[%" + indexWidth + "d] %s"; 
        for (int index = 0; index < array.length; index++) {
            this.add(format, index, array[index]);
        }
        
        return this;
    }
    
    /**
     * Add elements of the collection to the output, one per line, with indices
     * 
     * @param c Collection of objects to print
     * @return fluent interface
     */
    public PrettyPrinter addWithIndices(Collection<?> c) {
        return this.addIndexed(c.toArray(new Object[0]));
    }
    
    /**
     * Adds a pretty-printable object to the output, the object is responsible
     * for adding its own representation to this printer
     * 
     * @param printable object to add
     * @return fluent interface
     */
    public PrettyPrinter add(IPrettyPrintable printable) {
        if (printable != null) {
            printable.print(this);
        }
        return this;
    }
    
    /**
     * Print a formatted representation of the specified throwable with the
     * default indent (4)
     * 
     * @param th Throwable to print
     * @return fluent interface
     */
    public PrettyPrinter add(Throwable th) {
        return this.add(th, 4);
    }
    
    /**
     * Print a formatted representation of the specified throwable with the
     * specified indent
     * 
     * @param th Throwable to print
     * @param indent Indent size for stacktrace lines
     * @return fluent interface
     */
    public PrettyPrinter add(Throwable th, int indent) {
        String margin = Strings.repeat(" ", indent);
        this.add("%s: %s", th.getClass().getName(), th.getMessage());
        for (StackTraceElement st : th.getStackTrace()) {
            this.add("%s%s", margin, st);
        }
        return this;
    }
    
    /**
     * Adds the specified object to the output
     * 
     * @param object object to add
     * @return fluent interface
     */
    public PrettyPrinter add(Object object) {
        return this.add(object, 0);
    }
    
    /**
     * Adds the specified object to the output
     * 
     * @param object object to add
     * @param indent indent amount
     * @return fluent interface
     */
    public PrettyPrinter add(Object object, int indent) {
        String margin = Strings.repeat(" ", indent);
        return this.append(object, indent, margin);
    }
    
    private PrettyPrinter append(Object object, int indent, String margin) {
        if (object instanceof String) {
            return this.add("%s%s", margin, object);
        } else if (object instanceof Iterable) {
            for (Object entry : (Iterable<?>)object) {
                this.append(entry, indent, margin);
            }
            return this;
        } else if (object instanceof Map) {
            this.kvWidth(indent);
            return this.add((Map<?, ?>)object);
        } else if (object instanceof IPrettyPrintable) {
            return this.add((IPrettyPrintable)object);
        } else if (object instanceof Throwable) {
            return this.add((Throwable)object, indent);
        } else if (object.getClass().isArray()) {
            return this.add((Object[])object, indent + "%s");
        }
        return this.add("%s%s", margin, object);
    }
    
    /**
     * Adds a formatted line to the output, and attempts to wrap the line
     * content to the current wrap width
     *
     * @param format format string
     * @param args arguments
     * 
     * @return fluent interface
     */
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
     * Add all values of the specified map to this printer as key/value pairs
     * 
     * @param map Map with entries to add
     * @return fluent
     */
    public PrettyPrinter add(Map<?, ?> map) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey() == null ? "null" : entry.getKey().toString();
            this.kv(key, entry.getValue());
        }
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
     * Outputs this printer to stderr and to a logger decorated with the calling
     * class name with level {@link Level#DEBUG}
     * 
     * @return fluent interface
     */
    public PrettyPrinter trace() {
        return this.trace(PrettyPrinter.getDefaultLoggerName());
    }

    /**
     * Outputs this printer to stderr and to a logger decorated with the calling
     * class name at the specified level
     * 
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(Level level) {
        return this.trace(PrettyPrinter.getDefaultLoggerName(), level);
    }

    /**
     * Outputs this printer to stderr and to a logger decorated with specified
     * name with level {@link Level#DEBUG}
     * 
     * @param logger Logger name to write to
     * @return fluent interface
     */
    public PrettyPrinter trace(String logger) {
        return this.trace(System.err, LogManager.getLogger(logger));
    }

    /**
     * Outputs this printer to stderr and to a logger decorated with specified
     * name with the specified level
     * 
     * @param logger Logger name to write to
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(String logger, Level level) {
        return this.trace(System.err, LogManager.getLogger(logger), level);
    }

    /**
     * Outputs this printer to stderr and to the supplied logger with level
     * {@link Level#DEBUG}
     * 
     * @param logger Logger to write to
     * @return fluent interface
     */
    public PrettyPrinter trace(Logger logger) {
        return this.trace(System.err, logger);
    }
    
    /**
     * Outputs this printer to stderr and to the supplied logger with the
     * specified level
     * 
     * @param logger Logger to write to
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(Logger logger, Level level) {
        return this.trace(System.err, logger, level);
    }
    
    /**
     * Outputs this printer to the specified stream and to a logger decorated
     * with the calling class name with level {@link Level#DEBUG}
     * 
     * @param stream Output stream to print to
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream) {
        return this.trace(stream, PrettyPrinter.getDefaultLoggerName());
    }

    /**
     * Outputs this printer to the specified stream and to a logger decorated
     * with the calling class name with the specified level
     * 
     * @param stream Output stream to print to
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream, Level level) {
        return this.trace(stream, PrettyPrinter.getDefaultLoggerName(), level);
    }
    
    /**
     * Outputs this printer to the specified stream and to a logger with the
     * specified name with level {@link Level#DEBUG}
     * 
     * @param stream Output stream to print to
     * @param logger Logger name to write to
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream, String logger) {
        return this.trace(stream, LogManager.getLogger(logger));
    }
    
    /**
     * Outputs this printer to the specified stream and to a logger with the
     * specified name at the specified level
     * 
     * @param stream Output stream to print to
     * @param logger Logger name to write to
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream, String logger, Level level) {
        return this.trace(stream, LogManager.getLogger(logger), level);
    }
    
    /**
     * Outputs this printer to the specified stream and to the supplied logger
     * with level {@link Level#DEBUG}
     * 
     * @param stream Output stream to print to
     * @param logger Logger to write to
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream, Logger logger) {
        return this.trace(stream, logger, Level.DEBUG);
    }
    
    /**
     * Outputs this printer to the specified stream and to the supplied logger
     * with at the specified level
     * 
     * @param stream Output stream to print to
     * @param logger Logger to write to
     * @param level Log level to write messages
     * @return fluent interface
     */
    public PrettyPrinter trace(PrintStream stream, Logger logger, Level level) {
        this.log(logger, level);
        this.print(stream);
        return this;
    }
    
    /**
     * Print this printer to stderr
     * 
     * @return fluent interface
     */
    public PrettyPrinter print() {
        return this.print(System.err);
    }
    
    /**
     * Print this printer to the specified output
     * 
     * @param stream stream to print to
     * @return fluent interface
     */
    public PrettyPrinter print(PrintStream stream) {
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
        return this;
    }

    private void printSpecial(PrintStream stream, ISpecialEntry line) {
        stream.printf("/*%s*/\n", line.toString());
    }

    private void printString(PrintStream stream, String string) {
        stream.printf("/* %-" + this.width + "s */\n", string);
    }

    /**
     * Write this printer to the specified logger at {@link Level#INFO}
     * 
     * @param logger logger to log to
     * @return fluent interface
     */
    public PrettyPrinter log(Logger logger) {
        return this.log(logger, Level.INFO);
    }
    
    /**
     * Write this printer to the specified logger
     * 
     * @param logger logger to log to
     * @param level log level
     * @return fluent interface
     */
    public PrettyPrinter log(Logger logger, Level level) {
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
        return this;
    }

    private void logSpecial(Logger logger, Level level, ISpecialEntry line) {
        logger.log(level, "/*{}*/", line.toString());
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
    
    private static String getDefaultLoggerName() {
        String name = new Throwable().getStackTrace()[2].getClassName();
        int pos = name.lastIndexOf('.');
        return pos == -1 ? name : name.substring(pos + 1);
    }

    /**
     * Convenience method, alternative to using <tt>Thread.dumpStack</tt> which
     * prints to stderr in pretty-printed format.
     */
    public static void dumpStack() {
        new PrettyPrinter().add(new Exception("Stack trace")).print(System.err);
    }

    /**
     * Convenience methods, pretty-prints the specified throwable to stderr
     * 
     * @param th Throwable to log
     */
    public static void print(Throwable th) {
        new PrettyPrinter().add(th).print(System.err);
    }
    
}
