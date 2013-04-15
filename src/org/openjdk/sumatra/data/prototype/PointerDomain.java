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

import java.util.HashMap;
import java.util.IdentityHashMap;

final class PointerDomain {
    /**
     * A PointerDomain maps back and forth between Java Objects
     * and their stored location in flattened data.  If an object
     * has not yet been stored, locations that refer to it are
     * recorded instead, so that they may be updated when it is stored.
     *
     * This is NOT thread-safe.
     */

    private final IdentityHashMap<Object, Integer> objectToOpaque =
            new IdentityHashMap<Object, Integer>();
    private final HashMap<Integer, Object> opaqueToObject =
            new HashMap<Integer, Object>();

    private final IdentityHashMap<Object, Long> objectToNative =
            new IdentityHashMap<Object, Long>();
    private final HashMap<Long, Object> nativeToObject =
            new HashMap<Long, Object>();

    private static int lastHandle;

    @SuppressWarnings("restriction")
    static void _put(Object base, long l, long v) {
        if (Layout.pointerSizeAndAlign == Layout.BYTES_PER_INT)
            AtomLayout.u.putInt(base, l, (int) v);
        else
            AtomLayout.u.putLong(base, l, v);
    }

    @SuppressWarnings("restriction")
    static long _val(Object base, long l) {
        if (Layout.pointerSizeAndAlign == Layout.BYTES_PER_INT)
            return AtomLayout.u.getInt(base, l);
        else
            return AtomLayout.u.getLong(base, l);
    }

    Object fromNative(Object base, long asNative) {
        if (base != null)
            throw new UnsupportedOperationException(
                    "Don't handle pointers in heap-allocated memory yet");
        return asNative == 0 ? null : nativeToObject.get(asNative);
    }

    long toNative(Object base, long forLocation, Object o) {
        if (base != null)
            throw new UnsupportedOperationException(
                    "Don't handle pointers in heap-allocated memory yet");
        if (o == null)
            return 0;

        Long thing = objectToNative.get(o);
        if (thing == null) {
            objectToNative.put(o, new Long(forLocation | 1));
            return forLocation | 1;
        } else if ((thing & 1) == 1) {
            objectToNative.put(o, new Long(forLocation | 1));
            return thing;
        } else {
            return thing;
        }
    }

    // Could handle Opaques in heap-allocated pretty easily, just don't yet.
    Object fromOpaque(Object base, long asNative) {
        if (base != null)
            throw new UnsupportedOperationException(
                    "Don't handle pointers in heap-allocated memory yet");
        Object r =  asNative == 0 ? null : opaqueToObject.get((int)asNative);
        return r;
    }

    int toOpaque(Object o) {
        if (o == null)
            return 0;
        Integer thing = objectToOpaque.get(o);
        if (thing == null) {
            thing = ++lastHandle;
            objectToOpaque.put(o, thing);
            opaqueToObject.put(thing, o);
        }
        return thing;
    }

    /**
     * Indicate that o was just stored at l.
     * Correct dangling references.
     * @param o
     * @param l
     */
    void storeNative(Object base, Object o, long l) {
        if (base != null)
            throw new UnsupportedOperationException(
                    "Don't handle pointers in heap-allocated memory yet");
        Long thing = objectToNative.get(o);
        Long l_Long = new Long(l);
        objectToNative.put(o, l_Long);
        nativeToObject.put(l_Long, o);

        if (thing == null) {
            // all done.
        } else if ((thing & 1) == 1) {
            while (true) {
                long reference = thing & ~1;
                long next_thing = PointerDomain._val(null, reference);
                PointerDomain._put(null, reference, l);
                if (thing == next_thing)
                    break;
                thing = next_thing;
            }
        } else {
            // Need to think about what this means.
            // Two choices -- complain, or do nothing.
            // Doing nothing ought to increase the sharing/locality
            // of the flattened data, though the second value copy
            // will still exist.
            // For now, do nothing.
        }
    }
}
