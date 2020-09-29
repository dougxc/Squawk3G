/*
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * This is a part of the Squawk JVM.
 */
package com.sun.squawk;

import com.sun.squawk.pragma.*;
import com.sun.squawk.util.*;
import com.sun.squawk.vm.*;

/**
 * The Address class is used to abstract machine addresses. It is used instead
 * of 'int' or 'Object' for coding clarity and machine-portability (it can map to
 * 32 bit and 64 bit integral types).
 * <p>
 * <b>Variables of type <code>Address</code> will never be updated if they correspond with
 * an object that may be moved by the garbage collector. As such, they should never
 * appear in code that may have a collection occur in between the definition and use
 * of the variable. In general, variables of type address should not be used outside of
 * the garbage collector.</b>
 * <p>
 * This class is known specially by the translator as a {@link Modifier#SQUAWKPRIMITIVE}
 * and programming with it requires adhering to the restrictions implied by this
 * attribute.
 * <p>
 * Only the public methods of this class which do not override any of the
 * methods in java.lang.Object will be available in a {@link VM#isHosted() non-hosted}
 * environment. The translator replaces any calls to these methods to native
 * method calls.
 * <p>
 * This mechanism was largely inspired by the VM_Address class in the Jikes RVM.
 *
 * @author Doug Simon
 */
public final class Address {

    /**
     * Casts a word expressed as the appropriate Java primitive type for the platform (i.e. int or long)
     * into a value of type Address.
     *
     * @param  value an address expressed as an int or long
     * @return the canonical Address instance for <code>value</code>
     *
     * @vm2c code( return (Address)value; )
     */
    public static Address fromPrimitive(int/*S64*/ value) throws NativePragma {
        return new Address(value);
    }

    /**
     * Casts an object reference to an address.
     *
     * @param object   the object reference to cast
     * @return the object reference as an address
     *
     * @vm2c code( return (Address)object; )
     */
    public static Address fromObject(Object object) throws NativePragma {
        Assert.that(object instanceof Address);
        return (Address)object;
    }

    /**
     * Gets the canonical Address representation of <code>null</code>.
     *
     * @return the canonical Address representation of <code>null</code>
     *
     * @vm2c code( return (Address)0; )
     */
    public static Address zero() throws NativePragma {
        return get(0);
    }

    /**
     * Gets the largest possible machine address.
     *
     * @return  the largest possible machine address
     *
     * @vm2c code( return (Address)-1; )
     */
    public static Address max() throws NativePragma {
        return get(-1);
    }

    /**
     * Casts this address to an object reference.
     *
     * @return this address as an object reference
     */
    public Object toObject() throws NativePragma {
        return this;
    }

    /**
     * Casts this address to a UWord.
     *
     * @return this address as a UWord
     *
     * @vm2c code( return (UWord)this; )
     */
    public UWord toUWord() throws NativePragma {
        return UWord.fromPrimitive(value);
    }

    /**
     * Adds a 32 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to add
     * @return the result of adding <code>offset</code> to this address
     *
     * @vm2c code( return Address_add(this, offset); )
     */
    public Address add(int offset) throws NativePragma {
        return get(value + offset);
    }

    /**
     * Subtracts a 32 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to subract
     * @return the result of subtracting <code>offset</code> to this address
     *
     * @vm2c code( return Address_sub(this, offset); )
     */
    public Address sub(int offset) throws NativePragma {
        return get(value - offset);
    }

    /**
     * Adds a 32 or 64 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to add
     * @return the result of adding <code>offset</code> to this address
     *
     * @vm2c code( return Address_add(this, offset); )
     */
    public Address addOffset(Offset offset) throws NativePragma {
        return get(value + offset.toPrimitive());
    }

    /**
     * Subtracts a 32 or 64 bit offset to this address and return the resulting address.
     *
     * @param offset   the offset to subract
     * @return the result of subtracting <code>offset</code> to this address
     *
     * @vm2c code( return Address_sub(this, offset); )
     */
    public Address subOffset(Offset offset) throws NativePragma {
        return get(value - offset.toPrimitive());
    }

    /**
     * Logically OR a word with this address.
     *
     * @param word   the word to OR this address with
     * @return       the result of the OR operation
     *
     * @vm2c code( return (Address)((UWord)this | word); )
     */
    public Address or(UWord word) throws NativePragma {
        return get(value | word.toPrimitive());
    }

    /**
     * Logically AND a word with this address.
     *
     * @param word   the word to AND this address with
     * @return       the result of the AND operation
     *
     * @vm2c code( return (Address)((UWord)this & word); )
     */
    public Address and(UWord word) throws NativePragma {
        return get(value & word.toPrimitive());
    }

    /**
     * Calculates the offset between this address an another address.
     *
     * @param address2   the address to compare this address with
     * @return the offset that must be applied to this address to get <code>address2</code>
     *
     * @vm2c code( return Address_diff(this, address2); )
     */
    public Offset diff(Address address2) throws NativePragma {
        Assert.that(value >= address2.value);
        return Offset.fromPrimitive(value - address2.value);
    }

    /**
     * Determines if this address is <code>null</code>.
     *
     * @return true if this address is <code>null</code>
     *
     * @vm2c code( return this == 0; )
     */
    public boolean isZero() throws NativePragma {
        return this == zero();
    }

