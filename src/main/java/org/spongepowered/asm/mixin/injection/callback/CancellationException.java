/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.callback;

public class CancellationException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    public CancellationException()
    {
    }
    
    public CancellationException(String message)
    {
        super(message);
    }
    
    public CancellationException(Throwable cause)
    {
        super(cause);
    }
    
    public CancellationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
