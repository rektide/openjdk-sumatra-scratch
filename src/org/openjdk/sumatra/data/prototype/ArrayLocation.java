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

public final class ArrayLocation<T> extends Location<T[]> {

    ArrayLocation(Location base, ArrayLayout<T> layout, long addr) {
        super(base, layout, addr);
    }

    ArrayLocation(Object base_object, ArrayLayout<T> layout, long offset) {
        super(base_object, layout, offset);
    }

    public void put (long i, T val) {
        // Need to refactor this to get rid of the cast
        ArrayLayout<T> al = (ArrayLayout<T>) layout();
        al.put(this, i, val);
    }

    public T val (long i) {
        // Need to refactor this to get rid of the cast
        ArrayLayout<T> al = (ArrayLayout<T>) layout();
        return al.val(this, i);
    }

    public Layout<T> elementLayout() {
        return ((ArrayLayout<T>) layout()).elementLayout();
    }


    // Would be nice to figure out a lambda/methodhandle-taking setter/mutator.

    // Arrays also allow addressing of subarrays.

}
