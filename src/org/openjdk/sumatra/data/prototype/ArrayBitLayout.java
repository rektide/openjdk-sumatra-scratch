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

public final class ArrayBitLayout<T> extends ArrayLayout<T> {

    protected final AtomBitLayout<T> element;
    protected final boolean eltSizeIsPowerOfTwo;

    // BUG! The bitwise array layout decisions are scattered between this class
    // and the bitwise element layout classes!!!

    // Package protection, NOT protected-visibility
    ArrayBitLayout(AtomBitLayout<T> element, long count) {
        super(element.actualBytesRequiredFor(count),
                element.containerBitSizeAndAlignment() >>> Layout.LOG_BITS_PER_BYTE,
                element,
                count);
        this.element = element;
        long eltSize = element.byteOrBitSize();
        this.eltSizeIsPowerOfTwo = eltSize == 1 || eltSize == 2 ||
                eltSize == 4 || eltSize == 8 || eltSize == 16 || eltSize == 32;
    }

    @Override
    public Layout<T> elementLayout() {
        return element;
    }


    @Override
    <U> Location<U>  loc(Location<T[]> addr, long i) {

        // BUG.
        // Okay, what to do here?  It can really only be a location of T.
        // This borks Java type rules, but not memory safety.

        if (i < 0 || i >= length()) {
            throw new ArrayIndexOutOfBoundsException(String.valueOf(i));
        }

        if (eltSizeIsPowerOfTwo) {
            long raw = addr.addr();
            long bits = i * element.byteOrBitSize();
            int container = element.containerBitSizeAndAlignment();
            // put all byte offset into "bits"
            raw = raw + (bits >>> Layout.LOG_BITS_PER_BYTE);
            bits = bits & (Layout.BITS_PER_BYTE-1);
            // normalize to a container boundary
            int raw_excess = (int) (raw & ((container - 1) >>>
                                           Layout.LOG_BITS_PER_BYTE));
            raw = raw - raw_excess;
            bits = bits + (raw_excess << Layout.LOG_BITS_PER_BYTE);

            return (Location<U>) new Location<T>(addr,
                    element.atFixedOffset((int) bits),
                    raw);
        } else {
            Layout<T> thunked = new Layout.ThunkedForBitArrayIndexing<T>(
                    addr, this, i);
            return  (Location<U>) new Location<T>(null, thunked, 0);
        }
    }

    @Override
    void put(Object base, long l, T[] v) {
        int x = v.length;
        long s = element.byteOrBitSize();
        if (x > length()) x = (int) length();
        for (int i = 0; i < x; i++) {
            element.put(base, l, v[i]);  // Bit-packed, arena can be null.
            l += s;
        }
    }

    @Override
    T[] val(Object base, long l) {
        if (count > Integer.MAX_VALUE)
            throw new Error("Cannot convert flattened data to array; too many elements");
        int icount = (int) count;
        T[] o = (T[]) (java.lang.reflect.Array.newInstance(element.cls(), icount));
        element.fillArray(null, base, l, o); // Bit-packed, arena can be null.
        return o;
    }

    // NOT PUBLIC, but visible to ArrayLocation
    @Override
    T val(Location<T[]> loc, long l) {
       return element.val(loc.base(), loc.addr(), l * element.byteOrBitSize());
   }

   // NOT PUBLIC, but visible to ArrayLocation
    @Override
    void put(Location<T[]> loc, long l, T v) {
        element.put(loc.base(), loc.addr(), l * element.byteOrBitSize(), v);
    }

}
