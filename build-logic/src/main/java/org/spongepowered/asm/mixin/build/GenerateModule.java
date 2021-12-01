package org.spongepowered.asm.mixin.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.ModuleVisitor;
import org.objectweb.asm.Opcodes;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public abstract class GenerateModule extends DefaultTask {

    private static final long STABLE_TIMESTAMP = 0x386D4380; // 01/01/2000 00:00:00 java 8 breaks when using 0.

    @OutputFile
    public abstract RegularFileProperty getGeneratedJar();

    @Input
    public abstract Property<String> getModuleName();

    @TaskAction
    public void generateSyntheticModule() throws IOException {
        this.getGeneratedJar().get().getAsFile().getParentFile().mkdirs(); // create directories if needed

        // write a jar with a single file, of the synthetic module-info
        try (final JarOutputStream jos = new JarOutputStream(new FileOutputStream(this.getGeneratedJar().get().getAsFile()))) {
            final ZipEntry entry = new ZipEntry("module-info.class");
            entry.setTime(STABLE_TIMESTAMP);
            jos.putNextEntry(entry);
            this.writeModule(jos);
            jos.closeEntry();
        }
    }

    private void writeModule(final OutputStream os) throws IOException {
        final ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V9, Opcodes.ACC_MODULE, "module-info", null, null, null);
        final ModuleVisitor mv = cw.visitModule(this.getModuleName().get(), 0, null);
        mv.visitRequire("java.base", Opcodes.ACC_MANDATED, null);
        mv.visitEnd();
        cw.visitEnd();
        os.write(cw.toByteArray());
    }

}
