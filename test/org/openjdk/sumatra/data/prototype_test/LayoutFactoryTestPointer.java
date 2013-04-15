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

public class LayoutFactoryTestPointer extends TestCommon {

    public static class IP1 {
        @Override
        public final boolean equals(Object obj) {
            if (obj instanceof IP1) {
                IP1 other = (IP1) obj;
                return a == other.a && b == other.b;
            }
            return false;
        }
        IP1 a;
        IP1 b;
        IP1(IP1 a, IP1 b){this.a = a; this.b = b; }
        public IP1 a() { return a; }
        public IP1 b() { return b; }
        public static IP1 valueOf(IP1 a, IP1 b) {
            return new IP1(a, b);
        }

        public void setA(IP1 _a) {
            a = _a;
        }
        public void setB(IP1 _b) {
            b = _b;
        }
    }

    public static class IP2 {
        String a;
        String b;
        IP2(String a, String b){
            this.a = a; this.b = b; }
        public String a() { return a; }
        public String b() { return b; }
        public static IP2 valueOf(String a, String b) {
            return new IP2(a, b);
        }
    }

    @Test
    public void test1null() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a@", "b@");
        assertEquals("Size of tuple", 2 * Layout.pointerSizeAndAlign, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", Layout.pointerSizeAndAlign, ip1_layout.byteAlign());
        Location<IP1> loc = ip1_layout.allocate();
        IP1 v = new IP1(null, null);
        loc.put(v);
        IP1 ip = loc.val();
        assertNull(ip.a());
        assertNull(ip.b());
    }

    @Test
    public void test1tree() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a@", "b@");
        assertEquals("Size of tuple", 2 * Layout.pointerSizeAndAlign, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", Layout.pointerSizeAndAlign, ip1_layout.byteAlign());
        ArrayLayout<IP1> ar_layout= lf.array(ip1_layout, 3);
        ArrayLocation<IP1> loc = ar_layout.allocate();

        IP1 v1 = new IP1(null, null);
        IP1 v2 = new IP1(null, null);
        IP1 v = new IP1(v1, v2);
        loc.put(0,v);
        loc.put(1,v1);
        loc.put(2,v2);

        IP1 ip = loc.val(0);
        IP1 ip1 = ip.a();
        IP1 ip2 = ip.b();
        assertEquals(v1, ip1);
        assertEquals(v2, ip2);
    }

    @Test
    public void test1dag() {
        LayoutFactory lf = new LayoutFactory();
        Layout<IP1> ip1_layout = lf.tuple(IP1.class, "a@", "b@");
        assertEquals("Size of tuple", 2 * Layout.pointerSizeAndAlign, ip1_layout.byteOrBitSize());
        assertEquals("Align of tuple", Layout.pointerSizeAndAlign, ip1_layout.byteAlign());
        Location<IP1> loc = ip1_layout.allocate();
        IP1 v1 = new IP1(null, null);
        IP1 v = new IP1(v1, v1);
        loc.put(v);
        IP1 ip = loc.val();
        IP1 ip1 = ip.a();
        IP1 ip2 = ip.b();
        assertEquals(v1, ip1);
        assertEquals(v1, ip2);
    }

}
