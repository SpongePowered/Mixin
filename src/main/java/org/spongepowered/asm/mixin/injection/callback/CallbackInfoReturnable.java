/**
 * This file contributed from LiteLoader. Pending refactor. DO NOT ALTER THIS FILE.
 */

package org.spongepowered.asm.mixin.injection.callback;

import org.objectweb.asm.Type;

//import com.mumfrey.liteloader.core.event.EventCancellationException;

/**
 * EventInfo for events which have a return type
 * 
 * @author Adam Mummery-Smith
 *
 * @param <S> Source object type. For non-static methods this will be the containing object instance.
 * @param <R> Return type
 */
public class CallbackInfoReturnable<R> extends CallbackInfo
{
    private R returnValue;
    
    public CallbackInfoReturnable(String name, boolean cancellable)
    {
        super(name, cancellable);
        this.returnValue = null;
    }
    
    public CallbackInfoReturnable(String name, boolean cancellable, R returnValue)
    {
        super(name, cancellable);
        this.returnValue = returnValue;
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, byte returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Byte.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, char returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Character.valueOf(returnValue);
    }

    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, double returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Double.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, float returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Float.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, int returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Integer.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, long returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Long.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, short returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Short.valueOf(returnValue);
    }
    
    @SuppressWarnings("unchecked")
    public CallbackInfoReturnable(String name, boolean cancellable, boolean returnValue)
    {
        super(name, cancellable);
        this.returnValue = (R)Boolean.valueOf(returnValue);
    }
    
    /**
     * Sets a return value for this event and cancels the event (required in order to return the new value) 
     * 
     * @param returnValue
     */
    public void setReturnValue(R returnValue) throws CancellationException
    {
        super.cancel();
        
        this.returnValue = returnValue;
    }       
    
    public R getReturnValue()
    {
        return this.returnValue;
    }
    
    // All of the accessors below are to avoid having to generate unboxing conversions in bytecode
    public byte    getReturnValueB() { if (this.returnValue == null) return 0;     return (Byte)     this.returnValue; }
    public char    getReturnValueC() { if (this.returnValue == null) return 0;     return (Character)this.returnValue; }
    public double  getReturnValueD() { if (this.returnValue == null) return 0.0;   return (Double)   this.returnValue; }
    public float   getReturnValueF() { if (this.returnValue == null) return 0.0F;  return (Float)    this.returnValue; }
    public int     getReturnValueI() { if (this.returnValue == null) return 0;     return (Integer)  this.returnValue; }
    public long    getReturnValueJ() { if (this.returnValue == null) return 0;     return (Long)     this.returnValue; }
    public short   getReturnValueS() { if (this.returnValue == null) return 0;     return (Short)    this.returnValue; }
    public boolean getReturnValueZ() { if (this.returnValue == null) return false; return (Boolean)  this.returnValue; }

    public static String getReturnAccessor(Type returnType)
    {
        if (returnType.getSort() == Type.OBJECT)
        {
            return "getReturnValue";
        }
        
        return String.format("getReturnValue%s", returnType.getDescriptor());
    }

    public static String getReturnDescriptor(Type returnType)
    {
        if (returnType.getSort() == Type.OBJECT)
        {
            return String.format("()%s", CallbackInfo.OBJECT);
        }
        
        return String.format("()%s", returnType.getDescriptor());
    }
}
