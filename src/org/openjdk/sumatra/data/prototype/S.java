/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package org.openjdk.sumatra.data.prototype;

import org.openjdk.sumatra.data.prototype.AtomBitLayout;

/**
 * A layout for a 16-bit integer, in a 16-bit-sized container.
 */
public final class S extends AtomLayout<Short> {

    /*
     * These constants almost parameterize by container size,
     * except where Java implicitly widens and sign-extends to int
     * when doing various bits of integer arithmetic.
     */
    public final static int BITS_PER_BOX = BITS_PER_SHORT;
    public final static int LOG_BITS_PER_BOX = LOG_BITS_PER_SHORT;
    public final static int BYTES_PER_BOX = BYTES_PER_SHORT;
    public final static int BOX_MASK = -1 >>> (BITS_PER_INT - BITS_PER_BOX);

    /**
     * Used for wiping zero/sign bits across short bitfields stored in
     * an int-sized value.
     */
    public final static int ADJUST = BITS_PER_INT - BITS_PER_BOX;

    public static String hex(long s) {
        return "0x"+ Long.toHexString(BOX_MASK & s);
    }

    /**
     * @param b
     * @return
     */
    private static long bitOffsetToByteAddressOfBoxBoundary(long b) {
        return (b >>> LOG_BITS_PER_BOX) <<
                (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);
    }
    @SuppressWarnings("restriction")
    private static short getBox(Object base, long offset) {
        return u.getShort(base, offset);
    }

    @SuppressWarnings("restriction")
    private static void putBox(Object base, long offset, short val) {
        u.putShort(base, offset, val);
    }

    S(Class cl, int s, int a) {
        super(s, a, cl);
    }

    // NOT PUBLIC, but visible to Location
    short prim(Object base, long l) {
        return getBox(base, l);
    }

    // NOT PUBLIC, but visible to Location
    void putPrim(Object base, long l, short v) {
        putBox(base, l, v);
    }

    // NOT PUBLIC, but visible to Location
    @Override
    Short val(Object base, long l) {
        return new Short(prim(base, l));
    }

    // NOT PUBLIC, but visible to Location
    @Override
    void put(Object base, long l, Short v) {
        putPrim(base, l, v.shortValue());
    }

    // NOT PUBLIC, but visible to Location
    Object makeArray(Object base, long addr, int count) {
        short[] a = new short[count];
        long inc = byteOrBitSize();
        for (int i = 0; i < count; i++) {
            a[i] = prim(base, addr);
            addr += inc;
        }
        return a;
    }

    @Override
    void fillArray(Location arena, Object base, long addr, Short[] a) {
        int count = a.length;
        long inc = byteOrBitSize();
        for (int i = 0; i < count; i++) {
            a[i] = Short.valueOf(prim(base, addr));
            addr += inc;
        }
    }

    /*
     * For each signedness, for each endianness, there is one layout
     * for each possible bit width.
     */
    private final BitLE[] leUAccessors = new BitLE[BITS_PER_BOX];
    private final BitLE[] leSAccessors = new BitLE[BITS_PER_BOX];
    private final BitBE[] beUAccessors = new BitBE[BITS_PER_BOX];
    private final BitBE[] beSAccessors = new BitBE[BITS_PER_BOX];

    @Override
    AtomBitLayout<Short> bitsWideLE(int b) {
        if (b < 0) { // Signed
            b = -b;
            if (b > BITS_PER_BOX)
                throw new Error("Bit offset too large: (negative of) " + -b);
            BitLE rv = leSAccessors[b];
            if (rv == null) {
                rv = new BitLESigned(b);
                leSAccessors[b] = rv;
            }
            return rv;
        } else { // Unsigned
            if (b > BITS_PER_BOX)
                throw new Error("Bit offset too large: " + b);
            BitLE rv = leUAccessors[b];
            if (rv == null) {
                rv = new BitLEUnsigned(b);
                leUAccessors[b] = rv;
            }
            return rv;
        }
    }

    @Override
    AtomBitLayout<Short> bitsWideBE(int b) {
        if (b < 0) { // Signed
            b = -b;
            if (b > BITS_PER_BOX)
                throw new Error("Bit offset too large: (negative of) " + -b);
            BitBE rv = beSAccessors[b];
            if (rv == null) {
                rv = new BitBESigned(b);
                beSAccessors[b] = rv;
            }
            return rv;
        } else { // Unsigned
            if (b > BITS_PER_BOX)
                throw new Error("Bit offset too large: " + b);
            BitBE rv = beUAccessors[b];
            if (rv == null) {
                rv = new BitBEUnsigned(b);
                beUAccessors[b] = rv;
            }
            return rv;
        }
    }

