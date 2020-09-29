/*MAKE_ASSERTIONS_FATAL[false]*/
/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.VMThread.ExecutionPoint;

import com.sun.squawk.debugger.*;
import com.sun.squawk.debugger.DataType.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * Provides support for the Squawk Debugger Agent {@link com.sun.squawk.debugger.sda.SDA}
 * Some of this is to fit in to package encapsulation.
 */
public class DebuggerSupport {

    /**
     * Return the JDWP.ThreadStatus value for the given thread's internal state.
     *
     * @param thread VMThread
     * @return the JDWP.ThreadStatus value
     */
    public static int getThreadJDWPState(VMThread vmThread) {
        int combinedState = vmThread.getInternalStatus();
        int state = (combinedState >> 8) & 0xFF;
        int inqueue = (combinedState) & 0xFF;

        if (vmThread.isAlive()) {
            switch (inqueue) {
                case VMThread.Q_TIMER:           return JDWP.ThreadStatus_SLEEPING;
                case VMThread.Q_MONITOR:         return JDWP.ThreadStatus_MONITOR;
                case VMThread.Q_CONDVAR:
                case VMThread.Q_ISOLATEJOIN:
                case VMThread.Q_JOIN:            return JDWP.ThreadStatus_WAIT;
                case 0:
                case VMThread.Q_RUN:
                case VMThread.Q_EVENT:           return JDWP.ThreadStatus_RUNNING;
                case VMThread.Q_HIBERNATEDRUN:   return JDWP.ThreadStatus_ZOMBIE;
                default: throw Assert.shouldNotReachHere();
            }
        } else {
            return JDWP.ThreadStatus_ZOMBIE;
        }
    }

    /**
     * Gets the JNI signature of a klass. This conversion substitutes the
     * appropriate primitive type for the Squawk primitives.
     *
     * @param klass  a class
     * @return the JNI signature for <code>klass</code>
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jni/spec/types.html">JNI Signatures</a>
     */
    public static String getJNISignature(Klass klass) {
        if (klass.isSquawkPrimitive()) {
            return Klass.SQUAWK_64 ? Klass.LONG.getSignature() :
                                     Klass.INT.getSignature();
        }

        if (!klass.isArray()) {
            return klass.getSignature();
        }

        return "[" + getJNISignature(klass.getComponentType());
    }

    /**
     * Gets the JNI signature of a field's type.
     *
     * @param field  a field
     * @return the JNI signature for <code>field</code>
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jni/spec/types.html">JNI Signatures</a>
     */
    public static String getJNISignature(Field field) {
        return getJNISignature(field.getType());
    }

    /**
     * Gets the JNI signature of a method's parameters and return type.
     *
     * @param method  a method
     * @return the JNI signature for <code>method</code>
     *
     * @see <a href="http://java.sun.com/j2se/1.4.2/docs/guide/jni/spec/types.html">JNI Signatures</a>
     */
    public static String getJNISignature(Method method) {
        StringBuffer buf = new StringBuffer("(");
        Klass[] parameters = method.getParameterTypes();
        for (int i = 0; i != parameters.length; ++i) {
            buf.append(getJNISignature(parameters[i]));
        }
        buf.append(')').
            append(getJNISignature(method.getReturnType()));
        return buf.toString();
    }

    /**
     * Determines if a given object is in a read-only memory and returns
     * a non-zero identifier for the object if it is. The identifier can be
     * decoded by calling {@link #getROMObjectForID}
     *
     * @param object the object to test
     * @return  a non-zero positive identifier for <code>object</code> if it's in a read-only memory otherwise 0
     */
    public static int getIDForROMObject(Object object) {
        Address address = Address.fromObject(object);
        int id;
        if (VM.inRom(address)) {
            // ROM object IDs are relative to beginning of ROM
            Offset offset = VM.getOffsetInRom(address).bytesToWords();
            id = offset.toInt();
            Assert.that(id > 0);
        } else if (GC.inNvm(address)) {
            // NVM object IDs are relative to beginning of NVM plus the size of ROM
            int romSizeInWords = VM.getRomEnd().diff(VM.getRomStart()).bytesToWords().toInt();
            Offset offset = GC.getOffsetInNvm(address).bytesToWords();
            id = offset.toInt() + romSizeInWords;
        } else {
            id = 0;
        }
        return id;
    }

