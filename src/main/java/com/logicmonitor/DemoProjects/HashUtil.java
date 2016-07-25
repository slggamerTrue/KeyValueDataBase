package com.logicmonitor.DemoProjects;

/**
 * Created by caizhou.
 * Use default hash now
 */
public class HashUtil {
    public static int getHash(byte[] bytes) {
        return new String(bytes).hashCode();
    }

    public static int getHash(String str) {
        return str.hashCode();
    }
}