    /**
     * Determines if this address is equals to {@link #max() max}.
     *
     * @return true if this address is equals to {@link #max() max}
     *
     * @vm2c code( return this == (Address)-1; )
     */
    public boolean isMax() throws NativePragma {
        return this == max();
    }

    /**
     * Determines if this address is equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is equal to <code>address2</code>
     *
     * @vm2c code( return this == address2; )
     */
    public boolean eq(Address address2) throws NativePragma {
        return this == address2;
    }

    /**
     * Determines if this address is not equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is not equal to <code>address2</code>
     *
     * @vm2c code( return this != address2; )
     */
    public boolean ne(Address address2) throws NativePragma {
        return this != address2;
    }

    /**
     * Determines if this address is lower than a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is lower than or equals to <code>address2</code>
     *
     * @vm2c code( return this < address2; )
     */
    public boolean lo(Address address2) throws NativePragma {
        if (value >= 0 && address2.value >= 0) return value < address2.value;
        if (value < 0 && address2.value < 0) return value < address2.value;
        if (value < 0) return false;
        return true;
    }

    /**
     * Determines if this address is lower than or equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is lower than or equal to <code>address2</code>
     *
     * @vm2c code( return this <= address2; )
     */
    public boolean loeq(Address address2) throws NativePragma {
        return (this == address2) || lo(address2);
    }

    /**
     * Determines if this address is higher than a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is higher than <code>address2</code>
     *
     * @vm2c code( return this > address2; )
     */
    public boolean hi(Address address2) throws NativePragma {
        return address2.lo(this);
    }

    /**
     * Determines if this address is higher than or equal to a given address.
     *
     * @param address2   the address to compare this address against
     * @return true if this address is higher than or equal to <code>address2</code>
     *
     * @vm2c code( return this >= address2; )
     */
    public boolean hieq(Address address2) throws NativePragma {
        return address2.loeq(this);
    }

    /**
     * Rounds this address up based on a given alignment.
     *
     * @param alignment  this address is rounded up to be a multiple of this value
     * @return the new address
     *
     * @vm2c code( return (Address)roundUp((UWord)this, alignment); )
     */
    public Address roundUp(int alignment) throws NativePragma {
        return get((value + (alignment-1)) & ~(alignment-1));
    }

    /**
     * Rounds this address up to a machine word boundary.
     *
     * @return the new address
     *
     * @vm2c code( return (Address)roundUpToWord((UWord)this); )
     */
    public Address roundUpToWord() throws NativePragma {
        return get((value + (HDR.BYTES_PER_WORD-1)) & ~(HDR.BYTES_PER_WORD-1));
    }

    /**
     * Rounds this address down based on a given alignment.
     *
     * @param alignment  this address is rounded down to be a multiple of this value
     * @return the new address
     *
     * @vm2c code( return (Address)roundDown((UWord)this, alignment); )
     */
    public Address roundDown(int alignment) throws NativePragma {
        return get(value & ~(alignment-1));
    }

    /**
     * Rounds this address down to a machine word boundary.
     *
     * @return the new address
     *
     * @vm2c code( return (Address)roundDownToWord((UWord)this); )
     */
    public Address roundDownToWord() throws NativePragma {
        return get(value & ~(HDR.BYTES_PER_WORD-1));
    }

    /*-----------------------------------------------------------------------*\
     *                      Hosted execution support                         *
    \*-----------------------------------------------------------------------*/

    /**
     * Gets a hashcode value for this address which is just the address itself.
     *
     * @return  the value of this address
     */
    public int hashCode() throws HostedPragma {
        return (int)value;
    }

    /**
     * Gets a string representation of this address.
     *
     * @return String
     */
    public String toString() throws HostedPragma {
        return ""+value;
    }

    /**
     * The address represented.
     */
    private final int/*S64*/ value;

    /**
     * Unique instance pool.
     */
    private static /*S64*/IntHashtable pool;

    /**
     * Gets the canonical Address instance for a given address.
     *
     * @param  value   the machine address
     * @return the canonical Address instance for <code>value</code>
     */
    static Address get(int/*S64*/ value) throws HostedPragma {
        if (pool == null) {
            pool = new /*S64*/IntHashtable();
        }
        Address addr = (Address)pool.get(value);
        if (addr == null) {
            addr = new Address(value);
            try {
                pool.put(value, addr);
            } catch (OutOfMemoryError e) {
                throw new OutOfMemoryError("Failed to grow pool when adding " + value);
            }
        }
        return addr;
    }

    /**
     * Constructor.
     *
     * @param value  a machine address
     */
    private Address(int/*S64*/ value) throws HostedPragma {
        this.value = value;
    }

    /**
     * Checks that a given long value can be encoded as a 32 bit value.
     *
     * @param  value   the long value to check
     * @return 'value' converted to a 32 bit value
     */
    static int assert32(long value) throws HostedPragma {
        Assert.always((int)value == value, "address is out of 32 bit range");
        return (int)value;
    }

    /**
     * Converts the address into an integer index. This ensures that the memory being
     * modeled is 32 bit addressable which is a requirement given that the byte
     * array being used to model memory is only indexable by a 32 bit Java integer.
     *
     * @return this address as a 32 bit int
     */
    int asIndex() throws HostedPragma {
        return assert32(value);
    }
}
