
package com.lewa.support.util;

import android.util.Log;

public class Shell {
    public static boolean sLoaded;
    static {
        try {
            Log.d("Shell", "Shell load library");
            System.loadLibrary("lewa_shell");
            sLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.d("Shell", "Shell load library failed");
            sLoaded = false;
        }
    }
    public static boolean mkdirs(String path) {
        return native_mkdirs(path);
    }
    public static boolean mkdirs(String path, int mode) {
        return native_mkdirs(path) && native_chmod(path, mode, false);
    }
    public static boolean remove(String path) {
        return native_remove(path);
    }
    public static boolean link(String src, String dest) {
        return native_link(src, dest);
    }
    public static boolean move(String src, String dest) {
        return native_move(src, dest);
    }
    public static boolean copy(String src, String dest) {
        return native_copy(src, dest);
    }
    public static boolean chmod(String path, int mode) {
        return native_chmod(path, mode, false);
    }
    public static boolean chmodRecursive(String path, int mode) {
        return native_chmod(path, mode, true);
    }
    public static boolean chown(String path, int uid, int gid) {
        return native_chown(path, uid, gid, false);
    }
    public static boolean chownRecursive(String path, int uid, int gid) {
        return native_chown(path, uid, gid, true);
    }
    public static boolean run(String cmd) {
        return native_run(cmd);
    }
    public static boolean runShell(String cmd) {
        return native_run_shell(cmd);
    }
    public static boolean write(String path, String str) {
        return native_write(path, str);
    }
    private static native boolean native_mkdirs(String path);
    private static native boolean native_remove(String path);
    private static native boolean native_chmod(String path, int mode, boolean recursive);
    private static native boolean native_chown(String path, int uid, int gid, boolean recursive);
    private static native boolean native_link(String src, String dest);
    private static native boolean native_move(String src, String dest);
    private static native boolean native_copy(String src, String dest);
    private static native boolean native_run(String cmd);
    private static native boolean native_run_shell(String cmd);
    private static native boolean native_write(String path, String str);
}
