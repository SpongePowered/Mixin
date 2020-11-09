package org.spongepowered.asm.mixin.injection;

public @interface Desc {
	public Class<?> owner() default Void.class;
	public String value();
	public Class<?> ret() default Void.class;
	public Class<?>[] args() default { };
}
