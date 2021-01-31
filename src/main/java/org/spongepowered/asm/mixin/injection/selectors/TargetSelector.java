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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic.Kind;

import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorId;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.struct.InvalidMemberDescriptorException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.injection.struct.MemberMatcher;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.util.asm.ElementNode;
import org.spongepowered.asm.util.logging.MessageRouter;

import com.google.common.base.Strings;

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

    /**
     * A dynamic selector registration entry
     */
    static class DynamicSelectorEntry {
        
        final String namespace;
        
        final String id;
        
        final Class<? extends ITargetSelectorDynamic> type;
        
        final Method parse;

        DynamicSelectorEntry(String namespace, String id, Class<? extends ITargetSelectorDynamic> type) throws NoSuchMethodException {
            this.namespace = namespace;
            this.id = id;
            this.type = type;
            this.parse = type.getDeclaredMethod("parse", String.class, ISelectorContext.class);
            if (!Modifier.isStatic(this.parse.getModifiers())) {
                throw new MixinError("parse method for dynamic target selector [" + this.type.getName() + "] must be static");
            }
            if (!ITargetSelectorDynamic.class.isAssignableFrom(this.parse.getReturnType())) {
                throw new MixinError("parse method for dynamic target selector [" + this.type.getName()
                        + "] must return an ITargetSelectorDynamic subtype");
            }
        }
        
        String getCode() {
            return (this.namespace != null ? this.namespace + ":" : "") + this.id;
        }
        
        ITargetSelectorDynamic parse(String input, ISelectorContext context) throws ReflectiveOperationException {
            try {
                return (ITargetSelectorDynamic)this.parse.invoke(null, input, context);
            } catch (InvocationTargetException itex) {
                Throwable cause = itex.getCause();
                if (cause instanceof MixinException) {
                    throw (MixinException)cause;
                }
                Throwable ex = cause != null ? cause : itex;
                ex.printStackTrace();
                throw new MixinError("Error parsing dynamic target selector [" + this.type.getName() + "] for " + context, ex);
            }
        }
    }
    
    /**
     * Pattern for matching dynamic selectors
     */
    private static final Pattern PATTERN_DYNAMIC = Pattern.compile("^\\x40([a-z]+(:[a-z]+)?)(\\((.*)\\))?$", Pattern.CASE_INSENSITIVE);
    
     /**
     * Registered dynamic selectors
     */
    private static Map<String, DynamicSelectorEntry> dynamicSelectors = new LinkedHashMap<String, DynamicSelectorEntry>();
    
    static {
        TargetSelector.registerBuiltIn(DynamicSelectorDesc.class);
    }
    
    private TargetSelector() {
    }
    
    /**
     * Register a dynamic target selector class. The supplied class must be
     * decorated with an {@link SelectorId} annotation for registration to
     * succeed.
     * 
     * @param type ITargetSelectorDynamic to register
     * @param namespace namespace for SelectorId
     */
    public static void register(Class<? extends ITargetSelectorDynamic> type, String namespace) {
        SelectorId selectorId = type.<SelectorId>getAnnotation(SelectorId.class);
        if (selectorId == null) {
            throw new IllegalArgumentException("Dynamic target selector class " + type + " is not annotated with @SelectorId");
        }
        
        String annotationNamespace = selectorId.namespace();
        if (!Strings.isNullOrEmpty(annotationNamespace)) {
            namespace = annotationNamespace;
        }
        
        if (Strings.isNullOrEmpty(namespace)) {
            throw new IllegalArgumentException("Dynamic target selector class " + type
                    + " has no namespace. Please specify namespace in SelectorId annotation or declaring configuration");
        }

        DynamicSelectorEntry entry;
        try {
            entry = new DynamicSelectorEntry(namespace, selectorId.value().toLowerCase(Locale.ROOT), type);
        } catch (NoSuchMethodException ex) {
            throw new MixinError("Dynamic target selector class " + type.getName() + " does not contain a valid parse method");
        }
        DynamicSelectorEntry existing = TargetSelector.dynamicSelectors.get(entry.id);
        if (existing != null) { // && !existing.type.equals(type)) {
            MessageRouter.getMessager().printMessage(Kind.WARNING, String.format("Overriding target selector for @%s with %s (previously %s)",
                    entry.id, type.getName(), existing.type.getName()));
        } else {
            MessageRouter.getMessager().printMessage(Kind.OTHER, String.format("Registering new target selector for @%s with %s",
                    entry.id, type.getName()));
        }
        
        TargetSelector.dynamicSelectors.put(entry.getCode(), entry);
    }
    
    /**
     * Register a built-in target selector class. Skips validation and
     * namespacing checks
     * 
     * @param type ITargetSelectorDynamic to register
     */
    private static void registerBuiltIn(Class<? extends ITargetSelectorDynamic> type) {
        SelectorId selectorId = type.<SelectorId>getAnnotation(SelectorId.class);
        DynamicSelectorEntry entry;
        try {
            entry = new DynamicSelectorEntry(null, selectorId.value().toLowerCase(Locale.ROOT), type);
        } catch (NoSuchMethodException ex) {
            throw new MixinError("Dynamic target selector class " + type.getName() + " does not contain a valid parse method");
        }
        TargetSelector.dynamicSelectors.put(entry.getCode(), entry);
    }
    
    /**
     * Parse a target selector from a string and perform validation
     * 
     * @param string String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parseAndValidate(String string, ISelectorContext context) throws InvalidMemberDescriptorException {
        return TargetSelector.parse(string, context).validate();
    }
    
    /**
     * Parse a target selector from a string
     * 
     * @param string String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parse(String string, ISelectorContext context) {
        string = string.trim();
        
        // Ending with slash indicates a regex target, no other type of target
        // selector can end (legally) with a forward slash
        if (string.endsWith("/")) {
            MemberMatcher regexMatcher = MemberMatcher.parse(string, context);
            if (regexMatcher != null) {
                return regexMatcher;
            }
        }
        
        // Starting with @ indicates a dynamic target. Parse as a regular
        // MemberInfo if the selector does not start with @ 
        if (!string.startsWith("@")) {
            return MemberInfo.parse(string, context);
        }

        Matcher dynamic = TargetSelector.PATTERN_DYNAMIC.matcher(string);
        if (!dynamic.matches()) {
            return new InvalidSelector(new InvalidSelectorException("Dynamic selector was in an unrecognised format. Parsing selector: " + string));
        }
        
        String selectorId = dynamic.group(1).toLowerCase(Locale.ROOT);
        if (!TargetSelector.dynamicSelectors.containsKey(selectorId)) {
            return new InvalidSelector(new InvalidSelectorException("Dynamic selector with id '@" + dynamic.group(1)
                    + "' is not registered. Parsing selector: " + string));
        }
        
        try {
            return TargetSelector.dynamicSelectors.get(selectorId).parse(Strings.nullToEmpty(dynamic.group(4)).trim(), context);
        } catch (ReflectiveOperationException ex) {
            return new InvalidSelector(ex.getCause(), string);
        } catch (Exception ex) {
            return new InvalidSelector(ex);
        }
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
    public static String parseName(String name, ISelectorContext context) {
        ITargetSelector selector = TargetSelector.parse(name, context);
        if (!(selector instanceof ITargetSelectorByName)) {
            return name;
        }
        String mappedName = ((ITargetSelectorByName)selector).getName();
        return mappedName != null ? mappedName : name;
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