    /**
     * Gets the object in read-only memory corresponding to an identifier that was
     * returned from a call to {@link #getIDForROMObject}.
     *
     * @param id   an identifier for an object in read-only memory (must be positive)
     * @return the object corresponding to <code>id</code>
     */
    public static Object getROMObjectForID(int id) {
        Assert.that(id > 0);
        int romSizeInWords = VM.getRomEnd().diff(VM.getRomStart()).bytesToWords().toInt();
        if (id <= romSizeInWords) {
            Offset offset = Offset.fromPrimitive(id).wordsToBytes();
            return VM.getObjectInRom(offset);
        } else {
            Offset offset = Offset.fromPrimitive(id - romSizeInWords).wordsToBytes();
            return GC.getObjectInNvm(offset);
        }
    }

    /**
     * Determines if a given class is initialized in a given isolate.
     *
     * @param klass   the class to test
     * @param isolate the isolate context
     * @return true if <code>klass</code> is intialized in <code>isolate</code>
     */
    public static boolean isInitialized(Klass klass, Isolate isolate) {
        return klass.getState() == Klass.STATE_CONVERTED && (!klass.mustClinit() || isolate.getClassState(klass) != null);
    }

    /**
     * Reads a static reference variable.
     *
     * @param isolate  the isolate context to use for variable lookup
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    public static Object getStaticOop(Isolate isolate, Klass klass, int offset) {
        int wordOffset = offset + CS.firstVariable;
        Object ks = isolate.getClassState(klass);
        if (ks == null) {
            // Class is not yet initialized
            return null;
        }
        return NativeUnsafe.getObject(ks, wordOffset);
    }

    /**
     * Reads a static int variable.
     *
     * @param isolate  the isolate context to use for variable lookup
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    public static int getStaticInt(Isolate isolate, Klass klass, int offset) {
        int wordOffset = offset + CS.firstVariable;
        Object ks = isolate.getClassState(klass);
        if (ks == null) {
            // Class is not yet initialized
            return 0;
        }
        return (int) NativeUnsafe.getUWord(ks, wordOffset).toPrimitive();
    }

    /**
     * Reads a static long variable.
     *
     * @param isolate  the isolate context to use for variable lookup
     * @param klass  the class of the variable
     * @param offset the offset (in words) to the variable
     * @return the value
     */
    public static long getStaticLong(Isolate isolate, Klass klass, int offset) {
        int wordOffset = offset + CS.firstVariable;
        Object ks = isolate.getClassState(klass);
        if (ks == null) {
            // Class is not yet initialized
            return 0L;
        }
        return NativeUnsafe.getLongAtWord(ks, wordOffset);
    }

    /**
     * Gets the class in which a given method was defined.s
     *
     * @param methodBody  the body of a method
     * @return the class in which <code>methodBody</code> was defined
     */
    public static Klass getDefiningClass(Object methodBody) {
        Assert.that(GC.getKlass(methodBody) == Klass.BYTECODE_ARRAY, "methodBody is not a bytecode array");
        return VM.asKlass(NativeUnsafe.getObject(methodBody, HDR.methodDefiningClass));
    }

    /**
     * Gets the body of the method given a class and method table index.
     *
     * @param klass     the class that defined the method
     * @param offset    the index of the method in its enclosing methods table
     * @param isStatic  true if the requested method is static, false if it is virtual
     * @return  the designated method body
     */
    public static Object getMethodBody(Klass klass, int offset, boolean isStatic) {
        Object[] table = isStatic ? klass.getStaticMethods() : klass.getVirtualMethods();
        Assert.that(0 <= offset && offset < table.length);
        Object method = table[offset];
        Assert.that(isValidMethodBody(method));
        return method;
    }

    /**
     * Return the length of <code>methodBody</code> (the byte code array) in bytes.
     *
     * @param methodBody Object
     * @return number of bytecodes
     */
    public static int getMethodBodyLength(Object methodBody) {
        Assert.that(isValidMethodBody(methodBody));
        if (VM.isHosted()) {
            return ( (MethodBody) methodBody).getCode().length;
        } else {
            return GC.getArrayLength(methodBody);
        }
    }

    private static boolean isValidMethodBody(final Object methodBody) {
        return (methodBody != null) && ((VM.isHosted() && methodBody instanceof MethodBody) || (GC.getKlass(methodBody) == Klass.BYTECODE_ARRAY));
    }

