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
package org.spongepowered.asm.mixin.extensibility;

public interface IActivityContext {
    
    public interface IActivity {

        /**
         * End this activity (and any descendants) and begin the next activity
         * using the same activity handle
         * 
         * @param descriptionFormat New activity description format
         * @param args New activity description args
         */
        void next(String descriptionFormat, Object... args);

        /**
         * End this activity (and any descendants) and begin the next activity
         * using the same activity handle
         * 
         * @param description New activity description
         */
        void next(String description);

        /**
         * End this activity and remove it (and any descendants)
         */
        void end();

        /**
         * Append text to the activity description
         * 
         * @param textFormat Format for text to append
         * @param args Format args
         */
        void append(String textFormat, Object...args);

        /**
         * Append text to the activity description
         * 
         * @param text Text to append
         */
        void append(String text);
        
    }

    /**
     * Convert this activity stack to a string representation using the
     * specified glue string
     * 
     * @param glue glue string
     * @return string representation of this activity stack
     */
    public abstract String toString(String glue);

    /**
     * Begin a new activity (push it onto this activity stack)
     * 
     * @param descriptionFormat Activity description format
     * @param args format args
     * @return new activity handle
     */
    public abstract IActivity begin(String descriptionFormat, Object... args);

    /**
     * Begin a new activity (push it onto this activity stack)
     * 
     * @param description Activity description
     * @return new activity handle
     */
    public abstract IActivity begin(String description);

    /**
     * Clear the activity stack
     */
    public abstract void clear();

}
