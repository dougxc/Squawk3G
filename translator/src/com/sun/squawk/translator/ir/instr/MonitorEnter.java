/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM translator.
 */
package com.sun.squawk.translator.ir.instr;

import com.sun.squawk.translator.ir.*;

/**
 * An instance of <code>MonitorEnter</code> represents an instruction that
 * pops a referenced typed value off the operand stack and acquires a
 * lock on its monitor.
 *
 * @author  Doug Simon
 */
public final class MonitorEnter extends Instruction {

    /**
     * The object whose monitor is locked. This will be null if this
     * instruction is locking the monitor of the enclosing class to implement
     * static method synchronization.
     */
    private StackProducer object;

    /**
     * Creates a <code>MonitorEnter</code> instance representing an instruction
     * that pops a referenced typed value off the operand stack and acquires a
     * lock on its monitor.
     *
     * @param object  the object whose monitor is locked or null when this
     *                instruction is implementing static method synchronization
     */
    public MonitorEnter(StackProducer object) {
        this.object = object;
    }

    /**
     * Gets the object whose monitor is locked. This will be null if this
     * instruction is locking the monitor of the enclosing class to implement
     * static method synchronization.
     *
     * @return the object whose monitor is locked or <code>null</code>
     */
    public StackProducer getObject() {
        return object;
    }

    /**
     * {@inheritDoc}
     */
    public boolean constrainsStack() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void visit(InstructionVisitor visitor) {
        visitor.doMonitorEnter(this);
    }

    /**
     * {@inheritDoc}
     */
    public void visit(OperandVisitor visitor) {
        if (object != null) {
            object = visitor.doOperand(this, object);
        }
    }
}
