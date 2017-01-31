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

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.signature.SignatureReader;
import org.spongepowered.asm.lib.signature.SignatureVisitor;
import org.spongepowered.asm.lib.signature.SignatureWriter;
import org.spongepowered.asm.lib.tree.ClassNode;

/**
 * Represents an object-oriented view of a generic class signature. We use ASM's
 * {@link SignatureVisitor} to walk over an incoming signature in order to parse
 * out our internal tree. This is done so that incoming signatures from mixins
 * can be merged into the target class.
 */
public class ClassSignature {
    
    protected static final String OBJECT = "java/lang/Object";
    
    /**
     * Lazy evaluation support for class signatures
     */
    static class Lazy extends ClassSignature {
        
        private final String sig;
        
        private ClassSignature generated;

        Lazy(String sig) {
            this.sig = sig;
        }
        
        @Override
        public ClassSignature wake() {
            if (this.generated == null) {
                this.generated = ClassSignature.of(this.sig);
            }
            return this.generated;
        }

    }
    
    /**
     * A renamable type variable handle
     */
    static class TypeVar implements Comparable<TypeVar> {
    
        /**
         * Original type var name
         */
        private final String originalName;
        
        /**
         * Current name (conformed)
         */
        private String currentName;
        
        TypeVar(String name) {
            this.currentName = this.originalName = name;
        }

        @Override
        public int compareTo(TypeVar other) {
            return this.currentName.compareTo(other.currentName);
        }
        
        @Override
        public String toString() {
            return this.currentName;
        }
        
        String getOriginalName() {
            return this.originalName;
        }
        
        void rename(String name) {
            this.currentName = name;
        }
        
        public boolean matches(String originalName) {
            return this.originalName.equals(originalName);
        }
        
        @Override
        public boolean equals(Object obj) {
            return this.currentName.equals(obj);
        }
        
        @Override
        public int hashCode() {
            return this.currentName.hashCode();
        }
        
    }

    /**
     * Interface for signature tokens
     */
    static interface IToken {

        /**
         * Available wildcard modifiers
         */
        static final String WILDCARDS = "+-";
        
        /**
         * Return this token as a string representation of its type
         */
        public abstract String asType();
        
        /**
         * Return this token as a string representation of a bound
         */
        public abstract String asBound();

        /**
         * Return this token as a hard token object
         */
        public abstract Token asToken();
        
        /**
         * Set the arrayness status of this token, logical OR is applied between
         * the current internal arrayness flag and the incoming flag.
         * 
         * @param array arrayness flag
         * @return this token
         */
        public abstract IToken setArray(boolean array);
        
        /**
         * Set the wildcard on this token, only accepted if one of the valid
         * {@link #WILDCARDS}
         * 
         * @param wildcard wildcard to set
         * @return this token
         */
        public abstract IToken setWildcard(char wildcard);
        
    }
    
    /**
     * A token in the signature, can represent a type, a collection of bounds,
     * or a combination of the two. Includes internal arrayness and wildcard
     * flags as well as generic type arguments.
     */
    static class Token implements IToken {
        
        /**
         * Valid symbols, supports the {@link IToken#WILDCARDS} as well as
         * <tt>*</tt> for unbounded type args. 
         */
        static final String SYMBOLS = IToken.WILDCARDS + "*";
        
        /**
         * True if this token represents an inner type (output is not enclosed
         * in <tt>L;</tt>
         */
        private final boolean inner;
        
        /**
         * True if this token represents an array type
         */
        private boolean array;
        
        /**
         * Symbol (wildcard, unbound or base type)
         */
        private char symbol = 0;
        
        /**
         * Type descriptor, can be null if this type is only a symbol
         */
        private String type;
        
        /**
         * Class bounds on this token, null if no bounds have been set
         */
        private List<Token> classBound;
        
        /**
         * Interface bounds on this token, null if no bounds have been set
         */
        private List<Token> ifaceBound;
        
        /**
         * Generic signature components on this token, null if this is a raw
         * type token
         */
        private List<IToken> signature;
        
        /**
         * Suffix elements on this token, currently only used to append inner
         * class tokens to this token
         */
        private List<IToken> suffix;
        
