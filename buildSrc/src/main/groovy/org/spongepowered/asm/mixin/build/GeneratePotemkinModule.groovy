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
package org.spongepowered.asm.mixin.build

import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.Opcodes

import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Task which generates a "Potemkin Module" which is a jar which only contains
 * a module descriptor for the module it's pretending to be. This is just to
 * fool the compiler into thinking that the module exists without needing to
 * import the module itself.
 */
public abstract class GeneratePotemkinModule extends DefaultTask {

    /**
     * 01/01/2000 00:00:00 java 8 breaks when using 0
     */
    private static final long STABLE_TIMESTAMP = 0x386D4380

    /**
     * File property containing the path to the generated jar 
     */
    @OutputFile
    abstract RegularFileProperty getGeneratedJar()

    /**
     * Property defining the module name for which the potemkin is being
     * generated
     */
    @Input
    abstract Property<String> getModuleName()

    @TaskAction
    void generatePotemkinModule() {
        def moduleJarFile = this.generatedJar.get().asFile
        moduleJarFile.parentFile.mkdirs() // create directories if needed

        // write a jar with a single file, of the synthetic module-info
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(moduleJarFile))) {
            def entry = new ZipEntry("module-info.class")
            entry.time = STABLE_TIMESTAMP
            jos.putNextEntry(entry)
            GeneratePotemkinModule.writeModule(jos, this.moduleName.get())
            jos.closeEntry()
        }
    }

    private static void writeModule(final OutputStream os, String moduleName) {
        def cw = new ClassWriter(0)
        cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null)
        def mv = cw.visitModule(moduleName, 0, null)
        mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null)
        mv.visitEnd()
        cw.visitEnd()
        os.write(cw.toByteArray())
    }

}
