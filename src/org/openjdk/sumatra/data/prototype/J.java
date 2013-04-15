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
 * A layout for a 64-bit integer, in a full-sized container.
 * @author dr2chase
 */
public final class J extends AtomLayout<Long> {

    /*
     * These constants almost parameterize by container size,
     * except where Java implicitly widens and sign-extends to int
     * when doing various bits of integer arithmetic.
     */
    public final static int BITS_PER_BOX = BITS_PER_LONG;
    public final static int LOG_BITS_PER_BOX = LOG_BITS_PER_LONG;
    public final static int BYTES_PER_BOX = BYTES_PER_LONG;

    @SuppressWarnings("restriction")
    private static long getBox(Object base, long offset) {
        return u.getLong(base, offset);
    }

    @SuppressWarnings("restriction")
    private static void putBox(Object base, long offset, long val) {
        u.putLong(base, offset, val);
    }

    J(Class cl, int s, int a) {
        super(s, a, cl);
    }

    // NOT PUBLIC, but visible to Location
    long prim(Object base, long l) {
        return getBox(base, l);
    }

    // NOT PUBLIC, but visible to Location
    void putPrim(Object base, long l, long v) {
        putBox(base, l, v);
    }

    // NOT PUBLIC, but visible to Location
    @Override
    Long val(Object base, long l) {
        return new Long(prim(base, l));
    }

    // NOT PUBLIC, but visible to Location
    @Override
    void put(Object base, long l, Long v) {
        putPrim(base, l, v.longValue());
    }

    // NOT PUBLIC, but visible to Location
    Object makeArray(Object base, long addr, int count) {
        long[] a = new long[count];
        for (int i = 0; i < count; i++) {
            a[i] = prim(base, addr);
            addr += byteOrBitSize();
        }
        return a;
    }

    @Override
    void fillArray(Location arena, Object base, long addr, Long[] a) {
        int count = a.length;
        for (int i = 0; i < count; i++) {
            a[i] = Long.valueOf(prim(base, addr));
            addr += byteOrBitSize();
        }
    }

    private final BitLE[] leUAccessors = new BitLE[BITS_PER_BOX];
    private final BitLE[] leSAccessors = new BitLE[BITS_PER_BOX];
    private final BitBE[] beUAccessors = new BitBE[BITS_PER_BOX];
    private final BitBE[] beSAccessors = new BitBE[BITS_PER_BOX];

    @Override
    AtomBitLayout<Long> bitsWideLE(int b) {
        if (b < 0) { // Signed
            b = -b;
            if (b > BITS_PER_BOX)
                throw new Error("Improper bit offset (negative of) " + -b);
            BitLE rv = leSAccessors[b];
            if (rv == null) {
                rv = new BitLESigned(b);
                leSAccessors[b] = rv;
            }
            return rv;
        } else { // Unsigned
            if (b > BITS_PER_BOX)
                throw new Error("Improper bit offset " + b);
            BitLE rv = leUAccessors[b];
            if (rv == null) {
                rv = new BitLEUnsigned(b);
                leUAccessors[b] = rv;
            }
            return rv;
        }
    }

    @Override
    AtomBitLayout<Long> bitsWideBE(int b) {
        if (b < 0) { // Signed
            b = -b;
            if (b > BITS_PER_BOX)
                throw new Error("Improper bit offset (negative of) " + -b);
            BitBE rv = beSAccessors[b];
            if (rv == null) {
                rv = new BitBESigned(b);
                beSAccessors[b] = rv;
            }
            return rv;
        } else { // Unsigned
            if (b > BITS_PER_BOX)
                throw new Error("Improper bit offset " + b);
            BitBE rv = beUAccessors[b];
            if (rv == null) {
                rv = new BitBEUnsigned(b);
                beUAccessors[b] = rv;
            }
            return rv;
        }
    }


    abstract public static class Bit extends  AtomBitLayout<Long> {
        @Override
        int containerBitSizeAndAlignment() {
            return BITS_PER_BOX;
        }

        protected final long storeMask;

        // Cannot be protected or public.
        Bit(int s, long mask) {
            super(s, 0, Long.class);
            storeMask = mask;
        }

        abstract long prim(Object base, long l, long b);
        abstract void putPrim(Object base, long l, long b, long v);

        // NOT PUBLIC, but visible to Location
        long prim(Object base, long l) {
            throw new UnsupportedOperationException(
                    "bit-aligned fields do not support byte-addressing");
        }

