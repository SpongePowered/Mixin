package org.spongepowered.asm.mixin.build;

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.TaskProvider;

import javax.inject.Inject;

public class SyntheticModulesExtension {
    private final Project project;
    private final DirectoryProperty outputDirectory;

    @Inject
    public SyntheticModulesExtension(final ObjectFactory objects, final Project project) {
        this.project = project;
        this.outputDirectory = objects.directoryProperty();
    }

    public DirectoryProperty getOutputDirectory() {
        return this.outputDirectory;
    }

    // This is far from idiomatic gradle -- we should have a NamedDomainObjectCollection for the
    // data model, and create tasks based on the contents of that collection
    // however, this is a simple thing for internal use, so instead we just create tasks directly
    public void module(final String moduleName) {
        final String sanitizedName = moduleName.replace('.', '_');
        final TaskProvider<GenerateModule> t = this.project.getTasks().register("generateModule" + sanitizedName, GenerateModule.class, task -> {
            task.getModuleName().set(moduleName);
            task.getGeneratedJar().set(this.getOutputDirectory().file(sanitizedName + ".jar"));
        });

        this.project.getPlugins().withType(JavaPlugin.class, $ -> {
            this.project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, this.project.files(t.map(it -> it.getOutputs())));
        });
    }

}
