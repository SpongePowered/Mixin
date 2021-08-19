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

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Logger adapter which uses the built-in Java logging functionality to emit
 * logging messages.
 */
public class LoggerAdapterJava extends LoggerAdapterAbstract {
    
    private static final java.util.logging.Level[] LEVELS = {
        /* FATAL = */ java.util.logging.Level.SEVERE,
        /* ERROR = */ java.util.logging.Level.SEVERE,
        /* WARN =  */ java.util.logging.Level.WARNING,
        /* INFO =  */ java.util.logging.Level.INFO,
        /* DEBUG = */ java.util.logging.Level.FINE,
        /* TRACE = */ java.util.logging.Level.FINER
    };

    private final Logger logger;

    public LoggerAdapterJava(String name) {
        super(name);
        this.logger = LoggerAdapterJava.getLogger(name);
    }
    
    @Override
    public String getType() {
        return "java.util.logging Log Adapter";
    }

    @Override
    public void catching(Level level, Throwable t) {
        this.warn("Catching {}: {}", t.getClass().getName(), t.getMessage(), t);
    }

    @Override
    public void debug(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.fine(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.fine(formatted.getThrowable().toString());
        }
    }

    @Override
    public void debug(String message, Throwable t) {
        this.logger.fine(message);
        this.logger.fine(t.toString());
    }

    @Override
    public void error(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void error(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void fatal(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.severe(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.severe(formatted.getThrowable().toString());
        }
    }

    @Override
    public void fatal(String message, Throwable t) {
        this.logger.severe(message);
        this.logger.severe(t.toString());
    }

    @Override
    public void info(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.info(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.info(formatted.getThrowable().toString());
        }
    }

    @Override
    public void info(String message, Throwable t) {
        this.logger.info(message);
        this.logger.info(t.toString());
    }

    @Override
    public void log(Level level, String message, Object... params) {
        java.util.logging.Level logLevel = LoggerAdapterJava.LEVELS[level.ordinal()];
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.log(logLevel, formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.log(LoggerAdapterJava.LEVELS[level.ordinal()], formatted.getThrowable().toString());
        }
    }

    @Override
    public void log(Level level, String message, Throwable t) {
        java.util.logging.Level logLevel = LoggerAdapterJava.LEVELS[level.ordinal()];
        this.logger.log(logLevel, message);
        this.logger.log(logLevel, t.toString());
    }

    @Override
    public <T extends Throwable> T throwing(T t) {
        this.warn("Throwing {}: {}", t.getClass().getName(), t.getMessage(), t);
        return t;
    }

    @Override
    public void trace(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.finer(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.finer(formatted.getThrowable().toString());
        }
    }

    @Override
    public void trace(String message, Throwable t) {
        this.logger.finer(message);
        this.logger.finer(t.toString());
    }

    @Override
    public void warn(String message, Object... params) {
        FormattedMessage formatted = new FormattedMessage(message, params);
        this.logger.warning(formatted.getMessage());
        if (formatted.hasThrowable()) {
            this.logger.warning(formatted.getThrowable().toString());
        }
    }

    @Override
    public void warn(String message, Throwable t) {
        this.logger.warning(message);
        this.logger.warning(t.toString());
    }

    private static Logger getLogger(String name) {
        LogManager logManager = LogManager.getLogManager();
        Logger logger = logManager.getLogger(name);
        if (logger != null) {
            return logger;
        }
        return LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

}