        // NOT PUBLIC, but visible to Location
        void putPrim(Object base, long l, long v) {
            throw new UnsupportedOperationException(
                    "bit-aligned fields do not support byte-addressing");
        }

        // NOT PUBLIC, but visible to Location
        @Override
        Long val(Object base, long l) {
            return new Long(prim(base, l));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        void put(Object base, long l, Long v) {
            putPrim(base, l, v.longValue());
        }

        // NOT PUBLIC, but visible to Location
        @Override
        Long val(Object base, long l, long b) {
            return new Long(prim(base, l, b));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        void put(Object base, long l, long b, Long v) {
            putPrim(base, l, b, v.longValue());
        }

        // NOT PUBLIC, but visible to Location
        Object makeArray(Object base, long addr, int count) {
            long[] a = new long[count];
            long offset = 0;
            long bump = byteOrBitSize();
            for (int i = 0; i < count; i++) {
                a[i] = prim(base, addr, offset);
                offset += bump;
            }
            return a;
        }

        @Override
        void fillArray(Location arena, Object base, long addr, Long[] a) {
            int count = a.length;
            long offset = 0;
            long bump = byteOrBitSize();
            for (int i = 0; i < count; i++) {
                a[i] = Long.valueOf(prim(base, addr, offset));
                offset += bump;
            }
        }

        private final BitAt[] fixedAccessors = new BitAt[BITS_PER_BOX];

        @Override
        final Layout<Long> atFixedOffset(int b) {
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


    /**
     * A layout for an integer stored in a bit field, bit offset specified
     * in the accessors.  Will load/store fields that straddle boundaries.
     * @author dr2chase
     */
    abstract static class BitLE extends  Bit {
        BitLE(int s) {
            super(s, (1L << s) - 1);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        abstract long prim(Object base, long l, long b);

        // NOT PUBLIC, but visible to Location
        @Override
        final void putPrim(Object base, long l, long b, long v) {
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);
            int ib = (int) b & (BITS_PER_BOX-1);
            long container = getBox(base, l) & ~(storeMask << ib);
            int slop = ib + (int) byteOrBitSize() - BITS_PER_BOX;
            v = v & storeMask;
            putBox(base, l, container | (v << ib));
            if (slop > 0) {
                slop = (int) byteOrBitSize() - slop;
                // Should not happen with layouts derived from C data structures
                l += BYTES_PER_BOX;
                container = getBox(base, l) & ~(storeMask >>> slop);
                putBox(base, l, container | (v >>> slop));
            }
        }

    }

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
        final long prim(Object base, long l, long b) {
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
                long container = getBox(base, l) << before_bits_above;
                return (container >>> after_bits_above);
            } else {
                // Should not happen with layouts derived from C data structures
                before_bits_above = BITS_PER_BOX + before_bits_above;

                long container = getBox(base, l) & (storeMask << ib);
                long other_container =
                        getBox(base, l+BYTES_PER_BOX) << before_bits_above;
                return (container >>> ib) | (other_container >>> after_bits_above);
            }
        }
    }

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
        final long prim(Object base, long l, long b) {
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
                long container = getBox(base, l) << before_bits_above;
                return (container >> after_bits_above);
            } else {
                // Should not happen with layouts derived from C data structures
                before_bits_above = BITS_PER_BOX + before_bits_above;

                long container = getBox(base, l) & (storeMask << ib);
                long other_container =
                        getBox(base, l+BYTES_PER_BOX) << before_bits_above;
                return (container >>> ib) | (other_container >> after_bits_above);
            }
        }
    }

    /**
     * A layout for an integer stored in a bit field, bit offset specified
     * in the accessors.  Will load/store fields that straddle boundaries.
     * @author dr2chase
     */
    static abstract class BitBE extends Bit {

        // Cannot be protected or public.
        BitBE(int s) {
            super(s, (1L << s) - 1);
        }


