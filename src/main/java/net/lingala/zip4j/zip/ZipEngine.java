/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.CipherOutputStream;
import net.lingala.zip4j.io.DeflaterOutputStream;
import net.lingala.zip4j.io.SplitOutputStream;
import net.lingala.zip4j.io.ZipOutputStream;
import net.lingala.zip4j.model.EndCentralDirRecord;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.ArchiveMaintainer;
import net.lingala.zip4j.util.CRCUtil;
import net.lingala.zip4j.util.Zip4jUtil;

public class ZipEngine {
    private ZipModel zipModel;

    public ZipEngine(ZipModel zipModel) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null in ZipEngine constructor");
        }
        this.zipModel = zipModel;
    }

    public void addFiles(final ArrayList fileList, final ZipParameters parameters, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (fileList == null || parameters == null) {
            throw new ZipException("one of the input parameters is null when adding files");
        }
        if (fileList.size() <= 0) {
            throw new ZipException("no files to add");
        }
        progressMonitor.setCurrentOperation(0);
        progressMonitor.setState(1);
        progressMonitor.setResult(1);
        if (runInThread) {
            progressMonitor.setTotalWork(this.calculateTotalWork(fileList, parameters));
            progressMonitor.setFileName(((File)fileList.get(0)).getAbsolutePath());
            Thread thread = new Thread("Zip4j"){

                public void run() {
                    try {
                        ZipEngine.this.initAddFiles(fileList, parameters, progressMonitor);
                    } catch (ZipException zipException) {
                        // empty catch block
                    }
                }
            };
            thread.start();
        } else {
            this.initAddFiles(fileList, parameters, progressMonitor);
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void initAddFiles(ArrayList fileList, ZipParameters parameters, ProgressMonitor progressMonitor) throws ZipException {
        if (fileList == null) throw new ZipException("one of the input parameters is null when adding files");
        if (parameters == null) {
            throw new ZipException("one of the input parameters is null when adding files");
        }
        if (fileList.size() <= 0) {
            throw new ZipException("no files to add");
        }
        if (this.zipModel.getEndCentralDirRecord() == null) {
            this.zipModel.setEndCentralDirRecord(this.createEndOfCentralDirectoryRecord());
        }
        CipherOutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            this.checkParameters(parameters);
            this.removeFilesIfExists(fileList, parameters, progressMonitor);
            boolean isZipFileAlreadExists = Zip4jUtil.checkFileExists(this.zipModel.getZipFile());
            SplitOutputStream splitOutputStream = new SplitOutputStream(new File(this.zipModel.getZipFile()), this.zipModel.getSplitLength());
            outputStream = new ZipOutputStream(splitOutputStream, this.zipModel);
            if (isZipFileAlreadExists) {
                if (this.zipModel.getEndCentralDirRecord() == null) {
                    throw new ZipException("invalid end of central directory record");
                }
                splitOutputStream.seek(this.zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
            }
            byte[] readBuff = new byte[4096];
            int readLen = -1;
            int i = 0;
            while (true) {
                block47: {
                    block48: {
                        block45: {
                            block46: {
                                if (i >= fileList.size()) break block45;
                                if (progressMonitor.isCancelAllTasks()) {
                                    progressMonitor.setResult(3);
                                    progressMonitor.setState(0);
                                    return;
                                }
                                ZipParameters fileParameters = (ZipParameters)parameters.clone();
                                progressMonitor.setFileName(((File)fileList.get(i)).getAbsolutePath());
                                if (!((File)fileList.get(i)).isDirectory()) {
                                    if (fileParameters.isEncryptFiles() && fileParameters.getEncryptionMethod() == 0) {
                                        progressMonitor.setCurrentOperation(3);
                                        fileParameters.setSourceFileCRC((int)CRCUtil.computeFileCRC(((File)fileList.get(i)).getAbsolutePath(), progressMonitor));
                                        progressMonitor.setCurrentOperation(0);
                                        if (progressMonitor.isCancelAllTasks()) {
                                            progressMonitor.setResult(3);
                                            progressMonitor.setState(0);
                                            return;
                                        }
                                    }
                                    if (Zip4jUtil.getFileLengh((File)fileList.get(i)) == 0L) {
                                        fileParameters.setCompressionMethod(0);
                                    }
                                }
                                ((DeflaterOutputStream)outputStream).putNextEntry((File)fileList.get(i), fileParameters);
                                if (!((File)fileList.get(i)).isDirectory()) break block46;
                                ((DeflaterOutputStream)outputStream).closeEntry();
                                break block47;
                            }
                            inputStream = new FileInputStream((File)fileList.get(i));
                            break block48;
                        }
                        ((DeflaterOutputStream)outputStream).finish();
                        progressMonitor.endProgressMonitorSuccess();
                        return;
                    }
                    while ((readLen = inputStream.read(readBuff)) != -1) {
                        if (progressMonitor.isCancelAllTasks()) {
                            progressMonitor.setResult(3);
                            progressMonitor.setState(0);
                            return;
                        }
                        ((ZipOutputStream)outputStream).write(readBuff, 0, readLen);
                        progressMonitor.updateWorkCompleted(readLen);
                    }
                    ((DeflaterOutputStream)outputStream).closeEntry();
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                ++i;
            }
        } catch (ZipException e) {
            progressMonitor.endProgressMonitorError(e);
            throw e;
        } catch (Exception e) {
            progressMonitor.endProgressMonitorError(e);
            throw new ZipException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
        }
    }

    public void addStreamToZip(InputStream inputStream, ZipParameters parameters) throws ZipException {
        if (inputStream == null || parameters == null) {
            throw new ZipException("one of the input parameters is null, cannot add stream to zip");
        }
        CipherOutputStream outputStream = null;
        try {
            this.checkParameters(parameters);
            boolean isZipFileAlreadExists = Zip4jUtil.checkFileExists(this.zipModel.getZipFile());
            SplitOutputStream splitOutputStream = new SplitOutputStream(new File(this.zipModel.getZipFile()), this.zipModel.getSplitLength());
            outputStream = new ZipOutputStream(splitOutputStream, this.zipModel);
            if (isZipFileAlreadExists) {
                if (this.zipModel.getEndCentralDirRecord() == null) {
                    throw new ZipException("invalid end of central directory record");
                }
                splitOutputStream.seek(this.zipModel.getEndCentralDirRecord().getOffsetOfStartOfCentralDir());
            }
            byte[] readBuff = new byte[4096];
            int readLen = -1;
            ((DeflaterOutputStream)outputStream).putNextEntry(null, parameters);
            if (!parameters.getFileNameInZip().endsWith("/") && !parameters.getFileNameInZip().endsWith("\\")) {
                while ((readLen = inputStream.read(readBuff)) != -1) {
                    ((ZipOutputStream)outputStream).write(readBuff, 0, readLen);
                }
            }
            ((DeflaterOutputStream)outputStream).closeEntry();
            ((DeflaterOutputStream)outputStream).finish();
        } catch (ZipException e) {
            throw e;
        } catch (Exception e) {
            throw new ZipException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
        }
    }

    public void addFolderToZip(File file, ZipParameters parameters, ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (file == null || parameters == null) {
            throw new ZipException("one of the input parameters is null, cannot add folder to zip");
        }
        if (!Zip4jUtil.checkFileExists(file.getAbsolutePath())) {
            throw new ZipException("input folder does not exist");
        }
        if (!file.isDirectory()) {
            throw new ZipException("input file is not a folder, user addFileToZip method to add files");
        }
        if (!Zip4jUtil.checkFileReadAccess(file.getAbsolutePath())) {
            throw new ZipException("cannot read folder: " + file.getAbsolutePath());
        }
        String rootFolderPath = null;
        rootFolderPath = parameters.isIncludeRootFolder() ? (file.getAbsolutePath() != null ? (file.getAbsoluteFile().getParentFile() != null ? file.getAbsoluteFile().getParentFile().getAbsolutePath() : "") : (file.getParentFile() != null ? file.getParentFile().getAbsolutePath() : "")) : file.getAbsolutePath();
        parameters.setDefaultFolderPath(rootFolderPath);
        ArrayList<File> fileList = Zip4jUtil.getFilesInDirectoryRec(file, parameters.isReadHiddenFiles());
        if (parameters.isIncludeRootFolder()) {
            if (fileList == null) {
                fileList = new ArrayList<File>();
            }
            fileList.add(file);
        }
        this.addFiles(fileList, parameters, progressMonitor, runInThread);
    }

    private void checkParameters(ZipParameters parameters) throws ZipException {
        if (parameters == null) {
            throw new ZipException("cannot validate zip parameters");
        }
        if (parameters.getCompressionMethod() != 0 && parameters.getCompressionMethod() != 8) {
            throw new ZipException("unsupported compression type");
        }
        if (parameters.getCompressionMethod() == 8 && parameters.getCompressionLevel() < 0 && parameters.getCompressionLevel() > 9) {
            throw new ZipException("invalid compression level. compression level dor deflate should be in the range of 0-9");
        }
        if (parameters.isEncryptFiles()) {
            if (parameters.getEncryptionMethod() != 0 && parameters.getEncryptionMethod() != 99) {
                throw new ZipException("unsupported encryption method");
            }
            if (parameters.getPassword() == null || parameters.getPassword().length <= 0) {
                throw new ZipException("input password is empty or null");
            }
        } else {
            parameters.setAesKeyStrength(-1);
            parameters.setEncryptionMethod(-1);
        }
    }

    private void removeFilesIfExists(ArrayList fileList, ZipParameters parameters, ProgressMonitor progressMonitor) throws ZipException {
        if (this.zipModel == null || this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null || this.zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
            return;
        }
        RandomAccessFile outputStream = null;
        try {
            for (int i = 0; i < fileList.size(); ++i) {
                File file = (File)fileList.get(i);
                String fileName = Zip4jUtil.getRelativeFileName(file.getAbsolutePath(), parameters.getRootFolderInZip(), parameters.getDefaultFolderPath());
                FileHeader fileHeader = Zip4jUtil.getFileHeader(this.zipModel, fileName);
                if (fileHeader == null) continue;
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
                ArchiveMaintainer archiveMaintainer = new ArchiveMaintainer();
                progressMonitor.setCurrentOperation(2);
                HashMap retMap = archiveMaintainer.initRemoveZipFile(this.zipModel, fileHeader, progressMonitor);
                if (progressMonitor.isCancelAllTasks()) {
                    progressMonitor.setResult(3);
                    progressMonitor.setState(0);
                    return;
                }
                progressMonitor.setCurrentOperation(0);
                if (outputStream != null) continue;
                outputStream = this.prepareFileOutputStream();
                if (retMap == null || retMap.get("offsetCentralDir") == null) continue;
                long offsetCentralDir = -1L;
                try {
                    offsetCentralDir = Long.parseLong((String)retMap.get("offsetCentralDir"));
                } catch (NumberFormatException e) {
                    throw new ZipException("NumberFormatException while parsing offset central directory. Cannot update already existing file header");
                } catch (Exception e) {
                    throw new ZipException("Error while parsing offset central directory. Cannot update already existing file header");
                }
                if (offsetCentralDir < 0L) continue;
                outputStream.seek(offsetCentralDir);
            }
        } catch (IOException e) {
            throw new ZipException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {}
            }
        }
    }

    private RandomAccessFile prepareFileOutputStream() throws ZipException {
        String outPath = this.zipModel.getZipFile();
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
            throw new ZipException("invalid output path");
        }
        try {
            File outFile = new File(outPath);
            if (!outFile.getParentFile().exists()) {
                outFile.getParentFile().mkdirs();
            }
            return new RandomAccessFile(outFile, "rw");
        } catch (FileNotFoundException e) {
            throw new ZipException(e);
        }
    }

    private EndCentralDirRecord createEndOfCentralDirectoryRecord() {
        EndCentralDirRecord endCentralDirRecord = new EndCentralDirRecord();
        endCentralDirRecord.setSignature(101010256L);
        endCentralDirRecord.setNoOfThisDisk(0);
        endCentralDirRecord.setTotNoOfEntriesInCentralDir(0);
        endCentralDirRecord.setTotNoOfEntriesInCentralDirOnThisDisk(0);
        endCentralDirRecord.setOffsetOfStartOfCentralDir(0L);
        return endCentralDirRecord;
    }

    private long calculateTotalWork(ArrayList fileList, ZipParameters parameters) throws ZipException {
        if (fileList == null) {
            throw new ZipException("file list is null, cannot calculate total work");
        }
        long totalWork = 0L;
        for (int i = 0; i < fileList.size(); ++i) {
            String relativeFileName;
            FileHeader fileHeader;
            if (!(fileList.get(i) instanceof File) || !((File)fileList.get(i)).exists()) continue;
            totalWork = parameters.isEncryptFiles() && parameters.getEncryptionMethod() == 0 ? (totalWork += Zip4jUtil.getFileLengh((File)fileList.get(i)) * 2L) : (totalWork += Zip4jUtil.getFileLengh((File)fileList.get(i)));
            if (this.zipModel.getCentralDirectory() == null || this.zipModel.getCentralDirectory().getFileHeaders() == null || this.zipModel.getCentralDirectory().getFileHeaders().size() <= 0 || (fileHeader = Zip4jUtil.getFileHeader(this.zipModel, relativeFileName = Zip4jUtil.getRelativeFileName(((File)fileList.get(i)).getAbsolutePath(), parameters.getRootFolderInZip(), parameters.getDefaultFolderPath()))) == null) continue;
            totalWork += Zip4jUtil.getFileLengh(new File(this.zipModel.getZipFile())) - fileHeader.getCompressedSize();
        }
        return totalWork;
    }
}

