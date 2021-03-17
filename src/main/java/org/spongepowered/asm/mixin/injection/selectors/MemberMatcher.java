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
package org.spongepowered.asm.mixin.injection.selectors;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A {@link ITargetSelector Target Selector} which matches an element of
 * candidate members using a regular expression. This selector is not remappable
 * and is not suitable for use in obfuscated environments.
 * 
 * <p>Regular expressions for this selector should be enclosed in <tt>/</tt> as
 * a delimiter character, and <tt>\</tt> characters must be escaped per normal
 * Java conventions (eg. use <tt>\\s</tt> instead of <tt>\s</tt> since the
 * backslash must be escaped). Forward slash (<tt>/</tt>) characters in the
 * regex must be escaped as well to avoid being consumed as ending delimiters.
 * </p>
 * 
 * <p>By default the regex match is performed against the <tt>name</tt> of the
 * candidate, to specify matching against <tt>owner</tt> or <tt>desc</tt>, each
 * pattern should be prefixed with <tt>owner=</tt> or <tt>desc=</tt> <b>outside
 * the delimiting <tt>/</tt></b>.
 * 
 * <p>Some examples:</p>
 * <blockquote><pre>
 *   <del>// Matches candidates starting with "foo"</del>
 *   /^foo/
 *   
 *   <del>// Matches candidates ending with "Bar" and which take a single int
 *</del>   /bar$/ desc=/^\\(I\\)/
 *   
 *   <del>// The same example but with "name" explicitly specified (optional)
 *</del>   name=/bar$/ desc=/^\\(I\\)/
 *   
 *   <del>// Matches candidates whose name contains "Entity"</del>
 *   /Entity/
 *   
 *   <del>// Matches candidates whose owner contains "/google/", note the
 *   // escaping of the forward slash symbols</del>
 *   owner=/\\/google\\//</pre>
 * </blockquote>
 * 
 * <p>The <tt>owner</tt>, <tt>name</tt> and <tt>desc</tt> expressions can be
 * provided in any order.</p>
 */
public final class MemberMatcher implements ITargetSelector {

    /**
     * Regex selector, searches for targets using supplied regular expression
     */
    private static final Pattern PATTERN = Pattern.compile("((owner|name|desc)\\s*=\\s*)?/(.*?)(?<!\\\\)/");
    
    /**
     * Names of the positional source elements, just used for error messages
     */
    private static final String[] PATTERN_SOURCE_NAMES = { "owner", "name", "desc" };
    
    // Positional source elements
    private static final int SOURCE_OWNER = 0;
    private static final int SOURCE_NAME = 1;
    private static final int SOURCE_DESC = 2;
    
    /**
     * Positional patterns. The match sources are packed into 3-element arrays
     * just to make the iteration for matching simpler
     */
    private final Pattern[] patterns;
    
    /**
     * Stored exception during parse. The contract of parse prohibits us from
     * emitting (intentional) exceptions. Any exceptions are stored here so they
     * can be emitted in {@link #validate}. 
     */
    private final Exception parseException;
    
    /**
     * Input string, stored just so we can emit it along with the exception
     * message if {@link #validate} needs to throw a parse exception
     */
    private final String input;
    
    private MemberMatcher(Pattern[] patterns, Exception parseException, String input) {
        this.patterns = patterns;
        this.parseException = parseException;
        this.input = input;
    }
    
    /**
     * Parse a MemberMatcher from the supplied input string.
     * 
     * @param input Raw input string
     * @param context selector context
     * @return parsed MemberMatcher
     */
    public static MemberMatcher parse(final String input, ISelectorContext context) {
        Matcher matcher = MemberMatcher.PATTERN.matcher(input);
        Pattern[] patterns = new Pattern[3];
        Exception parseException = null;
        
        while (matcher.find()) {
            Pattern pattern;
            try {
                pattern = Pattern.compile(matcher.group(3));
            } catch (PatternSyntaxException ex) {
                parseException = ex;
                pattern = Pattern.compile(".*");
                ex.printStackTrace();
            }
            
            int patternId = "owner".equals(matcher.group(2)) ? MemberMatcher.SOURCE_OWNER : "desc".equals(matcher.group(2))
                    ? MemberMatcher.SOURCE_DESC : MemberMatcher.SOURCE_NAME;
            if (patterns[patternId] != null) {
                parseException = new InvalidSelectorException("Pattern for '" + MemberMatcher.PATTERN_SOURCE_NAMES[patternId]
                        + "' specified multiple times: Old=/" + patterns[patternId].pattern() + "/ New=/" + pattern.pattern() + "/");
            }
            patterns[patternId] = pattern;
        }

        return new MemberMatcher(patterns, parseException, input);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #validate()
     */
    @Override
    public ITargetSelector validate() throws InvalidSelectorException {
        if (this.parseException != null) {
            if (this.parseException instanceof InvalidSelectorException) {
                throw (InvalidSelectorException)this.parseException;
            }
            throw new InvalidSelectorException("Error parsing regex selector", this.parseException);
        }
        
        boolean validPattern = false;
        for (Pattern pattern : this.patterns) {
            validPattern |= pattern != null;
        }
        
        if (!validPattern) {
            throw new InvalidSelectorException("Error parsing regex selector, the input was in an unexpected format: " + this.input);
        }
        
        return this;
    }
    
    @Override
    public String toString() {
        return this.input;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #next()
     */
    @Override
    public ITargetSelector next() {
        return this; // Regex matcher flows into targets
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #configure(java.lang.String[])
     */
    @Override
    public ITargetSelector configure(Configure request, String... args) {
        request.checkArgs(args);
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #attach(org.spongepowered.asm.mixin.injection.selectors
     *      .ISelectorContext)
     */
    @Override
    public ITargetSelector attach(ISelectorContext context) throws InvalidSelectorException {
        return this;
    }
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #getMinMatchCount()
     */
    @Override
    public int getMinMatchCount() {
        return 0;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #getMaxMatchCount()
     */
    @Override
    public int getMaxMatchCount() {
        return Integer.MAX_VALUE;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.injection.selectors.ITargetSelector
     *      #match(org.spongepowered.asm.util.asm.ElementNode)
     */
    @Override
    public <TNode> MatchResult match(ElementNode<TNode> node) {
        return node == null ? MatchResult.NONE : this.matches(node.getOwner(), node.getName(), node.getDesc());
    }
    
    private MatchResult matches(String... args) {
        MatchResult result = MatchResult.NONE;
//        boolean exactOnly = true;
        
        for (int i = 0; i < this.patterns.length; i++) {
            if (this.patterns[i] == null || args[i] == null) {
                continue;
            }
            
            if (this.patterns[i].matcher(args[i]).find()) {
//                String pattern = this.patterns[i].pattern();
//                if (pattern.startsWith("^") && pattern.endsWith("$") && exactOnly) {
                    result = MatchResult.EXACT_MATCH;
//                } else {
//                    result = MatchResult.EXACT_MATCH;
//                    exactOnly = false;
//                }
            } else {
                result = MatchResult.NONE;
            }
        }
        
        return result;
    }

}
