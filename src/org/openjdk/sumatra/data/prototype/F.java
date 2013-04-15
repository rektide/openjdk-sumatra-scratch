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
 * A layout for a 32-bit float, in a 32-bit container.
 * @author dr2chase
 */
public final class F extends AtomLayout<Float> {

    /*
     * These constants almost parameterize by container size,
     * except where Java implicitly widens and sign-extends to int
     * when doing various bits of integer arithmetic.
     */
    public final static int BITS_PER_BOX = BITS_PER_INT;
    public final static int LOG_BITS_PER_BOX = LOG_BITS_PER_INT;
    public final static int BYTES_PER_BOX = BYTES_PER_INT;

    @SuppressWarnings("restriction")
    private static float getBox(Object base, long offset) {
        return u.getFloat(base, offset);
    }

    @SuppressWarnings("restriction")
    private static void putBox(Object base, long offset, float val) {
        u.putFloat(base, offset, val);
    }

    F(Class cl, int s, int a) {
        super(s, a, cl);
    }

    // NOT PUBLIC, but visible to Location
    float prim(Object base, long l) {
        return getBox(base, l);
    }

    // NOT PUBLIC, but visible to Location
    void putPrim(Object base, long l, float v) {
        putBox(base, l, v);
    }

    // NOT PUBLIC, but visible to Location
    @Override
    Float val(Object base, long l) {
        return new Float(prim(base, l));
    }

    // NOT PUBLIC, but visible to Location
    @Override
    void put(Object base, long l, Float v) {
        putPrim(base, l, v.intValue());
    }

    // NOT PUBLIC, but visible to Location
    Object makeArray(Object base, long addr, int count) {
        float[] a = new float[count];
        for (int i = 0; i < count; i++) {
            a[i] = prim(base, addr);
            addr += byteOrBitSize();
        }
        return a;
    }

    @Override
    void fillArray(Location arena, Object base, long addr, Float[] a) {
        int count = a.length;
        for (int i = 0; i < count; i++) {
            a[i] = Float.valueOf(prim(base, addr));
            addr += byteOrBitSize();
        }
    }

    @Override
    AtomBitLayout<Float> bitsWideLE(int b) {
        throw new Error("Floats don't fit in bitfields");
    }

    @Override
    AtomBitLayout<Float> bitsWideBE(int b) {
        throw new Error("Floats don't fit in bitfields");
    }
}