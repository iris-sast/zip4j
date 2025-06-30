/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.crypto.engine;

public class ZipCryptoEngine {
    private final int[] keys = new int[3];
    private static final int[] CRC_TABLE = new int[256];

    public void initKeys(char[] password) {
        this.keys[0] = 305419896;
        this.keys[1] = 591751049;
        this.keys[2] = 878082192;
        for (int i = 0; i < password.length; ++i) {
            this.updateKeys((byte)(password[i] & 0xFF));
        }
    }

    public void updateKeys(byte charAt) {
        this.keys[0] = this.crc32(this.keys[0], charAt);
        this.keys[1] = this.keys[1] + (this.keys[0] & 0xFF);
        this.keys[1] = this.keys[1] * 134775813 + 1;
        this.keys[2] = this.crc32(this.keys[2], (byte)(this.keys[1] >> 24));
    }

    private int crc32(int oldCrc, byte charAt) {
        return oldCrc >>> 8 ^ CRC_TABLE[(oldCrc ^ charAt) & 0xFF];
    }

    public byte decryptByte() {
        int temp = this.keys[2] | 2;
        return (byte)(temp * (temp ^ 1) >>> 8);
    }

    static {
        for (int i = 0; i < 256; ++i) {
            int r = i;
            for (int j = 0; j < 8; ++j) {
                if ((r & 1) == 1) {
                    r = r >>> 1 ^ 0xEDB88320;
                    continue;
                }
                r >>>= 1;
            }
            ZipCryptoEngine.CRC_TABLE[i] = r;
        }
    }
}

