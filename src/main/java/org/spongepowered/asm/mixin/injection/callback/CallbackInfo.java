/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.callback;

import org.objectweb.asm.Type;

//import com.mumfrey.liteloader.core.event.Cancellable;
//import com.mumfrey.liteloader.core.event.EventCancellationException;

/**
 * Contains information about an injected event, including the source object and whether the event
 * is cancellable and/or cancelled.
 * 
 * @author Adam Mummery-Smith
 *
 * @param <S> Source object type. For non-static methods this will be the containing object instance.
 */
public class CallbackInfo implements Cancellable
{
    protected static final String STRING = "Ljava/lang/String;";
    protected static final String OBJECT = "Ljava/lang/Object;";

    private final String name;
    
//    private final S source;
    
    private final boolean cancellable;

    private boolean cancelled;

    public CallbackInfo(String name, /*S source,*/ boolean cancellable)
    {
        this.name = name;
//        this.source = source;
        this.cancellable = cancellable;
    }
    
//    public S getSource()
//    {
//        return this.source;
//    }
    
    public String getName()
    {
        return this.name;
    }
    
//    protected String getSourceClass()
//    {
//        return this.source != null ? this.source.getClass().getSimpleName() : null;
//    }
    
    @Override
    public String toString()
    {
        return String.format("CallbackInfo[TYPE=%s,NAME=%s,CANCELLABLE=%s]", this.getClass().getSimpleName(), this.name, this.cancellable);
    }
    
    /* (non-Javadoc)
     * @see com.mumfrey.liteloader.core.event.Cancellable#isCancellable()
     */
    @Override
    public final boolean isCancellable()
    {
        return this.cancellable;
    }

    /* (non-Javadoc)
     * @see com.mumfrey.liteloader.transformers.event.Cancellable#isCancelled()
     */
    @Override
    public final boolean isCancelled()
    {
        return this.cancelled;
    }
    
    /* (non-Javadoc)
     * @see com.mumfrey.liteloader.transformers.event.Cancellable#cancel()
     */
    @Override
    public void cancel() throws CancellationException
    {
        if (!this.cancellable)
        {
            throw new CancellationException(String.format("The call %s is not cancellable.", this.name));
        }
        
        this.cancelled = true;
    }

    protected static String getCallInfoClassName()
    {
        return CallbackInfo.class.getName();
    }
    
    /**
     * @param returnType
     */
    protected static String getCallInfoClassName(Type returnType)
    {
        return (returnType.equals(Type.VOID_TYPE) ? CallbackInfo.class.getName() : CallbackInfoReturnable.class.getName()).replace('.', '/');
    }

    public static String getConstructorDescriptor(Type returnType)
    {
        if (returnType.equals(Type.VOID_TYPE))
        {
            return CallbackInfo.getConstructorDescriptor();
        }
        
        if (returnType.getSort() == Type.OBJECT)
        {
            return String.format("(%sZ%s)V", CallbackInfo.STRING, CallbackInfo.OBJECT);
        }
        
        return String.format("(%sZ%s)V", CallbackInfo.STRING, returnType.getDescriptor());
    }

    public static String getConstructorDescriptor()
    {
        return String.format("(%sZ)V", CallbackInfo.STRING);
    }

    public static String getIsCancelledMethodName()
    {
        return "isCancelled";
    }

    public static String getIsCancelledMethodSig()
    {
        return "()Z";
    }
}
