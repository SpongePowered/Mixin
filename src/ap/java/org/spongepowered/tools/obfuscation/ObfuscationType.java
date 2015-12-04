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
package org.spongepowered.tools.obfuscation;

enum ObfuscationType {
    
    SRG("searge", SupportedOptions.REOBF_SRG_FILE, SupportedOptions.OUT_SRG_SRG_FILE, SupportedOptions.OUT_SRG_REFMAP_FILE),
    NOTCH("notch", SupportedOptions.REOBF_NOTCH_FILE, SupportedOptions.OUT_NOTCH_SRG_FILE, SupportedOptions.OUT_NOTCH_REFMAP_FILE);
    
    private final String displayName;
    private final String srgFileArgName;
    private final String outSrgFileArgName;
    private final String refmapArgName;
    
    private ObfuscationType(String displayName, String srgFileArgName, String outSrgFileArgName, String refmapArgName) {
        this.displayName = displayName;
        this.srgFileArgName = srgFileArgName;
        this.outSrgFileArgName = outSrgFileArgName;
        this.refmapArgName = refmapArgName;
    }
    
    @Override
    public String toString() {
        return this.displayName;
    }
    
    public String getDisplayName() {
        return this.displayName;
    }
    
    public String getSrgFileOption() {
        return this.srgFileArgName;
    }
    
    public String getOutputSrgFileOption() {
        return this.outSrgFileArgName;
    }
    
    public String getRefMapFileOption() {
        return this.refmapArgName;
    }
    
    public boolean isSupported(IOptionProvider options) {
        return this.getSrgFileName(options) != null;
    }
    
    public String getSrgFileName(IOptionProvider options) {
        return options.getOption(this.srgFileArgName);
    }
    
    public String getOutputSrgFileName(IOptionProvider options) {
        return options.getOption(this.outSrgFileArgName);
    }
    
    public String getRefMapFileName(IOptionProvider options) {
        return options.getOption(this.refmapArgName);
    }
    
}
