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

public final class ArrayDefaultLayout<T> extends ArrayLayout<T> {

    protected final Layout<T> element;

    // Package protection, NOT protected-visibility
    ArrayDefaultLayout(Layout<T> element, long count) {
        super(count * element.byteOrBitSize(), element.byteAlign(), element, count);
        this.element = element;
    }

    @Override
    public Layout<T> elementLayout() {
        return element;
    }

    private long element_addr(long addr, long i) {
        if (i < 0 || i >= count) {
            throw new ArrayIndexOutOfBoundsException(String.valueOf(i));
        }
        // Wish we had exceptions on long overflow!
        return addr + i * element.byteOrBitSize();
    }

    @Override
    <U> Location<U>  loc(Location<T[]> addr, long i) {

        // BUG.
        // Okay, what to do here?  It can really only be a location of T.
        // This borks Java type rules, but not memory safety.
        return (Location<U>) new Location<T>(addr, element, element_addr(addr.addr(), i));
    }

    @Override
    T[] val(Location arena, Object base, long l) {
        if (count > Integer.MAX_VALUE)
            throw new Error("Cannot convert flattened data to array; too many elements");
        int icount = (int) count;
        T[] o = (T[]) (java.lang.reflect.Array.newInstance(element.cls(), icount));
        element.fillArray(arena, base, l, o);
        return o;
    }

    // NOT PUBLIC, but visible to ArrayLocation
     @Override
     T val(Location<T[]> loc, long l) {
         Object base = loc.base();
         Location arena = loc.rootLocation();
        long element_addr = element_addr(loc.addr(), l);
        return element.val(arena, base, element_addr);
    }

     @Override
     void put(Location arena, Object base, long l, T[] v) {
         int x = v.length;
         long s = element.byteOrBitSize();
         if (x > count) x = (int) count;
         for (int i = 0; i < x; i++) {
             element.put(arena, base, l, v[i]);
             l += s;
         }
     }

    // NOT PUBLIC, but visible to ArrayLocation
     @Override
     void put(Location<T[]> loc, long l, T v) {
         Object base = loc.base();
         Location arena = loc.rootLocation();
         long element_addr = element_addr(loc.addr(), l);
         element.put(arena, base, element_addr, v);
     }



}
