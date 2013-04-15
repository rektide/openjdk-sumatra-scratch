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

public final class AliasedLocation<T> extends Location<T> {

    /**
     * Returns a new location at the same address as an existing location,
     * but with a different type (Layout) association.  This can lead to
     * unportable punning between types, which is a bug or a feature, depending
     * on what is expected.
     *
     * @param layout the new layout for the old location
     * @param existing the old location
     */
    public AliasedLocation(Layout<T> layout, Location existing) {
        super(existing, ensureLayoutFits(layout, existing), existing.addr());
    }

    static <U> Layout<U> ensureLayoutFits(Layout<U> layout, Location existing) {
        if (layout.byteOrBitSize() > existing.size())
            throw new Error("Attempt to alias with oversized layout");
        return layout;
    }

}
