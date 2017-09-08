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
package org.spongepowered.asm.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a software version number in <code>major.minor.revision.build
 * </code> format as a sequence of four shorts packed into a long. This is to
 * facilitate meaningful comparison between version numbers.
 */
public final class VersionNumber implements Comparable<VersionNumber>, Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * Represents no version number or a version number which could not be
     * parsed
     */
    public static final VersionNumber NONE = new VersionNumber();
    
    /**
     * Regex for matching a version number specified as a string
     */
    private static final Pattern PATTERN =
            Pattern.compile("^(\\d{1,5})(?:\\.(\\d{1,5})(?:\\.(\\d{1,5})(?:\\.(\\d{1,5}))?)?)?(-[a-zA-Z0-9_\\-]+)?$");
    
    /**
     * The version number, packed as four shorts into a long
     */
    private final long value;
    
    /**
     * suffix such as -BETA or -SNAPSHOT, must be prefixed with "-" to be
     * matched
     */
    private final String suffix;
    
    private VersionNumber() {
        this.value = 0;
        this.suffix = "";
    }
    
    private VersionNumber(short[] parts) {
        this(parts, null);
    }
    
    private VersionNumber(short[] parts, String suffix) {
        this.value = VersionNumber.pack(parts);
        this.suffix = suffix != null ? suffix : "";
    }
    
    private VersionNumber(short major, short minor, short revision, short build) {
        this(major, minor, revision, build, null);
    }
    
    private VersionNumber(short major, short minor, short revision, short build, String suffix) {
        this.value = VersionNumber.pack(major, minor, revision, build);
        this.suffix = suffix != null ? suffix : "";
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        short[] parts = VersionNumber.unpack(this.value);
        
        return String.format("%d.%d%3$s%4$s%5$s",
                parts[0],
                parts[1],
                (this.value & Integer.MAX_VALUE) > 0 ? String.format(".%d", parts[2]) : "",
                (this.value & Short.MAX_VALUE) > 0 ? String.format(".%d", parts[3]) : "",
                this.suffix);
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(VersionNumber other) {
        if (other == null) {
            return 1;
        }
        long delta = this.value - other.value;
        return delta > 0 ? 1 : delta < 0 ? -1 : 0;  
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VersionNumber)) {
            return false;
        }
        
        return ((VersionNumber)other).value == this.value;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (int)(this.value >> 32) ^ (int)(this.value & 0xFFFFFFFFL); 
    }

    /**
     * Packs the supplied array of shorts into a long
     * 
     * @param shorts short values to pack
     * @return supplied shorts packed into a long
     */
    private static long pack(short... shorts) {
        return (long)shorts[0] << 48 | (long)shorts[1] << 32 | shorts[2] << 16 | shorts[3];
    }
    
    /**
     * Unpacks a long into an array of four shorts
     * 
     * @param along Long to unpack
     * @return Unpacked array of four shorts
     */
    private static short[] unpack(long along) {
        return new short[] {
            (short)(along >> 48),
            (short)(along >> 32 & Short.MAX_VALUE),
            (short)(along >> 16 & Short.MAX_VALUE),
            (short)(along & Short.MAX_VALUE) 
        };
    }
    
    /**
     * Parse a version number specified as a string
     * 
     * @param version Version number to parse
     * @return Version number
     */
    public static VersionNumber parse(String version) {
        return VersionNumber.parse(version, VersionNumber.NONE);
    }

    /**
     * Parse a version number specified as a string and return default if
     * parsing fails
     * 
     * @param version Version number to parse
     * @param defaultVersion Version number to return if parse fails 
     * @return Version number
     */
    public static VersionNumber parse(String version, String defaultVersion) {
        return VersionNumber.parse(version, VersionNumber.parse(defaultVersion));
    }
    
    /**
     * Parse a version number specified as a string and return default if
     * parsing fails
     * 
     * @param version Version number to parse
     * @param defaultVersion Version number to return if parse fails 
     * @return Version number
     */
    private static VersionNumber parse(String version, VersionNumber defaultVersion) {
        if (version == null) {
            return defaultVersion;
        }
        
        Matcher versionNumberPatternMatcher = VersionNumber.PATTERN.matcher(version);
        if (!versionNumberPatternMatcher.matches()) {
            return defaultVersion;
        }
        
        short[] parts = new short[4];
        for (int pos = 0; pos < 4; pos++) {
            String part = versionNumberPatternMatcher.group(pos + 1);
            if (part != null) {
                int value = Integer.parseInt(part);
                if (value > Short.MAX_VALUE) {
                    throw new IllegalArgumentException("Version parts cannot exceed " + Short.MAX_VALUE + ", found " + value);
                }
                parts[pos] = (short)value;
            }
        }
        
        return new VersionNumber(parts, versionNumberPatternMatcher.group(5));
    }
}
