/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.unzip;

import java.io.File;
import java.util.ArrayList;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.CentralDirectory;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.unzip.UnzipEngine;
import net.lingala.zip4j.util.InternalZipConstants;
import net.lingala.zip4j.util.Zip4jUtil;

public class Unzip {
    private ZipModel zipModel;

    public Unzip(ZipModel zipModel) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("ZipModel is null");
        }
        this.zipModel = zipModel;
    }

    public void extractAll(final UnzipParameters unzipParameters, final String outPath, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        CentralDirectory centralDirectory = this.zipModel.getCentralDirectory();
        if (centralDirectory == null || centralDirectory.getFileHeaders() == null) {
            throw new ZipException("invalid central directory in zipModel");
        }
        final ArrayList fileHeaders = centralDirectory.getFileHeaders();
        progressMonitor.setCurrentOperation(1);
        progressMonitor.setTotalWork(this.calculateTotalWork(fileHeaders));
        progressMonitor.setState(1);
        if (runInThread) {
            Thread thread = new Thread("Zip4j"){

                @Override
                public void run() {
                    try {
                        Unzip.this.initExtractAll(fileHeaders, unzipParameters, progressMonitor, outPath);
                        progressMonitor.endProgressMonitorSuccess();
                    } catch (ZipException zipException) {
                        // empty catch block
                    }
                }
            };
            thread.start();
        } else {
            this.initExtractAll(fileHeaders, unzipParameters, progressMonitor, outPath);
        }
    }

    private void initExtractAll(ArrayList fileHeaders, UnzipParameters unzipParameters, ProgressMonitor progressMonitor, String outPath) throws ZipException {
        for (int i = 0; i < fileHeaders.size(); ++i) {
            FileHeader fileHeader = (FileHeader)fileHeaders.get(i);
            this.initExtractFile(fileHeader, outPath, unzipParameters, null, progressMonitor);
            if (!progressMonitor.isCancelAllTasks()) continue;
            progressMonitor.setResult(3);
            progressMonitor.setState(0);
            return;
        }
    }

    public void extractFile(final FileHeader fileHeader, final String outPath, final UnzipParameters unzipParameters, final String newFileName, final ProgressMonitor progressMonitor, boolean runInThread) throws ZipException {
        if (fileHeader == null) {
            throw new ZipException("fileHeader is null");
        }
        progressMonitor.setCurrentOperation(1);
        progressMonitor.setTotalWork(fileHeader.getCompressedSize());
        progressMonitor.setState(1);
        progressMonitor.setPercentDone(0);
        progressMonitor.setFileName(fileHeader.getFileName());
        if (runInThread) {
            Thread thread = new Thread("Zip4j"){

                @Override
                public void run() {
                    try {
                        Unzip.this.initExtractFile(fileHeader, outPath, unzipParameters, newFileName, progressMonitor);
                        progressMonitor.endProgressMonitorSuccess();
                    } catch (ZipException zipException) {
                        // empty catch block
                    }
                }
            };
            thread.start();
        } else {
            this.initExtractFile(fileHeader, outPath, unzipParameters, newFileName, progressMonitor);
            progressMonitor.endProgressMonitorSuccess();
        }
    }

    private void initExtractFile(FileHeader fileHeader, String outPath, UnzipParameters unzipParameters, String newFileName, ProgressMonitor progressMonitor) throws ZipException {
        block13: {
            if (fileHeader == null) {
                throw new ZipException("fileHeader is null");
            }
            try {
                progressMonitor.setFileName(fileHeader.getFileName());
                if (!outPath.endsWith(InternalZipConstants.FILE_SEPARATOR)) {
                    outPath = outPath + InternalZipConstants.FILE_SEPARATOR;
                }
                String fileName = fileHeader.getFileName();
                String completePath = outPath + fileName;
                if (!new File(completePath).getCanonicalPath().startsWith(new File(outPath).getCanonicalPath())) {
                    throw new ZipException("illegal file name that breaks out of the target directory: " + fileHeader.getFileName());
                }
                if (fileHeader.isDirectory()) {
                    try {
                        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
                            return;
                        }
                        File file = new File(completePath);
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        break block13;
                    } catch (Exception e) {
                        progressMonitor.endProgressMonitorError(e);
                        throw new ZipException(e);
                    }
                }
                this.checkOutputDirectoryStructure(fileHeader, outPath, newFileName);
                UnzipEngine unzipEngine = new UnzipEngine(this.zipModel, fileHeader);
                try {
                    unzipEngine.unzipFile(progressMonitor, outPath, newFileName, unzipParameters);
                } catch (Exception e) {
                    progressMonitor.endProgressMonitorError(e);
                    throw new ZipException(e);
                }
            } catch (ZipException e) {
                progressMonitor.endProgressMonitorError(e);
                throw e;
            } catch (Exception e) {
                progressMonitor.endProgressMonitorError(e);
                throw new ZipException(e);
            }
        }
    }

    public ZipInputStream getInputStream(FileHeader fileHeader) throws ZipException {
        UnzipEngine unzipEngine = new UnzipEngine(this.zipModel, fileHeader);
        return unzipEngine.getInputStream();
    }

    private void checkOutputDirectoryStructure(FileHeader fileHeader, String outPath, String newFileName) throws ZipException {
        if (fileHeader == null || !Zip4jUtil.isStringNotNullAndNotEmpty(outPath)) {
            throw new ZipException("Cannot check output directory structure...one of the parameters was null");
        }
        String fileName = fileHeader.getFileName();
        if (Zip4jUtil.isStringNotNullAndNotEmpty(newFileName)) {
            fileName = newFileName;
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            return;
        }
        String compOutPath = outPath + fileName;
        try {
            File file = new File(compOutPath);
            String parentDir = file.getParent();
            File parentDirFile = new File(parentDir);
            if (!parentDirFile.exists()) {
                parentDirFile.mkdirs();
            }
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    private long calculateTotalWork(ArrayList fileHeaders) throws ZipException {
        if (fileHeaders == null) {
            throw new ZipException("fileHeaders is null, cannot calculate total work");
        }
        long totalWork = 0L;
        for (int i = 0; i < fileHeaders.size(); ++i) {
            FileHeader fileHeader = (FileHeader)fileHeaders.get(i);
            if (fileHeader.getZip64ExtendedInfo() != null && fileHeader.getZip64ExtendedInfo().getUnCompressedSize() > 0L) {
                totalWork += fileHeader.getZip64ExtendedInfo().getCompressedSize();
                continue;
            }
            totalWork += fileHeader.getCompressedSize();
        }
        return totalWork;
    }
}

