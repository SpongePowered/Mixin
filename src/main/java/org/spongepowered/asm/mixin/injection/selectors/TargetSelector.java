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

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic.Kind;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorAnnotation;
import org.spongepowered.asm.mixin.injection.selectors.ITargetSelectorDynamic.SelectorId;
import org.spongepowered.asm.mixin.injection.selectors.dynamic.DynamicSelectorDesc;
import org.spongepowered.asm.mixin.injection.selectors.throwables.SelectorConstraintException;
import org.spongepowered.asm.mixin.injection.struct.MemberInfo;
import org.spongepowered.asm.mixin.throwables.MixinError;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.util.Annotations;
import org.spongepowered.asm.util.asm.IAnnotationHandle;
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
        public final ElementNode<TNode> exactMatch;
        
        /**
         * All candidates returned by the query
         */
        public final List<ElementNode<TNode>> candidates;

        Result(ElementNode<TNode> exactMatch, List<ElementNode<TNode>> candidates) {
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
                return this.exactMatch.get();
            }
            if (resultCount == 1 || !strict) {
                return this.candidates.get(0).get();
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
        
        final Class<? extends Annotation> annotation;
        
        final Method mdParseString, mdParseAnnotation;
        
        DynamicSelectorEntry(String namespace, String id, Class<? extends ITargetSelectorDynamic> type) throws NoSuchMethodException {
            this.namespace = namespace;
            this.id = id;
            this.type = type;
            this.mdParseString = type.getDeclaredMethod("parse", String.class, ISelectorContext.class);
            if (!Modifier.isStatic(this.mdParseString.getModifiers())) {
                throw new MixinError("parse method for dynamic target selector [" + this.type.getName() + "] must be static");
            }
            if (!ITargetSelectorDynamic.class.isAssignableFrom(this.mdParseString.getReturnType())) {
                throw new MixinError("parse(String) method for dynamic target selector [" + this.type.getName()
                        + "] must return an ITargetSelectorDynamic subtype");
            }
            
            Class<? extends Annotation> annotation = null;
            Method mdParseAnnotation = null;

            SelectorAnnotation selectorAnnotation = type.<SelectorAnnotation>getAnnotation(SelectorAnnotation.class);
            if (selectorAnnotation != null) {
                annotation = selectorAnnotation.value();
                mdParseAnnotation = type.getDeclaredMethod("parse", IAnnotationHandle.class, ISelectorContext.class);
                
                if (!Modifier.isStatic(mdParseAnnotation.getModifiers())) {
                    throw new MixinError("parse method for dynamic target selector [" + this.type.getName() + "] must be static");
                }
                if (!ITargetSelectorDynamic.class.isAssignableFrom(mdParseAnnotation.getReturnType())) {
                    throw new MixinError("parse(Annotation) method for dynamic target selector [" + this.type.getName()
                            + "] must return an ITargetSelectorDynamic subtype");
                }
            }

            this.annotation = annotation;
            this.mdParseAnnotation = mdParseAnnotation;
        }
        
        String getCode() {
            return (this.namespace != null ? this.namespace + ":" : "") + this.id;
        }
        
        ITargetSelectorDynamic parse(String input, ISelectorContext context) throws ReflectiveOperationException {
            return this.parse(input, context, this.mdParseString);
        }
        
        ITargetSelectorDynamic parse(IAnnotationHandle input, ISelectorContext context) throws ReflectiveOperationException {
            return this.parse(input, context, this.mdParseAnnotation);
        }
        
        ITargetSelectorDynamic parse(Object input, ISelectorContext context, Method parseMethod) throws ReflectiveOperationException {
            try {
                return (ITargetSelectorDynamic)parseMethod.invoke(null, input, context);
            } catch (InvocationTargetException itex) {
                Throwable cause = itex.getCause();
                if (cause instanceof MixinException) {
                    throw (MixinException)cause;
                }
                Throwable ex = cause != null ? cause : itex;
                throw new MixinError("Error parsing dynamic target selector [" + this.type.getName() + "] for " + context, ex);
            }
        }
    }
    
    /**
     * Regex for dynamic selector ids
     */
    private static final String DYNAMIC_SELECTOR_ID = "[a-z]+(:[a-z]+)?";
    
    /**
     * Pattern for matching dynamic selectors
     */
    private static final Pattern PATTERN_DYNAMIC = Pattern.compile("(?i)^\\x40(" + TargetSelector.DYNAMIC_SELECTOR_ID + ")(\\((.*)\\))?$");
    
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
            entry = new DynamicSelectorEntry(namespace.toLowerCase(Locale.ROOT), selectorId.value().toLowerCase(Locale.ROOT), type);
        } catch (NoSuchMethodException ex) {
            throw new MixinError("Dynamic target selector class " + type.getName() + " does not contain a valid parse method");
        }
        
        String code = entry.getCode();
        if (!Pattern.matches(TargetSelector.DYNAMIC_SELECTOR_ID, code)) {
            throw new IllegalArgumentException("Dynamic target selector class " + type
                    + " has an invalid id. Only alpha characters can be used in selector ids and namespaces");
        }
        
        DynamicSelectorEntry existing = TargetSelector.dynamicSelectors.get(code);
        if (existing != null) { // && !existing.type.equals(type)) {
            MessageRouter.getMessager().printMessage(Kind.WARNING, String.format("Overriding target selector for @%s with %s (previously %s)",
                    code, type.getName(), existing.type.getName()));
        } else {
            MessageRouter.getMessager().printMessage(Kind.OTHER, String.format("Registering new target selector for @%s with %s",
                    code, type.getName()));
        }
        
        TargetSelector.dynamicSelectors.put(code, entry);
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
        TargetSelector.dynamicSelectors.put(entry.id, entry);
        TargetSelector.dynamicSelectors.put("mixin:" + entry.id, entry);
    }
    
    /**
     * Parse a target selector from the supplied annotation and perform
     * validation
     * 
     * @param annotation Annotation to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parseAndValidate(IAnnotationHandle annotation, ISelectorContext context) throws InvalidSelectorException {
        return TargetSelector.parse(annotation, context).validate();
    }
    
    /**
     * Parse a target selector from a string and perform validation
     * 
     * @param string String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parseAndValidate(String string, ISelectorContext context) throws InvalidSelectorException {
        return TargetSelector.parse(string, context).validate();
    }
    
    /**
     * Parse a collection of target selector representations (strings,
     * annotations, class literals) into selectors.
     * 
     * @param selectors Selectors to parse
     * @param context Selection context
     * @return parsed collection of selectors, uses LinkedHashSet to preserve
     *      parse ordering
     */
    public static Set<ITargetSelector> parseAndValidate(Iterable<?> selectors, ISelectorContext context) throws InvalidSelectorException {
        Set<ITargetSelector> parsed = TargetSelector.parse(selectors, context, new LinkedHashSet<ITargetSelector>());
        for (ITargetSelector selector : parsed) {
            selector.validate();
        }
        return parsed;
    }
    
    /**
     * Parse a collection of target selector representations (strings,
     * annotations, class literals) into selectors.
     * 
     * @param selectors Selectors to parse
     * @param context Selection context
     * @return parsed collection of selectors, uses LinkedHashSet to preserve
     *      parse ordering
     */
    public static Set<ITargetSelector> parse(Iterable<?> selectors, ISelectorContext context) {
        return TargetSelector.parse(selectors, context, new LinkedHashSet<ITargetSelector>());
    }
    
    /**
     * Parse a collection of target selector representations (strings,
     * annotations, class literals) into selectors and store them in the
     * provided collection.
     * 
     * @param selectors Selectors to parse
     * @param context Selection context
     * @param parsed Collection to add parsed selectors to, initialised as a
     *      LinkedHashSet if null
     * @return the same collection passed in via the <tt>parsed</tt> parameter,
     *      for convenience
     */
    public static Set<ITargetSelector> parse(Iterable<?> selectors, ISelectorContext context, Set<ITargetSelector> parsed) {
        if (parsed == null) {
            parsed = new LinkedHashSet<ITargetSelector>();
        }
        if (selectors != null) {
            for (Object selector : selectors) {
                if (selector instanceof IAnnotationHandle) {
                    parsed.add(TargetSelector.parse((IAnnotationHandle)selector, context));
                } else if (selector instanceof AnnotationNode) {
                    parsed.add(TargetSelector.parse(Annotations.handleOf(selector), context));
                } else if (selector instanceof String) {
                    parsed.add(TargetSelector.parse((String)selector, context));
                } else if (selector instanceof Class) {
                    String desc = Type.getType((Class<?>)selector).getDescriptor();
                    parsed.add(TargetSelector.parse(desc, context));
                } else if (selector != null) {
                    parsed.add(TargetSelector.parse(selector.toString(), context));
                }
            }
        }
        return parsed;
    }
    
    /**
     * Parse a target selector from the supplied annotation
     * 
     * @param annotation String to parse target selector from
     * @param context Context to use for reference mapping
     * @return parsed target selector
     */
    public static ITargetSelector parse(IAnnotationHandle annotation, ISelectorContext context) {
        for (DynamicSelectorEntry entry : TargetSelector.dynamicSelectors.values()) {
            if (entry.annotation != null && Annotations.getDesc(entry.annotation).equals(annotation.getDesc())) {
                try {
                    return entry.parse(annotation, context);
                } catch (ReflectiveOperationException ex) {
                    return new InvalidSelector(ex.getCause());
                } catch (Exception ex) {
                    return new InvalidSelector(ex);
                }
            }
        }
        
        return new InvalidSelector(new InvalidSelectorException("Dynamic selector for annotation " + annotation + " is not registered."));
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
     * @param selector Target selector
     * @param nodes Node collection to enumerate
     * @param <TNode> Node type
     * @return query result
     */
    public static <TNode> Result<TNode> run(ITargetSelector selector, Iterable<ElementNode<TNode>> nodes) {
        List<ElementNode<TNode>> candidates = new ArrayList<ElementNode<TNode>>();
        ElementNode<TNode> exactMatch = TargetSelector.runSelector(selector, nodes, candidates);
        return new Result<TNode>(exactMatch, candidates);
    }
    
    /**
     * Run query on supplied target nodes
     * 
     * @param selector Target selector
     * @param nodes Node collection to enumerate
     * @param <TNode> Node type
     * @return query result
     */
    public static <TNode> Result<TNode> run(Iterable<ITargetSelector> selector, Iterable<ElementNode<TNode>> nodes) {
        ElementNode<TNode> exactMatch = null;
        List<ElementNode<TNode>> candidates = new ArrayList<ElementNode<TNode>>();
        
        for (ITargetSelector target : selector) {
            ElementNode<TNode> selectorExactMatch = TargetSelector.runSelector(target, nodes, candidates);
            if (exactMatch == null) {
                exactMatch = selectorExactMatch;
            }
        }
        
        return new Result<TNode>(exactMatch, candidates);
    }

    private static <TNode> ElementNode<TNode> runSelector(ITargetSelector selector, Iterable<ElementNode<TNode>> nodes,
            List<ElementNode<TNode>> candidates) {
        int matchCount = 0;
        ElementNode<TNode> exactMatch = null;
        for (Iterator<ElementNode<TNode>> iterator = nodes.iterator(); iterator.hasNext();) {
            ElementNode<TNode> element = iterator.next();
            MatchResult match = selector.match(element);
            if (match.isMatch()) {
                matchCount++;
                if (matchCount > selector.getMaxMatchCount()) {
                    break;
                }
                if (!candidates.contains(element)) {
                    candidates.add(element);
                }
                if (exactMatch == null && match.isExactMatch()) {
                    exactMatch = element;
                }
            }
        }
        
        if (matchCount < selector.getMinMatchCount()) {
            throw new SelectorConstraintException(selector, String.format("%s did not match the required number of targets (required=%d, matched=%d)",
                    selector, selector.getMinMatchCount(), matchCount));
        }

        return exactMatch;
    }

}
