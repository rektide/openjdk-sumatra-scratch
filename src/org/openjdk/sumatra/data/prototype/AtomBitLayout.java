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

public abstract class AtomBitLayout<T> extends Layout<T> {

    AtomBitLayout(int size, int align, Class<T> cl) {
        super(size, align, cl);
    }

    abstract Layout<T> atFixedOffset(int bits);
    /**
     * Returns the size of the container that bitfields are bucketed into.
     * Affects layout of tuples, and size of arrays, interacts with endianness.
     * @return
     */
    abstract int containerBitSizeAndAlignment();

    // This has a layout policy embedded in it!
    public final long actualBytesRequiredFor(long count) {
        int container = containerBitSizeAndAlignment();
        long bit_size = count * byteOrBitSize();
        bit_size = (bit_size + (container-1)) & ~(container-1);
        return bit_size >>> LOG_BITS_PER_BYTE;
    }

    // NOT PUBLIC, but visible to Location
    abstract T val(Object base, long l, long b);

    // NOT PUBLIC, but visible to Location
    abstract void put(Object base, long l, long b, T v);


}
