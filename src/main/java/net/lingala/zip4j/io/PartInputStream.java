/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import net.lingala.zip4j.crypto.AESDecrypter;
import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.BaseInputStream;
import net.lingala.zip4j.unzip.UnzipEngine;

public class PartInputStream
extends BaseInputStream {
    private RandomAccessFile raf;
    private long bytesRead;
    private long length;
    private UnzipEngine unzipEngine;
    private IDecrypter decrypter;
    private byte[] oneByteBuff = new byte[1];
    private byte[] aesBlockByte = new byte[16];
    private int aesBytesReturned = 0;
    private boolean isAESEncryptedFile = false;
    private int count = -1;

    public PartInputStream(RandomAccessFile raf, long start, long len, UnzipEngine unzipEngine) {
        this.raf = raf;
        this.unzipEngine = unzipEngine;
        this.decrypter = unzipEngine.getDecrypter();
        this.bytesRead = 0L;
        this.length = len;
        this.isAESEncryptedFile = unzipEngine.getFileHeader().isEncrypted() && unzipEngine.getFileHeader().getEncryptionMethod() == 99;
    }

    public int available() {
        long amount = this.length - this.bytesRead;
        if (amount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int)amount;
    }

    public int read() throws IOException {
        if (this.bytesRead >= this.length) {
            return -1;
        }
        if (this.isAESEncryptedFile) {
            if (this.aesBytesReturned == 0 || this.aesBytesReturned == 16) {
                if (this.read(this.aesBlockByte) == -1) {
                    return -1;
                }
                this.aesBytesReturned = 0;
            }
            return this.aesBlockByte[this.aesBytesReturned++] & 0xFF;
        }
        return this.read(this.oneByteBuff, 0, 1) == -1 ? -1 : this.oneByteBuff[0] & 0xFF;
    }

    public int read(byte[] b) throws IOException {
        return this.read(b, 0, b.length);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        if ((long)len > this.length - this.bytesRead && (len = (int)(this.length - this.bytesRead)) == 0) {
            this.checkAndReadAESMacBytes();
            return -1;
        }
        if (this.unzipEngine.getDecrypter() instanceof AESDecrypter && this.bytesRead + (long)len < this.length && len % 16 != 0) {
            len -= len % 16;
        }
        RandomAccessFile randomAccessFile = this.raf;
        synchronized (randomAccessFile) {
            this.count = this.raf.read(b, off, len);
            if (this.count < len && this.unzipEngine.getZipModel().isSplitArchive()) {
                int newlyRead;
                this.raf.close();
                this.raf = this.unzipEngine.startNextSplitFile();
                if (this.count < 0) {
                    this.count = 0;
                }
                if ((newlyRead = this.raf.read(b, this.count, len - this.count)) > 0) {
                    this.count += newlyRead;
                }
            }
        }
        if (this.count > 0) {
            if (this.decrypter != null) {
                try {
                    this.decrypter.decryptData(b, off, this.count);
                } catch (ZipException e) {
                    throw new IOException(e.getMessage());
                }
            }
            this.bytesRead += (long)this.count;
        }
        if (this.bytesRead >= this.length) {
            this.checkAndReadAESMacBytes();
        }
        return this.count;
    }

    protected void checkAndReadAESMacBytes() throws IOException {
        if (this.isAESEncryptedFile && this.decrypter != null && this.decrypter instanceof AESDecrypter) {
            if (((AESDecrypter)this.decrypter).getStoredMac() != null) {
                return;
            }
            byte[] macBytes = new byte[10];
            int readLen = -1;
            readLen = this.raf.read(macBytes);
            if (readLen != 10) {
                if (this.unzipEngine.getZipModel().isSplitArchive()) {
                    this.raf.close();
                    this.raf = this.unzipEngine.startNextSplitFile();
                    int newlyRead = this.raf.read(macBytes, readLen, 10 - readLen);
                    readLen += newlyRead;
                } else {
                    throw new IOException("Error occured while reading stored AES authentication bytes");
                }
            }
            ((AESDecrypter)this.unzipEngine.getDecrypter()).setStoredMac(macBytes);
        }
    }

    public long skip(long amount) throws IOException {
        if (amount < 0L) {
            throw new IllegalArgumentException();
        }
        if (amount > this.length - this.bytesRead) {
            amount = this.length - this.bytesRead;
        }
        this.bytesRead += amount;
        return amount;
    }

    public void close() throws IOException {
        this.raf.close();
    }

    public void seek(long pos) throws IOException {
        this.raf.seek(pos);
    }

    public UnzipEngine getUnzipEngine() {
        return this.unzipEngine;
    }
}

