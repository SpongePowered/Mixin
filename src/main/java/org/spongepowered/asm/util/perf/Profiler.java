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

import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;

import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.PrettyPrinter.Alignment;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Performance profiler for Mixin.
 */
public final class Profiler {
    
    private static final String METRONOME_AGENT_CLASS = "org.spongepowered.metronome.Agent";

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
     * Base abstract profiler section. When disabled, the profiler itself
     * returns {@link DisabledSection} so as to consume minimal resources
     * without needing to introduce nullability checks everwhere that sections
     * are used. When enabled, {@link LiveSection} is used to record timings.
     */
    public abstract static class Section {
        
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
        
        protected int getCursor() {
            return 0;
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
            return this;
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
        
        /**
         * Internal accessor for markedTime from LiveSection, used by
         * ResultSection 
         */
        protected long getMarkedTime() {
            return 0L;
        }
        
        /**
         * Internal accessor for markedCount from LiveSection, used by
         * ResultSection 
         */
        protected int getMarkedCount() {
            return 0;
        }
        
    }
    
    /**
     * Section used when the profiler is not active
     */
    class DisabledSection extends Section {

        DisabledSection(String name) {
            super(name);
        }
        
        /**
         * Stop timing of this section and end it (pop from profiler stack)
         * 
         * @return fluent
         */
        @Override
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
        @Override
        public Section next(String name) {
            this.end();
            return Profiler.this.begin(name);
        }
        
    }
    
