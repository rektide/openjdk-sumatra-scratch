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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 *
 * A LayoutFactory objects generates layouts.
 * The primitive types (and their boxed equivalents) are already initialized to
 * appropriate size, alignment, and endianness.  From these, additional layouts
 * can be constructed by forming tuples of layouts, arrays of layouts, bitfield
 * layouts, and translated and opaque pointers (translated pointers are not
 * quite supported yet).
 *
 *
 * @author dr2chase
 */
public class LayoutFactory  {

    private static final String VALUE_OF_NAME = "valueOf";

    /**
     * Given a primitive or boxed-primitive integral or boolean class and a bit
     * width, return a layout for that type stored in that many bits on the
     * current platform.  Within tuples bitfield layouts will behave in exactly
     * the same way as the native C compiler on that platform fed the
     * corresponding types.  Within arrays a different policy is used, and
     * no padding is inserted; bitfields may straddle byte boundaries.
     *
     * By default bitfields are unsigned.  A signed bitfield is specified
     * by using the negative of its intended width as the bit_width parameter.
     *
     * Bitfields in general are not (yet) multithread-friendly.
     *
     * @param rt
     * @param bit_width
     * @return
     * @throws Error
     */
    public <T> Layout<T> bitfieldLayoutFor(Class<T> rt, int bit_width) throws Error {
        Layout<T> lo = layouts.get(rt);
        if (lo == null) {
            throw new Error("Could not find layout for type " + rt);
        }
        if (bit_width != 0) {
            if (lo instanceof AtomLayout) {
                lo = ((AtomLayout<T>) lo).bitsWide(bit_width);
            } else {
                throw new Error(
                        "Bit width specification requires primitive type, not "
                                + rt);
            }
        }
        return lo;
    }

    /**
     * Returns an already-defined layout for a class (either one of the
     * builtin primitives or any boxed type defined through tuple).
     *
     * @param rt
     * @return
     * @throws Error
     */
    public <T> Layout<T> layoutFor(Class<T> rt) throws Error {
        Layout<T> lo = layouts.get(rt);
        if (lo == null) {
            throw new Error("Could not find layout for type " + rt);
        }
       return lo;
    }

    /**
     * Returns a layout for a fixed-size array of an layout l.
     * This interface is provided to allow creation of arrays of bitfields
     * and arrays of pointers.
     *
     * @param l
     * @param count
     * @return
     */
    public <T> ArrayLayout<T> array(Layout<T> l, long count) {
        if (l instanceof AtomBitLayout) {
            return new ArrayBitLayout<T>((AtomBitLayout<T>)l, count);
        } else
            // could these be cached?
            return new ArrayDefaultLayout<T>(l, count);
    }

    /**
     * Returns a layout for a fixed-size array of the existing layout of
     * the class cls.  For object types this will be the flattened (tuple,
     * not pointer) layout, for primitive/boxed types it will be the full-width
     * versions of their layouts (that is, not a bitfield).
     *
     * @param cls
     * @param count
     * @return
     */
    public <T> ArrayLayout<T> array(Class<T> cls, long count) {
        Layout<T> layout = layouts.get(cls);
        if (layout == null)
            throw new Error("No layout created for type " + cls);
        // could these be cached?
        // return new ArrayDefaultLayout<T>((Layout<T>)layout, count);
        return array(layout, count);
    }

    /**
     * Returns a layout for a translated pointer; that is, a reference in the
     * flattened data structure to the flattened representation of an object of
     * this type.  A layout for cls need not exist at the time this layout is
     * created.
     *
     * @param cls
     * @return
     */
    public <T> TranslatedPointerLayout<T> pointer(Class<T> cls) {
        TranslatedPointerLayout tpl = pointerLayouts.get(cls);
        if (tpl == null) {
            tpl = new TranslatedPointerLayout(cls);
            CompoundLayout vlo = (CompoundLayout) layouts.get(cls);
            if (vlo != null) {
                vlo.setPointerReferent(tpl);
            }
            pointerLayouts.put(cls,  tpl);
        }
        return tpl;
    }

