/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

/**
 * @test
 * @bug 8048190
 * @summary Test that the CNFE saves original exception during class initialization.
 *          And is unloaded
 * @requires vm.opt.final.ClassUnloading
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm -Xlog:class+unload InitExceptionUnloadTest
 */

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import jdk.test.lib.classloader.ClassUnloadCommon;

public class InitExceptionUnloadTest {
    static public class ThrowsRuntimeException { static int x = 1/0; }
    static public class ThrowsError { static { if (true) throw new Error(); } }

    private static void verify_stack(Throwable e, String expected, String cause) throws Exception {
        ByteArrayOutputStream byteOS = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteOS);
        e.printStackTrace(printStream);
        printStream.close();
        String stackTrace = byteOS.toString("ASCII");
        if (!stackTrace.contains(expected) && (cause == null || !stackTrace.contains(cause))) {
            throw new RuntimeException(expected + " and " + cause + " missing from stacktrace: " + stackTrace);
        }
    }

    static String[] expected = new String[] {
        "java.lang.ExceptionInInitializerError",
        "Caused by: java.lang.ArithmeticException: / by zero",
        "java.lang.NoClassDefFoundError: Cound not initialize class InitExceptionUnloadTest$ThrowsRuntimeException",
        "Caused by: java.lang.ArithmeticException: / by zero at InitExceptionUnloadTest$ThrowsRuntimeException.<clinit>(InitExceptionUnloadTest.java:38)",
        "java.lang.Error",
        null,
        "java.lang.NoClassDefFoundError: Cound not initialize class InitExceptionUnloadTest$ThrowsError",
        "Caused by: java.lang.Error at InitExceptionUnloadTest$ThrowsError.<clinit>(InitExceptionUnloadTest.java:39)"
    };

    static void test() throws Throwable {
        ClassLoader cl = ClassUnloadCommon.newClassLoader();
        int i = 0;
        for (String className : new String[] {
                 "InitExceptionUnloadTest$ThrowsRuntimeException",
                 "InitExceptionUnloadTest$ThrowsError" }) {
            System.err.println("--- try to load " + className);
            for (int tries = 2; tries--> 0; ) {
                try {
                    Class<?> c = cl.loadClass(className);
                    Object inst = c.newInstance();
                } catch (Throwable t) {
                    t.printStackTrace();
                    verify_stack(t, expected[i], expected[i+1]);
                    i += 2;
                }
            }
        }
        cl = null;
        ClassUnloadCommon.triggerUnloading();  // should unload these classes
    }
    public static void main(java.lang.String[] unused) throws Throwable {
        test();
        ClassUnloadCommon.triggerUnloading();  // should unload these classes
        test();
    }
}
