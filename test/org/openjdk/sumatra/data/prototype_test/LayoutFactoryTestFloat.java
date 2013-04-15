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

package org.openjdk.sumatra.data.prototype_test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openjdk.sumatra.data.prototype.Layout;
import org.openjdk.sumatra.data.prototype.LayoutFactory;
import org.openjdk.sumatra.data.prototype.Location;
import org.openjdk.sumatra.data.prototype.Matrix;
import org.openjdk.sumatra.data.prototype.MatrixFactory;

public class LayoutFactoryTestFloat extends TestCommon {

    static class Pair<T> {
        private final T a, b;
        Pair(T a, T b) {
            this.a = a; this.b = b;
        }

        T a() { return a; }
        T b() { return b; }

    }

    public static class IP1 {
        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof IP1) {
                IP1 other = (IP1) obj;
                return a == other.a && b == other.b;
            }
            return false;
        }
        float a;
        float b;
        IP1(float a, float b){this.a = a; this.b = b; }
        public Float a() { return a; }
        public Float b() { return b; }
        public static IP1 valueOf(Float a, Float b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    public static class IP2 {
        float a;
        Float[] b;
        IP2(float a, Float[] b){
            this.a = a; this.b = b; }
        public Float a() { return a; }
        public Float[] b() { return b; }
        public static IP2 valueOf(Float a, Float[] b) {
            return new IP2(a.intValue(), b);
        }
    }

    public static class IP3 {
        float a;
        float b;
        IP3(float a, float b){this.a = a; this.b = b; }
        public float a() { return a; }
        public Float b() { return b; }
        public static IP3 valueOf(float a, Float b) {
            return new IP3(a, b.intValue());
        }
    }


    // native int nativeGet(long a, int i);

    @Test
    public void testArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<Float[]> ar_layout = lf.array(Float.class, 12);
        assertEquals("Size of array", 12 * LayoutFactory.FS, ar_layout.byteOrBitSize());
        assertEquals("Align of array", LayoutFactory.FA, ar_layout.byteAlign());

        Location<Float[]> ar_loc = ar_layout.allocate();
        testArray_common(ar_loc);
        ar_loc = ar_layout.allocateWithinArray();
        testArray_common(ar_loc);

    }

    /**
     * @param ar_loc
     */
    private void testArray_common(Location<Float[]> ar_loc) {
        for (int i = 0; i < 12; i++) {
            Location<Float> i_loc = ar_loc.loc(i);
            assertEquals("Location of array element", i_loc.addr(),
                    ar_loc.addr() + i * LayoutFactory.FS);
        }

        for (int i = 0; i < 12; i++) {
            Location<Float> i_loc = ar_loc.loc(i);
            i_loc.put((float)i);
            assertEquals("Value of array element", i, i_loc.val().intValue());

        }
        Float[] v = ar_loc.val();
        for (int i = 0; i < 12; i++) {
            assertEquals("Value of result", i, v[i].intValue());
        }
    }

    @Test
    public void testTuple() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a", "b");
        assertEquals("Size of tuple", 2 * LayoutFactory.FS, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.FA, ip1_layout.byteAlign());

        simpleStoreLoad(ip1_layout);
    }

    /**
     * @param ip1_layout
     */
    private Pair<Location<IP1>>  simpleStoreLoad(Layout<IP1> ip1_layout) {
        Location<IP1> loc1 = ip1_layout.allocate();
        Location<IP1> loc2 = ip1_layout.allocateWithinArray();
        simpleStoreLoad_common(loc1);
        simpleStoreLoad_common(loc2);
        return new Pair<Location<IP1>>(loc1, loc2);

    }

    /**
     * @param loc
     * @return
     */
    private Location<IP1> simpleStoreLoad_common(Location<IP1> loc) {
        loc.put(new IP1(3,4));
        IP1 ip = loc.val();
        assertEquals(ip.a, 3, 0.0);
        assertEquals(ip.b, 4, 0.0);
        return loc;
    }



    @Test
    public void testTupleWithArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP2> ip2_layout = lf.tuple(IP2.class, "a", "b[4]");
        assertEquals("Size of tuple", 5 * LayoutFactory.FS, ip2_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.FA, ip2_layout.byteAlign());

        Location<IP2> loc = ip2_layout.allocate();
        Float[] a = {2.0F,3.0F,4.0F,5.0F};
        loc.put(new IP2(1.0F, a));
        IP2 ip = loc.val();
        assertEquals(ip.a, 1.0, 0.0);
        assertArrayEquals(a, ip.b);
    }

    @Test
    public void testTupleWithPrimitive() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP3> ip3_layout = lf.tuple(IP3.class, "a", "b");
        assertEquals("Size of tuple", 2 * LayoutFactory.FS, ip3_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.FA, ip3_layout.byteAlign());
        Location<IP3> loc = ip3_layout.allocate();

        loc.put(new IP3(3,4));
        IP3 ip = loc.val();
        assertEquals(ip.a, 3, 0.0);
        assertEquals(ip.b, 4, 0.0);
    }

    @Test
    public void testAliasTupleWithArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP2> ip2_layout = lf.tuple(IP2.class, "a", "b[4]");
        Location<IP2> loc = ip2_layout.allocate();
        Float[] a = {2.0F,3.0F,4.0F,5.0F};
        loc.put(new IP2(1.0F, a));
        // Ugh, would not let me do this as byte[]
        dumpBytes(loc, lf);
    }

    @Test
    public void testMatrix() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1 = lf.tuple(IP1.class, "a", "b");

        MatrixFactory mf = new MatrixFactory(lf);
        Matrix<IP1> m = mf.matrix(ip1, 3, 4);
        testMatrix_common(lf, m);

        Matrix<IP1> mm = mf.matrixInHeap(ip1, 3, 4);
        testMatrix_common(lf, mm);

        try {
            m.check(3,0);
            fail("Did not see expected exception");
        } catch (ArrayIndexOutOfBoundsException ex) {

        }
        try {
            m.check(0,4);
            fail("Did not see expected exception");
        } catch (ArrayIndexOutOfBoundsException ex) {

        }
        try {
            m.check(-1,0);
            fail("Did not see expected exception");
        } catch (ArrayIndexOutOfBoundsException ex) {

        }
        try {
            m.check(0, -1);
            fail("Did not see expected exception");
        } catch (ArrayIndexOutOfBoundsException ex) {

        }

        try {
            Matrix.productOfPositivesExcludingOverflows(0, -1);
            fail("Did not see expected exception");
        } catch (Error ex) {

        }

        try {
            Matrix.productOfPositivesExcludingOverflows(-1, 0);
            fail("Did not see expected exception");
        } catch (Error ex) {

        }

        try {
            Matrix.productOfPositivesExcludingOverflows(0xb504f334L, 0xb504f334L);
            fail("Did not see expected exception");
        } catch (Error ex) {

        }

        try {
            Matrix.productOfPositivesExcludingOverflows(0xffffffffL, 0xffffffffL);
            fail("Did not see expected exception");
        } catch (Error ex) {

        }

    }

    /**
     * @param lf
     * @param m
     */
    private void testMatrix_common(LayoutFactory lf, Matrix<IP1> m) {
        for (int i = 0 ; i < m.nRows(); i++)
            for (int j = 0; j < m.nCols(); j++)
                m.put(i, j, new IP1(1+i, 17+j));

        for (int i = 0 ; i < m.nRows(); i++)
            for (int j = 0; j < m.nCols(); j++)
                assertEquals(new IP1(1+i, 17+j), m.val(i, j));

        dumpBytes(m.array(), lf);
    }

}
