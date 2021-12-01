package org.spongepowered.asm.mixin.build;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.plugins.ide.eclipse.EclipsePlugin;
import org.gradle.plugins.ide.eclipse.model.EclipseModel;

public class SyntheticModulesPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        this.createExtension(project);

        // create aggregator task
        final TaskProvider<?> aggregator = this.createAggregatorTask(project.getTasks());

        // setup IDE integration
        this.configureIdes(project, aggregator);
    }

    private void createExtension(final Project project) {
        final SyntheticModulesExtension extension = project.getExtensions()
            .create("syntheticModules", SyntheticModulesExtension.class, project);

        extension.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir("generated-modules"));
    }

    private TaskProvider<?> createAggregatorTask(final TaskContainer tasks) {
        return tasks.register("generateAllSyntheticModules", task -> {
            task.dependsOn(tasks.withType(GenerateModule.class));
        });
    }

    private void configureIdes(final Project project, final TaskProvider<?> aggregatorTask) {
        project.getPlugins().withType(EclipsePlugin.class, pl -> {
            project.getExtensions().getByType(EclipseModel.class).synchronizationTasks(aggregatorTask);
        });
    }

}
