
package com.lewa.support.util;


public class Process {
    private Process() {
    }

    public static final int UID_SYSTEM = android.os.Process.SYSTEM_UID;

    private static final String DEFAULT_PACKAGE_NAME = "com.lewa.launcher";

    public static final boolean IS_SYSTEM = isSystemProcess();

    public static final String PACKAGE_NAME = getPackageName();

    private static boolean isSystemProcess() {
//        return android.os.Process.myUid() == UID_SYSTEM;
        return false;
    }

    private static String getPackageName() {
        String pkg = null;
//        try {
//            pkg = ActivityThread.currentPackageName();
//            int i = pkg.lastIndexOf(':');
//            if (i != -1) {
//                pkg = pkg.substring(0, i);
//            }
//        } catch (Exception e) {
//        }
        return pkg == null ? DEFAULT_PACKAGE_NAME : pkg;
    }
}
