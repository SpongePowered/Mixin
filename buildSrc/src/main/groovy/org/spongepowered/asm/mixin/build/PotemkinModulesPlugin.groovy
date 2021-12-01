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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel

/**
 * Plugin which creates "potemkin" modules specified in the build script, this
 * is to facilitate <tt>import</tt> directives for modules which aren't
 * available at compile time, or rather are available but have a different name.
 * This is needed so that modules which have changed name over time (eg.
 * previously had an automatic module name but later versions have an explicit
 * module name) can be supported without things breaking at runtime.
 */
public class PotemkinModulesPlugin implements Plugin<Project> {

    @Override
    void apply(final Project project) {
        this.createExtension(project)

        // create aggregator task
        final TaskProvider<?> aggregator = this.createAggregatorTask(project.tasks)

        // setup IDE integration
        this.configureIdes(project, aggregator)
    }

    private void createExtension(final Project project) {
        def extension = project.extensions.create("potemkinModules", PotemkinModulesExtension.class, project)
        extension.outputDirectory.set(project.layout.buildDirectory.dir("generated-modules"))
    }

    private TaskProvider<?> createAggregatorTask(final TaskContainer tasks) {
        return tasks.register("generateAllPotemkinModules", task -> {
            task.dependsOn(tasks.withType(GeneratePotemkinModule.class))
        })
    }

    private void configureIdes(final Project project, final TaskProvider<?> aggregatorTask) {
        project.plugins.withType(EclipsePlugin.class, pl -> {
            project.extensions.getByType(EclipseModel.class).synchronizationTasks(aggregatorTask)
        })
    }

}
