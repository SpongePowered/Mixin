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
package org.spongepowered.asm.service.modlauncher;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.logging.Level;
import org.spongepowered.asm.logging.LoggerAdapterAbstract;

public class LoggerAdapterLog4j2 extends LoggerAdapterAbstract {
    
    private static final org.apache.logging.log4j.Level[] LEVELS = {
        /* FATAL = */ org.apache.logging.log4j.Level.FATAL,
        /* ERROR = */ org.apache.logging.log4j.Level.ERROR,
        /* WARN =  */ org.apache.logging.log4j.Level.WARN,
        /* INFO =  */ org.apache.logging.log4j.Level.INFO,
        /* DEBUG = */ org.apache.logging.log4j.Level.DEBUG,
        /* TRACE = */ org.apache.logging.log4j.Level.TRACE
    };
    
    private final Logger logger;

    public LoggerAdapterLog4j2(String name) {
        super(name);
        this.logger = LogManager.getLogger(name);
    }
    
    @Override
    public String getType() {
        return "Log4j2 (via ModLauncher)";
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.logger.catching(LoggerAdapterLog4j2.LEVELS[level.ordinal()], t);
    }

    @Override
    public void catching(Throwable t) {
        this.logger.catching(t);
    }

    @Override
    public void debug(String message, Object... params) {
        this.logger.debug(message, params);
    }

    @Override
    public void debug(String message, Throwable t) {
        this.logger.debug(message, t);
    }

    @Override
    public void error(String message, Object... params) {
        this.logger.error(message, params);
    }

    @Override
    public void error(String message, Throwable t) {
        this.logger.error(message, t);
    }

    @Override
    public void fatal(String message, Object... params) {
        this.logger.fatal(message, params);
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.logger.fatal(message, t);
    }

    @Override
    public void info(String message, Object... params) {
        this.logger.info(message, params);
    }

    @Override
    public void info(String message, Throwable t) {
        this.logger.info(message, t);
    }

    @Override
    public void log(Level level, String message, Object... params) {
        this.logger.log(LoggerAdapterLog4j2.LEVELS[level.ordinal()], message, params);
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        this.logger.log(LoggerAdapterLog4j2.LEVELS[level.ordinal()], message, t);
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        return this.logger.throwing(t);
    }

    @Override
    public void trace(String message, Object... params) {
        this.logger.trace(message, params);
    }

    @Override
    public void trace(String message, Throwable t) {
        this.logger.trace(message, t);
    }

    @Override
    public void warn(String message, Object... params) {
        this.logger.warn(message, params);
    }

    @Override
    public void warn(String message, Throwable t) {
        this.logger.warn(message, t);
    }

}
