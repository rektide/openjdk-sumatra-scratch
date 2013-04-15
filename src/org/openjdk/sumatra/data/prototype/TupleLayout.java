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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

/**
 * Produces a simple linear packed tuple with aligned elements.
 * @author dr2chase
 */
public class TupleLayout<T> extends CompoundLayout<T> {

    private final Layout[] elements; // wish this were truly readonly.
    private final long[] offsets; // wish this were truly readonly.
    private final Method[] getters;
    private final Method valueOf;

    private final static Object[] NO_ARGS = new Object[0];

    /**
     * Helper class for constructor
     * @author dr2chase
     */
    private static class SizeAlignOffsets {
        final long size;
        final int align;
        final long[] offsets;
        final int[] bitOffsets;
        final Layout[] elements;
        SizeAlignOffsets(long _size, int _align, long[] _offsets, int[] _bit_offsets, Layout[] _elements) {
            size = _size; align = _align; offsets = _offsets; bitOffsets = _bit_offsets;
            elements = _elements;
        }

    }

    /**
     * Helper method for constructor
     * @author dr2chase
     */
    static SizeAlignOffsets fooOf(Layout ... rest) {
        int max_a = 0; // maximum alignment seen (in bytes)
        long size = 0; // byte portion of current size
        long bit_extra_size = 0; // bit portion of current size
        int i = 0;
        long[] offs = new long[rest.length];
        int[] bit_offs = new int[rest.length];
        Layout[] elements = new Layout[rest.length];

        for (Layout l : rest ) {
            int a = l.byteAlign();
            long s = l.byteOrBitSize();
            if (a == 0) {
              // bit field
                if (l instanceof AtomBitLayout) {
                    AtomBitLayout al = (AtomBitLayout) l;
                    int cbsaa = al.containerBitSizeAndAlignment();
                    // Ensure that the container is properly aligned
                    a = cbsaa >>> Layout.LOG_BITS_PER_BYTE;

                    long next_bit_extra_size = bit_extra_size + s;

                    if ((next_bit_extra_size-1)/cbsaa > bit_extra_size/cbsaa) {
                        // whoops, crossed a boundary, must move bitfield.
                        bit_extra_size = cbsaa * (1 + bit_extra_size/cbsaa);
                        next_bit_extra_size = bit_extra_size + s;
                    }

                    // split bit_extra_size into a byte/bit offset
                    // normalize byte and bit offsets
//                    long byte_extra_bits =
//                            bit_extra_size & ~(cbsaa - 1);
//                    size += byte_extra_bits / cbsaa;
//                    bit_extra_size -= byte_extra_bits;
//                    next_bit_extra_size -= byte_extra_bits;

                    long container_base = size & ~(a-1);
                    long container_offset =
                            (size & (a-1)) * cbsaa + bit_extra_size;

                    // Normalize to a container boundary.
                    if (container_offset >= cbsaa) {
                        long containers = container_offset / cbsaa;
                        container_offset -= containers * cbsaa;
                        container_base +=
                                (cbsaa >>> LOG_BITS_PER_BYTE) * containers;
                    }

                    offs[i] = container_base;
                    l = al.atFixedOffset((int)
                            (container_offset));

                    bit_extra_size = next_bit_extra_size;
                } else {
                    throw new Error("Unpossible; an unaligned non-bitfield.");
                }
            } else {
                if (bit_extra_size != 0) {
                    // zero b_e_s and update size
                    size += (bit_extra_size + Layout.BITS_PER_BYTE-1) >>>
                                   Layout.LOG_BITS_PER_BYTE;
                    bit_extra_size = 0;
                }
                size = roundUp(size, a);
                offs[i] = size;
                size += s;
            }
            elements[i] = l;
            max_a = a > max_a ? a : max_a;
            i++;
        }

        if (bit_extra_size != 0) {
            size += (bit_extra_size + Layout.BITS_PER_BYTE-1) >>>
            Layout.LOG_BITS_PER_BYTE;
        }
        // This next bit is imperfect.
        // What should the default align for a bit-field-only tuple be?
        if (max_a == 0) {
            max_a = LayoutFactory.BA;
        }
        size = roundUp(size, max_a);
        return new SizeAlignOffsets(size, max_a, offs, bit_offs, elements);
    }

    static <T> TupleLayout<T> valueOf(Class<T> cls, Layout[] rest, Method valueOf, List<Method> getters) {
        return new TupleLayout<T>(fooOf(rest), cls, rest, valueOf, getters);
    }

