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

import sun.misc.Unsafe;

/**
 * A type that is laid out in memory.
 * A Layout can allocate a Location, which is an address paired with the Layout
 * that allocated it.
 *
 * @author dr2chase
 */
public abstract class Layout<T> {

    public final static int BITS_PER_BYTE = 8;
    public final static int BITS_PER_SHORT = 16;
    public final static int BITS_PER_INT = 32;
    public final static int BITS_PER_LONG = 64;

    public final static int LOG_BITS_PER_BYTE = 3;
    public final static int LOG_BITS_PER_SHORT = 4;
    public final static int LOG_BITS_PER_INT = 5;
    public final static int LOG_BITS_PER_LONG = 6;

    public final static int BYTES_PER_BYTE = 1;
    public final static int BYTES_PER_SHORT = 2;
    public final static int BYTES_PER_INT = 4;
    public final static int BYTES_PER_LONG = 8;

    @SuppressWarnings("restriction")
    public final static int pointerSizeAndAlign = sun.misc.Unsafe.ADDRESS_SIZE;

    private final int align;
    private final long size;
    private final Class<T> cls;

    @Override
    public String toString() {
        return cls.getName() + ",s="+size+",a="+align;
    }

    /**
     * The alignment in bytes required to store a T.
     * If zero then this is bit-aligned.
     * @return
     */
    public int byteAlign() { return align; }
    /**
     * The size in bytes or bits required to store a T.
     * This is a bit count IFF byteAlign() = zero.
     * @return
     */
    public long byteOrBitSize() { return size; }
    /**
     * The class that this layout can format to memory.
     * @return
     */
    public Class<T> cls() { return cls; }

    // NOT PUBLIC, but visible to Location
    /**
     * Given a base and offset, return the T flat-stored at that address.
     * Access must be restricted to only from Location objects.
     *
     * @param base  Object containing storage, or null if non-GC'd
     * @param l Offset if object is non-null, or storage address.
     * @return
     */
    abstract T val(Object base, long l);
    T val(Location arena, Object base, long l) {
        return val(base, l);
    }

    // NOT PUBLIC, but visible to Location
    /**
     * Given a base, offset, and value,flat-store a T at that address.
     * Access must be restricted to only from Location objects.
     *
     * @param base  Object containing storage, or null if non-GC'd
     * @param l Offset if object is non-null, or storage address.
     * @param v Value to flatten at address.
     * @return
     */
    abstract void put(Object base, long l, T v);
    void put(Location arena, Object base, long l, T v) {
        put(base, l, v);
    }

    // NOT PUBLIC, but visible to Location
    void fillArray(Location arena, Object base, long addr, T[] array) {
        int count = array.length;
        for (int i = 0; i < count; i++) {
            array[i] = val(arena, base, addr);
            addr += byteOrBitSize();
        }
    }

    /**
     * Allocates a new location in the native (C) heap for this layout.
     *
     * @return a newly allocate location.
     */
    public Location<T> allocate() {
        @SuppressWarnings("restriction")
        long a = AtomLayout.u.allocateMemory(byteOrBitSize());
        return new Location<T>(null, this,  a);
    }

    /**
     * Allocates a location in an existing array of long for this layout.
     * If the array is not large enough an exception will be thrown.
     *
     * @return the location
     */
    @SuppressWarnings("restriction")
    public Location<T> allocateWithinArray(long[] bytes) {
        int n = bytes.length;
        long a = Unsafe.ARRAY_LONG_BASE_OFFSET;

        if (LayoutFactory.JS != Unsafe.ARRAY_LONG_INDEX_SCALE) {
            throw new Error("Long alignment and array scale index don't match");
        } else if (align > Unsafe.ARRAY_LONG_INDEX_SCALE) {
            throw new Error("Layout alignment too large for array scale index don't match");
        } else if (n * LayoutFactory.JS < size) {
            throw new Error("Array is not large enough for layout");
        }

        return new Location<T>(bytes, this,  a);
    }

    /**
     * Allocates a location within a mapped byte buffer.
     * If the buffer is too small or is not adequately aligned for this layout,
     * and exception will be thrown.
     *
     * @return the location.
     */
    public Location<T> allocateWithinMappedByteBuffer(java.nio.MappedByteBuffer mbb) {
        long a =
         (Long) PrivateUtil.getField(java.nio.Buffer.class, mbb, "address");
        long c = mbb.capacity();
        if (a == 0)
            throw new Error("Unexpected address (zero) from MappedByteBuffer");
        if (c < size)
            throw new Error("Buffer not large enough for layout");
        if (a % align != 0)
            throw new Error("Buffer not sufficiently aligned for layout, needed " +
                             align + " from address 0x" + Long.toHexString(a));
        return new Location<T>(null, this,  a);
    }

    /**
     * Allocates a location in a newly allocated array of long for this layout.
     *
     * @return the location
     */
    public Location<T> allocateWithinArray() {
        int n = (int) roundUp(size, LayoutFactory.JS) / LayoutFactory.JS;
        return allocateWithinArray(new long[n]);
    }

    Layout(long size, int align, Class<T> cls) {
        this.align = align;
        this.size = size;
        this.cls = cls;
    }

    public static long roundUp(long size, int align) {
        int x = (int) (size % align);
        if (x == 0) return size;
        return size + align - x;
    }

    /**
     * This class allows an expedient implementation of corner cases for
     * container-spanning bitfield accesses.
     *
     * @author dr2chase
     */

    static final class ThunkedForBitArrayIndexing<T> extends Layout<T> {
        private final long thunkedIndex;
        private final Location<T[]> thunkedArray;
        private final ArrayLayout<T> thunkedLayout;

        ThunkedForBitArrayIndexing(Location<T[]> addr,
                                   ArrayLayout<T> layout, long i) {
            super(layout.elementLayout().byteOrBitSize(),
                    layout.elementLayout().byteAlign(),
                    layout.elementLayout().cls()
                    );
            thunkedArray = addr;
            thunkedIndex = i;
            thunkedLayout = layout;
        }

        @Override
        T val(Object base, long l) {
            return thunkedLayout.val(thunkedArray, thunkedIndex);
        }

        @Override
        void put(Object base, long l, T v) {
            thunkedLayout.put(thunkedArray, thunkedIndex, v);
        }

    }


}
