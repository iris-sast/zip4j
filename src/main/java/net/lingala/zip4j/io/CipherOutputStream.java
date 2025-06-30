/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.io;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import net.lingala.zip4j.core.HeaderWriter;
import net.lingala.zip4j.crypto.AESEncrpyter;
import net.lingala.zip4j.crypto.IEncrypter;
import net.lingala.zip4j.crypto.StandardEncrypter;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.BaseOutputStream;
import net.lingala.zip4j.io.SplitOutputStream;
import net.lingala.zip4j.model.AESExtraDataRecord;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.EndCentralDirRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.LocalFileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Raw;
import net.lingala.zip4j.util.Zip4jUtil;

public class CipherOutputStream
extends BaseOutputStream {
    protected OutputStream outputStream;
    private File sourceFile;
    protected FileHeader fileHeader;
    protected LocalFileHeader localFileHeader;
    private IEncrypter encrypter;
    protected ZipParameters zipParameters;
    protected ZipModel zipModel;
    private long totalBytesWritten;
    protected CRC32 crc;
    private long bytesWrittenForThisFile;
    private byte[] pendingBuffer;
    private int pendingBufferLength;
    private long totalBytesRead;

    public CipherOutputStream(OutputStream outputStream, ZipModel zipModel) {
        this.outputStream = outputStream;
        this.initZipModel(zipModel);
        this.crc = new CRC32();
        this.totalBytesWritten = 0L;
        this.bytesWrittenForThisFile = 0L;
        this.pendingBuffer = new byte[16];
        this.pendingBufferLength = 0;
        this.totalBytesRead = 0L;
    }

    public void putNextEntry(File file, ZipParameters zipParameters) throws ZipException {
        if (!zipParameters.isSourceExternalStream() && file == null) {
            throw new ZipException("input file is null");
        }
        if (!zipParameters.isSourceExternalStream() && !Zip4jUtil.checkFileExists(file)) {
            throw new ZipException("input file does not exist");
        }
        try {
            this.sourceFile = file;
            this.zipParameters = (ZipParameters)zipParameters.clone();
            if (!zipParameters.isSourceExternalStream()) {
                if (this.sourceFile.isDirectory()) {
                    this.zipParameters.setEncryptFiles(false);
                    this.zipParameters.setEncryptionMethod(-1);
                    this.zipParameters.setCompressionMethod(0);
                }
            } else {
                if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.zipParameters.getFileNameInZip())) {
                    throw new ZipException("file name is empty for external stream");
                }
                if (this.zipParameters.getFileNameInZip().endsWith("/") || this.zipParameters.getFileNameInZip().endsWith("\\")) {
                    this.zipParameters.setEncryptFiles(false);
                    this.zipParameters.setEncryptionMethod(-1);
                    this.zipParameters.setCompressionMethod(0);
                }
            }
            this.createFileHeader();
            this.createLocalFileHeader();
            if (this.zipModel.isSplitArchive() && (this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null || this.zipModel.getCentralDirectory().getFileHeaders().size() == 0)) {
                byte[] intByte = new byte[4];
                Raw.writeIntLittleEndian(intByte, 0, 134695760);
                this.outputStream.write(intByte);
                this.totalBytesWritten += 4L;
            }
            if (this.outputStream instanceof SplitOutputStream) {
                if (this.totalBytesWritten == 4L) {
                    this.fileHeader.setOffsetLocalHeader(4L);
                } else {
                    this.fileHeader.setOffsetLocalHeader(((SplitOutputStream)this.outputStream).getFilePointer());
                }
            } else if (this.totalBytesWritten == 4L) {
                this.fileHeader.setOffsetLocalHeader(4L);
            } else {
                this.fileHeader.setOffsetLocalHeader(this.totalBytesWritten);
            }
            HeaderWriter headerWriter = new HeaderWriter();
            this.totalBytesWritten += (long)headerWriter.writeLocalFileHeader(this.zipModel, this.localFileHeader, this.outputStream);
            if (this.zipParameters.isEncryptFiles()) {
                this.initEncrypter();
                if (this.encrypter != null) {
                    if (zipParameters.getEncryptionMethod() == 0) {
                        byte[] headerBytes = ((StandardEncrypter)this.encrypter).getHeaderBytes();
                        this.outputStream.write(headerBytes);
                        this.totalBytesWritten += (long)headerBytes.length;
                        this.bytesWrittenForThisFile += (long)headerBytes.length;
                    } else if (zipParameters.getEncryptionMethod() == 99) {
                        byte[] saltBytes = ((AESEncrpyter)this.encrypter).getSaltBytes();
                        byte[] passwordVerifier = ((AESEncrpyter)this.encrypter).getDerivedPasswordVerifier();
                        this.outputStream.write(saltBytes);
                        this.outputStream.write(passwordVerifier);
                        this.totalBytesWritten += (long)(saltBytes.length + passwordVerifier.length);
                        this.bytesWrittenForThisFile += (long)(saltBytes.length + passwordVerifier.length);
                    }
                }
            }
            this.crc.reset();
        } catch (CloneNotSupportedException e) {
            throw new ZipException(e);
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private void initEncrypter() throws ZipException {
        if (!this.zipParameters.isEncryptFiles()) {
            this.encrypter = null;
            return;
        }
        switch (this.zipParameters.getEncryptionMethod()) {
            case 0: {
                this.encrypter = new StandardEncrypter(this.zipParameters.getPassword(), (this.localFileHeader.getLastModFileTime() & 0xFFFF) << 16);
                break;
            }
            case 99: {
                this.encrypter = new AESEncrpyter(this.zipParameters.getPassword(), this.zipParameters.getAesKeyStrength());
                break;
            }
            default: {
                throw new ZipException("invalid encprytion method");
            }
        }
    }

    private void initZipModel(ZipModel zipModel) {
        this.zipModel = zipModel == null ? new ZipModel() : zipModel;
        if (this.zipModel.getEndCentralDirRecord() == null) {
            this.zipModel.setEndCentralDirRecord(new EndCentralDirRecord());
        }
        if (this.zipModel.getCentralDirectory() == null) {
            this.zipModel.setCentralDirectory(new CentralDirectory());
        }
        if (this.zipModel.getCentralDirectory().getFileHeaders() == null) {
            this.zipModel.getCentralDirectory().setFileHeaders(new ArrayList());
        }
        if (this.zipModel.getLocalFileHeaderList() == null) {
            this.zipModel.setLocalFileHeaderList(new ArrayList());
        }
        if (this.outputStream instanceof SplitOutputStream && ((SplitOutputStream)this.outputStream).isSplitZipFile()) {
            this.zipModel.setSplitArchive(true);
            this.zipModel.setSplitLength(((SplitOutputStream)this.outputStream).getSplitLength());
        }
        this.zipModel.getEndCentralDirRecord().setSignature(101010256L);
    }

    public void write(int bval) throws IOException {
        byte[] b = new byte[]{(byte)bval};
        this.write(b, 0, 1);
    }

    public void write(byte[] b) throws IOException {
        if (b == null) {
            throw new NullPointerException();
        }
        if (b.length == 0) {
            return;
        }
        this.write(b, 0, b.length);
    }

    public void write(byte[] b, int off, int len) throws IOException {
        if (len == 0) {
            return;
        }
        if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == 99) {
            if (this.pendingBufferLength != 0) {
                if (len >= 16 - this.pendingBufferLength) {
                    System.arraycopy(b, off, this.pendingBuffer, this.pendingBufferLength, 16 - this.pendingBufferLength);
                    this.encryptAndWrite(this.pendingBuffer, 0, this.pendingBuffer.length);
                    off = 16 - this.pendingBufferLength;
                    len -= off;
                    this.pendingBufferLength = 0;
                } else {
                    System.arraycopy(b, off, this.pendingBuffer, this.pendingBufferLength, len);
                    this.pendingBufferLength += len;
                    return;
                }
            }
            if (len != 0 && len % 16 != 0) {
                System.arraycopy(b, len + off - len % 16, this.pendingBuffer, 0, len % 16);
                this.pendingBufferLength = len % 16;
                len -= this.pendingBufferLength;
            }
        }
        if (len != 0) {
            this.encryptAndWrite(b, off, len);
        }
    }

    private void encryptAndWrite(byte[] b, int off, int len) throws IOException {
        if (this.encrypter != null) {
            try {
                this.encrypter.encryptData(b, off, len);
            } catch (ZipException e) {
                throw new IOException(e.getMessage());
            }
        }
        this.outputStream.write(b, off, len);
        this.totalBytesWritten += (long)len;
        this.bytesWrittenForThisFile += (long)len;
    }

    public void closeEntry() throws IOException, ZipException {
        if (this.pendingBufferLength != 0) {
            this.encryptAndWrite(this.pendingBuffer, 0, this.pendingBufferLength);
            this.pendingBufferLength = 0;
        }
        if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == 99) {
            if (this.encrypter instanceof AESEncrpyter) {
                this.outputStream.write(((AESEncrpyter)this.encrypter).getFinalMac());
                this.bytesWrittenForThisFile += 10L;
                this.totalBytesWritten += 10L;
            } else {
                throw new ZipException("invalid encrypter for AES encrypted file");
            }
        }
        this.fileHeader.setCompressedSize(this.bytesWrittenForThisFile);
        this.localFileHeader.setCompressedSize(this.bytesWrittenForThisFile);
        if (this.zipParameters.isSourceExternalStream()) {
            this.fileHeader.setUncompressedSize(this.totalBytesRead);
            if (this.localFileHeader.getUncompressedSize() != this.totalBytesRead) {
                this.localFileHeader.setUncompressedSize(this.totalBytesRead);
            }
        }
        long crc32 = this.crc.getValue();
        if (this.fileHeader.isEncrypted() && this.fileHeader.getEncryptionMethod() == 99) {
            crc32 = 0L;
        }
        if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == 99) {
            this.fileHeader.setCrc32(0L);
            this.localFileHeader.setCrc32(0L);
        } else {
            this.fileHeader.setCrc32(crc32);
            this.localFileHeader.setCrc32(crc32);
        }
        this.zipModel.getLocalFileHeaderList().add(this.localFileHeader);
        this.zipModel.getCentralDirectory().getFileHeaders().add(this.fileHeader);
        HeaderWriter headerWriter = new HeaderWriter();
        this.totalBytesWritten += (long)headerWriter.writeExtendedLocalHeader(this.localFileHeader, this.outputStream);
        this.crc.reset();
        this.bytesWrittenForThisFile = 0L;
        this.encrypter = null;
        this.totalBytesRead = 0L;
    }

    public void finish() throws IOException, ZipException {
        this.zipModel.getEndCentralDirRecord().setOffsetOfStartOfCentralDir(this.totalBytesWritten);
        HeaderWriter headerWriter = new HeaderWriter();
        headerWriter.finalizeZipFile(this.zipModel, this.outputStream);
    }

    public void close() throws IOException {
        if (this.outputStream != null) {
            this.outputStream.close();
        }
    }

    private void createFileHeader() throws ZipException {
        boolean isFileNameCharsetSet;
        this.fileHeader = new FileHeader();
        this.fileHeader.setSignature(33639248);
        this.fileHeader.setVersionMadeBy(20);
        this.fileHeader.setVersionNeededToExtract(20);
        if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == 99) {
            this.fileHeader.setCompressionMethod(99);
            this.fileHeader.setAesExtraDataRecord(this.generateAESExtraDataRecord(this.zipParameters));
        } else {
            this.fileHeader.setCompressionMethod(this.zipParameters.getCompressionMethod());
        }
        if (this.zipParameters.isEncryptFiles()) {
            this.fileHeader.setEncrypted(true);
            this.fileHeader.setEncryptionMethod(this.zipParameters.getEncryptionMethod());
        }
        String fileName = null;
        if (this.zipParameters.isSourceExternalStream()) {
            this.fileHeader.setLastModFileTime((int)Zip4jUtil.javaToDosTime(System.currentTimeMillis()));
            if (!Zip4jUtil.isStringNotNullAndNotEmpty(this.zipParameters.getFileNameInZip())) {
                throw new ZipException("fileNameInZip is null or empty");
            }
            fileName = this.zipParameters.getFileNameInZip();
        } else {
            this.fileHeader.setLastModFileTime((int)Zip4jUtil.javaToDosTime(Zip4jUtil.getLastModifiedFileTime(this.sourceFile, this.zipParameters.getTimeZone())));
            this.fileHeader.setUncompressedSize(this.sourceFile.length());
            fileName = Zip4jUtil.getRelativeFileName(this.sourceFile.getAbsolutePath(), this.zipParameters.getRootFolderInZip(), this.zipParameters.getDefaultFolderPath());
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("fileName is null or empty. unable to create file header");
        }
        this.fileHeader.setFileName(fileName);
        if (Zip4jUtil.isStringNotNullAndNotEmpty(this.zipModel.getFileNameCharset())) {
            this.fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName, this.zipModel.getFileNameCharset()));
        } else {
            this.fileHeader.setFileNameLength(Zip4jUtil.getEncodedStringLength(fileName));
        }
        if (this.outputStream instanceof SplitOutputStream) {
            this.fileHeader.setDiskNumberStart(((SplitOutputStream)this.outputStream).getCurrSplitFileCounter());
        } else {
            this.fileHeader.setDiskNumberStart(0);
        }
        int fileAttrs = 0;
        if (!this.zipParameters.isSourceExternalStream()) {
            fileAttrs = this.getFileAttributes(this.sourceFile);
        }
        byte[] externalFileAttrs = new byte[]{(byte)fileAttrs, 0, 0, 0};
        this.fileHeader.setExternalFileAttr(externalFileAttrs);
        if (this.zipParameters.isSourceExternalStream()) {
            this.fileHeader.setDirectory(fileName.endsWith("/") || fileName.endsWith("\\"));
        } else {
            this.fileHeader.setDirectory(this.sourceFile.isDirectory());
        }
        if (this.fileHeader.isDirectory()) {
            this.fileHeader.setCompressedSize(0L);
            this.fileHeader.setUncompressedSize(0L);
        } else if (!this.zipParameters.isSourceExternalStream()) {
            long fileSize = Zip4jUtil.getFileLengh(this.sourceFile);
            if (this.zipParameters.getCompressionMethod() == 0) {
                if (this.zipParameters.getEncryptionMethod() == 0) {
                    this.fileHeader.setCompressedSize(fileSize + 12L);
                } else if (this.zipParameters.getEncryptionMethod() == 99) {
                    int saltLength = 0;
                    switch (this.zipParameters.getAesKeyStrength()) {
                        case 1: {
                            saltLength = 8;
                            break;
                        }
                        case 3: {
                            saltLength = 16;
                            break;
                        }
                        default: {
                            throw new ZipException("invalid aes key strength, cannot determine key sizes");
                        }
                    }
                    this.fileHeader.setCompressedSize(fileSize + (long)saltLength + 10L + 2L);
                } else {
                    this.fileHeader.setCompressedSize(0L);
                }
            } else {
                this.fileHeader.setCompressedSize(0L);
            }
            this.fileHeader.setUncompressedSize(fileSize);
        }
        if (this.zipParameters.isEncryptFiles() && this.zipParameters.getEncryptionMethod() == 0) {
            this.fileHeader.setCrc32(this.zipParameters.getSourceFileCRC());
        }
        byte[] shortByte = new byte[]{Raw.bitArrayToByte(this.generateGeneralPurposeBitArray(this.fileHeader.isEncrypted(), this.zipParameters.getCompressionMethod())), (isFileNameCharsetSet = Zip4jUtil.isStringNotNullAndNotEmpty(this.zipModel.getFileNameCharset())) && this.zipModel.getFileNameCharset().equalsIgnoreCase("UTF8") || !isFileNameCharsetSet && Zip4jUtil.detectCharSet(this.fileHeader.getFileName()).equals("UTF8") ? (byte)8 : 0};
        this.fileHeader.setGeneralPurposeFlag(shortByte);
    }

    private void createLocalFileHeader() throws ZipException {
        if (this.fileHeader == null) {
            throw new ZipException("file header is null, cannot create local file header");
        }
        this.localFileHeader = new LocalFileHeader();
        this.localFileHeader.setSignature(67324752);
        this.localFileHeader.setVersionNeededToExtract(this.fileHeader.getVersionNeededToExtract());
        this.localFileHeader.setCompressionMethod(this.fileHeader.getCompressionMethod());
        this.localFileHeader.setLastModFileTime(this.fileHeader.getLastModFileTime());
        this.localFileHeader.setUncompressedSize(this.fileHeader.getUncompressedSize());
        this.localFileHeader.setFileNameLength(this.fileHeader.getFileNameLength());
        this.localFileHeader.setFileName(this.fileHeader.getFileName());
        this.localFileHeader.setEncrypted(this.fileHeader.isEncrypted());
        this.localFileHeader.setEncryptionMethod(this.fileHeader.getEncryptionMethod());
        this.localFileHeader.setAesExtraDataRecord(this.fileHeader.getAesExtraDataRecord());
        this.localFileHeader.setCrc32(this.fileHeader.getCrc32());
        this.localFileHeader.setCompressedSize(this.fileHeader.getCompressedSize());
        this.localFileHeader.setGeneralPurposeFlag((byte[])this.fileHeader.getGeneralPurposeFlag().clone());
    }

    private int getFileAttributes(File file) throws ZipException {
        if (file == null) {
            throw new ZipException("input file is null, cannot get file attributes");
        }
        if (!file.exists()) {
            return 0;
        }
        if (file.isDirectory()) {
            if (file.isHidden()) {
                return 18;
            }
            return 16;
        }
        if (!file.canWrite() && file.isHidden()) {
            return 3;
        }
        if (!file.canWrite()) {
            return 1;
        }
        if (file.isHidden()) {
            return 2;
        }
        return 0;
    }

    private int[] generateGeneralPurposeBitArray(boolean isEncrpyted, int compressionMethod) {
        int[] generalPurposeBits = new int[8];
        generalPurposeBits[0] = isEncrpyted ? 1 : 0;
        if (compressionMethod != 8) {
            generalPurposeBits[1] = 0;
            generalPurposeBits[2] = 0;
        }
        generalPurposeBits[3] = 1;
        return generalPurposeBits;
    }

    private AESExtraDataRecord generateAESExtraDataRecord(ZipParameters parameters) throws ZipException {
        if (parameters == null) {
            throw new ZipException("zip parameters are null, cannot generate AES Extra Data record");
        }
        AESExtraDataRecord aesDataRecord = new AESExtraDataRecord();
        aesDataRecord.setSignature(39169L);
        aesDataRecord.setDataSize(7);
        aesDataRecord.setVendorID("AE");
        aesDataRecord.setVersionNumber(2);
        if (parameters.getAesKeyStrength() == 1) {
            aesDataRecord.setAesStrength(1);
        } else if (parameters.getAesKeyStrength() == 3) {
            aesDataRecord.setAesStrength(3);
        } else {
            throw new ZipException("invalid AES key strength, cannot generate AES Extra data record");
        }
        aesDataRecord.setCompressionMethod(parameters.getCompressionMethod());
        return aesDataRecord;
    }

    public void decrementCompressedFileSize(int value) {
        if (value <= 0) {
            return;
        }
        if ((long)value <= this.bytesWrittenForThisFile) {
            this.bytesWrittenForThisFile -= (long)value;
        }
    }

    protected void updateTotalBytesRead(int toUpdate) {
        if (toUpdate > 0) {
            this.totalBytesRead += (long)toUpdate;
        }
    }

    public void setSourceFile(File sourceFile) {
        this.sourceFile = sourceFile;
    }

    public File getSourceFile() {
        return this.sourceFile;
    }
}

