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

abstract class AtomLayout<T> extends Layout<T> {

    @SuppressWarnings("restriction")
    private static Unsafe  getUnsafe() {
        Unsafe unsafe =
                (Unsafe) PrivateUtil.getField(sun.misc.Unsafe.class, null, "theUnsafe");
        return unsafe;
    }

    @SuppressWarnings("restriction")
    private static boolean isBigEndian() {
        long addr = u.allocateMemory(8);
        u.putLong(addr, 0x1122334455667788L);
        boolean rc =  u.getByte(addr) == 0x11;
        u.freeMemory(addr);
        return rc;
    }

    @SuppressWarnings("restriction")
    static final Unsafe u = getUnsafe();
    static final boolean isBigEndian = isBigEndian();

    AtomLayout(int size, int align, Class<T> cl) {
        super(size, align, cl);
    }

    /**
     * Return a layout for an n-bit wide primitive type.
     * The returned layout will have an endianness appropriate for the
     * architecture on which the application is runnning.
     *
     * @param b
     * @return
     */
    AtomBitLayout<T> bitsWide(int b) {
        return isBigEndian ? bitsWideBE(b) : bitsWideLE(b);
    }

    /**
     * Returned a layout for an n-bit wide primitive type,
     * stored in little-endian order.
     *
     * Little-endian means that bit 0 of the underlying container
     * corresponds to the value 1.
     *
     * @param bits
     * @return
     */
    abstract AtomBitLayout<T> bitsWideLE(int bits);

    /**
     * Returned a layout for an n-bit wide primitive type,
     * stored in big-endian order.
     *
     * Big-endian means that bit 0 of the underlying container
     * is the high-order (unsigned) or sign (signed) bit.
     *
     * @param bits
     * @return
     */
    abstract AtomBitLayout<T> bitsWideBE(int bits);
}