    /**
     * Gets the JDWP identifier for a method given the method's body.
     *
     * @param definingClass the class that defined the method
     * @param methodBody    the method's body
     * @return the JDWP identifier for <code>methodBody</code>
     */
    public static MethodID getIDForMethodBody(Klass definingClass, final Object methodBody) {
        Assert.that(isValidMethodBody(methodBody));
        Assert.that(definingClass == getDefiningClass(methodBody));
        Object [] methods = definingClass.getVirtualMethods();
        for (int i = 0; i != methods.length; i++) {
            if (methods[i] == methodBody) {
                return new MethodID(i, false);
            }
        }

        methods = definingClass.getStaticMethods();
        for (int i = 0; i != methods.length; i++) {
            if (methods[i] == methodBody) {
                return new MethodID(i, true);
            }
        }
        throw Assert.shouldNotReachHere();
    }

    /**
     * Determines if the current hit breakpoint (if any) is an exception breakpoint.
     *
     * @return true if the given thread is at an exception breakpoint.
     */
    public static boolean isAtExceptionBreakpoint(VMThread vmThread) {
        Assert.that(vmThread != null);
        return vmThread.getHitBreakpoint() != null && vmThread.getHitBreakpoint().getException() != null;
    }

    /*-----------------------------------------------------------------------*\
     *                          Call stack inspection                        *
    \*-----------------------------------------------------------------------*/

    static final boolean DEBUG_STACK_INSPECTION = false;

    /**
     * Traverses one or more stack frames in a thread (that must be {@link VMThread#suspendForDebugger suspended}).
     *
     * @param inspector  the frame inspector delegate
     * @param from       the location in the call stack of the thread at which to start inspecting.
     *                   If null, then inspection starts at the inner most frame.
     * @param frameNo    the frame to inspect or -1 if all frames are to be inspected. A frame
     *                   is identified by the number of frames between it and the inner most frame
     *                   (i.e. the inner most frame will be 0)
     * @return the number of frames inspected
     */
    public static int inspectStack(StackInspector inspector, ExecutionPoint from, int frameNo) {
        VMThread thread = inspector.vmThread;
        Assert.that(VMThread.currentThread() != thread, "can't visit frames of the current thread");
        Assert.that(thread.getDebuggerSuspendCount() != 0, "can't visit stack frames of non-suspended thread");

        Object stack = thread.getStack();
        Address stackAddress = Address.fromObject(stack);

        Object mp;
        Offset bci;
        Offset frame;
        Address fp;
        boolean isInnerMostActivation;

        if (from != null) {
            mp = from.mp;
            bci = from.bci;
            frame = from.frame;
            isInnerMostActivation = false;
        } else {
            fp = NativeUnsafe.getAddress(stack, SC.lastFP);
            mp = VM.getMP(fp);
            frame = thread.framePointerAsOffset(fp);
            bci = NativeUnsafe.getUWord(stack, SC.lastBCI).toOffset();
            isInnerMostActivation = true;
        }

        Address ip;

        int thisFrame = 0;
        int inspectedFrames = 0;

        if (DEBUG_STACK_INSPECTION) {
            VM.println("inspecting stack: frameNo = " + frameNo + " from=" + from);
        }

        while (true) {
            if (frameNo == -1 || frameNo == thisFrame) {
                if (DEBUG_STACK_INSPECTION) {
                    VM.print("inspecting frame " + thisFrame + "@");
                    VM.printOffset(bci);
                    VM.println(":");
                }
                inspector.inspectFrame(mp, bci, thisFrame, frame);
                if (inspector.doSlots) {

                    int localCount = isInnerMostActivation ? 1 : MethodBody.decodeLocalCount(mp);
                    int parameterCount = MethodBody.decodeParameterCount(mp);
                    Klass[] typeMap = MethodBody.decodeTypeMap(mp);

                    // Visit the parameters
                    int slot = 0;
                    if (DEBUG_STACK_INSPECTION) {
                        VM.println("  inspecting " + parameterCount + " parameters slots: ");
                    }
                    slot = inspectSlots(parameterCount, FP.parm0, slot, typeMap, thread, frame, inspector, true);
                    if (DEBUG_STACK_INSPECTION) {
                        VM.println("  inspecting " + localCount + " local slots: ");
                    }
                    slot = inspectSlots(localCount, FP.local0, slot, typeMap, thread, frame, inspector, false);
                }
                inspectedFrames++;
            }
            isInnerMostActivation = false;
            thisFrame++;

            stackAddress = Address.fromObject(stack);
            fp = thread.frameOffsetAsPointer(frame);
            ip = VM.getPreviousIP(fp);
            fp = VM.getPreviousFP(fp);
            if (fp.isZero()) {
                break;
            }
            mp = VM.getMP(fp);
            bci = ip.diff(Address.fromObject(mp));
            frame = thread.framePointerAsOffset(fp);
        }

        inspector.postInspection();
        return inspectedFrames;
    }

