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
package org.spongepowered.asm.util.perf;

import java.text.DecimalFormat;
import java.util.*;

import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.PrettyPrinter.Alignment;

import com.google.common.base.Joiner;

/**
 * Performance profiler for Mixin.
 */
public final class Profiler {
    
    /**
     * Flag to indicate a root section. Root sections are always recorded at the
     * root wherever they occur, but may appear under other sections in order to
     * show the time share of the root section relative to the parent. 
     */
    public static final int ROOT = 0x01;
    
    /**
     * Flag to indicate a fine section. Fine sections are always recorded, but
     * are only displayed in the printed output if the includeFine flag is set.
     */
    public static final int FINE = 0x02;
    
    /**
     * Profiler section. Normal sections do nothing so that the profiler itself
     * consumes minimal resources when disabled.
     */
    public class Section {
        
        static final String SEPARATOR_ROOT = " -> ";
        
        static final String SEPARATOR_CHILD = ".";

        /**
         * Section name
         */
        private final String name;
        
        /**
         * True if this is a ROOT section
         */
        private boolean root;

        /**
         * True if this is a FINE section
         */
        private boolean fine;
        
        /**
         * True if this section has been invalidated by a call to Profiler#clear
         */
        protected boolean invalidated;
        
        /**
         * Auxilliary info for this section, used for context
         */
        private String info;
        
        Section(String name) {
            this.name = name;
            this.info = name;
        }
        
        /**
         * Get the delegate (root section) for this section
         */
        Section getDelegate() {
            return this;
        }
        
        Section invalidate() {
            this.invalidated = true;
            return this;
        }
        
        /**
         * Mark this section as ROOT
         * 
         * @return fluent
         */
        Section setRoot(boolean root) {
            this.root = root;
            return this;
        }
        
        /**
         * Get whether this is a root section
         */
        public boolean isRoot() {
            return this.root;
        }
        
        /**
         * Set this section as FINE
         * 
         * @return fluent
         */
        Section setFine(boolean fine) {
            this.fine = fine;
            return this;
        }
        
        /**
         * Get whether this section is FINE
         */
        public boolean isFine() {
            return this.fine;
        }

        /**
         * Get the section name
         */
        public String getName() {
            return this.name;
        }
        
        /**
         * Get the base name for this section, for delegated sections this is
         * the name of the parent section, minus the root
         */
        public String getBaseName() {
            return this.name;
        }
        
        /**
         * Set the auxilliary info for this section
         * 
         * @param info aux info
         */
        public void setInfo(String info) {
            this.info = info;
        }
        
        /**
         * Get the auxilliary info for this section
         */
        public String getInfo() {
            return this.info;
        }

        /**
         * Start timing on this section
         * 
         * @return fluent
         */
        Section start() {
            return this;
        }
        
        /**
         * Stop timing of this section
         * 
         * @return fluent
         */
        protected Section stop() {
            return this;
        }
        
        /**
         * Stop timing of this section and end it (pop from profiler stack)
         * 
         * @return fluent
         */
        public Section end() {
            if (!this.invalidated) {
                Profiler.this.end(this);
            }
            return this;
        }
        
        /**
         * Stop timing of this section and start a new section at the same level
         * 
         * @param name name of the next section
         * @return new section
         */
        public Section next(String name) {
            this.end();
            return Profiler.this.begin(name);
        }

        /**
         * Mark off a profiling slice in this section. Each timing slice is used
         * to benchmark a different phase of operations. Calling this method
         * pushes the current time into the times array and resets the time and
         * counter to zero
         */
        void mark() {
        }

        /**
         * Get the current time in milliseconds in the current phase 
         */
        public long getTime() {
            return 0;
        }

        /**
         * Get the current time in milliseconds in all phases
         */
        public long getTotalTime() {
            return 0;
        }

        /**
         * Get the current time in seconds in the current phase 
         */
        public double getSeconds() {
            return 0.0D;
        }

        /**
         * Get the current time in seconds in all phases
         */
        public double getTotalSeconds() {
            return 0.0D;
        }

        /**
         * Get all available time slices including the current one in
         * milliseconds
         */
        public long[] getTimes() {
            return new long[1];
        }
        
