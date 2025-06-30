/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto.PBKDF2;

class BinTools {
    public static final String hex = "0123456789ABCDEF";

    BinTools() {
    }

    public static String bin2hex(byte[] b) {
        if (b == null) {
            return "";
        }
        StringBuffer sb = new StringBuffer(2 * b.length);
        for (int i = 0; i < b.length; ++i) {
            int v = (256 + b[i]) % 256;
            sb.append(hex.charAt(v / 16 & 0xF));
            sb.append(hex.charAt(v % 16 & 0xF));
        }
        return sb.toString();
    }

    public static byte[] hex2bin(String s) {
        String m = s;
        if (s == null) {
            m = "";
        } else if (s.length() % 2 != 0) {
            m = "0" + s;
        }
        byte[] r = new byte[m.length() / 2];
        int i = 0;
        int n = 0;
        while (i < m.length()) {
            char h = m.charAt(i++);
            char l = m.charAt(i++);
            r[n] = (byte)(BinTools.hex2bin(h) * 16 + BinTools.hex2bin(l));
            ++n;
        }
        return r;
    }

    public static int hex2bin(char c) {
        if (c >= '0' && c <= '9') {
            return c - 48;
        }
        if (c >= 'A' && c <= 'F') {
            return c - 65 + 10;
        }
        if (c >= 'a' && c <= 'f') {
            return c - 97 + 10;
        }
        throw new IllegalArgumentException("Input string may only contain hex digits, but found '" + c + "'");
    }
}