    /**
     * The superclass of all accessor-offset-specified
     * big/little signed/unsigned short bit fields.
     * These are used for defining layouts and arrays.
     * The bitfields described by this class and its
     * subtypes may straddle a box boundary.
     *
     * Note that bitfields within tuples are described
     * by different layouts derived from BitAt.
     */
    abstract public static class Bit extends  AtomBitLayout<Short> {
        @Override
        int containerBitSizeAndAlignment() {
            return BITS_PER_BOX;
        }

        protected final int storeMask;

        // Cannot be protected or public.
        Bit(int s, int mask) {
            super(s, 0, Short.class);
            storeMask = mask;
        }

        abstract short prim(Object base, long l, long b);
        abstract void putPrim(Object base, long l, long b, short v);

        // NOT PUBLIC, but visible to Location
        short prim(Object base, long l) {
            throw new UnsupportedOperationException(
                    "bit-aligned fields do not support byte-addressing");
        }

        // NOT PUBLIC, but visible to Location
        void putPrim(Object base, long l, short v) {
            throw new UnsupportedOperationException(
                    "bit-aligned fields do not support byte-addressing");
        }

        // NOT PUBLIC, but visible to Location
        @Override
        Short val(Object base, long l) {
            return new Short(prim(base, l));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        void put(Object base, long l, Short v) {
            putPrim(base, l, v.shortValue());
        }

        // NOT PUBLIC, but visible to Location
        @Override
        Short val(Object base, long l, long b) {
            return new Short(prim(base, l, b));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        void put(Object base, long l, long b, Short v) {
            putPrim(base, l, b, v.shortValue());
        }

        // NOT PUBLIC, but visible to Location
        Object makeArray(Object base, long addr, int count) {
            short[] a = new short[count];
            long offset = 0;
            long bump = byteOrBitSize();
            for (int i = 0; i < count; i++) {
                a[i] = prim(base, addr, offset);
                offset += bump;
            }
            return a;
        }

        @Override
        void fillArray(Location arena, Object base, long addr, Short[] a) {
            int count = a.length;
            long offset = 0;
            long bump = byteOrBitSize();
            for (int i = 0; i < count; i++) {
                a[i] = Short.valueOf(prim(base, addr, offset));
                offset += bump;
            }
        }

        private final BitAt[] fixedAccessors = new BitAt[BITS_PER_BOX];

        @Override
        final Layout<Short> atFixedOffset(int b) {
            if (b < 0 || b + byteOrBitSize() > BITS_PER_BOX)
                throw new Error("Improper bit offset " + b);
            BitAt rv = fixedAccessors[b];
            if (rv == null) {
                rv = newBitAt(b);
                fixedAccessors[b] = rv;
            }
            return rv;
        }

        abstract BitAt newBitAt(int bit_offset);
    }

    /** Little-endian bitfields, either signed or unsigned */
    abstract static class BitLE extends  Bit {
        BitLE(int s) {
            super(s, (1 << s) - 1);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        abstract short prim(Object base, long l, long b);

        // NOT PUBLIC, but visible to Location
        @Override
        final void putPrim(Object base, long l, long b, short v) {
            l += bitOffsetToByteAddressOfBoxBoundary(b);
            int ib = (int) b & (BITS_PER_BOX-1);
            int container = getBox(base, l) & ~(storeMask << ib);
            int slop = ib + (int) byteOrBitSize() - BITS_PER_BOX;
            v = (short)(v & storeMask);
            short store = (short)(container | (v << ib));
            putBox(base, l, store);
            if (slop > 0) {
                slop = (int) byteOrBitSize() - slop;
                // Should not happen with layouts derived from C data structures
                l += BYTES_PER_BOX;
                container = getBox(base, l) & ~(storeMask >>> slop);
                store = (short)(container | (v >>> slop));
                putBox(base, l, store);
            }
        }

    }

    /** Little-endian unsigned bitfields. */
    public final static class BitLEUnsigned extends BitLE {
        // Cannot be protected or public.
        BitLEUnsigned(int s) {
            super(s);
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtUnsigned(this, bit_offset);
        }

        @Override
        final short prim(Object base, long l, long b) {
            // There's container-size assumptions buried here.
            // Designed to work on little-endian hardware (Intel)
            l += bitOffsetToByteAddressOfBoxBoundary(b);
            int ib = (int) b & (BITS_PER_BOX-1);

            // number of unused bits "above" this bitfield in the first word
            // negative if it extends into another word.
            int after_bits_above = BITS_PER_BOX - (int) byteOrBitSize();
            int before_bits_above = after_bits_above - ib;
            /*
             * Careful -- note that this is little endian bitfields,
             * but the pictures are big-endian.
             *
             *  within word:
             *  MSB aaaVVVVb LSB
             *  bitsAbove = 3 = 8 - 1 - 4.
             *
             *  across word
             *  MSB _______V VVVbbbbb LSB
             *  bitsAbove = -1  = 8 - 5 - 4
             */

            if (before_bits_above >= 0) {
                int container = getBox(base, l) << (ADJUST + before_bits_above);
                return (short) (container >>> (ADJUST + after_bits_above));
            } else {
                // Should not happen with layouts derived from C data structures
                before_bits_above = BITS_PER_BOX + before_bits_above;

                int container = getBox(base, l) & BOX_MASK & (storeMask << ib) ;
                int other_container =
                        getBox(base, l+BYTES_PER_BOX) << (ADJUST + before_bits_above);
                return (short)((container >>> ib) |
                           (other_container >>> (ADJUST + after_bits_above)));
            }
        }
    }

    /** Little-endian signed bitfields. */
    public final static class BitLESigned extends BitLE {
        // Cannot be protected or public.
        BitLESigned(int s) {
            super(s);
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtSigned(this, bit_offset);
        }

        @Override
        final short prim(Object base, long l, long b) {
            // There's container-size assumptions buried here.
            // Designed to work on little-endian hardware (Intel)
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);
            int ib = (int) b & (BITS_PER_BOX-1);

            // number of unused bits "above" this bitfield in the first word
            // negative if it extends into another word.
            int after_bits_above = BITS_PER_BOX - (int) byteOrBitSize();
            int before_bits_above = after_bits_above - ib;
            /*
             * Careful -- note that this is little endian bitfields,
             * but the pictures are big-endian.
             *
             *  within word:
             *  MSB aaaVVVVb LSB
             *  bitsAbove = 3 = 8 - 1 - 4.
             *
             *  across word
             *  MSB _______V VVVbbbbb LSB
             *  bitsAbove = -1  = 8 - 5 - 4
             */

            if (before_bits_above >= 0) {
                int container = getBox(base, l) << (ADJUST + before_bits_above);
                return (short)(container >> (ADJUST + after_bits_above));
            } else {
                // Should not happen with layouts derived from C data structures
                before_bits_above = BITS_PER_BOX + before_bits_above;

                int container = getBox(base, l) & BOX_MASK& (storeMask << ib);
                int other_container =
                        getBox(base, l+BYTES_PER_BOX) << (ADJUST + before_bits_above);
                return (short)((container >>> ib) |
                        (other_container >> (ADJUST + after_bits_above)));
            }
        }
    }

    /** Big-endian bitfields, either signed or unsigned */
    static abstract class BitBE extends Bit {

        // Cannot be protected or public.
        BitBE(int s) {
            super(s, (1 << s) - 1);
        }


        // NOT PUBLIC, but visible to Location
        @Override
        final void putPrim(Object base, long l, long b, short v) {
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);
            int ib0 = (int) b & (BITS_PER_BOX-1);
            int ib = BITS_PER_BOX - ib0 - (int) byteOrBitSize();
            v = (short)(v & storeMask);
            if (ib >= 0) {
                int container = getBox(base, l) & ~(storeMask << ib);
                putBox(base, l, (short)(container | (v << ib)));
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                // Note that excess bits from v are shifted off one end or the
                // other for the two stores.
                int slop = BITS_PER_BOX + ib;
                ib = -ib;
                int container = getBox(base, l) & ~(storeMask >>> ib);
                putBox(base, l, (short)(container | (v >>> ib)));

                l += BYTES_PER_BOX;
                int other_container = getBox(base, l) & ~(storeMask << slop);
                putBox(base, l, (short)(other_container | (v << slop)));
            }
        }

    }

