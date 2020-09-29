/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 *
 * $Id: TeqTest.java,v 1.3 2005/02/03 00:56:40 dl156546 Exp $
 */

package com.sun.squawk.compiler.asm.arm.tests;

import com.sun.squawk.compiler.asm.*;
import com.sun.squawk.compiler.asm.arm.*;
import junit.framework.*;

/**
 * Test fixtures for the teq{cond} assembler instructions.
 *
 * @author David Liu
 * @version 1.0
 */
public class TeqTest extends TestCase {
    public TeqTest() {
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (suite());
    }

    public static junit.framework.Test suite() {
        return new TestSuite(TeqTest.class);
    }

    private CodeBuffer buffer;
    private ARMAssembler asm;
    private ARMDisassembler dis;

    protected void setUp() {
        buffer = new CodeBuffer ();
        asm = new ARMAssembler (buffer);
        dis = new ARMDisassembler (buffer);
    }

    /**
     * Tests the teq instruction.
     */
    public void testTeq() {
        asm.teq(asm.SP, Operand2.imm(0x254));
        asm.teq(asm.R1, Operand2.reg(asm.R3));
        asm.teq(asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0xe3, 0x3d, 0x0f, 0x95,
        0xe1, 0x31, 0x00, 0x03,
        0xe1, 0x3f, 0x08, 0xcd }));

        assertEquals("teq sp, #0x254", dis.disassembleNext());
        assertEquals("teq r1, r3", dis.disassembleNext());
        assertEquals("teq pc, sp, asr #17", dis.disassembleNext());
    }

    /**
     * Tests the teqcond instruction.
     */
    public void testTeqcond() {
        asm.teqcond(asm.COND_CC, asm.SP, Operand2.imm(0x254));
        asm.teqcond(asm.COND_GT, asm.R1, Operand2.reg(asm.R3));
        asm.teqcond(asm.COND_PL, asm.PC, Operand2.asr(asm.SP, 17));

        assertTrue(ArmTests.compareCode (buffer, new int [] {
        0x33, 0x3d, 0x0f, 0x95,
        0xc1, 0x31, 0x00, 0x03,
        0x51, 0x3f, 0x08, 0xcd }));

        assertEquals("teqcc sp, #0x254", dis.disassembleNext());
        assertEquals("teqgt r1, r3", dis.disassembleNext());
        assertEquals("teqpl pc, sp, asr #17", dis.disassembleNext());
    }
}
