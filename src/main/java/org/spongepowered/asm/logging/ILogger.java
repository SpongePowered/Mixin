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
 * Interface extracted from Log4j2's Logger (<tt>org.apache.logging.log4j.Logger
 * </tt>) with only the main methods used by Mixin included. This is to
 * facilitate the delegation of logging to mixin services, and to sever the
 * dependency on log4j2 from Mixin's core.
 */
public interface ILogger {

    /**
     * Get the id of this logger
     */
    public abstract String getId();

    /**
     * Get a short human-readable name of this logger type
     */
    public abstract String getType();

    /**
     * Logs an exception or error that has been caught.
     * 
     * @param level The logging Level.
     * @param t The Throwable.
     */
    public abstract void catching(Level level, Throwable t);

    /**
     * Logs an exception or error that has been caught.
     * @param t The Throwable.
     */
    public abstract void catching(Throwable t);

    /**
     * Logs a message with parameters at the {@link Level#DEBUG DEBUG} level.
     * 
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void debug(String message, Object... params);

    /**
     * Logs a message at the {@link Level#DEBUG DEBUG} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void debug(String message, Throwable t);

     /**
     * Logs a message with parameters at the {@link Level#ERROR ERROR} level
     *
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void error(String message, Object... params);

    /**
     * Logs a message at the {@link Level#ERROR ERROR} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void error(String message, Throwable t);

    /**
     * Logs a message with parameters at the {@link Level#FATAL FATAL} level.
     *
     * @param message the message to log
     * @param params parameters to the message
     */
    void fatal(String message, Object... params);

    /**
     * Logs a message at the {@link Level#FATAL FATAL} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    void fatal(String message, Throwable t);

    /**
     * Logs a message with parameters at the {@link Level#INFO INFO} level.
     *
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void info(String message, Object... params);

    /**
     * Logs a message at the {@link Level#INFO INFO} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void info(String message, Throwable t);

    /**
     * Logs a message with parameters at the given level.
     *
     * @param level the logging level
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void log(Level level, String message, Object... params);

    /**
     * Logs a message at the given level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param level the logging level
     * @param message the message to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void log(Level level, String message, Throwable t);

    /**
     * Logs an exception or error to be thrown.
     *
     * @param <T> the Throwable type
     * @param t The Throwable
     * @return the Throwable
     */
    public <T extends Throwable> T throwing(T t);

    /**
     * Logs a message with parameters at the {@link Level#TRACE TRACE} level.
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void trace(String message, Object... params);

    /**
     * Logs a message at the {@link Level#TRACE TRACE} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void trace(String message, Throwable t);

    /**
     * Logs a message with parameters at the {@link Level#WARN WARN} level.
     * 
     * @param message the message to log
     * @param params parameters to the message
     */
    public abstract void warn(String message, Object... params);

    /**
     * Logs a message at the {@link Level#WARN WARN} level including the
     * stack trace of the {@link Throwable} <code>t</code> passed as parameter.
     *
     * @param message the message object to log
     * @param t the exception to log, including its stack trace
     */
    public abstract void warn(String message, Throwable t);

}
