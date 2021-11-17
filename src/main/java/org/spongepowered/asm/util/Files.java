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

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * Utility class for file operations
 */
public final class Files {

    private Files() {
    }
    
    /**
     * Convert a URL to a file but handle a corner case with UNC paths which do
     * not parse correctly. 
     * 
     * @param url URL to convert
     * @return File
     * @throws URISyntaxException if the URL cannot be converted to a URI 
     */
    public static File toFile(URL url) throws URISyntaxException {
        return url != null ? Files.toFile(url.toURI()) : null;
    }
    
    /**
     * Convert a URI to a file but handle a corner case with UNC paths which do
     * not parse correctly. 
     * 
     * @param uri URI to convert
     * @return File
     */
    public static File toFile(URI uri) {
        if (uri == null) {
            return null;
        }
        
        if ("file".equals(uri.getScheme()) && uri.getAuthority() != null) {
            String strUri = uri.toString();
            if (strUri.startsWith("file://") && !strUri.startsWith("file:///")) {
                try {
                    uri = new URI("file:////" + strUri.substring(7));
                } catch (URISyntaxException ex) {
                    throw new IllegalArgumentException(ex.getMessage());
                }
            }
        }
        
        return new File(uri);
    }

}
