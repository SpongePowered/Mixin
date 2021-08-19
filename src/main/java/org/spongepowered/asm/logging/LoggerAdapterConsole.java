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
package org.spongepowered.asm.logging;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.common.base.Strings;

/**
 * A very basic logger adapter which does not log anything to file and simply
 * emits formatted log messages to the console printstreams
 */
public class LoggerAdapterConsole extends LoggerAdapterAbstract {
    
    /**
     * Date format for console messages
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss");
    
    /**
     * Printstream for DEBUG-level messages, null by default
     */
    private PrintStream debug;

    /**
     * @param name Logger name
     */
    public LoggerAdapterConsole(String name) {
        super(Strings.nullToEmpty(name));
    }
    
    @Override
    public String getType() {
        return "Default Console Logger";
    }
    
    /**
     * Set output stream for DEBUG-level messages
     * 
     * @param debug New PrintStream for debug messages
     * @return fluent
     */
    public LoggerAdapterConsole setDebugStream(PrintStream debug) {
        this.debug = debug;
        return this;
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.log(Level.WARN, "Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

    @Override
    public void log(Level level, String message, Object... params) {
        PrintStream out = this.getOutputStream(level);
        if (out != null) {
            FormattedMessage formatted = new FormattedMessage(message, params);
            out.println(String.format("[%s] [%s/%s] %s", LoggerAdapterConsole.DATE_FORMAT.format(new Date()), this.getId(), level, formatted));
            if (formatted.hasThrowable()) {
                formatted.getThrowable().printStackTrace(out);
            }
        }
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        PrintStream out = this.getOutputStream(level);
        if (out != null) {
            out.println(String.format("[%s] [%s/%s] %s", LoggerAdapterConsole.DATE_FORMAT.format(new Date()), this.getId(), level, message));
            t.printStackTrace(out);
        }
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        this.log(Level.WARN, "Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
        return t;
    }

    private PrintStream getOutputStream(Level level) {
        if (level == Level.FATAL || level == Level.ERROR || level == Level.WARN) {
            return System.err;
        } else if (level == Level.INFO) {
            return System.out;
        } else if (level == Level.DEBUG) {
            return this.debug;
        }
        return null;
    }

}
