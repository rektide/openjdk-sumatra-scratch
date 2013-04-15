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

/**
 * A type (structure) tagged address.
 * Locations can get and put values, and can generate the Locations of their
 * components.
 * @author dr2chase
 */
public class Location<T> {
    private final Layout<T> layout;
    private final long addr;
    // For GC purposes; root locations can have deallocating finalizers.
    // This keeps the root alive.
    private final Location rootLocation;
    private final Object baseObject;
    private PointerDomain pointerDomain; // Lazily allocated

    /**
     * Returns value of address; useful for testing.
     * This is an offset with an object if base() is non-null,
     * otherwise it is an actual memory address.
     *
     * @return the address.
     */
    public final long addr() {
        return addr;
    }

    /**
     * Returns size of whatever is stored at this location.
     * @return the size
     */
    public final long size() {
        return layout.byteOrBitSize();
    }

    /**
     * Returns layout of whatever is stored at this location.
     * @return the layout
     */
    public final Layout<T> layout() {
        return layout;
    }

    /**
     * Returns the base object for this location's storage (an array of long),
     * if any.  May be null, which means that the memory is allocated from the
     * native heap (e.g., the C malloc/free heap) instead.
     *
     * @return the base object
     */
    public final Object base() {
        return baseObject;
    }

    /**
     * Returns the root location for a given location.
     * The root location may have associated finalizers for "manual" storage
     * management, and it may have information about translated pointers stored
     * within it.
     *
     * @return the root location.
     */
    public final Location rootLocation() {
        return rootLocation;
    }

    /**
     * Returns the pointer domain for this location.
     * All locations with the same root share the same pointer domain.
     * Pointer domains deal with translation and encoding of pointers
     * stored in locations; are lazily allocated; are currently NOT thread-safe.
     *
     * @return the pointer domain
     */
    public final PointerDomain pointerDomain() {
        if (pointerDomain == null) {
            if (rootLocation() != this)
                pointerDomain = rootLocation().pointerDomain();
            else
                pointerDomain = new PointerDomain();
        }
        return pointerDomain;
    }

    /**
     * Returns the location of internal element number i, starting at zero.
     * Both tuples and arrays are indexed in this way, thus the use of a long
     * for i.
     *
     * The type parameter is just wrong; this probably needs to be moved into
     * the particular subclasses.
     *
     * @param i
     * @return
     */
    public final <U> Location<U> loc(long i) {
        if (layout instanceof CompoundLayout)
            return ((CompoundLayout<T>)layout).<U>loc(this, i);
        throw new Error("Cannot extract parts of a not-compound layout");
    }

    /**
     * Returns a copy of the value stored at this Location.
     * @return the stored value copy
     */
    public final T val() {
        return layout.val(rootLocation, baseObject, addr);
    }

    /**
     * Stores a new value at this location.
     * @param val the value to store
     */
    public final void put(T val) {
        layout.put(rootLocation, baseObject, addr, val);
    }

    // NOT public, since this is a total loophole to peeking and poking.
    Location (Location base_location, Layout<T> layout, long addr) {
        this.rootLocation = base_location == null ? this : base_location.rootLocation();
        this.addr = addr;
        this.layout = layout;
        this.baseObject = base_location == null ? null : base_location.baseObject;
    }

    // NOT public, since this is a total loophole to peeking and poking.
    Location (Object base_object, Layout<T> layout, long addr) {
        this.rootLocation = this;
        this.addr = addr;
        this.layout = layout;
        this.baseObject = base_object;
    }

    // TODO need to sort out finalizers so that non-GC heap will be freed.
}