    /**
     * Counts the number of inner most frames on a call stack representing the
     * entry into the debugger system on a thread that hit a breakpoint, threw
     * an exception or completed a single step.
     *
     * @param stack  the call stack to examine
     * @return the number of system frames on the stack
     */
    private static int countSystemFrames(Object stack) {

        Address fp = NativeUnsafe.getAddress(stack, SC.lastFP);
        Object mp = VM.getMP(fp);
        Offset fpOffset = fp.diff(Address.fromObject(stack));

        int thisFrame = 0;

        while (true) {
            fp = VM.getPreviousFP(Address.fromObject(stack).addOffset(fpOffset));
            fpOffset = fp.diff(Address.fromObject(stack));
            if (fp.isZero()) {
                return 0;
            } else {
                if (MethodBody.isInterpreterInvoked(mp)) {
                    return thisFrame + 1;
                }
            }
            mp = VM.getMP(Address.fromObject(stack).addOffset(fpOffset));
            thisFrame++;
        }
    }

    /**
     * Traverses zero or more slots in a stack frame with a StackInspector object.
     *
     * @param count      the number of physical slots to traverse (longs and doubles are 2 slots)
     * @param offset     the offset (in words) from the frame pointer of the first slot
     * @param slot       the logical index of the first slot
     * @param typeMap    the types of the slots
     * @param vmThread   the thread
     * @param fo         the frame pointer as an offset (in bytes) from the top of the stack
     * @param inspector  the StackInspector object
     * @param parameters specifies if the slots being traversed by this call hold the parameters
     * @return the logical index of the slot one past the last slot visited by this call
     */
    private static int inspectSlots(int count, int offset, int slot, Klass[] typeMap, VMThread vmThread, Offset fo, StackInspector inspector, boolean parameters) {
        Object stack = vmThread.getStack();
        while (count-- > 0) {
            Klass type = typeMap[slot];
            int varOffset = vmThread.frameOffsetAsPointer(fo).diff(Address.fromObject(stack)).bytesToWords().toInt() + (parameters ? offset : -offset);
            if (type.isReferenceType()) {
                Object value = NativeUnsafe.getObject(stack, varOffset);
                if (DEBUG_STACK_INSPECTION) {
                    VM.print("    slot " + slot + ": type = " + type.getInternalName() + " isParameter = " + parameters + ", value = ");
                    VM.printAddress(value);
                    if (value != null) {
                        VM.print(", real type = " + GC.getKlass(value).getInternalName());
                    }
                    VM.println();
                }
                inspector.inspectSlot(parameters, slot, value);
            } else {
                long value;
                boolean skipSlot = false;
                boolean isTwoWordLongLocal = false;
                if (!Klass.SQUAWK_64 && type.isDoubleWord()) {
                    value = NativeUnsafe.getLongAtWord(stack, parameters ? varOffset : varOffset - 1);
                    skipSlot = true;
                    isTwoWordLongLocal = !parameters;
                } else {
                    value = NativeUnsafe.getAsUWord(stack, varOffset).toPrimitive();
                }

                if (DEBUG_STACK_INSPECTION) {
                    VM.println("   slot " + (isTwoWordLongLocal ? slot + 1 : slot) + ": type = " + type.getInternalName() + " isParameter = " + parameters + ", value = " + value);
                }
                inspector.inspectSlot(parameters, (isTwoWordLongLocal ? slot + 1 : slot), type, value);

                if (skipSlot) {
                    // skip the slot holding the second half of the double-word
                    count--;
                    slot++;
                    offset++;
                }
            }
            offset++;
            slot++;
        }
        return slot;
    }

    /**
     * Attaches or detaches a debugger to an isolate.
     *
     * @param isolate   the target debuggee isolate
     * @param debugger  the debugger wanting to attach or detach
     * @param attach    specifies if this in an attach or detach operation
     */
    public static void setDebugger(Isolate isolate, Debugger debugger, boolean attach) {
        Assert.that(debugger != null);
        if (attach) {
            if (isolate.getDebugger() != null) {
                throw new RuntimeException("Isolate is already being debugged: " + isolate);
            }
            isolate.setDebugger(debugger);
        } else {
            if (isolate.getDebugger() != debugger) {
                throw new RuntimeException("Isolate is not being debugged by this debugger: " + isolate);
            }
            isolate.setDebugger(null);
        }
    }

    /**
     * A StackInspector instance is used in conjunction with {@link #inspectStack}
     * to traverse one or more stack frames in a suspended thread. The values of the slots
     * in each frame can be inspected.
     */
    public static class StackInspector {

