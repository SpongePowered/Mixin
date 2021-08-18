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

/**
 * Abstract base adapter which contains convenience methods for formatting
 * log4j2-style messages and also routes all level-specific overloads to calls
 * to <tt>log</tt>, which can simplify some implementations.
 */
public abstract class LoggerAdapterAbstract implements ILogger {

    /**
     * Logger id
     */
    private final String id;

    /**
     * @param id Logger id
     */
    protected LoggerAdapterAbstract(String id) {
        this.id = id;
    }
    
    /**
     * Get the id of this logger
     */
    @Override
    public String getId() {
        return this.id;
    }

    /**
     * This is a very naive implementation of log4j2's
     * ParameterizedMessage::format method which is less efficient and less
     * defensive because it doesn't need to handle all the cases that the log4j2
     * formatter does. All we're really doing here is substituting in the values
     * for <tt>{}</tt> placeholders because I know that in mixin there aren't
     * any cases where we need to handle anything else, such as escaped <tt>{
     * </tt> characters or whatever.
     * 
     * @param message Message patterm
     * @param params Message parameters
     * @return Message with {} placeholders filled in
     */
    protected static String getFormattedMessage(String message, Object... params) {
        if (params.length == 0) {
            return message;
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        for (int param = 0; pos < message.length() && param < params.length; param++) {
            int delimPos = message.indexOf("{}", pos);
            if (delimPos < 0) {
                return sb.append(message.substring(pos)).toString();
            }
            sb.append(message.substring(pos, delimPos)).append(params[param]);
            pos = delimPos + 2;
        }
        if (pos < message.length()) {
            sb.append(message.substring(pos));
        }
        return sb.toString();
    }

    @Override
    public void catching(Throwable t) {
        this.catching(Level.WARN, t);
    }

    @Override
    public void debug(String message, Object... params) {
        this.log(Level.DEBUG, message, params);
    }

    @Override
    public void debug(String message, Throwable t) {
        this.log(Level.DEBUG, message, t);
    }

    @Override
    public void error(String message, Object... params) {
        this.log(Level.ERROR, message, params);
    }

    @Override
    public void error(String message, Throwable t) {
        this.log(Level.ERROR, message, t);
    }

    @Override
    public void fatal(String message, Object... params) {
        this.log(Level.FATAL, message, params);
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.log(Level.FATAL, message, t);
    }

    @Override
    public void info(String message, Object... params) {
        this.log(Level.INFO, message, params);
    }

    @Override
    public void info(String message, Throwable t) {
        this.log(Level.INFO, message, t);
    }

    @Override
    public void trace(String message, Object... params) {
        this.log(Level.TRACE, message, params);
    }

    @Override
    public void trace(String message, Throwable t) {
        this.log(Level.TRACE, message, t);
    }

    @Override
    public void warn(String message, Object... params) {
        this.log(Level.WARN, message, params);
    }

    @Override
    public void warn(String message, Throwable t) {
        this.log(Level.WARN, message, t);
    }

}
