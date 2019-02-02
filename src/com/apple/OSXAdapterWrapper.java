/**
 * @author Tres Finocchiaro
 *
 * Copyright (C) 2019 Tres Finocchiaro, QZ Industries, LLC
 *
 * LGPL 2.1 This is free software.  This software and source code are released under
 * the "LGPL 2.1 License".  A copy of this license should be distributed with
 * this software. http://www.gnu.org/licenses/lgpl-2.1.html
 */

package com.apple;

import com.github.zafarkhaja.semver.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qz.common.Constants;
import qz.utils.MacUtilities;

import java.awt.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Java 9+ compatible shim around Apple's OSXAdapter
 *
 * @author Tres Finocchiaro
 */
public class OSXAdapterWrapper implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(OSXAdapterWrapper.class);
    public static final boolean legacyMode = Constants.JAVA_VERSION.lessThan(Version.valueOf("9.0.0"));

    private Object target;
    private Method handler;

    public OSXAdapterWrapper(Object target, Method handler) {
        this.target = target;
        this.handler = handler;
    }

    public static void setQuitHandler(Object target, Method handler) {
        if (!legacyMode) {
            // Java 9+
            wrap("java.awt.desktop.QuitHandler", "setQuitHandler", target, handler);
        } else {
            // Java 7, 8
            OSXAdapter.setAboutHandler(target, handler);
        }
    }

    public static void setAboutHandler(Object target, Method handler) {
        if (!legacyMode) {
            // Java 9+
            wrap("java.awt.desktop.AboutHandler", "setAboutHandler", target, handler);
        } else {
            // Java 7, 8
            OSXAdapter.setAboutHandler(target, handler);
        }
    }

    public static void wrap(String className, String methodName, Object target, Method handler) {
        try {
            Class desktop = Desktop.getDesktop().getClass();
            Class<?> handlerClass = Class.forName(className);
            Method method = desktop.getDeclaredMethod(methodName, new Class<?>[] {handlerClass});
            Object proxy = Proxy.newProxyInstance(MacUtilities.class.getClassLoader(), new Class<?>[] {handlerClass}, new OSXAdapterWrapper(target, handler));
            method.invoke(Desktop.getDesktop(), new Object[] {proxy});
        } catch (Exception e) {
            log.warn("Failed to set {}", className, e.getMessage());
        }
    }

    public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
        handler.invoke(target);
        return null;
    }
}
