/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.unzip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.zip.CRC32;
import net.lingala.zip4j.core.HeaderReader;
import net.lingala.zip4j.crypto.AESDecrypter;
import net.lingala.zip4j.crypto.IDecrypter;
import net.lingala.zip4j.crypto.StandardDecrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.InflaterInputStream;
import net.lingala.zip4j.io.PartInputStream;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.unzip.UnzipUtil;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class UnzipEngine {
    private ZipModel zipModel;
    private FileHeader fileHeader;
    private int currSplitFileCounter = 0;
    private LocalFileHeader localFileHeader;
    private IDecrypter decrypter;
    private CRC32 crc;

    public UnzipEngine(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
        if (zipModel == null || fileHeader == null) {
            throw new ZipException("Invalid parameters passed to StoreUnzip. One or more of the parameters were null");
        }
        this.zipModel = zipModel;
        this.fileHeader = fileHeader;
        this.crc = new CRC32();
    }

    /*
     * WARNING - Removed back jump from a try to a catch block - possible behaviour change.
     * Unable to fully structure code
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    public void unzipFile(ProgressMonitor progressMonitor, String outPath, String newFileName, UnzipParameters unzipParameters) throws ZipException {
        block8: {
            if (this.zipModel == null) throw new ZipException("Invalid parameters passed during unzipping file. One or more of the parameters were null");
            if (this.fileHeader == null) throw new ZipException("Invalid parameters passed during unzipping file. One or more of the parameters were null");
            if (!Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
                throw new ZipException("Invalid parameters passed during unzipping file. One or more of the parameters were null");
            }
            is = null;
            os = null;
            try {
                buff = new byte[4096];
                readLength = -1;
                is = this.getInputStream();
                os = this.getOutputStream(outPath, newFileName);
                while ((readLength = is.read(buff)) != -1) {
                    os.write(buff, 0, readLength);
                    progressMonitor.updateWorkCompleted(readLength);
                    if (!progressMonitor.isCancelAllTasks()) continue;
                    progressMonitor.setResult(3);
                    progressMonitor.setState(0);
                    break block8;
                }
                ** GOTO lbl-1000
            } catch (IOException e) {
                try {
                    throw new ZipException(e);
                    catch (Exception e) {
                        throw new ZipException(e);
                    }
                } catch (Throwable var9_11) {
                    this.closeStreams(is, os);
                    throw var9_11;
                }
            }
        }
        this.closeStreams(is, os);
        return;
lbl-1000:
        // 1 sources

        {
            this.closeStreams(is, os);
            UnzipUtil.applyFileAttributes(this.fileHeader, new File(this.getOutputFileNameWithPath(outPath, newFileName)), unzipParameters);
        }
        this.closeStreams(is, os);
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public ZipInputStream getInputStream() throws ZipException {
        if (this.fileHeader == null) {
            throw new ZipException("file header is null, cannot get inputstream");
        }
        RandomAccessFile raf = null;
        try {
            raf = this.createFileHandler("r");
            String errMsg = "local header and file header do not match";
            if (!this.checkLocalHeader()) {
                throw new ZipException(errMsg);
            }
            this.init(raf);
            long comprSize = this.localFileHeader.getCompressedSize();
            long offsetStartOfData = this.localFileHeader.getOffsetStartOfData();
            if (this.localFileHeader.isEncrypted()) {
                if (this.localFileHeader.getEncryptionMethod() == 99) {
                    if (!(this.decrypter instanceof AESDecrypter)) throw new ZipException("invalid decryptor when trying to calculate compressed size for AES encrypted file: " + this.fileHeader.getFileName());
                    comprSize -= (long)(((AESDecrypter)this.decrypter).getSaltLength() + ((AESDecrypter)this.decrypter).getPasswordVerifierLength() + 10);
                    offsetStartOfData += (long)(((AESDecrypter)this.decrypter).getSaltLength() + ((AESDecrypter)this.decrypter).getPasswordVerifierLength());
                } else if (this.localFileHeader.getEncryptionMethod() == 0) {
                    comprSize -= 12L;
                    offsetStartOfData += 12L;
                }
            }
            int compressionMethod = this.fileHeader.getCompressionMethod();
            if (this.fileHeader.getEncryptionMethod() == 99) {
                if (this.fileHeader.getAesExtraDataRecord() == null) throw new ZipException("AESExtraDataRecord does not exist for AES encrypted file: " + this.fileHeader.getFileName());
                compressionMethod = this.fileHeader.getAesExtraDataRecord().getCompressionMethod();
            }
            raf.seek(offsetStartOfData);
            switch (compressionMethod) {
                case 0: {
                    return new ZipInputStream(new PartInputStream(raf, offsetStartOfData, comprSize, this));
                }
                case 8: {
                    return new ZipInputStream(new InflaterInputStream(raf, offsetStartOfData, comprSize, this));
                }
            }
            throw new ZipException("compression type not supported");
        } catch (ZipException e) {
            if (raf == null) throw e;
            try {
                raf.close();
                throw e;
            } catch (IOException e1) {
                // empty catch block
            }
            throw e;
        } catch (Exception e) {
            if (raf == null) throw new ZipException(e);
            try {
                raf.close();
                throw new ZipException(e);
            } catch (IOException iOException) {
                // empty catch block
            }
            throw new ZipException(e);
        }
    }

    private void init(RandomAccessFile raf) throws ZipException {
        if (this.localFileHeader == null) {
            throw new ZipException("local file header is null, cannot initialize input stream");
        }
        try {
            this.initDecrypter(raf);
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void initDecrypter(RandomAccessFile raf) throws ZipException {
        if (this.localFileHeader == null) {
            throw new ZipException("local file header is null, cannot init decrypter");
        }
        if (this.localFileHeader.isEncrypted()) {
            if (this.localFileHeader.getEncryptionMethod() == 0) {
                this.decrypter = new StandardDecrypter(this.fileHeader, this.getStandardDecrypterHeaderBytes(raf));
            } else if (this.localFileHeader.getEncryptionMethod() == 99) {
                this.decrypter = new AESDecrypter(this.localFileHeader, this.getAESSalt(raf), this.getAESPasswordVerifier(raf));
            } else {
                throw new ZipException("unsupported encryption method");
            }
        }
    }

    private byte[] getStandardDecrypterHeaderBytes(RandomAccessFile raf) throws ZipException {
        try {
            byte[] headerBytes = new byte[12];
            raf.seek(this.localFileHeader.getOffsetStartOfData());
            raf.read(headerBytes, 0, 12);
            return headerBytes;
        } catch (IOException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private byte[] getAESSalt(RandomAccessFile raf) throws ZipException {
        if (this.localFileHeader.getAesExtraDataRecord() == null) {
            return null;
        }
        try {
            AESExtraDataRecord aesExtraDataRecord = this.localFileHeader.getAesExtraDataRecord();
            byte[] saltBytes = new byte[this.calculateAESSaltLength(aesExtraDataRecord)];
            raf.seek(this.localFileHeader.getOffsetStartOfData());
            raf.read(saltBytes);
            return saltBytes;
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    private byte[] getAESPasswordVerifier(RandomAccessFile raf) throws ZipException {
        try {
            byte[] pvBytes = new byte[2];
            raf.read(pvBytes);
            return pvBytes;
        } catch (IOException e) {
            throw new ZipException(e);
        }
    }

    private int calculateAESSaltLength(AESExtraDataRecord aesExtraDataRecord) throws ZipException {
        if (aesExtraDataRecord == null) {
            throw new ZipException("unable to determine salt length: AESExtraDataRecord is null");
        }
        switch (aesExtraDataRecord.getAesStrength()) {
            case 1: {
                return 8;
            }
            case 2: {
                return 12;
            }
            case 3: {
                return 16;
            }
        }
        throw new ZipException("unable to determine salt length: invalid aes key strength");
    }

    public void checkCRC() throws ZipException {
        if (this.fileHeader != null) {
            if (this.fileHeader.getEncryptionMethod() == 99) {
                if (this.decrypter != null && this.decrypter instanceof AESDecrypter) {
                    byte[] tmpMacBytes = ((AESDecrypter)this.decrypter).getCalculatedAuthenticationBytes();
                    byte[] storedMac = ((AESDecrypter)this.decrypter).getStoredMac();
                    byte[] calculatedMac = new byte[10];
                    if (calculatedMac == null || storedMac == null) {
                        throw new ZipException("CRC (MAC) check failed for " + this.fileHeader.getFileName());
                    }
                    System.arraycopy(tmpMacBytes, 0, calculatedMac, 0, 10);
                    if (!Arrays.equals(calculatedMac, storedMac)) {
                        throw new ZipException("invalid CRC (MAC) for file: " + this.fileHeader.getFileName());
                    }
                }
            } else {
                long calculatedCRC = this.crc.getValue() & 0xFFFFFFFFL;
                if (calculatedCRC != this.fileHeader.getCrc32()) {
                    String errMsg = "invalid CRC for file: " + this.fileHeader.getFileName();
                    if (this.localFileHeader.isEncrypted() && this.localFileHeader.getEncryptionMethod() == 0) {
                        errMsg = errMsg + " - Wrong Password?";
                    }
                    throw new ZipException(errMsg);
                }
            }
        }
    }

    private boolean checkLocalHeader() throws ZipException {
        RandomAccessFile rafForLH = null;
        try {
            rafForLH = this.checkSplitFile();
            if (rafForLH == null) {
                rafForLH = new RandomAccessFile(new File(this.zipModel.getZipFile()), "r");
            }
            HeaderReader headerReader = new HeaderReader(rafForLH);
            this.localFileHeader = headerReader.readLocalFileHeader(this.fileHeader);
            if (this.localFileHeader == null) {
                throw new ZipException("error reading local file header. Is this a valid zip file?");
            }
            if (this.localFileHeader.getCompressionMethod() != this.fileHeader.getCompressionMethod()) {
                boolean bl = false;
                return bl;
            }
            boolean bl = true;
            return bl;
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } finally {
            if (rafForLH != null) {
                try {
                    rafForLH.close();
                } catch (IOException e) {
                } catch (Exception e) {}
            }
        }
    }

    private RandomAccessFile checkSplitFile() throws ZipException {
        if (this.zipModel.isSplitArchive()) {
            int diskNumberStartOfFile = this.fileHeader.getDiskNumberStart();
            this.currSplitFileCounter = diskNumberStartOfFile + 1;
            String curZipFile = this.zipModel.getZipFile();
            String partFile = null;
            partFile = diskNumberStartOfFile == this.zipModel.getEndCentralDirRecord().getNoOfThisDisk() ? this.zipModel.getZipFile() : (diskNumberStartOfFile >= 9 ? curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z" + (diskNumberStartOfFile + 1) : curZipFile.substring(0, curZipFile.lastIndexOf(".")) + ".z0" + (diskNumberStartOfFile + 1));
            try {
                RandomAccessFile raf = new RandomAccessFile(partFile, "r");
                if (this.currSplitFileCounter == 1) {
                    byte[] splitSig = new byte[4];
                    raf.read(splitSig);
                    if ((long)Raw.readIntLittleEndian(splitSig, 0) != 134695760L) {
                        throw new ZipException("invalid first part split file signature");
                    }
                }
                return raf;
            } catch (FileNotFoundException e) {
                throw new ZipException(e);
            } catch (IOException e) {
                throw new ZipException(e);
            }
        }
        return null;
    }

    private RandomAccessFile createFileHandler(String mode) throws ZipException {
        if (this.zipModel == null || !Zip4jUtil.isStringNotNullAndNotEmpty(this.zipModel.getZipFile())) {
            throw new ZipException("input parameter is null in getFilePointer");
        }
        try {
            RandomAccessFile raf = null;
            raf = this.zipModel.isSplitArchive() ? this.checkSplitFile() : new RandomAccessFile(new File(this.zipModel.getZipFile()), mode);
            return raf;
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private FileOutputStream getOutputStream(String outPath, String newFileName) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
            throw new ZipException("invalid output path");
        }
        try {
            File file = new File(this.getOutputFileNameWithPath(outPath, newFileName));
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.exists()) {
                file.delete();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            return fileOutputStream;
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        }
    }

    private String getOutputFileNameWithPath(String outPath, String newFileName) throws ZipException {
        String fileName = null;
        fileName = Zip4jUtil.isStringNotNullAndNotEmpty(newFileName) ? newFileName : this.fileHeader.getFileName();
        return outPath + System.getProperty("file.separator") + fileName;
    }

    public RandomAccessFile startNextSplitFile() throws IOException, FileNotFoundException {
        String currZipFile = this.zipModel.getZipFile();
        String partFile = null;
        partFile = this.currSplitFileCounter == this.zipModel.getEndCentralDirRecord().getNoOfThisDisk() ? this.zipModel.getZipFile() : (this.currSplitFileCounter >= 9 ? currZipFile.substring(0, currZipFile.lastIndexOf(".")) + ".z" + (this.currSplitFileCounter + 1) : currZipFile.substring(0, currZipFile.lastIndexOf(".")) + ".z0" + (this.currSplitFileCounter + 1));
        ++this.currSplitFileCounter;
        try {
            if (!Zip4jUtil.checkFileExists(partFile)) {
                throw new IOException("zip split file does not exist: " + partFile);
            }
        } catch (ZipException e) {
            throw new IOException(e.getMessage());
        }
        return new RandomAccessFile(partFile, "r");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void closeStreams(InputStream is, OutputStream os) throws ZipException {
        try {
            if (is != null) {
                is.close();
                is = null;
            }
        } catch (IOException e) {
            if (e != null && Zip4jUtil.isStringNotNullAndNotEmpty(e.getMessage()) && e.getMessage().indexOf(" - Wrong Password?") >= 0) {
                throw new ZipException(e.getMessage());
            }
        } finally {
            try {
                if (os != null) {
                    os.close();
                    os = null;
                }
            } catch (IOException iOException) {}
        }
    }

    public void updateCRC(int b) {
        this.crc.update(b);
    }

    public void updateCRC(byte[] buff, int offset, int len) {
        if (buff != null) {
            this.crc.update(buff, offset, len);
        }
    }

    public FileHeader getFileHeader() {
        return this.fileHeader;
    }

    public IDecrypter getDecrypter() {
        return this.decrypter;
    }

    public ZipModel getZipModel() {
        return this.zipModel;
    }

    public LocalFileHeader getLocalFileHeader() {
        return this.localFileHeader;
    }
}

