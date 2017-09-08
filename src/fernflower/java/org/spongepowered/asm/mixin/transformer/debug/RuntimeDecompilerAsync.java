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
package org.spongepowered.asm.mixin.transformer.debug;

import java.io.File;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Asynchronous decompiler, runs mixin export task in a separate thread to avoid
 * slowing down loading time
 */
public class RuntimeDecompilerAsync extends RuntimeDecompiler implements Runnable, UncaughtExceptionHandler {
    
    private final BlockingQueue<File> queue = new LinkedBlockingQueue<File>();
    
    private final Thread thread;
    
    private boolean run = true;

    public RuntimeDecompilerAsync(File outputPath) {
        super(outputPath);
        this.thread = new Thread(this, "Decompiler thread");
        this.thread.setDaemon(true);
        this.thread.setPriority(Thread.MIN_PRIORITY);
        this.thread.setUncaughtExceptionHandler(this);
        this.thread.start();
    }

    @Override
    public void decompile(File file) {
        if (this.run) {
            this.queue.offer(file);
        } else {
            super.decompile(file);
        }
    }

    @Override
    public void run() {
        while (this.run) {
            try {
                File file = this.queue.take();
                super.decompile(file);
            } catch (InterruptedException ex) {
                this.run = false;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        this.logger.error("Async decompiler encountered an error and will terminate. Further decompile requests will be handled synchronously. {} {}",
                ex.getClass().getName(), ex.getMessage());
        this.flush();
    }

    private void flush() {
        this.run = false;
        for (File file; (file = this.queue.poll()) != null;) {
            this.decompile(file);
        }
    }
}
