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
import org.openjdk.sumatra.data.prototype.ArrayLayout;
import org.openjdk.sumatra.data.prototype.ArrayLocation;
import org.openjdk.sumatra.data.prototype.Layout;
import org.openjdk.sumatra.data.prototype.LayoutFactory;
import org.openjdk.sumatra.data.prototype.Location;
import org.openjdk.sumatra.data.prototype.Matrix;
import org.openjdk.sumatra.data.prototype.MatrixFactory;

public class LayoutFactoryTestLong extends TestCommon {

    public static class Pair<T> {
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
        long a;
        long b;
        IP1(long a, long b){this.a = a; this.b = b; }
        public Long a() { return a; }
        public Long b() { return b; }
        public static IP1 valueOf(Long a, Long b) {
            return new IP1(a.longValue(), b.longValue());
        }
    }

    public static class IP2 {
        long a;
        Long[] b;
        IP2(long a, Long[] b){
            this.a = a; this.b = b; }
        public Long a() { return a; }
        public Long[] b() { return b; }
        public static IP2 valueOf(Long a, Long[] b) {
            return new IP2(a.longValue(), b);
        }
    }

    public static class IP3 {
        long a;
        long b;
        IP3(long a, long b){this.a = a; this.b = b; }
        public long a() { return a; }
        public Long b() { return b; }
        public static IP3 valueOf(long a, Long b) {
            return new IP3(a, b.longValue());
        }
    }

    public static class IP4 {
        long a;
        long b;
        long c;
        long d;
        IP4(long a, long b, long c, long d) {
            this.a = a; this.b = b;
            this.c = c; this.d = d;
        }
        public long a() { return a; }
        public long b() { return b; }
        public long c() { return c; }
        public long d() { return d; }
        public static IP4 valueOf(long a, long b, long c, long d) {
            return new IP4(a, b, c, d);
        }
    }



    // native int nativeGet(long a, int i);

    @Test
    public void testArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<Long[]> ar_layout = lf.array(Long.class, 12);
        assertEquals("Size of array", 12 * LayoutFactory.JS, ar_layout.byteOrBitSize());
        assertEquals("Align of array", LayoutFactory.JA, ar_layout.byteAlign());

        Location<Long[]> ar_loc = ar_layout.allocate();
        testArray_common(ar_loc);
        ar_loc = ar_layout.allocateWithinArray();
        testArray_common(ar_loc);

    }

    /**
     * @param ar_loc
     */
    private void testArray_common(Location<Long[]> ar_loc) {
        for (int i = 0; i < 12; i++) {
            Location<Long> i_loc = ar_loc.loc(i);
            assertEquals("Location of array element", i_loc.addr(),
                    ar_loc.addr() + i * LayoutFactory.JS);
        }

        for (int i = 0; i < 12; i++) {
            Location<Long> i_loc = ar_loc.loc(i);
            i_loc.put((long)i);
            assertEquals("Value of array element", i, i_loc.val().longValue());

        }
        Long[] v = ar_loc.val();
        for (int i = 0; i < 12; i++) {
            assertEquals("Value of result", i, v[i].longValue());
        }
    }

    @Test
    public void testTuple() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a", "b");
        assertEquals("Size of tuple", 2 * LayoutFactory.JS, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip1_layout.byteAlign());

        simpleStoreLoad(lf, ip1_layout);
    }

    /**
     * @param ip1_layout
     */
    private Pair<Location<IP1>>  simpleStoreLoad(LayoutFactory lf, Layout<IP1> ip1_layout) {
        Location<IP1> loc1 = ip1_layout.allocate();
        Location<IP1> loc2 = ip1_layout.allocateWithinArray();
        simpleStoreLoad_common(lf, loc2);
        simpleStoreLoad_common(lf, loc1);
        return new Pair<Location<IP1>>(loc1, loc2);

    }

    /**
     * @param loc
     * @return
     */
    private Location<IP1> simpleStoreLoad_common(LayoutFactory lf, Location<IP1> loc) {
        loc.put(new IP1(3,4));
        System.out.println("Storing 3, 4 in " + loc.layout());
        dumpBytes(loc, lf);
        IP1 ip = loc.val();
        assertEquals(3, ip.a);
        assertEquals(4, ip.b);
        return loc;
    }

    @Test
    public void testBitTuple1() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a:3", "b:3");
        assertEquals("Size of tuple", LayoutFactory.JS, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip1_layout.byteAlign());

        testBitTuple_common(lf, ip1_layout);
    }

    /**
     * @param lf
     * @param ip1_layout
     */
    private void testBitTuple_common(LayoutFactory lf, Layout<IP1> ip1_layout) {
        System.out.println(ip1_layout);
        Pair<Location<IP1>> loc = simpleStoreLoad(lf, ip1_layout);
        dumpBytes(loc.a(), lf);
        dumpBytes(loc.b(), lf);
    }

    @Test
    public void testBitTuple2() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a:8", "b:8");
        assertEquals("Size of tuple", LayoutFactory.JS, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip1_layout.byteAlign());

        testBitTuple_common(lf, ip1_layout);
    }

    @Test
    public void testBitTuple3() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a:33", "b:33");
        assertEquals("Size of tuple", 2*LayoutFactory.JS, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip1_layout.byteAlign());

        testBitTuple_common(lf, ip1_layout);
    }

    @Test
    public void testBitTuple4() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP4> ip_layout = lf.tuple(IP4.class, "a:20", "b:20", "c:14", "d:15");
        assertEquals("Size of tuple", 2*LayoutFactory.JS, ip_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip_layout.byteAlign());

        System.out.println(ip_layout);
        Location<IP4> loc1 = ip_layout.allocate();
        Location<IP4> loc2 = ip_layout.allocateWithinArray();

        tBT4_common(lf, loc2);
        tBT4_common(lf, loc1);
    }

    /**
     * @param lf
     * @param loc
     */
    private void tBT4_common(LayoutFactory lf, Location<IP4> loc) {
        loc.put(new IP4(15,17, 33, 16385));
        IP4 ip = loc.val();
        dumpBytes(loc, lf);

        assertEquals(15, ip.a);
        assertEquals(17, ip.b);
        assertEquals(33, ip.c);
        assertEquals(16385, ip.d);
    }

    @Test
    public void testBitArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<Long> elt = lf.bitfieldLayoutFor(Long.class, 5);
        ArrayLayout<Long> ar_layout = lf.array(elt, 12);
        assertEquals("Size of array", 2 * LayoutFactory.IS, ar_layout.byteOrBitSize());
        assertEquals("Align of array", LayoutFactory.JA, ar_layout.byteAlign());

        ArrayLocation<Long> ar_loc = ar_layout.allocateWithinArray();