        /**
         * The last suffix element added. When a suffix element is added all
         * calls to <tt>add...</tt> are delegated to the tail element.
         */
        private Token tail;
        
        Token() {
            this(false);
        }
        
        Token(String type) {
            this(type, false);
        }
        
        Token(char symbol) {
            this();
            this.symbol = symbol;
        }
        
        Token(boolean inner) {
            this(null, inner);
        }
        
        Token(String type, boolean inner) {
            this.inner = inner;
            this.type = type;
        }
        
        Token setSymbol(char symbol) {
            if (this.symbol == 0 && Token.SYMBOLS.indexOf(symbol) > -1) {
                this.symbol = symbol;
            }
            return this;
        }
        
        Token setType(String type) {
            if (this.type == null) {
                this.type = type;
            }
            return this;
        }
        
        boolean hasClassBound() {
            return this.classBound != null;
        }
        
        boolean hasInterfaceBound() {
            return this.ifaceBound != null;
        }
        
        @Override
        public IToken setArray(boolean array) {
            this.array |= array;
            return this;
        }
        
        @Override
        public IToken setWildcard(char wildcard) {
            if (IToken.WILDCARDS.indexOf(wildcard) == -1) {
                return this;
            }
            return this.setSymbol(wildcard);
        }
        
        private List<Token> getClassBound() {
            if (this.classBound == null) {
                this.classBound = new ArrayList<Token>();
            }
            return this.classBound;
        }
        
        private List<Token> getIfaceBound() {
            if (this.ifaceBound == null) {
                this.ifaceBound = new ArrayList<Token>();
            }
            return this.ifaceBound;
        }
        
        private List<IToken> getSignature() {
            if (this.signature == null) {
                this.signature = new ArrayList<IToken>();
            }
            return this.signature;
        }
        
        private List<IToken> getSuffix() {
            if (this.suffix == null) {
                this.suffix = new ArrayList<IToken>();
            }
            return this.suffix;
        }
        
        /**
         * Add a type argument to this symbol
         * 
         * @param symbol argument to add
         * @return new token
         */
        IToken addTypeArgument(char symbol) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(symbol);
            }
            
