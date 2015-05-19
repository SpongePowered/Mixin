/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered.org <http://www.spongepowered.org>
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
package org.spongepowered.asm.mixin.transformer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SortedSet;

import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Traversal;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

import com.google.common.base.Charsets;
import com.google.common.io.Files;


/**
 * Checks whether interfaces declared on mixin target classes are actually fully
 * implemented and generates reports to the console and to files on disk
 */
public class MixinTransformerModuleInterfaceChecker implements IMixinTransformerModule {
    
    /**
     * CSV Report file
     */
    private final File csv;
    
    /**
     * Text Report file
     */
    private final File report;
    
    public MixinTransformerModuleInterfaceChecker() {
        File debugOutputFolder = new File(Constants.DEBUG_OUTPUT_PATH);
        debugOutputFolder.mkdirs();
        this.csv = new File(debugOutputFolder, "mixin_implementation_report.csv");
        this.report = new File(debugOutputFolder, "mixin_implementation_report.txt");
        
        try {
            Files.write("Class,Method,Signature,Interface\n", this.csv, Charsets.ISO_8859_1);
        } catch (IOException ex) {
            // well this sucks
        }
        
        try {
            String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            Files.write("Mixin Implementation Report generated on " + dateTime + "\n", this.report, Charsets.ISO_8859_1);
        } catch (IOException ex) {
            // hmm :(
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *      #preTransform(java.lang.String, org.objectweb.asm.tree.ClassNode,
     *      java.util.SortedSet)
     */
    @Override
    public void preApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *      #postTransform(java.lang.String, org.objectweb.asm.tree.ClassNode,
     *      java.util.SortedSet)
     */
    @Override
    public void postApply(String transformedName, ClassNode targetClass, SortedSet<MixinInfo> mixins) {
        ClassInfo targetClassInfo = ClassInfo.forName(targetClass.name);

        if (targetClassInfo.isAbstract()) {
            return;
        }

        String className = targetClassInfo.getName().replace('/', '.');
        int missingMethodCount = 0;
        PrettyPrinter printer = new PrettyPrinter();
        
        printer.add("Class: %s", className).hr();
        printer.add("%-32s %-47s  %s", "Return Type", "Missing Method", "From Interface").hr();

        for (Method method : targetClassInfo.getInterfaceMethods()) {
            Method found = targetClassInfo.findMethodInHierarchy(method.getName(), method.getDesc(), true, Traversal.ALL);

            if (found != null) {
                continue;
            }

            if (missingMethodCount > 0) {
                printer.add();
            }

            SignaturePrinter signaturePrinter = new SignaturePrinter(method.getName(), method.getDesc()).setModifiers("");
            String iface = method.getOwner().getName().replace('/', '.');
            missingMethodCount++;
            printer.add("%-32s%s", signaturePrinter.getReturnType(), signaturePrinter);
            printer.add("%-80s  %s", "", iface);
            
            this.appendToCSVReport(className, method, iface);
        }

        if (missingMethodCount > 0) {
            printer.hr().add("%82s%s: %d", "", "Total unimplemented", missingMethodCount);
            printer.print(System.err);
            this.appendToTextReport(printer);
        }
    }

    private void appendToCSVReport(String className, Method method, String iface) {
        try {
            Files.append(String.format("%s,%s,%s,%s\n", className, method.getName(), method.getDesc(), iface), this.csv, Charsets.ISO_8859_1);
        } catch (IOException ex) {
            // Not the end of the world
        }
    }

    private void appendToTextReport(PrettyPrinter printer) {
        FileOutputStream fos = null;
        
        try {
            fos = new FileOutputStream(this.report, true);
            PrintStream stream = new PrintStream(fos);
            stream.print("\n");
            printer.print(stream);
        } catch (Exception ex) {
            // never mind
        } finally {
            IOUtils.closeQuietly(fos);
        }
    }
}