    /**
     * Live profiler section. Actually records timings for when the profiler is
     * active.
     */
    class LiveSection extends DisabledSection {

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
        protected int getCursor() {
            return this.cursor;
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
        
        @Override
        protected long getMarkedTime() {
            return this.markedTime;
        }
        
        @Override
        protected int getMarkedCount() {
            return this.markedCount;
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
     * A result section is an aggregate of all sections with the same name from
     * all profilers, used when printing a global summary
     */
    static class ResultSection extends Section {
        
        /**
         * Sections to summarise
         */
        private List<Section> sections = new ArrayList<Section>();

        ResultSection(String name) {
            super(name);
        }
        
        void add(Section section) {
            this.sections.add(section);
        }
        
        @Override
        public long getTime() {
            long time = 0L;
            for (Section section : this.sections) {
                time += section.getTime();
            }
            return time;
        }
        
        @Override
        public long getTotalTime() {
            long totalTime = 0L;
            for (Section section : this.sections) {
                totalTime += section.getTotalTime();
            }
            return totalTime;
        }
        
        @Override
        public double getSeconds() {
            double seconds = 0.0D;
            for (Section section : this.sections) {
                seconds += section.getSeconds();
            }
            return seconds;
        }
        
        @Override
        public double getTotalSeconds() {
            double totalSeconds = 0.0D;
            for (Section section : this.sections) {
                totalSeconds += section.getTotalSeconds();
            }
            return totalSeconds;
        }
        
        @Override
        public long[] getTimes() {
            int cursor = 0;
            for (Section section : this.sections) {
                cursor = Math.max(cursor, section.getCursor());
            }
            
            long[] times = new long[cursor + 1];
            for (Section section : this.sections) {
                long[] sectionTimes = section.getTimes();
                for (int i = 0; i < sectionTimes.length; i++) {
                    times[i] += sectionTimes[i];
                }
            }
            
            return times;
        }
        
        @Override
        public int getCount() {
            int count = 0;
            for (Section section : this.sections) {
                count += section.getCount();
            }
            return count;
        }
        
        @Override
        public int getTotalCount() {
            int totalCount = 0;
            for (Section section : this.sections) {
                totalCount += section.getTotalCount();
            }
            return totalCount;
        }
        
        @Override
        protected long getMarkedTime() {
            long markedTime = 0L;
            for (Section section : this.sections) {
                markedTime += section.getMarkedTime();
            }
            return markedTime;
        }
        
        @Override
        protected int getMarkedCount() {
            int markedCount = 0;
            for (Section section : this.sections) {
                markedCount += section.getMarkedCount();
            }
            return markedCount;
        }

        @Override
        public double getAverageTime() {
            int count = this.getCount();
            return count > 0 ? (double)(this.getTime()) / count : 0.0D;
        }
        
        @Override
        public double getTotalAverageTime() {
            int count = this.getCount();
            return count > 0 ? (double)(this.getTime() + this.getMarkedTime()) / (count + this.getMarkedCount()) : 0.0D;
        }
        
    }
    
    /**
     * All Profiler instances
     */
    private static final Map<String, Profiler> profilers = new HashMap<String, Profiler>();

    /**
     * True when profilers are active
     */
    private static boolean active;
    
    /**
     * Profiler id 
     */
    private final String id;
    
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
    
    public Profiler(String id) {
        this.id = id;
        this.phases.add("Initial");
    }
    
    @Override
    public String toString() {
        return this.id;
    }

    /**
     * Set the active state of the profiler.
     * 
     * @param active new active state
     */
    public static void setActive(boolean active) {
        Profiler.active = active;
    }
    
    /**
     * Reset all profiler state
     */
    public synchronized void reset() {
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
    public synchronized Section get(String name) {
        Section section = this.sections.get(name);
        if (section == null) {
            section = Profiler.active ? new LiveSection(name, this.phases.size() - 1) : new DisabledSection(name);
            this.sections.put(name, section);
        }
        return section;
    }
    
    private synchronized Section getSubSection(String name, String baseName, Section root) {
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
    public synchronized Section begin(int flags, String name) {
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
        if (root && head != null && Profiler.active) {
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
    synchronized void end(Section section) {
        try {
            for (Section head = this.stack.pop(), next = head; next != section; next = this.stack.pop()) {
                if (next == null && Profiler.active) {
                    if (head == null) {
                        throw new IllegalStateException("Attempted to pop " + section + " but the stack is empty");
                    }
                    throw new IllegalStateException("Attempted to pop " + section + " which was not in the stack, head was " + head);
                }
            }
        } catch (NoSuchElementException ex) {
            if (Profiler.active) {
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
    public synchronized void mark(String phase) {
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
    public synchronized Collection<Section> getSections() {
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
        return Profiler.printer(includeFine, group, this.phases, this.sections);
    }
    
    private static PrettyPrinter printer(boolean includeFine, boolean group, List<String> phases, Map<String, Section> sections) {
        PrettyPrinter printer = new PrettyPrinter();
        
        // 4 extra columns, name, total, count, avg
        int colCount = phases.size() + 4;
        
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
            } else if (col - columns[2] < phases.size()) {
                headers[pos] = phases.get(col - columns[2]);
            } else {
                headers[pos] = "";
            }
        }
        
        printer.table(headers).th().hr().add();
        
        for (Section section : sections.values()) {
            if ((section.isFine() && !includeFine) || (group && section.getDelegate() != section)) {
                continue;
            }

            // Add row for this section
            Profiler.printSectionRow(printer, colCount, columns, section, group);
            
            // If grouping, print sections which have this section as delegate
            if (group) {
                for (Section subSection : sections.values()) {
                    Section delegate = subSection.getDelegate();
                    if ((subSection.isFine() && !includeFine) || delegate != section || delegate == subSection) {
                        continue;
                    }
                    
                    Profiler.printSectionRow(printer, colCount, columns, subSection, group);
                }   
            }
        }

        return printer.add();
    }

    private static void printSectionRow(PrettyPrinter printer, int colCount, int[] columns, Section section, boolean group) {
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

    /**
     * Print summary of this profiler's recorded performance to the console
     */
    public void printSummary() {
        Profiler.printSummary(this.id, this.phases, this.sections);
    }

    /**
     * Print summary of mixin performance from all active profilers to the
     * console
     */
    public static void printAuditSummary() {
        String id;
        Set<String> allPhases; 
        Map<String, Section> allSections;

        // Collect sections from all profilers into ResultSections
        synchronized (Profiler.profilers) {
            id = Joiner.on(',').join(Profiler.profilers.values());
            allPhases = new LinkedHashSet<String>();
            allSections = new TreeMap<String, Section>() {

                private static final long serialVersionUID = 1L;

                @Override
                public Section get(Object name) {
                    Section section = super.get(name);
                    if (section == null) {
                        this.put(name.toString(), section = new ResultSection(name.toString()));
                    }
                    return section;
                }
            };
            for (Profiler profiler : Profiler.profilers.values()) {
                for (String phase : profiler.phases) {
                    allPhases.add(phase);
                }

                for (Entry<String, Section> section : profiler.sections.entrySet()) {
                    ((ResultSection)allSections.get(section.getKey())).add(section.getValue());
                }
            }
        }
        
        Profiler.printSummary(id, new ArrayList<String>(allPhases), allSections);
    }
    
    private static void printSummary(String id, List<String> phases, Map<String, Section> sections) {
        
        DecimalFormat threedp = new DecimalFormat("(###0.000");
        DecimalFormat onedp = new DecimalFormat("(###0.0");
        PrettyPrinter printer = Profiler.printer(false, false, phases, sections);
        
        long prepareTime = sections.get("mixin.prepare").getTotalTime();
        long readTime = sections.get("mixin.read").getTotalTime();
        long applyTime = sections.get("mixin.apply").getTotalTime();
        long writeTime = sections.get("mixin.write").getTotalTime();
        long totalMixinTime = sections.get("mixin").getTotalTime();
        
        long loadTime = sections.get("class.load").getTotalTime();
        long transformTime = sections.get("class.transform").getTotalTime();
        long exportTime = sections.get("mixin.debug.export").getTotalTime();
        long actualTime = totalMixinTime - loadTime - transformTime - exportTime;
        double timeSliceMixin = ((double)actualTime / (double)totalMixinTime) * 100.0D;
        double timeSliceLoad = ((double)loadTime / (double)totalMixinTime) * 100.0D;
        double timeSliceTransform = ((double)transformTime / (double)totalMixinTime) * 100.0D;
        double timeSliceExport = ((double)exportTime / (double)totalMixinTime) * 100.0D;
        
        long worstTransformerTime = 0L;
        Section worstTransformer = null;
        
        for (Section section : sections.values()) {
            long transformerTime = section.getName().startsWith("class.transform.") ? section.getTotalTime() : 0L;
            if (transformerTime > worstTransformerTime) {
                worstTransformerTime = transformerTime;
                worstTransformer = section;
            }
        }
        
        printer.hr().add("Summary for Profiler[%s]", id).hr().add();
        
        String format = "%9d ms %12s seconds)";
        printer.kv("Total mixin time", format, totalMixinTime, threedp.format(totalMixinTime * 0.001)).add();
        printer.kv("Preparing mixins", format, prepareTime, threedp.format(prepareTime * 0.001));
        printer.kv("Reading input", format, readTime, threedp.format(readTime * 0.001));
        printer.kv("Applying mixins", format, applyTime, threedp.format(applyTime * 0.001));
        printer.kv("Writing output", format, writeTime, threedp.format(writeTime * 0.001)).add();
        
        printer.kv("of which","");
        printer.kv("Time spent loading from disk", format, loadTime, threedp.format(loadTime * 0.001));
        printer.kv("Time spent transforming classes", format, transformTime, threedp.format(transformTime * 0.001)).add();
        
        if (worstTransformer != null) {
            printer.kv("Worst transformer", worstTransformer.getName());
            printer.kv("Class", worstTransformer.getInfo());
            printer.kv("Time spent", "%s seconds", worstTransformer.getTotalSeconds());
            printer.kv("called", "%d times", worstTransformer.getTotalCount()).add();
        }
        
        printer.kv("   Time allocation:     Processing mixins", "%9d ms %10s%% of total)", actualTime, onedp.format(timeSliceMixin));
        printer.kv("Loading classes", "%9d ms %10s%% of total)", loadTime, onedp.format(timeSliceLoad));
        printer.kv("Running transformers", "%9d ms %10s%% of total)", transformTime, onedp.format(timeSliceTransform));
        if (exportTime > 0L) {
            printer.kv("Exporting classes (debug)", "%9d ms %10s%% of total)", exportTime, onedp.format(timeSliceExport));
        }
        printer.add();
        
        try {
            Class<?> agent = MixinService.getService().getClassProvider().findAgentClass(Profiler.METRONOME_AGENT_CLASS, false);
            Method mdGetTimes = agent.getDeclaredMethod("getTimes");
            
            @SuppressWarnings("unchecked")
            Map<String, Long> times = (Map<String, Long>)mdGetTimes.invoke(null);
            
            printer.hr().add("Transformer Times").hr().add();

            int longest = 10;
            for (Entry<String, Long> entry : times.entrySet()) {
                longest = Math.max(longest, entry.getKey().length());
            }
            
            for (Entry<String, Long> entry : times.entrySet()) {
                String name = entry.getKey();
                long mixinTime = 0L;
                for (Section section : sections.values()) {
                    if (name.equals(section.getInfo())) {
                        mixinTime = section.getTotalTime();
                        break;
                    }
                }
                
                if (mixinTime > 0L) {
                    printer.add("%-" + longest + "s %8s ms %8s ms in mixin)", name, entry.getValue() + mixinTime, "(" + mixinTime);
                } else {
                    printer.add("%-" + longest + "s %8s ms", name, entry.getValue());
                }
            }
            
            printer.add();
            
        } catch (Throwable th) {
            // Metronome agent not loaded
        }

        printer.print();
    }
    
    /**
     * Get the specified performance profiler
     * 
     * @param id Profiler id
     * @return profiler
     */
    public static Profiler getProfiler(String id) {
        synchronized (Profiler.profilers) {
            Profiler profiler = Profiler.profilers.get(id);
            if (profiler == null) {
                Profiler.profilers.put(id, profiler = new Profiler(id));
            }
            return profiler;
        }
    }
    
    /**
     * Get all available performance profilers
     * 
     * @return immutable collection of profilers
     */
    public static Collection<Profiler> getProfilers() {
        Builder<Profiler> list = ImmutableList.<Profiler>builder();
        synchronized (Profiler.profilers) {
            list.addAll(Profiler.profilers.values());
        }
        return list.build();
    }
    
}