//        for (int i = 0; i < 12; i++) {
//            Location<Long> i_loc = ar_loc.loc(i);
//            System.out.println(i_loc);
////            assertEquals("Location of array element", i_loc.addr(),
////                    ar_loc.addr() + i * StaticLayoutFactory.JS);
//        }

        for (int i = 0; i < 12; i++) {
            Long s = new Long(i);
            ar_loc.put(i, s);
            Long[] v = ar_loc.val();
            assertEquals("Value of array element", i, ar_loc.val(i).longValue());
        }
        Long[] v = ar_loc.val();
        for (int i = 0; i < 12; i++) {
            assertEquals("Value of result", i, v[i].longValue());
        }
    }

    @Test
    public void testTupleWithArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP2> ip2_layout = lf.tuple(IP2.class, "a", "b[4]");
        assertEquals("Size of tuple", 5 * LayoutFactory.JS, ip2_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip2_layout.byteAlign());

        Location<IP2> loc = ip2_layout.allocate();
        Long[] a = {2L,3L,4L,5L};
        loc.put(new IP2(1, a));
        IP2 ip = loc.val();
        assertEquals(ip.a, 1);
        assertArrayEquals(a, ip.b);
    }

    @Test
    public void testTupleWithPrimitive() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP3> ip3_layout = lf.tuple(IP3.class, "a", "b");
        assertEquals("Size of tuple", 2 * LayoutFactory.JS, ip3_layout.byteOrBitSize());
        assertEquals("Align of tuple", LayoutFactory.JA, ip3_layout.byteAlign());
        Location<IP3> loc = ip3_layout.allocate();

        loc.put(new IP3(3,4));
        IP3 ip = loc.val();
        assertEquals(ip.a, 3);
        assertEquals(ip.b, 4);
    }

    @Test
    public void testAliasTupleWithArray() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP2> ip2_layout = lf.tuple(IP2.class, "a", "b[4]");
        Location<IP2> loc = ip2_layout.allocate();
        Long[] a = {2L,3L,4L,5L};
        loc.put(new IP2(1, a));
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

    @Test
    public void testBitMatrix() {
        LayoutFactory lf = new LayoutFactory();
        MatrixFactory mf = new MatrixFactory(lf);

        { // native mem, unsigned
            Layout<Long> elt = lf.bitfieldLayoutFor(Long.class, 5);
            Matrix<Long> m = mf.matrix(elt, 5, 6);
            // Quantities 0-31 unsigned, storing as large as 30
            testBitMatrix_common(lf, m, 0);
        }
        { // native mem, signed
            Layout<Long> elt = lf.bitfieldLayoutFor(Long.class, -5);
            Matrix<Long> m = mf.matrix(elt, 5, 6);
            // Quantities -16 to 15 unsigned,
            testBitMatrix_common(lf, m, -16);
        }
        { // heap mem, unsigned
            Layout<Long> elt = lf.bitfieldLayoutFor(Long.class, 5);
            Matrix<Long> m = mf.matrixInHeap(elt, 5, 6);
            // Quantities 0-31 unsigned, storing as large as 31
            testBitMatrix_common(lf, m, 1);
        }
        { // heap mem, signed
            Layout<Long> elt = lf.bitfieldLayoutFor(Long.class, -5);
            Matrix<Long> m = mf.matrixInHeap(elt, 5, 6);
            // Quantities -16 to 15 unsigned,
            testBitMatrix_common(lf, m, -15);
        }
    }

    /**
     * @param lf
     * @param m
     */
    private void testBitMatrix_common(LayoutFactory lf, Matrix<Long> m, int bias) {
        for (int i = 0 ; i < m.nRows(); i++)
            for (int j = 0; j < m.nCols(); j++)
                m.put(i, j, (long)(bias + (int) m.nCols()*i+j));

        for (int i = 0 ; i < m.nRows(); i++)
            for (int j = 0; j < m.nCols(); j++)
                assertEquals(new Long(bias + (int) m.nCols()*i+j), m.val(i, j));

        dumpBytes(m.array(), lf);
        dumpBits(m.array(), lf, (int) m.elementLayout().byteOrBitSize());
    }

}
