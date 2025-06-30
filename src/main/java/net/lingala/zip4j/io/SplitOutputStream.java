/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class SplitOutputStream
extends OutputStream {
    private RandomAccessFile raf;
    private long splitLength;
    private File zipFile;
    private File outFile;
    private int currSplitFileCounter;
    private long bytesWrittenForThisPart;

    public SplitOutputStream(String name) throws FileNotFoundException, ZipException {
        this(Zip4jUtil.isStringNotNullAndNotEmpty(name) ? new File(name) : null);
    }

    public SplitOutputStream(File file) throws FileNotFoundException, ZipException {
        this(file, -1L);
    }

    public SplitOutputStream(String name, long splitLength) throws FileNotFoundException, ZipException {
        this(!Zip4jUtil.isStringNotNullAndNotEmpty(name) ? new File(name) : null, splitLength);
    }

    public SplitOutputStream(File file, long splitLength) throws FileNotFoundException, ZipException {
        if (splitLength >= 0L && splitLength < 65536L) {
            throw new ZipException("split length less than minimum allowed split length of 65536 Bytes");
        }
        this.raf = new RandomAccessFile(file, "rw");
        this.splitLength = splitLength;
        this.outFile = file;
        this.zipFile = file;
        this.currSplitFileCounter = 0;
        this.bytesWrittenForThisPart = 0L;
    }

    @Override
    public void write(int b) throws IOException {
        byte[] buff = new byte[]{(byte)b};
        this.write(buff, 0, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (len <= 0) {
            return;
        }
        if (this.splitLength != -1L) {
            if (this.splitLength < 65536L) {
                throw new IOException("split length less than minimum allowed split length of 65536 Bytes");
            }
            if (this.bytesWrittenForThisPart >= this.splitLength) {
                this.startNextSplitFile();
                this.raf.write(b, off, len);
                this.bytesWrittenForThisPart = len;
            } else if (this.bytesWrittenForThisPart + (long)len > this.splitLength) {
                if (this.isHeaderData(b)) {
                    this.startNextSplitFile();
                    this.raf.write(b, off, len);
                    this.bytesWrittenForThisPart = len;
                } else {
                    this.raf.write(b, off, (int)(this.splitLength - this.bytesWrittenForThisPart));
                    this.startNextSplitFile();
                    this.raf.write(b, off + (int)(this.splitLength - this.bytesWrittenForThisPart), (int)((long)len - (this.splitLength - this.bytesWrittenForThisPart)));
                    this.bytesWrittenForThisPart = (long)len - (this.splitLength - this.bytesWrittenForThisPart);
                }
            } else {
                this.raf.write(b, off, len);
                this.bytesWrittenForThisPart += (long)len;
            }
        } else {
            this.raf.write(b, off, len);
            this.bytesWrittenForThisPart += (long)len;
        }
    }

    private void startNextSplitFile() throws IOException {
        try {
            String zipFileWithoutExt = Zip4jUtil.getZipFileNameWithoutExt(this.outFile.getName());
            File currSplitFile = null;
            String zipFileName = this.zipFile.getAbsolutePath();
            String parentPath = this.outFile.getParent() == null ? "" : this.outFile.getParent() + System.getProperty("file.separator");
            currSplitFile = this.currSplitFileCounter < 9 ? new File(parentPath + zipFileWithoutExt + ".z0" + (this.currSplitFileCounter + 1)) : new File(parentPath + zipFileWithoutExt + ".z" + (this.currSplitFileCounter + 1));
            this.raf.close();
            if (currSplitFile.exists()) {
                throw new IOException("split file: " + currSplitFile.getName() + " already exists in the current directory, cannot rename this file");
            }
            if (!this.zipFile.renameTo(currSplitFile)) {
                throw new IOException("cannot rename newly created split file");
            }
            this.zipFile = new File(zipFileName);
            this.raf = new RandomAccessFile(this.zipFile, "rw");
            ++this.currSplitFileCounter;
        } catch (ZipException e) {
            throw new IOException(e.getMessage());
        }
    }

    private boolean isHeaderData(byte[] buff) {
        if (buff == null || buff.length < 4) {
            return false;
        }
        int signature = Raw.readIntLittleEndian(buff, 0);
        long[] allHeaderSignatures = Zip4jUtil.getAllHeaderSignatures();
        if (allHeaderSignatures != null && allHeaderSignatures.length > 0) {
            for (int i = 0; i < allHeaderSignatures.length; ++i) {
                if (allHeaderSignatures[i] == 134695760L || allHeaderSignatures[i] != (long)signature) continue;
                return true;
            }
        }
        return false;
    }

    public boolean checkBuffSizeAndStartNextSplitFile(int bufferSize) throws ZipException {
        if (bufferSize < 0) {
            throw new ZipException("negative buffersize for checkBuffSizeAndStartNextSplitFile");
        }
        if (!this.isBuffSizeFitForCurrSplitFile(bufferSize)) {
            try {
                this.startNextSplitFile();
                this.bytesWrittenForThisPart = 0L;
                return true;
            } catch (IOException e) {
                throw new ZipException(e);
            }
        }
        return false;
    }

    public boolean isBuffSizeFitForCurrSplitFile(int bufferSize) throws ZipException {
        if (bufferSize < 0) {
            throw new ZipException("negative buffersize for isBuffSizeFitForCurrSplitFile");
        }
        if (this.splitLength >= 65536L) {
            return this.bytesWrittenForThisPart + (long)bufferSize <= this.splitLength;
        }
        return true;
    }

    public void seek(long pos) throws IOException {
        this.raf.seek(pos);
    }

    @Override
    public void close() throws IOException {
        if (this.raf != null) {
            this.raf.close();
        }
    }

    @Override
    public void flush() throws IOException {
    }

    public long getFilePointer() throws IOException {
        return this.raf.getFilePointer();
    }

    public boolean isSplitZipFile() {
        return this.splitLength != -1L;
    }

    public long getSplitLength() {
        return this.splitLength;
    }

    public int getCurrSplitFileCounter() {
        return this.currSplitFileCounter;
    }
}