    /**
     * Returns a layout for an opaque pointer.  An opaque pointer
     * cannot be dereferenced by flattened data clients, but they can move
     * pointers around and the rearranged opaque pointers will translate back
     * to their original referents when the flattened data is unmarshalled.
     *
     * @param cls
     * @return
     */
    public <T> OpaquePointerLayout<T> opaque(Class<T> cls) {
        OpaquePointerLayout tpl = opaqueLayouts.get(cls);
        if (tpl == null) {
            tpl = new OpaquePointerLayout(cls);
            opaqueLayouts.put(cls, tpl);
        }
        return tpl;
    }

    /**
     * Returns a tuple layout for class cls.
     * Cls should have getter methods with names matching those in getters.
     * A "getter" is an instance method of no parameters returning something.
     * The order of getters defines the flattened layout.
     *
     * Cls must also have a static factory method named "valueOf" that takes
     * parameters in the same order as the getters appear.
     *
     * Getter names for array-typed values must include a (currently) single
     * dimension D within square brackets as a suffix, as in "foo[17]". The
     * type of the array elements will be inferred from the getter's reflective
     * signature.
     *
     * When bit width specifications are handled, they will appear after the
     * name but before any dimensions, as a colon and number of bits.
     * For example, "sign:1" or "flags:1[5]" (to indicate 5 1-bit flags).
     *
     * A translated pointer is indicated by following a field name with a "*".
     * TRANSLATED POINTERS DON'T WORK YET.
     *
     * An opaque pointer is indicated by following a field name with a "@".
     *
     * For now, things that fit in bit fields need to be primitive types or
     * arrays of primitive types.
     *
     * @param cls
     * @param getters
     * @return
     */
    public <T> Layout<T> tuple(Class<T> cls, String... getters) {
        if (0 == (Modifier.PUBLIC & cls.getModifiers())) {
            throw new Error("Class cls must be public");
        }
        Layout<T> _result = layouts.get(cls);
        if (_result instanceof TupleLayout) {
            return _result;
        }
        if (_result != null) {
            throw new IllegalArgumentException("Layout exists for " + cls + " but is not a tuple type");
        }
        TupleLayout<T> result;
        // contains dimension of each field, or -1 for scalar.
        int[] dimensions = new int[getters.length];
        // contains bit width of each field, or -1 if none specified.
        int[] bits = new int[getters.length];
        boolean[] pointers = new boolean[getters.length];
        boolean[] opaques = new boolean[getters.length];

        // In the class, look up the getters in order.
        ArrayList<Method> methods = new ArrayList<Method>();
        int i = 0;
        for (String g : getters) {
            Method m;
            String modified_g = g;
            try {
                // Getter specifies pointer to value, not direct value?
                int star = modified_g.indexOf('*');
                int splat = modified_g.indexOf('@');
                if (star != -1) {
                    modified_g = modified_g.substring(0, star);
                    pointers[i] = true;
                    // need some sanity checking on the type, perhaps?
                } else if (splat != -1) {
                    modified_g = modified_g.substring(0, splat);
                    opaques[i] = true;
                }

                // Getter specifies a dimension?
                int brack = modified_g.indexOf('[');
                int dim = -1;
                if (brack != -1) {
                    String dim_string = modified_g.substring(brack+1);
                    modified_g = modified_g.substring(0, brack);
                    brack = dim_string.indexOf(']');
                    if (brack == -1) {
                        throw new Error(
                                "Dimension specification lacked closing ']', " + g);
                    }
                    dim_string = dim_string.substring(0, brack);
                    if (dim_string.length() == 0) {
                        throw new Error("Empty dimension specification, " + g);
                    }
                    try {
                        dim = Integer.parseInt(dim_string, 10);
                    } catch (NumberFormatException ex) {
                        throw new Error("Problems with dimension " + dim_string);
                    }
                    if (dim <= 0)
                        throw new Error(
                                "Dimension for field must be larger than zero");
                }
                dimensions[i] = dim;
                // Getter specifies a bit field width?
                int colon = modified_g.indexOf(':');
                int bit_width = 0;
                if (colon != -1) {
                    String bit_string = modified_g.substring(colon+1);
                    modified_g = modified_g.substring(0, colon);
                    if (bit_string.length() == 0) {
                        throw new Error("Empty bit width specification, " + g);
                    }
                    try {
                        bit_width = Integer.parseInt(bit_string, 10);
                    } catch (NumberFormatException ex) {
                        throw new Error("Problems with bit width " + g);
                    }
                }
                bits[i] = bit_width;
                m = cls.getMethod(modified_g);
            } catch (NoSuchMethodException | SecurityException e) {
                // e.printStackTrace();
                throw new Error("Failed to find getter method " + modified_g);
            }
            methods.add(m);
            i++;
        }
        // Next look up layouts in our table, indexed by type.
        // Getters that return an array type (beginning "[") must correspond
        // to a greater-than-zero dimension.

        // Layouts for tuple members; array members have array layouts
        Layout[] elements = new Layout[methods.size()];

        // Types passed to the valueOf factory
        Class[] element_types = new Class[methods.size()];
        i = 0;
        for (Method m : methods) {
            Class rt = m.getReturnType();
            element_types[i] = rt;
            Layout lo = null;

            boolean is_array = rt.isArray();
            if (opaques[i]) {
                lo = opaque(rt);
            } else if (pointers[i]) {
                // Note this enforces a restriction, right here.
                lo = pointer(rt);
            } else if (dimensions[i] > 0) {
                if (!is_array)
                    throw new Error(
                            "Getter name " + getters[i]+
                            " specifies array but return type type "+ rt +" does not.");
                rt = rt.getComponentType();
                lo = bitfieldLayoutFor(rt, bits[i]);
                lo = array(lo, dimensions[i]);
            } else {
                if (is_array)
                    throw new Error("Getter return type type "+ rt +
                            " specifies array but name " + getters[i] +" does not.");
                lo = bitfieldLayoutFor(rt, bits[i]);
            }

            elements[i] = lo;
            i++;
        }
        // Get the factory method
        Method factory;
        try {
            factory = cls.getMethod(VALUE_OF_NAME, element_types);
        } catch (NoSuchMethodException | SecurityException e) {
            // e.printStackTrace();
            List a = new ArrayList(); for (Class cl : element_types) a.add(cl);
            throw new Error("Failed to find valueOf factory in class " + cls +
                    " for parameters " + a);
        }
        // pass cls, layouts, factory, getters.
        result =  TupleLayout.valueOf(cls, elements, factory, methods);
        layouts.put(cls, result);
        TranslatedPointerLayout tpl = pointerLayouts.get(cls);
        if (tpl != null)
            result.setPointerReferent(tpl);
        return result;
    }