        /**
         * Determines if the inspector should be given a chance to inspect the value
         * of the slots in each frame.
         */
        protected final boolean doSlots;

        /**
         * The thread whose call stack is being inspected.
         */
        protected final VMThread vmThread;

        /**
         * Constructor.
         *
         * @param thread   the thread whose call stack is being inspected. This thread must be suspended
         *                 at the time the inspection is performed.
         * @param doSlots  true if this inspector should be given a change to inspect the value
         *                 of the slots in each frame by means of having it {@link #inspectSlot} methods invoked
         */
        protected StackInspector(VMThread thread, boolean doSlots) {
            this.vmThread = thread;
            this.doSlots = doSlots;
        }

        /**
         * Invoked to inspect a frame in the thread's call stack
         *
         * @param mp        the method to which the frame pertains
         * @param bci       the current bytecode index of the instruction pointer
         * @param frame     the frame's identifier within the call stack. A frame
         *                  is identified by the number of frames between it and the inner most frame
         *                  (i.e. the inner most frame will be 0)
         * @param fo        the frame's offset from the top of the stack
         */
        public void inspectFrame(Object mp, Offset bci, int frame, Offset fo) {}

        /**
         * Invoked to inspect the value of a reference typed slot within a frame in the thread's call stack
         *
         * @param isParameter  true if the slot holds the value of a parameter
         * @param slot         the slot index
         * @param value        the value in the slot
         */
        public void inspectSlot(boolean isParameter, int slot, Object value) {}

        /**
         * Invoked to inspect the value of a primitive typed slot within a frame in the thread's call stack
         *
         * @param isParameter  true if the slot holds the value of a parameter
         * @param slot         the slot index
         * @param type         {@link Klass#INT int}, {@link Klass#FLOAT float}, {@link Klass#LONG long},
         *                     {@link Klass#DOUBLE double}, {@link Klass#ADDRESS address},
         *                     {@link Klass#OFFSET offset} or {@link Klass#UWORD uword}
         * @param value        the value in the slot
         */
        public void inspectSlot(boolean isParameter, int slot, Klass type, long value) {}

        /**
         * Hooks for any inspector specific behaviour that must run after the frame inspection is completed.
         */
        public void postInspection() {}

        /**
         * Gets the result (if any) of the inspection. This is also useful for propogating
         * exceptions out to the caller.
         *
         * @return a value computed as a result of the inspection
         */
        public Object getResult() {
            return null;
        }
    }

    /**
     * Count the number of frames on the stack, while <code>thread</code> is temporarily suspended.
     *
     * @param thread     the thread to count the frames of
     * @param from       the location in the call stack of the thread at which to start inspecting.
     *                   If null, then inspection starts at the inner most frame.
     * @return number of frames
     *
     */
    public static int countStackFrames(VMThread vmThread, ExecutionPoint from) {
        Assert.that(vmThread.getDebuggerSuspendCount() > 0);
        return inspectStack(new StackInspector(vmThread, false), from, -1);
    }

    /**
     * Prints stack trace of <code>thread</code> to the VM debug output stream.
     * The thread must be suspended.
     *
     * @param thread   the thread to display
     */
    public static void printStackTrace(VMThread vmThread) {
        Assert.that(vmThread.getDebuggerSuspendCount() > 0);
        StackInspector inspector = new StackInspector(vmThread, false) {
            public void inspectFrame(Object mp, Offset bci, int frame, Offset fo) {
                Klass klass = getDefiningClass(mp);
                Method method = klass.findMethod(mp);
                VM.print("    [");
                VM.print(frame);
                VM.print("] ");
                if (method == null) {
                    VM.print("in a method of ");
                    VM.print(klass.getName());
                    VM.print("(bci=");
                    VM.printOffset(bci);
                    VM.print(")");
                } else {
                    MethodID id = getIDForMethodBody(klass, mp);
                    VM.print("at ");
                    VM.print(klass.getName());
                    VM.print(".");
                    VM.print(method.getName());
                    VM.print("(");
                    String src = klass.getSourceFileName();
                    if (src == null) {
                        src = "?";
                    }
                    VM.print(src);
                    VM.print(":");
                    int[] lnt = method.getLineNumberTable();
                    if (lnt != null) {
                        int lno = Method.getLineNumber(lnt, bci.toInt());
                        VM.print(lno);
                    } else {
                        VM.print("bci=");
                        VM.printOffset(bci);
                    }
                    VM.print(") [id=");
                    VM.print(id.toString());
                    VM.print("]");
                }
                VM.println();
            }
        };

        inspectStack(inspector, null, -1);
    }
}
