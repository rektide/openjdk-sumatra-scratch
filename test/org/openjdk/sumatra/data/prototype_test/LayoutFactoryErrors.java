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

import static org.junit.Assert.*;

import org.junit.Test;
import org.openjdk.sumatra.data.prototype.Layout;
import org.openjdk.sumatra.data.prototype.LayoutFactory;


public class LayoutFactoryErrors {

    public static class IP0 {
        // this should work; all public, all matching types.
        int a;
        int b;
        IP0(int a, int b){this.a = a; this.b = b; }
        public Integer a() { return new Integer(a); }
        public Integer b() { return b; }
        public static IP1 valueOf(Integer a, Integer b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    public static class IP1 {
        // mismatched getter/factory types
        int a;
        int b;
        IP1(int a, int b){this.a = a; this.b = b; }
        public Short a() { return new Short((short) a); }
        public Integer b() { return b; }
        public static IP1 valueOf(Integer a, Integer b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    static class IP1X {
        // not public class, not allowed
        int a;
        int b;
        IP1X(int a, int b){this.a = a; this.b = b; }
        public Integer a() { return new Integer(a); }
        public Integer b() { return b; }
        public static IP1 valueOf(Integer a, Integer b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    public static class IP2 {
        // not public getter, not allowed
        int a;
        int b;
        IP2(int a, int b){this.a = a; this.b = b; }
        Integer a() { return a; }
        public Integer b() { return b; }
        public static IP1 valueOf(Integer a, Integer b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    public static class IP3 {
        // not public factory, not allowed
        int a;
        int b;
        IP3(int a, int b){this.a = a; this.b = b; }
        public Integer a() { return a; }
        public Integer b() { return b; }
        static IP1 valueOf(Integer a, Integer b) {
            return new IP1(a.intValue(), b.intValue());
        }
    }

    public static class IP4 {
        // Wrong return type on factory, not allowed.
        int a;
        int b;
        IP4(int a, int b){this.a = a; this.b = b; }
        public Integer a() { return a; }
        public Integer b() { return b; }
        public static IP1 valueOf(Integer a, Integer b, Integer c) {
            return new IP1(a.intValue(), b.intValue());
        }
    }


    @Test
    public void test1() {
        LayoutFactory lf = new LayoutFactory();
        try {
            Layout layout = lf.tuple(IP1.class, "a", "b");
        } catch (Throwable th) {
            System.out.println("test1 Saw expected exception " + th);
            return;
        }
        fail("Exception was expected");
    }

    @Test
    public void test1x() {
        LayoutFactory lf = new LayoutFactory();
        try {
            Layout layout = lf.tuple(IP1X.class, "a", "b");
        } catch (Throwable th) {
            System.out.println("test1x Saw expected exception " + th);
            return;
        }
        fail("Exception was expected");
    }

    @Test
    public void test2() {
        LayoutFactory lf = new LayoutFactory();
        try {
            Layout layout = lf.tuple(IP2.class, "a", "b");
        } catch (Throwable th) {
            System.out.println("test2 Saw expected exception " + th);
            return;
        }
        fail("Exception was expected");
    }

    @Test
    public void test3() {
        LayoutFactory lf = new LayoutFactory();
        try {
            Layout layout = lf.tuple(IP3.class, "a", "b");
        } catch (Throwable th) {
            System.out.println("test3 Saw expected exception " + th);
            return;
        }
        fail("Exception was expected");
    }

    @Test
    public void test4() {
        LayoutFactory lf = new LayoutFactory();
        try {
            Layout layout = lf.tuple(IP4.class, "a", "b");
        } catch (Throwable th) {
            System.out.println("test4 Saw expected exception " + th);
            return;
        }
        fail("Exception was expected");
    }



}