    public final static class BitBEUnsigned extends BitBE {
        // Cannot be protected or public.
        BitBEUnsigned(int s) {
            super(s);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        short prim(Object base, long l, long b) {
            // There's endianness and container-size assumptions buried here.
            // Designed to work on big-endian hardware (Sparc)
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);

            // big endian, treat the high order bit as "zero",
            // shift the field up against its least-numbered end.

            // offset from HO (High Order) bit
            int ib0 = (int) b & (BITS_PER_BOX-1);
            // shift to align LO of field with LO of result.
            int ib = BITS_PER_BOX - ib0 - (int) byteOrBitSize();

            if (ib >= 0) {
                // left shift
                int container = getBox(base, l);
                // Assume bitfields do not span an int-sized boundary.
                return (short)((container >>> ib) & storeMask);
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                int slop = BITS_PER_BOX + ib;
                ib = -ib;
                int container = getBox(base, l);
                int other_container = BOX_MASK & getBox(base, l+BYTES_PER_BOX) ;
                return (short)(storeMask & ((container << ib) | (other_container >>> slop)));
            }
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtUnsigned(this, BITS_PER_BOX - bit_offset - (int) byteOrBitSize());
        }

    }

    public final static class BitBESigned extends BitBE {
        // Cannot be protected or public.
        BitBESigned(int s) {
            super(s);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        short prim(Object base, long l, long b) {
            // There's endianness and container-size assumptions buried here.
            // Designed to work on big-endian hardware (Sparc)
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);

            // big endian, treat the high order bit as "zero",
            // shift the field up against its least-numbered end.

            // offset from HO (High Order) bit
            int ib0 = (int) b & (BITS_PER_BOX-1);
            // shift right to align LO of field with LO of result.
            int ib1 = BITS_PER_BOX - (int) byteOrBitSize();
            int ib = ib1 - ib0;

            if (ib >= 0) {
                // left shift
                int container = getBox(base, l) << (ADJUST + ib0);
                // Assume bitfields do not span an int-sized boundary.
                return (short)(container >> (ADJUST + ib1));
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                int slop = BITS_PER_BOX + ib;
                int container = getBox(base, l) << (ADJUST + ib0);
                int other_container = getBox(base, l+BYTES_PER_BOX) & BOX_MASK;
                return (short)((container >> (ADJUST + ib1)) | (other_container >>> slop));
            }
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtSigned(this, BITS_PER_BOX - bit_offset - (int) byteOrBitSize());
        }

    }