        /**
         * Get the number of total time periods recorded in the current slice
         */
        public int getCount() {
            return 0;
        }
        
        /**
         * Get the number of total time periods recorded in the all slices
         */
        public int getTotalCount() {
            return 0;
        }
        
        /**
         * Get the average time in milliseconds of each time period recorded in
         * the current slice
         */
        public double getAverageTime() {
            return 0.0D;
        }
        
        /**
         * Get the average time in milliseconds of each time period recorded in
         * the all slices
         */
        public double getTotalAverageTime() {
            return 0.0D;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public final String toString() {
            return this.name;
        }
        
    }
    
    /**
     * Live profiler section. Actually records timings for when the profiler is
     * active.
     */
    class LiveSection extends Section {

        /**
         * Cursor points at the current active time slice. The current time
         * slice is not in the array of times.
         */
        private int cursor = 0;
        
        /**
         * Historical time slicess (times recorded by calls to {@link #mark}) 
         */
        private long[] times = new long[0];
        
        /**
         * Start time. Zero when not recording a time slice
         */
        private long start = 0L;
        
        /**
         * Current accumulated time and time in all previous slices
         */
        private long time, markedTime;
        
        /**
         * Current period count and period count in all previous slices 
         */
        private int count, markedCount;
        
        LiveSection(String name, int cursor) {
            super(name);
            this.cursor = cursor;
        }
        
        @Override
        Section start() {
            this.start = System.currentTimeMillis();
            return this;
        }

        @Override
        protected Section stop() {
            if (this.start > 0L) {
                this.time += System.currentTimeMillis() - this.start;
            }
            this.start = 0L;
            this.count++;
            return this;
        }
        
        @Override
        public Section end() {
            this.stop();
            if (!this.invalidated) {
                Profiler.this.end(this);
            }
            return this;
        }
        
        @Override
        void mark() {
            if (this.cursor >= this.times.length) {
                this.times = Arrays.copyOf(this.times, this.cursor + 4);
            }
            this.times[this.cursor] = this.time;
            this.markedTime += this.time;
            this.markedCount += this.count;
            this.time = 0;
            this.count = 0;
            this.cursor++;
        }
        
        @Override
        public long getTime() {
            return this.time;
        }
        
        @Override
        public long getTotalTime() {
            return this.time + this.markedTime;
        }
        
        @Override
        public double getSeconds() {
            return this.time * 0.001D;
        }
        
        @Override
        public double getTotalSeconds() {
            return (this.time + this.markedTime) * 0.001D;
        }
        
        @Override
        public long[] getTimes() {
            long[] times = new long[this.cursor + 1];
            System.arraycopy(this.times, 0, times, 0, Math.min(this.times.length, this.cursor));
            times[this.cursor] = this.time;
            return times;
        }
        
        @Override
        public int getCount() {
            return this.count;
        }
        
        @Override
        public int getTotalCount() {
            return this.count + this.markedCount;
        }
        
        @Override
        public double getAverageTime() {
            return this.count > 0 ? (double)(this.time) / this.count : 0.0D;
        }
        
        @Override
        public double getTotalAverageTime() {
            return this.count > 0 ? (double)(this.time + this.markedTime) / (this.count + this.markedCount) : 0.0D;
        }

    }
    
    /**
     * Sub-profiler-section, acts as a delegate to a root section when recording
     * a root section under another parent. Calls to methods in this section are
     * handled locally and in the delegated root.
     */
    class SubSection extends LiveSection {

        /**
         * Section base name 
         */
        private final String baseName;
        
        /**
         * Delegated root
         */
        private final Section root;

        SubSection(String name, int cursor, String baseName, Section root) {
            super(name, cursor);
            this.baseName = baseName;
            this.root = root;
        }
        
        @Override
        Section invalidate() {
            this.root.invalidate();
            return super.invalidate();
        }
        
        @Override
        public String getBaseName() {
            return this.baseName;
        }
        
        @Override
        public void setInfo(String info) {
            this.root.setInfo(info);
            super.setInfo(info);
        }
        
        @Override
        Section getDelegate() {
            return this.root;
        }
        
        @Override
        Section start() {
            this.root.start();
            return super.start();
        }
        
