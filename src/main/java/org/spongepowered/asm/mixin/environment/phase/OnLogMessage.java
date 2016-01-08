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
package org.spongepowered.asm.mixin.environment.phase;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;

public class OnLogMessage extends AbstractPhaseTransition {
    
    static final class ObserverAppender extends AbstractAppender {
        
        private static final Map<String, ObserverAppender> appenders = new HashMap<String, ObserverAppender>();
        
        private final List<OnLogMessage> transitions = new ArrayList<OnLogMessage>();
        private final Logger log;

        private ObserverAppender(String name) {
            super("ObserverAppender", null, null);
            this.log = (Logger)LogManager.getLogger(name);;
            ObserverAppender.appenders.put(name, this);
        }

        @Override
        public void append(LogEvent event) {
            for (OnLogMessage transition : this.transitions) {
                transition.onLogEvent(event);
            }
        }
        
        void addTransition(OnLogMessage transition) {
            if (this.transitions.size() == 0) {
                this.start();
                this.log.addAppender(this);
            }
            this.transitions.add(transition);
        }
        
        void removeTransition(OnLogMessage transition) {
            this.transitions.remove(transition);
            if (this.transitions.size() == 0) {
                this.log.removeAppender(this);
                this.stop();
            }
        }
        
        static ObserverAppender forLogger(String name) {
            ObserverAppender appender = ObserverAppender.appenders.get(name);
            if (appender == null) {
                appender = new ObserverAppender(name);
            }
            return appender;
        }
    }
    
    protected final ObserverAppender appender;
    
    protected final Level level;
    
    protected final String format;

    public OnLogMessage(String loggerName, String format) {
        this(loggerName, format, null);
    }
    
    public OnLogMessage(String loggerName, String format, Level level) {
        this.appender = ObserverAppender.forLogger(checkNotNull(loggerName, "loggerName cannot be null"));
        this.level = level;
        this.format = checkNotNull(format, "format cannot be null");
        this.appender.addTransition(this);
    }

    public void onLogEvent(LogEvent event) {
        if ((this.level == null || this.level == event.getLevel()) && this.format.equals(event.getMessage().getFormat())) {
            this.phase.begin();
            this.appender.removeTransition(this);
        }
    }
    
}