    private final HashMap<Class, Layout> layouts;
    private final HashMap<Class, TranslatedPointerLayout> pointerLayouts;
    private final HashMap<Class, OpaquePointerLayout> opaqueLayouts;

    public final static int BS = 1;
    public final static int BA = 1;
    public final static int ZS = 1;
    public final static int ZA = 1;

    public final static int CS = 2;
    public final static int CA = 2;
    public final static int SS = 2;
    public final static int SA = 2;

    public final static int IS = 4;
    public final static int IA = 4;
    public final static int FS = 4;
    public final static int FA = 4;

    public final static int JS = 8;
    public final static int JA = 8;
    public final static int DS = 8;
    public final static int DA = 8;

    public LayoutFactory() {
        layouts =
                new HashMap<Class, Layout>();
        pointerLayouts =
                new HashMap<Class, TranslatedPointerLayout>();
        opaqueLayouts =
                new HashMap<Class, OpaquePointerLayout>();
        layouts.put(Integer.class, new I(Integer.class, IS, IA));
        layouts.put(Integer.TYPE, new I(Integer.TYPE, IS, IA));
        layouts.put(Float.class, new F(Float.class, FS, FA));
        layouts.put(Float.TYPE, new F(Float.TYPE, FS, FA));

        layouts.put(Short.class, new S(Short.class, SS, SA));
        layouts.put(Short.TYPE, new S(Short.TYPE, SS, SA));

        layouts.put(Byte.class, new B(Byte.class, BS, BA));
        layouts.put(Byte.TYPE, new B(Byte.TYPE, BS, BA));
        layouts.put(Boolean.class, new Z(Boolean.class, BS, BA));
        layouts.put(Boolean.TYPE, new Z(Boolean.TYPE, BS, BA));

        layouts.put(Double.class, new D(Double.class, DS, DA));
        layouts.put(Double.TYPE, new D(Double.TYPE, DS, DA));
        layouts.put(Long.class, new J(Long.class, JS, JA));
        layouts.put(Long.TYPE, new J(Long.TYPE, JS, JA));
    }
}
