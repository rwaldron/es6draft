/**
 * Copyright (c) 2012-2014 André Bargull
 * Alle Rechte vorbehalten / All Rights Reserved.  Use is subject to license terms.
 *
 * <https://github.com/anba/es6draft>
 */
package com.github.anba.es6draft.repl.console;

import java.io.Console;

import com.github.anba.es6draft.runtime.Realm;

/**
 * {@link ReplConsole} implementation for native consoles
 */
public final class NativeConsole implements ReplConsole {
    private final Console console;

    public NativeConsole(Console console) {
        this.console = console;
    }

    @Override
    public boolean addCompletion(Realm realm) {
        return false;
    }

    @Override
    public boolean isAnsiSupported() {
        return false;
    }

    @Override
    public String readLine(String prompt) {
        if (!prompt.isEmpty()) {
            putstr(prompt);
        }
        return readLine();
    }

    @Override
    public void printf(String format, Object... args) {
        console.printf(format, args);
    }

    @Override
    public String readLine() {
        return console.readLine();
    }

    @Override
    public void putstr(String s) {
        console.writer().print(s);
    }

    @Override
    public void print(String s) {
        console.writer().println(s);
    }

    @Override
    public void printErr(String s) {
        System.err.println(s);
    }
}