        // NOT PUBLIC, but visible to Location
        @Override
        void putPrim(Object base, long l, long b, long v) {
            l += (b >>> LOG_BITS_PER_BOX) << (LOG_BITS_PER_BOX - LOG_BITS_PER_BYTE);
            int ib0 = (int) b & (BITS_PER_BOX-1);
            int ib = BITS_PER_BOX - ib0 - (int) byteOrBitSize();
            v = v & storeMask;
            if (ib >= 0) {
                long container = getBox(base, l) & ~(storeMask << ib);
                putBox(base, l, container | (v << ib));
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                // Note that excess bits from v are shifted off one end or the
                // other for the two stores.
                int slop = BITS_PER_BOX + ib;
                ib = -ib;
                long container = getBox(base, l) & ~(storeMask >>> ib);
                putBox(base, l, container | (v >>> ib));

                l += BYTES_PER_BOX;
                long other_container = getBox(base, l) & ~(storeMask << slop);
                putBox(base, l, other_container | (v << slop));
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
        long prim(Object base, long l, long b) {
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
                long container = getBox(base, l);
                // Assume bitfields do not span an long-sized boundary.
                return (container >>> ib) & storeMask;
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                int slop = BITS_PER_BOX + ib;
                ib = -ib;
                long container = getBox(base, l);
                long other_container = getBox(base, l+BYTES_PER_BOX) ;
                return storeMask & ((container << ib) | (other_container >>> slop));
            }
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtUnsigned(this, BITS_PER_LONG - bit_offset - (int) byteOrBitSize());
        }

    }

    public final static class BitBESigned extends BitBE {
        // Cannot be protected or public.
        BitBESigned(int s) {
            super(s);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        long prim(Object base, long l, long b) {
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
                long container = getBox(base, l) << ib0;
                // Assume bitfields do not span an long-sized boundary.
                return container >> (ib1);
            } else {
                // Should not happen with layouts derived from C data structures
                // But it handles the array case nicely.
                int slop = BITS_PER_BOX + ib;
                long container = getBox(base, l) << ib0;
                long other_container = getBox(base, l+BYTES_PER_BOX) ;
                return ((container >> ib1) | (other_container >>> slop));
            }
        }

        @Override
        final BitAt newBitAt(int bit_offset) {
            return new BitAtSigned(this, BITS_PER_LONG - bit_offset - (int) byteOrBitSize());
        }

    }

    public static class BitAtSigned extends BitAt {
        BitAtSigned(Bit layout, int bit_offset) {
            super(layout, bit_offset);
        }
        @Override
        long prim(Object base, long l) {
            long container = getBox(base, l) << bitsAbove;
            // Assume bitfields do not span an long-sized boundary.
            return (container >> (bitOffset + bitsAbove));
        }
    }
    public static class BitAtUnsigned extends BitAt {
        BitAtUnsigned(Bit layout, int bit_offset) {
            super(layout, bit_offset);
        }
        @Override
        long prim(Object base, long l) {
            long container = getBox(base, l) << bitsAbove;
            // Assume bitfields do not span an long-sized boundary.
            return (container >>> (bitOffset + bitsAbove));
        }
    }
    /**
     * A layout for an integer stored in a bit field at a particular offset.
     * The bitfield is restricted to not span a word boundary (because author
     * is lazy, and so were the authors of C compilers on tested platforms,
     * MacOS-Intel and Solaris-Sparc).
     * @author dr2chase
     */
    abstract public static class BitAt extends Layout<Long> {
        protected final int bitOffset;
        protected final int bitsAbove;
        protected final long storeMask;
        BitAt(Bit layout, int bit_offset) {
            super((int) layout.byteOrBitSize(), 0, layout.cls());
            storeMask =  ((1L << layout.byteOrBitSize()) - 1) << bit_offset;
            bitOffset = bit_offset;
            bitsAbove = BITS_PER_BOX - bit_offset - (int) layout.byteOrBitSize();
        }

        @Override
        public String toString() {
            return super.toString() + ",@b" + bitOffset;
        }

        // NOT PUBLIC, but visible to Location
        abstract long prim(Object base, long l);

        // NOT PUBLIC, but visible to Location
        final void putPrim(Object base, long l, long v) {
            v = (storeMask & (v << bitOffset));
            long container = getBox(base, l) & ~storeMask;
            // Assume bitfields are unsigned unless they are full width.
            // Assume bitfields do not span an long-sized boundary.
            putBox(base, l, container | v);
        }

        // NOT PUBLIC, but visible to Location
        @Override
        final Long val(Object base, long l) {
            return new Long(prim(base, l));
        }

        // NOT PUBLIC, but visible to Location
        @Override
        final void put(Object base, long l, Long v) {
            putPrim(base, l, v.longValue());
        }

        @Override
        final void fillArray(Location arena, Object base, long addr, Long[] a) {
            throw new UnsupportedOperationException(
                "bit-aligned, offset-pinned fields do not support array ops");
        }
    }
}