            Token token = new Token(symbol);
            this.getSignature().add(token);
            return token;
        }

        /**
         * Add a type argument to this symbol
         * 
         * @param name argument to add
         * @return new token
         */
        IToken addTypeArgument(String name) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(name);
            }
            
            Token token = new Token(name);
            this.getSignature().add(token);
            return token;
        }
        
        /**
         * Add a type argument to this symbol
         * 
         * @param token argument to add
         * @return new token
         */
        IToken addTypeArgument(Token token) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(token);
            }
            
            this.getSignature().add(token);
            return token;
        }
        
        /**
         * Add a type argument to this symbol
         * 
         * @param token argument to add
         * @return new token
         */
        IToken addTypeArgument(TokenHandle token) {
            if (this.tail != null) {
                return this.tail.addTypeArgument(token);
            }
            
            TokenHandle handle = token.clone();
            this.getSignature().add(handle);
            return handle;
        }
        
        /**
         * Add a class or interface bound to this symbol
         * 
         * @param bound bound specifier
         * @param classBound true to add a class bound, false to add an
         *      interface bound
         * @return new token
         */
        Token addBound(String bound, boolean classBound) {
            if (classBound) {
                return this.addClassBound(bound);
            }
            
            return this.addInterfaceBound(bound);
        }

        /**
         * Add a class bound to this symbol
         * 
         * @param bound bound specifier
         * @return new token
         */
        Token addClassBound(String bound) {
            Token token = new Token(bound);
            this.getClassBound().add(token);
            return token;
        }

        /**
         * Add a class bound to this symbol
         * 
         * @param bound bound specifier
         * @return new token
         */
        Token addInterfaceBound(String bound) {
            Token token = new Token(bound);
            this.getIfaceBound().add(token);
            return token;
        }

        /**
         * Add an inner class suffix to this symbol
         * 
         * @param name inner class name
         * @return new tail token
         */
        Token addInnerClass(String name) {
            this.tail = new Token(name, true);
            this.getSuffix().add(this.tail);
            return this.tail;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.asType();
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken#asBound()
         */
        @Override
        public String asBound() {
            StringBuilder sb = new StringBuilder();
            
            if (this.type != null) {
                sb.append(this.type);
            }
            
            if (this.classBound != null) {
                for (Token token : this.classBound) {
                    sb.append(token.asType());
                }
            }
            
            if (this.ifaceBound != null) {
                for (Token token : this.ifaceBound) {
                    sb.append(':').append(token.asType());
                }
            }
            
            return sb.toString();
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken#asType()
         */
        @Override
        public String asType() {
            return this.asType(false);
        }
        
        /**
         * Convert this token to a string representation of its type, optionally
         * generate only a raw representation (no generic signature)
         * 
         * @param raw if true, only emits the raw type
         * @return string representation of this token
         */
        public String asType(boolean raw) {
            StringBuilder sb = new StringBuilder();
            
            if (this.array) {
                sb.append('[');
            }
            
            if (this.symbol != 0) {
                sb.append(this.symbol);
            }
            
            if (this.type == null) {
                return sb.toString();
            }

            if (!this.inner) {
                sb.append('L');
            }
            
            sb.append(this.type);
            
            if (!raw) {
                if (this.signature != null) {
                    sb.append('<');
                    for (IToken token : this.signature) {
                        sb.append(token.asType());
                    }
                    sb.append('>');
                }
                
                if (this.suffix != null) {
                    for (IToken token : this.suffix) {
                        sb.append('.').append(token.asType());
                    }
                }
            }
            
            if (!this.inner) {
                sb.append(';');
            }
            
            return sb.toString();
        }
        
        boolean isRaw() {
            return this.signature == null;
        }
        
        String getClassType() {
            return this.type != null ? this.type : ClassSignature.OBJECT;
        }

        @Override
        public Token asToken() {
            return this;
        }

    }
    
    /**
     * TokenHandle is used to provide indirection to {@link Token}, primarily
     * for storing mappings of TypeVars to tokens but also to store Tokens as
     * type parameters on other Tokens where we may need to include a wildcard
     * or array marker without altering the underlying Token.
     */
    class TokenHandle implements IToken {
        
        /**
         * Inner token, never null
         */
        final Token token;
        
        /**
         * True if this handle is an array type
         */
        boolean array;
        
        /**
         * Wildcard for this handle
         */
        char wildcard;
        
        TokenHandle() {
            this(new Token());
        }

        TokenHandle(Token token) {
            this.token = token;
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken
         *      #setArray(boolean)
         */
        @Override
        public IToken setArray(boolean array) {
            this.array |= array;
            return this;
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken
         *      #setWildcard(char)
         */
        @Override
        public IToken setWildcard(char wildcard) {
            if (IToken.WILDCARDS.indexOf(wildcard) > -1) {
                this.wildcard = wildcard;
            }
            return this;
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken#asBound()
         */
        @Override
        public String asBound() {
            return this.token.asBound();
        }

        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken#asType()
         */
        @Override
        public String asType() {
            StringBuilder sb = new StringBuilder();
            
            if (this.wildcard > 0) {
                sb.append(this.wildcard);
            }
            
            if (this.array) {
                sb.append('[');
            }
            
            return sb.append(ClassSignature.this.getTypeVar(this)).toString();
        }
        
        /* (non-Javadoc)
         * @see org.spongepowered.asm.util.ClassSignature.IToken#asToken()
         */
        @Override
        public Token asToken() {
            return this.token;
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return this.token.toString();
        }
        
        /* (non-Javadoc)
         * @see java.lang.Object#clone()
         */
        @Override
        public TokenHandle clone() {
            return new TokenHandle(this.token);
        }
        
    }
    
    /**
     * Base visitor for parsing signatures using {@link SignatureReader}, a
     * reference to instances of this class are deliberately not maintained in
     * order to free the memory consumed by the visitors after parsing.
     */
    class SignatureParser extends SignatureVisitor {
        
        /**
         * Base for the various element visitors used
         */
        abstract class SignatureElement extends SignatureVisitor {
            
            public SignatureElement() {
                super(Opcodes.ASM5);
            }
            
        }
    
        /**
         * Base class for elements which propagate information through to an
         * underlying token
         */
        abstract class TokenElement extends SignatureElement {
            
            /**
             * The token for this element
             */
            protected Token token;
            
            /**
             * True if the next element to be appended is an array element
             */
            private boolean array;
    
            public Token getToken() {
                if (this.token == null) {
                    this.token = new Token();
                }
                return this.token;
            }
            
            protected void setArray() {
                this.array = true;
            }
    
            private boolean getArray() {
                boolean array = this.array;
                this.array = false;
                return array;
            }
            
            @Override
            public void visitClassType(String name) {
                this.getToken().setType(name);
            }
            
            @Override
            public SignatureVisitor visitClassBound() {
                this.getToken();
                return new BoundElement(this, true);
            }
            
            @Override
            public SignatureVisitor visitInterfaceBound() {
                this.getToken();
                return new BoundElement(this, false);
            }
    
            @Override
            public void visitInnerClassType(String name) {
                this.token.addInnerClass(name);
            }
            
            @Override
            public SignatureVisitor visitArrayType() {
                this.setArray();
                return this;
            }
    
            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return new TypeArgElement(this, wildcard);
            }
    
            Token addTypeArgument() {
                return this.token.addTypeArgument('*').asToken();
            }
            
            IToken addTypeArgument(char symbol) {
                return this.token.addTypeArgument(symbol).setArray(this.getArray());
            }
            
            IToken addTypeArgument(String name) {
                return this.token.addTypeArgument(name).setArray(this.getArray());
            }
    
            IToken addTypeArgument(Token token) {
                return this.token.addTypeArgument(token).setArray(this.getArray());
            }
            
            IToken addTypeArgument(TokenHandle token) {
                return this.token.addTypeArgument(token).setArray(this.getArray());
            }
            
        }
        
        /**
         * A formal type parameter
         */
        class FormalParamElement extends TokenElement {
            
            /**
             * Handle to the underlying token for this type
             */
            private final TokenHandle handle;
            
            FormalParamElement(String param) {
                this.handle = ClassSignature.this.getType(param);
                this.token = this.handle.asToken();
            }
            
        }
        
        /**
         * A type argument
         */
        class TypeArgElement extends TokenElement {
            
            /**
             * Element that this is an argument for
             */
            private final TokenElement type;
            
            /**
             * True if the next token appended has a wildcard qualifier
             */
            private final char wildcard;
    
            TypeArgElement(TokenElement type, char wildcard) {
                this.type = type;
                this.wildcard = wildcard;
            }
            
            @Override
            public SignatureVisitor visitArrayType() {
                this.type.setArray();
                return this;
            }
            
            @Override
            public void visitBaseType(char descriptor) {
                this.token = this.type.addTypeArgument(descriptor).asToken();
            }
            
            @Override
            public void visitTypeVariable(String name) {
                TokenHandle token = ClassSignature.this.getType(name);
                this.token = this.type.addTypeArgument(token).setWildcard(this.wildcard).asToken();
            }
            
            @Override
            public void visitClassType(String name) {
                this.token = this.type.addTypeArgument(name).setWildcard(this.wildcard).asToken();
            }
            
            @Override
            public void visitTypeArgument() {
                this.token.addTypeArgument('*');
            }
            
            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return new TypeArgElement(this, wildcard);
            }
            
            @Override
            public void visitEnd() {
            }
            
        }
    
        /**
         * A class (or interface) bound element
         */
        class BoundElement extends TokenElement {
            
            /**
             * The element which owns this bound
             */
            private final TokenElement type;
            
            /**
             * True if this element represents a class bound, false if interface
             */
            private final boolean classBound;
    
            BoundElement(TokenElement type, boolean classBound) {
                this.type = type;
                this.classBound = classBound;
            }
            
            @Override
            public void visitClassType(String name) {
                this.token = this.type.token.addBound(name, this.classBound);
            }
            
            @Override
            public void visitTypeArgument() {
                this.token.addTypeArgument('*');
            }
            
            @Override
            public SignatureVisitor visitTypeArgument(char wildcard) {
                return new TypeArgElement(this, wildcard);
            }
            
        }
        
        /**
         * Superclass element
         */
        class SuperClassElement extends TokenElement {
            
            @Override
            public void visitEnd() {
                ClassSignature.this.setSuperClass(this.token);
            }
            
        }
    
        /**
         * Interface element
         */
        class InterfaceElement extends TokenElement {
            
            @Override
            public void visitEnd() {
                ClassSignature.this.addInterface(this.token);
            }
            
        }
        
        /**
         * The params element. For some reason {@link #visitFormalTypeParameter}
         * doesn't return a visitor so we need to proxy the relevant calls
         * through to the current params element.
         */
        private FormalParamElement param;

        SignatureParser() {
            super(Opcodes.ASM5);
        }
        
        @Override
        public void visitFormalTypeParameter(String name) {
            this.param = new FormalParamElement(name);
        }

        @Override
        public SignatureVisitor visitClassBound() {
            return this.param.visitClassBound();
        }

        @Override
        public SignatureVisitor visitInterfaceBound() {
            return this.param.visitInterfaceBound();
        }

        @Override
        public SignatureVisitor visitSuperclass() {
            return new SuperClassElement();
        }

        @Override
        public SignatureVisitor visitInterface() {
            return new InterfaceElement();
        }

    }
    
    /**
     * Remapper
     */
    class SignatureRemapper extends SignatureWriter {
        
        private final Set<String> localTypeVars = new HashSet<String>();
        
        @Override
        public void visitFormalTypeParameter(String name) {
            this.localTypeVars.add(name);
            super.visitFormalTypeParameter(name);
        }
        
        @Override
        public void visitTypeVariable(String name) {
            if (!this.localTypeVars.contains(name)) {
                TypeVar typeVar = ClassSignature.this.getTypeVar(name);
                if (typeVar != null) {
                    super.visitTypeVariable(typeVar.toString());
                    return;
                }
            }
            super.visitTypeVariable(name);
        }
        
    }
    
    /**
     * Type vars defined in this signature, represents the list of formal
     * parameters, ordered
     */
    private final Map<TypeVar, TokenHandle> types = new LinkedHashMap<TypeVar, TokenHandle>();
    
    /**
     * The superclass defined in the signature
     */
    private Token superClass = new Token(ClassSignature.OBJECT);
    
    /**
     * Interfaces mentioned in the signature
     */
    private final List<Token> interfaces = new ArrayList<Token>();
    
    /**
     * Interfaces added manually
     */
    private final Deque<String> rawInterfaces = new LinkedList<String>();
    
    ClassSignature() {
    }
    
    /**
     * Use a {@link SignatureReader} to visit the supplied signature and build
     * the signature model
     * 
     * @param signature signature string to parse
     * @return fluent interface
     */
    private ClassSignature read(String signature) {
        if (signature != null) {
            try {
                new SignatureReader(signature).accept(new SignatureParser());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return this;
    }
    
    /**
     * Get the type var for the specified var name
     * 
     * @param varName type var to lookup
     * @return type var for the supplied type var name
     */
    protected TypeVar getTypeVar(String varName) {
        for (TypeVar typeVar : this.types.keySet()) {
            if (typeVar.matches(varName)) {
                return typeVar;
            }
        }
        return null;
    }

    /**
     * Get the token for the specified type var name, creating it if necessary
     * 
     * @param varName type var to lookup
     * @return type token for the supplied type var name
     */
    protected TokenHandle getType(String varName) {
        for (TypeVar typeVar : this.types.keySet()) {
            if (typeVar.matches(varName)) {
                return this.types.get(typeVar);
            }
        }
        
        TokenHandle handle = new TokenHandle();
        this.types.put(new TypeVar(varName), handle);
        return handle;
    }

    /**
     * Get the type var matching the supplied type token, or the raw token type
     * if no mapping exists for the supplied token handle
     * 
     * @param handle type token handle to lookup
     * @return type var (with prefix and suffix) or raw type name
     */
    protected String getTypeVar(TokenHandle handle) {
        for (Entry<TypeVar, TokenHandle> type : this.types.entrySet()) {
            TypeVar typeVar = type.getKey();
            TokenHandle typeHandle = type.getValue();
            if (handle == typeHandle || handle.asToken() == typeHandle.asToken()) {
                return "T" + typeVar + ";";
            }
        }
        return handle.token.asType();
    }
    
    /**
     * Add a type var to this signature, the type var must not exist 
     * 
     * @param typeVar type var to add
     * @param handle type var's type token
     * @throws IllegalArgumentException if the specified type var already exists
     */
    protected void addTypeVar(TypeVar typeVar, TokenHandle handle) throws IllegalArgumentException {
        if (this.types.containsKey(typeVar)) {
            throw new IllegalArgumentException("TypeVar " + typeVar + " is already present on " + this);
        }
        
        this.types.put(typeVar, handle);
    }

    /**
     * Set the superclass for this signature
     * 
     * @param superClass super class to set
     */
    protected void setSuperClass(Token superClass) {
        this.superClass = superClass;
    }

    /**
     * Get the raw superclass type of this signature as a string
     * 
     * @return superclass type as a string
     */
    public String getSuperClass() {
        return this.superClass.asType(true);
    }

    /**
     * Add an interface to this signature
     * 
     * @param iface interface to add
     */
    protected void addInterface(Token iface) {
        if (!iface.isRaw()) {
            String raw = iface.asType(true);
            for (ListIterator<Token> iter = this.interfaces.listIterator(); iter.hasNext();) {
                Token intrface = iter.next();
                if (intrface.isRaw() && intrface.asType(true).equals(raw)) {
                    iter.set(iface);
                    return;
                }
            }
        }
        
        this.interfaces.add(iface);
    }
    
    /**
     * Add a raw interface declaration to this signature
     * 
     * @param iface interface name to add (bin format)
     */
    public void addInterface(String iface) {
        this.rawInterfaces.add(iface);
    }
    
    /**
     * Add a raw interface which was previously enqueued
     * 
     * @param iface interface to add
     */
    protected void addRawInterface(String iface) {
        Token token = new Token(iface);
        String raw = token.asType(true);
        for (Token intrface : this.interfaces) {
            if (intrface.asType(true).equals(raw)) {
                return;
            }
        }
        this.interfaces.add(token);
    }
    
    /**
     * Merges another class signature into this one. The other signature is
     * first conformed so that no formal type parameters overlap with formal
     * type parameters defined on this signature. No attempt is made to combine
     * formal type parameters, this method merely ensures that parameters do
     * not overlap.
     * 
     * @param other Class signature to merge into this one
     */
    public void merge(ClassSignature other) {
        try {
            Set<String> typeVars = new HashSet<String>();
            for (TypeVar typeVar : this.types.keySet()) {
                typeVars.add(typeVar.toString());
            }
            
            other.conform(typeVars);
        } catch (IllegalStateException ex) {
            // Oh crap, this means we couldn't conform one or more type params!
            ex.printStackTrace();
            return;
        }

        for (Entry<TypeVar, TokenHandle> type : other.types.entrySet()) {
            this.addTypeVar(type.getKey(), type.getValue());
        }
        
        for (Token iface : other.interfaces) {
            this.addInterface(iface);
        }
    }

    private void conform(Set<String> typeVars) {
        for (TypeVar typeVar : this.types.keySet()) {
            String name = this.findUniqueName(typeVar.getOriginalName(), typeVars);
            typeVar.rename(name);
            typeVars.add(name);
        }
    }
    
    /**
     * Finds a unique name for <tt>typeVar</tt> which does not exist in the
     * <tt>typeVars</tt> which is as close as possible to the original name.
     * 
     * @param typeVar type var to conform
     * @param typeVars existing type vars
     * @return new name for the type var
     */
    private String findUniqueName(String typeVar, Set<String> typeVars) {
        if (!typeVars.contains(typeVar)) {
            return typeVar;
        }
        
        if (typeVar.length() == 1) {
            String name = this.findOffsetName(typeVar.charAt(0), typeVars);
            if (name != null) {
                return name;
            }
        }

        String name = this.findOffsetName('T', typeVars, "", typeVar);
        if (name != null) {
            return name;
        }
        
        name = this.findOffsetName('T', typeVars, typeVar, "");
        if (name != null) {
            return name;
        }
        
        name = this.findOffsetName('T', typeVars, "T", typeVar);
        if (name != null) {
            return name;
        }
        
        name = this.findOffsetName('T', typeVars, "", typeVar + "Type");
        if (name != null) {
            return name;
        }
        
        throw new IllegalStateException("Failed to conform type var: " + typeVar);
    }

    /**
     * Find an offset name for a single-char typevar.
     * 
     * @param c type var
     * @param typeVars existing type var
     * @return offset type var or null if var could not be conformed
     */
    private String findOffsetName(char c, Set<String> typeVars) {
        return this.findOffsetName(c, typeVars, "", "");
    }
    
    /**
     * Find an offset name for a type var
     * 
     * @param c Char to rotate
     * @param typeVars existing vars
     * @param prefix type var prefix
     * @param suffix type var suffix
     * @return conformed var name or null if none can be found
     */
    private String findOffsetName(char c, Set<String> typeVars, String prefix, String suffix) {
        String name = String.format("%s%s%s", prefix, c, suffix);
        if (!typeVars.contains(name)) {
            return name;
        }

        if (c > 0x40 && c < 0x5B) {
            for (int s = c - 0x40; s + 0x41 != c; s = ++s % 0x1A) {
                name = String.format("%s%s%s", prefix, (char)(s + 0x41), suffix);
                if (!typeVars.contains(name)) {
                    return name;
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get a remapper for type vars in this signature
     * 
     * @return signature visitor
     */
    public SignatureVisitor getRemapper() {
        return new SignatureRemapper();
    }
    
    /**
     * Converts this signature into a string representation compatible with the
     * signature attribute of a Java class
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        while (this.rawInterfaces.size() > 0) {
            this.addRawInterface(this.rawInterfaces.remove());
        }
        
        StringBuilder sb = new StringBuilder();
        
        if (this.types.size() > 0) {
            boolean valid = false;
            StringBuilder types = new StringBuilder();
            for (Entry<TypeVar, TokenHandle> type : this.types.entrySet()) {
                String bound = type.getValue().asBound();
                if (!bound.isEmpty()) {
                    types.append(type.getKey()).append(':').append(bound);
                    valid = true;
                }
            }
            
            if (valid) {
                sb.append('<').append(types).append('>');
            }
        }
        
        sb.append(this.superClass.asType());
        
        for (Token iface : this.interfaces) {
            sb.append(iface.asType());
        }
        
        return sb.toString();
    }

    /**
     * Wake up this signature if it is lazy-loaded
     */
    public ClassSignature wake() {
        return this;
    }
    
    /**
     * Parse a generic class signature from the supplied string
     * 
     * @param signature signature string to parse
     * @return parsed signature object
     */
    public static ClassSignature of(String signature) {
        return new ClassSignature().read(signature);
    }
    
    /**
     * Parse a generic class signature from the supplied class node, uses the
     * declared signature if present, else falls back to generating a raw
     * signature from the class itself
     * 
     * @param classNode class node to parse
     * @return parsed signature
     */
    public static ClassSignature of(ClassNode classNode) {
        if (classNode.signature != null) {
            return ClassSignature.of(classNode.signature);
        }

        return ClassSignature.generate(classNode);
    }
    
    /**
     * Returns a lazy-evaluated signature object. For classes with a signature
     * present this saves having to do the parse until we actually need it. For
     * classes with no signature we just go ahead and generate it from the
     * supplied ClassNode while we have it 
     * 
     * @param classNode class node to parse
     * @return parsed signature or lazy-load handle
     */
    public static ClassSignature ofLazy(ClassNode classNode) {
        if (classNode.signature != null) {
            return new ClassSignature.Lazy(classNode.signature);
        }

        return ClassSignature.generate(classNode);
    }

    /**
     * Generate a rough (raw) signature from the supplied classnode
     * 
     * @param classNode class node to generate a signature for 
     * @return generated signature
     */
    private static ClassSignature generate(ClassNode classNode) {
        ClassSignature generated = new ClassSignature();
        generated.setSuperClass(new Token(classNode.superName != null ? classNode.superName : ClassSignature.OBJECT));
        for (String iface : classNode.interfaces) {
            generated.addInterface(new Token(iface));
        }
        return generated;
    }
    
}
