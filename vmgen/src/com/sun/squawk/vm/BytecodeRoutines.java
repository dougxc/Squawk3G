/* **** GENERATED FILE -- DO NOT EDIT ****
 *      generated by com.sun.squawk.builder.gen.BytecodeRoutines
 *
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */

package com.sun.squawk.vm;

import com.sun.squawk.compiler.*;

/**
 * This class defines the routines used by the Squawk interpreter and jitter.
 *
 * @author   Nik Shaylor
 */
abstract public class BytecodeRoutines implements Types {
    abstract protected void do_const(int n);
    abstract protected void do_object(int n);
    abstract protected void do_load(int n);
    abstract protected void do_store(int n);
    abstract protected void do_loadparm(int n);
    abstract protected void do_wide(int n);
    abstract protected void do_wide_short();
    abstract protected void do_wide_int();
    abstract protected void do_escape();
    abstract protected void do_escape_wide(int n);
    abstract protected void do_escape_wide_short();
    abstract protected void do_escape_wide_int();
    abstract protected void do_catch();
    abstract protected void do_const_null();
    abstract protected void do_const_byte();
    abstract protected void do_const_short();
    abstract protected void do_const_char();
    abstract protected void do_const_int();
    abstract protected void do_const_long();
    abstract protected void do_object();
    abstract protected void do_load();
    abstract protected void do_load_i2();
    abstract protected void do_store();
    abstract protected void do_store_i2();
    abstract protected void do_loadparm();
    abstract protected void do_loadparm_i2();
    abstract protected void do_storeparm();
    abstract protected void do_storeparm_i2();
    abstract protected void do_inc();
    abstract protected void do_dec();
    abstract protected void do_incparm();
    abstract protected void do_decparm();
    abstract protected void do_goto();
    abstract protected void do_if(int operands, int cc, Type t);
    abstract protected void do_getstatic(Type t);
    abstract protected void do_class_getstatic(Type t);
    abstract protected void do_putstatic(Type t);
    abstract protected void do_class_putstatic(Type t);
    abstract protected void do_getfield(Type t);
    abstract protected void do_getfield0(Type t);
    abstract protected void do_putfield(Type t);
    abstract protected void do_putfield0(Type t);
    abstract protected void do_invokevirtual(Type t);
    abstract protected void do_invokestatic(Type t);
    abstract protected void do_invokesuper(Type t);
    abstract protected void do_invokenative(Type t);
    abstract protected void do_findslot();
    abstract protected void do_extend();
    abstract protected void do_invokeslot(Type t);
    abstract protected void do_return(Type t);
    abstract protected void do_tableswitch(Type t);
    abstract protected void do_extend0();
    abstract protected void do_add(Type t);
    abstract protected void do_sub(Type t);
    abstract protected void do_and(Type t);
    abstract protected void do_or(Type t);
    abstract protected void do_xor(Type t);
    abstract protected void do_shl(Type t);
    abstract protected void do_shr(Type t);
    abstract protected void do_ushr(Type t);
    abstract protected void do_mul(Type t);
    abstract protected void do_div(Type t);
    abstract protected void do_rem(Type t);
    abstract protected void do_neg(Type t);
    abstract protected void do_i2b();
    abstract protected void do_i2s();
    abstract protected void do_i2c();
    abstract protected void do_l2i();
    abstract protected void do_i2l();
    abstract protected void do_throw();
    abstract protected void do_pop(int n);
    abstract protected void do_monitorenter();
    abstract protected void do_monitorexit();
    abstract protected void do_class_monitorenter();
    abstract protected void do_class_monitorexit();
    abstract protected void do_arraylength();
    abstract protected void do_new();
    abstract protected void do_newarray();
    abstract protected void do_newdimension();
    abstract protected void do_class_clinit();
    abstract protected void do_bbtarget_sys();
    abstract protected void do_bbtarget_app();
    abstract protected void do_instanceof();
    abstract protected void do_checkcast();
    abstract protected void do_aload(Type t);
    abstract protected void do_astore(Type t);
    abstract protected void do_lookup(Type t);
    abstract protected void do_pause();

/*if[FLOATS]*/
    abstract protected void do_fcmpl();
    abstract protected void do_fcmpg();
    abstract protected void do_dcmpl();
    abstract protected void do_dcmpg();
    abstract protected void do_const_float();
    abstract protected void do_const_double();
    abstract protected void do_i2f();
    abstract protected void do_l2f();
    abstract protected void do_f2i();
    abstract protected void do_f2l();
    abstract protected void do_i2d();
    abstract protected void do_l2d();
    abstract protected void do_f2d();
    abstract protected void do_d2i();
    abstract protected void do_d2l();
    abstract protected void do_d2f();
/*end[FLOATS]*/
}
