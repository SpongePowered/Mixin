package org.spongepowered.asm.synthetic.args;

/**
 * On modern ModLauncher, generation of synthetic `Args` classes fails unless the module already exists.
 * For the module to exist, a class needs to be present in its package prior to generation, so that's what this class is for.
 */
class Dummy {
}
