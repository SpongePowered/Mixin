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
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.TaskProvider

import javax.inject.Inject

/**
 * Extension to allow the potemkin modules to be specified in the build script
 */
public class PotemkinModulesExtension {

    @PackageScope final Project project
    private final DirectoryProperty outputDirectory

    @Inject
    PotemkinModulesExtension(final Project project, final ObjectFactory objects) {
        this.project = project
        this.outputDirectory = objects.directoryProperty()
    }

    DirectoryProperty getOutputDirectory() {
        return this.outputDirectory
    }

    // This is far from idiomatic gradle -- we should have a NamedDomainObjectCollection for the
    // data model, and create tasks based on the contents of that collection
    // however, this is a simple thing for internal use, so instead we just create tasks directly
    void module(final String moduleName) {
        final String sanitisedName = moduleName.replace('.', '_').replaceAll('(^|_)([a-z])', { it[2].toUpperCase() })
        final TaskProvider<GeneratePotemkinModule> taskProvider = this.project.tasks.register("generatePotemkinFor${sanitisedName}", GeneratePotemkinModule.class, task -> {
            task.moduleName.set(moduleName)
            task.generatedJar.set(this.outputDirectory.file("${moduleName}-potemkin.jar"))
        })

        project.plugins.withType(JavaPlugin.class, {
            project.dependencies.add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, project.files(taskProvider.map(task -> task.outputs)))
        })
    }

}
