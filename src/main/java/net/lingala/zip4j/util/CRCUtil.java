/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.progress.ProgressMonitor;
import net.lingala.zip4j.util.Zip4jUtil;

public class CRCUtil {
    private static final int BUF_SIZE = 16384;

    public static long computeFileCRC(String inputFile) throws ZipException {
        return CRCUtil.computeFileCRC(inputFile, null);
    }

    public static long computeFileCRC(String inputFile, ProgressMonitor progressMonitor) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(inputFile)) {
            throw new ZipException("input file is null or empty, cannot calculate CRC for the file");
        }
        InputStream inputStream = null;
        try {
            Zip4jUtil.checkFileReadAccess(inputFile);
            inputStream = new FileInputStream(new File(inputFile));
            byte[] buff = new byte[16384];
            int readLen = -2;
            CRC32 crc32 = new CRC32();
            while ((readLen = inputStream.read(buff)) != -1) {
                crc32.update(buff, 0, readLen);
                if (progressMonitor == null) continue;
                progressMonitor.updateWorkCompleted(readLen);
                if (!progressMonitor.isCancelAllTasks()) continue;
                progressMonitor.setResult(3);
                progressMonitor.setState(0);
                long l = 0L;
                return l;
            }
            long l = crc32.getValue();
            return l;
        } catch (IOException e) {
            throw new ZipException(e);
        } catch (Exception e) {
            throw new ZipException(e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new ZipException("error while closing the file after calculating crc");
                }
            }
        }
    }
}

