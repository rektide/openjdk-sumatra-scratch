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

public class Matrix<T> {

    final ArrayLocation<T> arrayLoc;
    final long base;
    final long r_count;
    final long c_count;
    final long r_stride;
    final long c_stride;

    // Not public
    Matrix (ArrayLocation<T>a_loc, long rows, long columns) {
        base = 0;
        arrayLoc = a_loc;
        r_count = rows;
        c_count = columns;
        r_stride = c_count;
        c_stride = 1;
        // Would love to find a way to guarantee that legal row and column
        // could avoid subscript math, but optimizer must worry about overflow
        // to small before the product test.
        if (rows < 0 ||
            columns < 0 ||
            productOfPositivesExcludingOverflows(rows, columns) >
                                     ((ArrayLayout)arrayLoc.layout()).length())
            throw new Error("Row and column specifications are out-of-bounds");
    }

    /**
     * Multiplication without overflow. Necessary to ensure secure of unsafe
     * peeks and pokes.  Overflows throw exceptions.
     * @param a
     * @param b
     * @return
     */
    public static long productOfPositivesExcludingOverflows(long a, long b) {
        long p = a * b;
        // Without loss of generality, a is smaller.
        if (a > b) {long t = a; a = b; b = t;}
        if (a < 0)
            throw new Error("Invalid input, negative factors not allowed");
        if (p < b)
            throw new Error("Multiplicative overflow");
        /* What's fastest way to check for overflow?
         * Max long is 2**63-1 , floor(sqrt) = 0xb504f333
         * For now, do division by smaller, check answer.
         */
        long q = p / a;
        if (q != b)
            throw new Error("Multiplicative overflow");
        return p;
    }

    /**
     * Bounds check.
     * @param row
     * @param column
     */
    public final void check(long row, long column) {
        if (row < 0)
            throw new
            ArrayIndexOutOfBoundsException
            ("Row index is less than zero: " + row);
        if (row >= r_count)
            throw new
            ArrayIndexOutOfBoundsException
            ("Row index is not less than bound: " + row);
        if (column < 0)
            throw new
            ArrayIndexOutOfBoundsException
            ("Column index is less than zero: " + row);
        if (column >= c_count)
            throw new
            ArrayIndexOutOfBoundsException
            ("Column index is not less than bound: " + row);
    }

    public final Layout<T> elementLayout() {
        return ((ArrayLayout<T>) arrayLoc.layout()).elementLayout();
    }

    public final Location<T> loc(long i, long j) {
        check(i,j);
        return arrayLoc.loc(base + i * r_stride + j * c_stride);
    }

    public final void put(long i, long j, T val) {
        check(i,j);
        arrayLoc.put(base + i * r_stride + j * c_stride, val);
    }
    public final T val(long i, long j) {
        check(i,j);
        return arrayLoc.val(base + i * r_stride + j * c_stride);
    }
    public final long nRows() {
        return r_count;
    }
    public final long nCols() {
        return c_count;
    }

    public final ArrayLocation<T> array() {
        return arrayLoc;
    }
}
