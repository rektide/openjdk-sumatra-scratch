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

abstract public class ArrayLayout<T> extends CompoundLayout<T[]> {

    protected final long count;

    // Package protection, NOT protected-visibility
    ArrayLayout(long size, int align, Layout<T> element, long count) {
        super(size, align, arrayClassFor(element.cls()));
        this.count = count;
    }

    /**
     * The number of elements in this fixed-size array.
     * @return the number of elements
     */
    public long length() {
        return count;
    }

    /**
     * Returns the layout of the element type of this array.
     * @return the element type layout
     */
    abstract public Layout<T> elementLayout();

    // NOT PUBLIC, but visible to ArrayLocation
    abstract T val( Location<T[]> loc, long l);

    // NOT PUBLIC, but visible to ArrayLocation
    abstract void put( Location<T[]> loc, long l, T v);

    @Override
    public ArrayLocation<T> allocate() {
        @SuppressWarnings("restriction")
        long a = AtomLayout.u.allocateMemory(byteOrBitSize());
        return new ArrayLocation<T>(null, this,  a);
    }

    @Override
    public ArrayLocation<T> allocateWithinArray() {
        int n = (int) roundUp(byteOrBitSize(), LayoutFactory.JS) / LayoutFactory.JS;
        long[] bytes = new long[n];
        @SuppressWarnings("restriction")
        long data_offset = Unsafe.ARRAY_LONG_BASE_OFFSET;
        return new ArrayLocation<T>(bytes, this, data_offset);
    }

    static <U> Class<U[]> arrayClassFor(Class<U> c) {
        String array_class_name = "[L" + c.getName()+ ";";
        try {
            return (Class<U[]>) (Class.forName(array_class_name));
        } catch (ClassNotFoundException e) {
            throw new Error("Failed to forName " + array_class_name);
        }
    }


}