    private TupleLayout(SizeAlignOffsets foo, Class<T> cls, Layout[] rest,
                        Method valueOf, List<Method> getters) {
        super(foo.size, foo.align, cls);
        elements = foo.elements; // need to normalize the layouts.
        offsets = foo.offsets;
        // bitOffsets = foo.bitOffsets;
        this.valueOf = valueOf;
        this.getters = getters.toArray(new Method[getters.size()]);

        Class<?>[] valueOfParams = valueOf.getParameterTypes();

        /*
         * Ensure that all visibility and consistency rules are obeyed.
         * These tests may be redundant, but layouts should be created
         * relatively rarely so the cost is not that important.
         */
        if (this.getters.length != rest.length) {
            throw new Error("Require same length arrays for layouts and getters");
        }
        if (this.getters.length != valueOfParams.length) {
            throw new Error("Requires same number of parameter types and getters");
        }
        if (! Modifier.isStatic(valueOf.getModifiers())) {
            throw new Error("valueOf method must be static");
        }
        if (! Modifier.isPublic(valueOf.getModifiers())) {
            throw new Error("valueOf method must be public");
        }
        int i = 0;
        for (Method m : this.getters) {
            if (Modifier.isStatic(m.getModifiers())) {
                throw new Error("getter method must not be static: " + m);
            }
            if (!Modifier.isPublic(m.getModifiers())) {
                throw new Error("getter method must be public: " + m);
            }
            if (m.getGenericParameterTypes().length != 0) {
                throw new Error("getter method must have no parameters: " + m);
            }
            Layout l = rest[i];
            Class<?> vo_p = valueOfParams[i];
            // short and Short disagree here, need to think about this.
            if (!(l.cls().equals(vo_p)) && ! isAllowedTypePun(l.cls(), vo_p)) {
                throw new Error("layout and valueOf param types do not match at index " +
                 i + " " + l.cls() + " " + vo_p );
            }
            if (!(m.getReturnType().equals(vo_p))) {
                throw new Error("getter return and valueOf param types do not match at index " +
                        i + " " + m.getReturnType() + " " + vo_p );
            }
            i++;
        }
    }


    private boolean isAllowedTypePun(Class cls, Class vo_p) {
        return
                cls.equals(Integer.class) && vo_p.equals(Integer.TYPE) ||
                cls.equals(Short.class) && vo_p.equals(Short.TYPE) ||
                cls.equals(Byte.class) && vo_p.equals(Byte.TYPE) ||
                cls.equals(Long.class) && vo_p.equals(Long.TYPE) ||
                cls.equals(Float.class) && vo_p.equals(Float.TYPE) ||
                cls.equals(Double.class) && vo_p.equals(Double.TYPE) ||
                cls.equals(Boolean.class) && vo_p.equals(Boolean.TYPE);
    }

    @Override
    T val(Location arena, Object base, long l) {
        //  Obtain component values
        int ll = elements.length;
        Object[] components = new Object[ll];
        for (int i = 0; i < ll; i++) {
            components[i] = elements[i].val(arena, base, l + offsets[i]);
        }
        //  Apply factory method
        try {
            return (T) valueOf.invoke(null,  components);
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
            throw new Error("Call to valueOf failed.");
        }
    }

    @Override
    void put(Location arena, Object base, long l, Object v) {
        //  Extract components from v
        //  Recursively store in parts.

        int ll = elements.length;
        for (int i = 0; i < ll; i++) {
            try {
                elements[i].put(arena, base, l + offsets[i], getters[i].invoke(v, NO_ARGS));
            } catch (IllegalAccessException | IllegalArgumentException
                    | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        if (getPointerReferent() != null) {
            arena.pointerDomain().storeNative(base, v, l);
        }
    }

    @Override
    <U> Location<U> loc(Location<T> addr, long i) {
        int ii = (int) i;
        if (ii < 0 || ii >= elements.length) {
            throw new ArrayIndexOutOfBoundsException(ii);
        }
        return new Location(addr, elements[ii], addr.addr() + offsets[ii]);
    }

    @Override
    public String toString() {
       String s = super.toString();
       s += "[";
       for (int i = 0; i < elements.length; i++) {
           s += elements[i].toString();
           s += "@B" + offsets[i];
           if (i+1 < elements.length) {
               s += "; ";
           }
       }
       s+= "]";
       return s;
    }
}