        @Override
        public Section end() {
            this.root.stop();
            return super.end();
        }
        
        @Override
        public Section next(String name) {
            super.stop();
            return this.root.next(name);
        }
        
    }
    
    /**
     * All profiler sections
     */
    private final Map<String, Section> sections = new TreeMap<String, Profiler.Section>();
    
    /**
     * Profiler phases, recorded by calling {@link #mark}
     */
    private final List<String> phases = new ArrayList<String>();
    
    /**
     * Profiler section stack
     */
    private final Deque<Section> stack = new LinkedList<Section>();
    
    /**
     * True when profiler is active
     */
    private boolean active;
    
    public Profiler() {
        this.phases.add("Initial");
    }

    /**
     * Set the active state of the profiler. When activating the profiler is
     * always reset.
     * 
     * @param active new active state
     */
    public void setActive(boolean active) {
        if ((!this.active && active) || !active) {
            this.reset();
        }
        this.active = active;
    }
    
    /**
     * Reset all profiler state
     */
    public void reset() {
        for (Section section : this.sections.values()) {
            section.invalidate();
        }
        
        this.sections.clear();
        this.phases.clear();
        this.phases.add("Initial");
        this.stack.clear();
    }

    /**
     * Get the specified profiler section
     * 
     * @param name section name
     * @return profiler section
     */
    public Section get(String name) {
        Section section = this.sections.get(name);
        if (section == null) {
            section = this.active ? new LiveSection(name, this.phases.size() - 1) : new Section(name);
            this.sections.put(name, section);
        }
        
        return section;
    }
    
    private Section getSubSection(String name, String baseName, Section root) {
        Section section = this.sections.get(name);
        if (section == null) {
            section = new SubSection(name, this.phases.size() - 1, baseName, root);
            this.sections.put(name, section);
        }
        
        return section;
    }
    
    boolean isHead(Section section) {
        return this.stack.peek() == section;
    }
    
    /**
     * Begin a new profiler section using the specified path
     * 
     * @param path path parts
     * @return new profiler section
     */
    public Section begin(String... path) {
        return this.begin(0, path);
    }
    
    /**
     * Begin a new profiler section using the specified path and flags
     * 
     * @param flags section flags
     * @param path path parts
     * @return new profiler section
     */
    public Section begin(int flags, String... path) {
        return this.begin(flags, Joiner.on('.').join(path));
    }

    /**
     * Begin a new profiler section using the specified name
     * 
     * @param name section name
     * @return new profiler section
     */
    public Section begin(String name) {
        return this.begin(0, name);
    }
    
    /**
     * Begin a new profiler section using the specified name and flags
     * 
     * @param flags section flags
     * @param name section name
     * @return new profiler section
     */
    public Section begin(int flags, String name) {
        boolean root = (flags & Profiler.ROOT) != 0;
        boolean fine = (flags & Profiler.FINE) != 0;
        
        String path = name;
        Section head = this.stack.peek();
        if (head != null) {
            path = head.getName() + (root ? Section.SEPARATOR_ROOT : Section.SEPARATOR_CHILD) + path;
            if (head.isRoot() && !root) {
                int pos = head.getName().lastIndexOf(Section.SEPARATOR_ROOT);
                name = (pos > -1 ? head.getName().substring(pos + 4) : head.getName()) + Section.SEPARATOR_CHILD + name;
                root = true;
            }
        }
        
        Section section = this.get(root ? name : path);
        if (root && head != null && this.active) {
            section = this.getSubSection(path, head.getName(), section);
        }
        
        section.setFine(fine).setRoot(root);
        this.stack.push(section);
        
        return section.start();
    }
    
    /**
     * Callback from section when {@link Section#end} is called, pops the
     * section from the profiler stack
     * 
     * @param section section ending
     */
    void end(Section section) {
        try {
            for (Section head = this.stack.pop(), next = head; next != section; next = this.stack.pop()) {
                if (next == null && this.active) {
                    if (head == null) {
                        throw new IllegalStateException("Attempted to pop " + section + " but the stack is empty");
                    }
                    throw new IllegalStateException("Attempted to pop " + section + " which was not in the stack, head was " + head);
                }
            }
        } catch (NoSuchElementException ex) {
            if (this.active) {
                throw new IllegalStateException("Attempted to pop " + section + " but the stack is empty");
            }
        }
    }
    
