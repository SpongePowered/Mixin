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
package org.spongepowered.asm.mixin.transformer;

import org.spongepowered.asm.mixin.extensibility.IActivityContext;

/**
 * Tracker for processors which want to express their activity stack on crash in
 * a user-readable way.
 */
public class ActivityStack implements IActivityContext {
    
    /**
     * An activity node in the activity stack (yes it's actually a doubly-linked
     * list).
     */
    public class Activity implements IActivity {
        
        /**
         * Description of this activity
         */
        public String description;
        
        Activity last, next;
        
        Activity(Activity last, String description) {
            if (last != null) {
                last.next = this;
            }
            this.last = last;
            this.description = description;
        }
        
        /**
         * Append text to the activity description
         * 
         * @param text Text to append
         */
        @Override
        public void append(String text) {
            this.description = this.description != null ? this.description + text : text;
        }
        
        /**
         * Append text to the activity description
         * 
         * @param textFormat Format for text to append
         * @param args Format args
         */
        @Override
        public void append(String textFormat, Object...args) {
            this.append(String.format(textFormat, args));
        }
        
        /**
         * End this activity and remove it (and any descendants)
         */
        @Override
        public void end() {
            // Cannot end head or ended activity
            if (this.last != null) {
                ActivityStack.this.end(this);
                this.last = null;
            }
        }
        
        /**
         * End this activity (and any descendants) and begin the next activity
         * using the same activity handle
         * 
         * @param description New activity description
         */
        @Override
        public void next(String description) {
            if (this.next != null) {
                this.next.end();
            }
            this.description = description;
        }
        
        /**
         * End this activity (and any descendants) and begin the next activity
         * using the same activity handle
         * 
         * @param descriptionFormat New activity description format
         * @param args New activity description args
         */
        @Override
        public void next(String descriptionFormat, Object... args) {
            if (descriptionFormat == null) {
                descriptionFormat = "null";
            }
            this.next(String.format(descriptionFormat, args));
        }
        
    }

    public static final String GLUE_STRING = " -> ";

    private final Activity head;
    private Activity tail;
    
    private String glue;
    
    public ActivityStack() {
        this(null, ActivityStack.GLUE_STRING);
    }
    
    public ActivityStack(String root) {
        this(root, ActivityStack.GLUE_STRING);
    }
    
    public ActivityStack(String root, String glue) {
        this.head = this.tail = new Activity(null, root);
        this.glue = glue;
    }
    
    /**
     * Clear the activity stack
     */
    @Override
    public void clear() {
        this.tail = this.head;
        this.head.next = null;
    }
    
    /**
     * Begin a new activity (push it onto this activity stack)
     * 
     * @param description Activity description
     * @return new activity handle
     */
    @Override
    public IActivity begin(String description) {
        return this.tail = new Activity(this.tail, description != null ? description : "null");
    }
    
    /**
     * Begin a new activity (push it onto this activity stack)
     * 
     * @param descriptionFormat Activity description format
     * @param args format args
     * @return new activity handle
     */
    @Override
    public IActivity begin(String descriptionFormat, Object... args) {
        if (descriptionFormat == null) {
            descriptionFormat = "null";
        }
        return this.tail = new Activity(this.tail, String.format(descriptionFormat, args));
    }

    void end(Activity activity) {
        this.tail = activity.last;
        this.tail.next = null;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return this.toString(this.glue);
    }

    /**
     * Convert this activity stack to a string representation using the
     * specified glue string
     * 
     * @param glue glue string
     * @return string representation of this activity stack
     */
    @Override
    public String toString(String glue) {
        if (this.head.description == null && this.head.next == null) {
            return "Unknown";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Activity activity = this.head; activity != null; activity = activity.next) {
            if (activity.description != null) {
                sb.append(activity.description);
                if (activity.next != null) {
                    sb.append(glue);
                }
            }
        }
        return sb.toString();
    }

}
