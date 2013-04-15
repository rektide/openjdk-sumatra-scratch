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

import static org.junit.Assert.assertEquals;

import org.openjdk.sumatra.data.prototype.AliasedLocation;
import org.openjdk.sumatra.data.prototype.ArrayLayout;
import org.openjdk.sumatra.data.prototype.Layout;
import org.openjdk.sumatra.data.prototype.LayoutFactory;
import org.openjdk.sumatra.data.prototype.Location;

public class TestCommon {
    /**
     * @param loc
     * @param lf
     */
     final void dumpBytes(Location loc, LayoutFactory lf) {
        ArrayLayout<Byte> as_bytes = lf.array(Byte.class, loc.size());
        Location<Byte[]> loc_as_bytes = new AliasedLocation<Byte[]>(as_bytes, loc);
        System.out.print("hex");
        for (int i = 0; i < as_bytes.length(); i++)
            System.out.print(" " + Integer.toHexString(((Byte)loc_as_bytes.loc(i).val()).byteValue() & 0xFF ));
        System.out.println();
    }

     final void dumpBits(Location loc, LayoutFactory lf, int bit_width) {
        Layout for_loc = loc.layout();
        long count = for_loc.byteOrBitSize();
        if (for_loc.byteAlign() != 0) {
            count = count * Layout.BITS_PER_BYTE;
        }
        count = count / bit_width;

        Layout<Byte> elt = lf.bitfieldLayoutFor(Byte.class, bit_width);
        ArrayLayout<Byte> as_bits = lf.array(elt, count);
        Location<Byte[]> loc_as_bytes = new AliasedLocation<Byte[]>(as_bits, loc);
        Byte[] val = loc_as_bytes.val();
        for (int i = 0; i < as_bits.length(); i++) {
            System.out.print(" " + val[i]);
            byte via_loc = (byte) loc_as_bytes.loc(i).val();
            assertEquals("Two ways of accessing same element",
                         val[i].byteValue(), via_loc);
        }
        System.out.println();
    }


}