    /**
     * A layout for an integer stored in a bit field at a particular bit offset
     * within a container at a specified byte address.
     * The bitfield is restricted to not span a word boundary because that
     * corresponds to the usual behavior of C compiler laying out bitfields.
     * These bitfields are endian-agnostic; they rely on their caller to choose
     * the appropriate shift distance.
     */
    abstract public static class BitAt extends Layout<Short> {
        protected final int bitOffset;
        protected final int bitsAbove;
        protected final int storeMask;
        BitAt(Bit layout, int bit_offset) {
            super((int) layout.byteOrBitSize(), 0, layout.cls());
            storeMask =  ((1 << layout.byteOrBitSize()) - 1) << bit_offset;
            bitOffset = bit_offset;
            // Bits per int, not Bits per box, because of int container.
            bitsAbove = BITS_PER_INT - bit_offset - (int) layout.byteOrBitSize();
        }

        @Override
        public String toString() {
            return super.toString() + ",@b" + bitOffset;
        }

        // NOT PUBLIC, but visible to Location
        abstract short prim(Object base, long l);

        // NOT PUBLIC, but visible to Location
        final void putPrim(Object base, long l, short v) {
            //System.out.println("Layout " + this + ", storing " + hex(v) + " at byte " + hex(l));
            v = (short) (storeMask & (v << bitOffset));
            int container = getBox(base, l) & ~storeMask;
            // Assume bitfields are unsigned unless they are full width.
            // Assume bitfields do not span an int-sized boundary.
            short store = (short)(container | v);
            //System.out.println("Old container=" + hex(container) + ", new value=" + hex(store));
            putBox(base, l, store);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        final Short val(Object base, long l) {
            return new Short(prim(base, l));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        final void put(Object base, long l, Short v) {
            putPrim(base, l, v.shortValue());
        }

        @Override
        final void fillArray(Location arena, Object base, long addr, Short[] a) {
            throw new UnsupportedOperationException(
                "bit-aligned, offset-pinned fields do not support array ops");
        }
    }

    public static class BitAtSigned extends BitAt {
        BitAtSigned(Bit layout, int bit_offset) {
            super(layout, bit_offset);
        }
        @Override
        short prim(Object base, long l) {
            int container = getBox(base, l) << bitsAbove;
            // Assume bitfields do not span an int-sized boundary.
            return (short)(container >> (bitOffset + bitsAbove));
        }
    }

    public static class BitAtUnsigned extends BitAt {
        BitAtUnsigned(Bit layout, int bit_offset) {
            super(layout, bit_offset);
        }
        @Override
        short prim(Object base, long l) {
            int container = getBox(base, l) << bitsAbove;
            // Assume bitfields do not span an int-sized boundary.
            return (short)(container >>> (bitOffset + bitsAbove));
        }
    }
}