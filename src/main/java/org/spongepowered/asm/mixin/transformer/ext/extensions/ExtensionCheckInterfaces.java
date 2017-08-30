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
package org.spongepowered.asm.mixin.transformer.ext.extensions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Option;
import org.spongepowered.asm.mixin.transformer.ClassInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Method;
import org.spongepowered.asm.mixin.transformer.ClassInfo.SearchType;
import org.spongepowered.asm.mixin.transformer.ClassInfo.Traversal;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.PrettyPrinter;
import org.spongepowered.asm.util.SignaturePrinter;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;

/**
 * Checks whether interfaces declared on mixin target classes are actually fully
 * implemented and generates reports to the console and to files on disk
 */
public class ExtensionCheckInterfaces implements IExtension {

    private static final String AUDIT_DIR = "audit";
    private static final String IMPL_REPORT_FILENAME = "mixin_implementation_report";
    private static final String IMPL_REPORT_CSV_FILENAME = ExtensionCheckInterfaces.IMPL_REPORT_FILENAME + ".csv";
    private static final String IMPL_REPORT_TXT_FILENAME = ExtensionCheckInterfaces.IMPL_REPORT_FILENAME + ".txt";

    private static final Logger logger = LogManager.getLogger("mixin");

    /**
     * CSV Report file
     */
    private final File csv;

    /**
     * Text Report file
     */
    private final File report;

    /**
     * Methods from interfaces that are already in the class before mixins are
     * applied.
     */
    private final Multimap<ClassInfo, Method> interfaceMethods = HashMultimap.create();
    
    /**
     * Strict mode 
     */
    private boolean strict;

    public ExtensionCheckInterfaces() {
        File debugOutputFolder = new File(Constants.DEBUG_OUTPUT_DIR, ExtensionCheckInterfaces.AUDIT_DIR);
        debugOutputFolder.mkdirs();
        this.csv = new File(debugOutputFolder, ExtensionCheckInterfaces.IMPL_REPORT_CSV_FILENAME);
        this.report = new File(debugOutputFolder, ExtensionCheckInterfaces.IMPL_REPORT_TXT_FILENAME);

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
     * @see org.spongepowered.asm.mixin.transformer.ext.IExtension#checkActive(
     *      org.spongepowered.asm.mixin.MixinEnvironment)
     */
    @Override
    public boolean checkActive(MixinEnvironment environment) {
        this.strict = environment.getOption(Option.CHECK_IMPLEMENTS_STRICT);
        return environment.getOption(Option.CHECK_IMPLEMENTS);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *     #preApply(org.spongepowered.asm.mixin.transformer.TargetClassContext)
     */
    @Override
    public void preApply(ITargetClassContext context) {
        ClassInfo targetClassInfo = context.getClassInfo();
        for (Method m : targetClassInfo.getInterfaceMethods(false)) {
            this.interfaceMethods.put(targetClassInfo, m);
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.IMixinTransformerModule
     *    #postApply(org.spongepowered.asm.mixin.transformer.TargetClassContext)
     */
    @Override
    public void postApply(ITargetClassContext context) {
        ClassInfo targetClassInfo = context.getClassInfo();
        
        // If the target is abstract and strict mode is not enabled, skip this class
        if (targetClassInfo.isAbstract() && !this.strict) {
            ExtensionCheckInterfaces.logger.info("{} is skipping abstract target {}", this.getClass().getSimpleName(), context);
            return;
        }

        String className = targetClassInfo.getName().replace('/', '.');
        int missingMethodCount = 0;
        PrettyPrinter printer = new PrettyPrinter();

        printer.add("Class: %s", className).hr();
        printer.add("%-32s %-47s  %s", "Return Type", "Missing Method", "From Interface").hr();

        Set<Method> interfaceMethods = targetClassInfo.getInterfaceMethods(true);
        Set<Method> implementedMethods = new HashSet<Method>(targetClassInfo.getSuperClass().getInterfaceMethods(true));
        implementedMethods.addAll(this.interfaceMethods.removeAll(targetClassInfo));

        for (Method method : interfaceMethods) {
            Method found = targetClassInfo.findMethodInHierarchy(method.getName(), method.getDesc(), SearchType.ALL_CLASSES, Traversal.ALL);

            // If method IS found and IS implemented, then do nothing (don't print an error)
            if (found != null && !found.isAbstract()) {
                continue;
            }

            // Don't blame the subclass for not implementing methods that it does not need to implement.
            if (implementedMethods.contains(method)) {
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
    
    /* (non-Javadoc)
     * @see org.spongepowered.asm.mixin.transformer.ext.IExtension#export(
     *      org.spongepowered.asm.mixin.MixinEnvironment, java.lang.String,
     *      boolean, byte[])
     */
    @Override
    public void export(MixinEnvironment env, String name, boolean force, byte[] bytes) {
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
