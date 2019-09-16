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

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.refmap.IMixinContext;
import org.spongepowered.asm.mixin.refmap.IReferenceMapper;
import org.spongepowered.asm.util.asm.ElementNode;

/**
 * Utility class for parsing selectors
 */
public final class TargetSelector {
    
    /**
     * Query result struct
     * 
     * @param <TNode> Node type
     */
    public static class Result<TNode> {
        
        /**
         * Any exact match returned by the query
         */
        public final TNode exactMatch;
        
        /**
         * All candidates returned by the query
         */
        public final List<TNode> candidates;

        Result(TNode exactMatch, List<TNode> candidates) {
            this.exactMatch = exactMatch;
            this.candidates = candidates;
        }
        
        /**
         * Get only a single result from this handle. Preferentially returns an
         * exact match if one was found, or returns the first result if only one
         * result was found or if <tt>strict</tt> is <tt>false</tt>. If <tt>
         * strict</tt> is <tt>true</tt> and more than one candidate was found, a
         * {@link IllegalStateException} is thrown. If no results are found then
         * the exception is also thrown. 
         * 
         * @param strict True to only return the first (non-exact) result
         *          <b>if</b> exactly one result was found. If more than one
         *          candidate was found, throws {@link IllegalStateException}
         */
        public TNode getSingleResult(boolean strict) {
            int resultCount = this.candidates.size();
            if (this.exactMatch != null) {
                return this.exactMatch;
            }
            if (resultCount == 1 || !strict) {
                return this.candidates.get(0);
            }
            throw new IllegalStateException((resultCount == 0 ? "No" : "Multiple") + " candidates were found");
        }
        
    }

    private TargetSelector() {
    }
    
    /**
     * Parse a target selector from a string and perform validation
     * 
     * @param string String to parse target selector from
     * @return parsed target selector
     */
    public static ITargetSelector parseAndValidate(String string) throws InvalidMemberDescriptorException {
        return TargetSelector.parse(string, null, null).validate();
    }
    
    /**
     * Parse a target selector from a string and perform validation
     * 
     * @param string String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parseAndValidate(String string, IMixinContext context) throws InvalidMemberDescriptorException {
        return TargetSelector.parse(string, context.getReferenceMapper(), context.getClassRef()).validate();
    }
    
    /**
     * Parse a target selector from a string
     * 
     * @param string String to parse target selector from
     * @return parsed target selector
     */
    public static ITargetSelector parse(String string) {
        return TargetSelector.parse(string, null, null);
    }
    
    /**
     * Parse a target selector from a string
     * 
     * @param string String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parse(String string, IMixinContext context) {
        return TargetSelector.parse(string, context.getReferenceMapper(), context.getClassRef());
    }

    /**
     * Parse a target selector from the supplied name, and then return the name
     * of the match. This is used mainly to remap input names in the same
     * context as a selector without needing the selector itself. If the
     * supplied name does not successfully parse to a name-based selector, then
     * the name is returned unchanged.
     * 
     * @param name Name to parse
     * @param context Mixin context
     * @return remapped name or original name
     */
    public static String parseName(String name, IMixinContext context) {
        ITargetSelector selector = TargetSelector.parse(name, context);
        if (!(selector instanceof ITargetSelectorByName)) {
            return name;
        }
        String mappedName = ((ITargetSelectorByName)selector).getName();
        return mappedName != null ? mappedName : name;
    }
    
    private static ITargetSelector parse(String input, IReferenceMapper refMapper, String mixinClass) {
        return MemberInfo.parse(input, refMapper, mixinClass);
    }
    
    /**
     * Run query on supplied target nodes
     * 
     * @param target Target selector
     * @param nodes Node collection to enumerate
     * @param <TNode> Node type
     * @return query result
     */
    public static <TNode> Result<TNode> run(ITargetSelector target, List<ElementNode<TNode>> nodes) {
        List<TNode> candidates = new ArrayList<TNode>();
        TNode exactMatch = TargetSelector.runSelector(target, nodes, candidates);
        return new Result<TNode>(exactMatch, candidates);
    }
    
    /**
     * Run query on supplied target nodes
     * 
     * @param targets Target selector
     * @param nodes Node collection to enumerate
     * @param <TNode> Node type
     * @return query result
     */
    public static <TNode> Result<TNode> run(Iterable<ITargetSelector> targets, List<ElementNode<TNode>> nodes) {
        TNode exactMatch = null;
        List<TNode> candidates = new ArrayList<TNode>();
        
        for (ITargetSelector target : targets) {
            TNode selectorExactMatch = TargetSelector.runSelector(target, nodes, candidates);
            if (exactMatch == null) {
                exactMatch = selectorExactMatch;
            }
        }
        
        return new Result<TNode>(exactMatch, candidates);
    }

    private static <TNode> TNode runSelector(ITargetSelector target, List<ElementNode<TNode>> nodes, List<TNode> candidates) {
        TNode exactMatch = null;
        for (ElementNode<TNode> element : nodes) {
            MatchResult match = target.match(element);
            if (match.isMatch()) {
                TNode node = element.get();
                if (!candidates.contains(node)) {
                    candidates.add(node);
                }
                if (exactMatch == null && match.isExactMatch()) {
                    exactMatch = node;
                }
            }
        }
        return exactMatch;
    }

}