    /**
     * Mark a new phase (time slice) for this profiler, all sections record
     * their current times and then reset to zero. If no times have been
     * recorded in the current phase, the phase is discarded.
     * 
     * @param phase Name of the phase
     */
    public void mark(String phase) {
        long currentPhaseTime = 0L;
        for (Section section : this.sections.values()) {
            currentPhaseTime += section.getTime();
        }
        
        // If no accumulated time in the current phase, just discard it
        if (currentPhaseTime == 0L) {
            int size = this.phases.size();
            this.phases.set(size - 1, phase);
            return;
        }
        
        this.phases.add(phase);
        for (Section section : this.sections.values()) {
            section.mark();
        }
    }
    
    /**
     * Get all recorded profiler sections
     */
    public Collection<Section> getSections() {
        return Collections.<Section>unmodifiableCollection(this.sections.values());
    }

    /**
     * Get the profiler state with all sections in a {@link PrettyPrinter}.
     * 
     * @param includeFine Include sections marked as FINE
     * @param group Group delegated sections with their root instead of in the
     *      normal alphabetical order
     * @return PrettyPrinter with section data
     */
    public PrettyPrinter printer(boolean includeFine, boolean group) {
        PrettyPrinter printer = new PrettyPrinter();
        
        // 4 extra columns, name, total, count, avg
        int colCount = this.phases.size() + 4;
        
        //                Which columns go where
        //                Name  Total  Phases  Count         Average
        //                |     |      |       |             |
        int[] columns = { 0,    1,     2,      colCount - 2, colCount - 1 };
        
        Object[] headers = new Object[(colCount) * 2];
        for (int col = 0, pos = 0; col < colCount; col++, pos = col * 2) {
            headers[pos + 1] = Alignment.RIGHT;
            if (col == columns[0]) {
                headers[pos] = (group ? "" : "  ") + "Section";
                headers[pos + 1] = Alignment.LEFT;
            } else if (col == columns[1]) {
                headers[pos] = "    TOTAL";
            } else if (col == columns[3]) {
                headers[pos] = "    Count";
            } else if (col == columns[4]) {
                headers[pos] = "Avg. ";
            } else if (col - columns[2] < this.phases.size()) {
                headers[pos] = this.phases.get(col - columns[2]);
            } else {
                headers[pos] = "";
            }
        }
        
        printer.table(headers).th().hr().add();
        
        for (Section section : this.sections.values()) {
            if ((section.isFine() && !includeFine) || (group && section.getDelegate() != section)) {
                continue;
            }

            // Add row for this section
            this.printSectionRow(printer, colCount, columns, section, group);
            
            // If grouping, print sections which have this section as delegate
            if (group) {
                for (Section subSection : this.sections.values()) {
                    Section delegate = subSection.getDelegate();
                    if ((subSection.isFine() && !includeFine) || delegate != section || delegate == subSection) {
                        continue;
                    }
                    
                    this.printSectionRow(printer, colCount, columns, subSection, group);
                }   
            }
        }

        return printer.add();
    }

    private void printSectionRow(PrettyPrinter printer, int colCount, int[] columns, Section section, boolean group) {
        boolean isDelegate = section.getDelegate() != section;
        Object[] values = new Object[colCount];
        int col = 1;
        if (group) {
            values[0] = isDelegate ? "  > " + section.getBaseName() : section.getName();
        } else {
            values[0] = (isDelegate ? "+ " : "  ") + section.getName();
        }
        
        long[] times = section.getTimes();
        for (long time : times) {
            if (col == columns[1]) {
                values[col++] = section.getTotalTime() + " ms";
            }
            if (col >= columns[2] && col < values.length) {
                values[col++] = time + " ms";
            }
        }
        
        values[columns[3]] = section.getTotalCount();
        values[columns[4]] = new DecimalFormat("   ###0.000 ms").format(section.getTotalAverageTime());

        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                values[i] = "-";
            }
        }
        
        printer.tr(values);
    }
    
}
