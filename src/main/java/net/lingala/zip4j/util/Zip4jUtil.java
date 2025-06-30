/*
 * Decompiled with CFR 0.153-SNAPSHOT (3d1d0f4).
 */
package net.lingala.zip4j.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipModel;
import net.lingala.zip4j.util.InternalZipConstants;

public class Zip4jUtil {
    public static boolean isStringNotNullAndNotEmpty(String str) {
        return str != null && str.trim().length() > 0;
    }

    public static boolean checkOutputFolder(String path) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
            throw new ZipException(new NullPointerException("output path is null"));
        }
        File file = new File(path);
        if (file.exists()) {
            if (!file.isDirectory()) {
                throw new ZipException("output folder is not valid");
            }
            if (!file.canWrite()) {
                throw new ZipException("no write access to output folder");
            }
        } else {
            try {
                file.mkdirs();
                if (!file.isDirectory()) {
                    throw new ZipException("output folder is not valid");
                }
                if (!file.canWrite()) {
                    throw new ZipException("no write access to destination folder");
                }
            } catch (Exception e) {
                throw new ZipException("Cannot create destination folder");
            }
        }
        return true;
    }

    public static boolean checkFileReadAccess(String path) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
            throw new ZipException("path is null");
        }
        if (!Zip4jUtil.checkFileExists(path)) {
            throw new ZipException("file does not exist: " + path);
        }
        try {
            File file = new File(path);
            return file.canRead();
        } catch (Exception e) {
            throw new ZipException("cannot read zip file");
        }
    }

    public static boolean checkFileWriteAccess(String path) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
            throw new ZipException("path is null");
        }
        if (!Zip4jUtil.checkFileExists(path)) {
            throw new ZipException("file does not exist: " + path);
        }
        try {
            File file = new File(path);
            return file.canWrite();
        } catch (Exception e) {
            throw new ZipException("cannot read zip file");
        }
    }

    public static boolean checkFileExists(String path) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(path)) {
            throw new ZipException("path is null");
        }
        File file = new File(path);
        return Zip4jUtil.checkFileExists(file);
    }

    public static boolean checkFileExists(File file) throws ZipException {
        if (file == null) {
            throw new ZipException("cannot check if file exists: input file is null");
        }
        return file.exists();
    }

    public static boolean isWindows() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.indexOf("win") >= 0;
    }

    public static void setFileReadOnly(File file) throws ZipException {
        if (file == null) {
            throw new ZipException("input file is null. cannot set read only file attribute");
        }
        if (file.exists()) {
            file.setReadOnly();
        }
    }

    public static void setFileHidden(File file) throws ZipException {
    }

    public static void setFileArchive(File file) throws ZipException {
    }

    public static void setFileSystemMode(File file) throws ZipException {
    }

    public static long getLastModifiedFileTime(File file, TimeZone timeZone) throws ZipException {
        if (file == null) {
            throw new ZipException("input file is null, cannot read last modified file time");
        }
        if (!file.exists()) {
            throw new ZipException("input file does not exist, cannot read last modified file time");
        }
        return file.lastModified();
    }

    public static String getFileNameFromFilePath(File file) throws ZipException {
        if (file == null) {
            throw new ZipException("input file is null, cannot get file name");
        }
        if (file.isDirectory()) {
            return null;
        }
        return file.getName();
    }

    public static long getFileLengh(String file) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(file)) {
            throw new ZipException("invalid file name");
        }
        return Zip4jUtil.getFileLengh(new File(file));
    }

    public static long getFileLengh(File file) throws ZipException {
        if (file == null) {
            throw new ZipException("input file is null, cannot calculate file length");
        }
        if (file.isDirectory()) {
            return -1L;
        }
        return file.length();
    }

    public static long javaToDosTime(long time) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        int year = cal.get(1);
        if (year < 1980) {
            return 0x210000L;
        }
        return year - 1980 << 25 | cal.get(2) + 1 << 21 | cal.get(5) << 16 | cal.get(11) << 11 | cal.get(12) << 5 | cal.get(13) >> 1;
    }

    public static long dosToJavaTme(int dosTime) {
        int sec = 2 * (dosTime & 0x1F);
        int min = dosTime >> 5 & 0x3F;
        int hrs = dosTime >> 11 & 0x1F;
        int day = dosTime >> 16 & 0x1F;
        int mon = (dosTime >> 21 & 0xF) - 1;
        int year = (dosTime >> 25 & 0x7F) + 1980;
        Calendar cal = Calendar.getInstance();
        cal.set(year, mon, day, hrs, min, sec);
        cal.set(14, 0);
        return cal.getTime().getTime();
    }

    public static FileHeader getFileHeader(ZipModel zipModel, String fileName) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot determine file header for fileName: " + fileName);
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file name is null, cannot determine file header for fileName: " + fileName);
        }
        FileHeader fileHeader = null;
        fileHeader = Zip4jUtil.getFileHeaderWithExactMatch(zipModel, fileName);
        if (fileHeader == null && (fileHeader = Zip4jUtil.getFileHeaderWithExactMatch(zipModel, fileName = fileName.replaceAll("\\\\", "/"))) == null) {
            fileName = fileName.replaceAll("/", "\\\\");
            fileHeader = Zip4jUtil.getFileHeaderWithExactMatch(zipModel, fileName);
        }
        return fileHeader;
    }

    public static FileHeader getFileHeaderWithExactMatch(ZipModel zipModel, String fileName) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("zip model is null, cannot determine file header with exact match for fileName: " + fileName);
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file name is null, cannot determine file header with exact match for fileName: " + fileName);
        }
        if (zipModel.getCentralDirectory() == null) {
            throw new ZipException("central directory is null, cannot determine file header with exact match for fileName: " + fileName);
        }
        if (zipModel.getCentralDirectory().getFileHeaders() == null) {
            throw new ZipException("file Headers are null, cannot determine file header with exact match for fileName: " + fileName);
        }
        if (zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
            return null;
        }
        ArrayList fileHeaders = zipModel.getCentralDirectory().getFileHeaders();
        for (int i = 0; i < fileHeaders.size(); ++i) {
            FileHeader fileHeader = (FileHeader)fileHeaders.get(i);
            String fileNameForHdr = fileHeader.getFileName();
            if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameForHdr) || !fileName.equalsIgnoreCase(fileNameForHdr)) continue;
            return fileHeader;
        }
        return null;
    }

    public static int getIndexOfFileHeader(ZipModel zipModel, FileHeader fileHeader) throws ZipException {
        if (zipModel == null || fileHeader == null) {
            throw new ZipException("input parameters is null, cannot determine index of file header");
        }
        if (zipModel.getCentralDirectory() == null) {
            throw new ZipException("central directory is null, ccannot determine index of file header");
        }
        if (zipModel.getCentralDirectory().getFileHeaders() == null) {
            throw new ZipException("file Headers are null, cannot determine index of file header");
        }
        if (zipModel.getCentralDirectory().getFileHeaders().size() <= 0) {
            return -1;
        }
        String fileName = fileHeader.getFileName();
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("file name in file header is empty or null, cannot determine index of file header");
        }
        ArrayList fileHeaders = zipModel.getCentralDirectory().getFileHeaders();
        for (int i = 0; i < fileHeaders.size(); ++i) {
            FileHeader fileHeaderTmp = (FileHeader)fileHeaders.get(i);
            String fileNameForHdr = fileHeaderTmp.getFileName();
            if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileNameForHdr) || !fileName.equalsIgnoreCase(fileNameForHdr)) continue;
            return i;
        }
        return -1;
    }

    public static ArrayList getFilesInDirectoryRec(File path, boolean readHiddenFiles) throws ZipException {
        if (path == null) {
            throw new ZipException("input path is null, cannot read files in the directory");
        }
        ArrayList<File> result = new ArrayList<File>();
        File[] filesAndDirs = path.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        if (!path.canRead()) {
            return result;
        }
        for (int i = 0; i < filesDirs.size(); ++i) {
            File file = filesDirs.get(i);
            if (file.isHidden() && !readHiddenFiles) {
                return result;
            }
            result.add(file);
            if (!file.isDirectory()) continue;
            ArrayList deeperList = Zip4jUtil.getFilesInDirectoryRec(file, readHiddenFiles);
            result.addAll(deeperList);
        }
        return result;
    }

    public static String getZipFileNameWithoutExt(String zipFile) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(zipFile)) {
            throw new ZipException("zip file name is empty or null, cannot determine zip file name");
        }
        String tmpFileName = zipFile;
        if (zipFile.indexOf(System.getProperty("file.separator")) >= 0) {
            tmpFileName = zipFile.substring(zipFile.lastIndexOf(System.getProperty("file.separator")));
        }
        if (tmpFileName.indexOf(".") > 0) {
            tmpFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf("."));
        }
        return tmpFileName;
    }

    public static byte[] convertCharset(String str) throws ZipException {
        try {
            byte[] converted = null;
            String charSet = Zip4jUtil.detectCharSet(str);
            converted = charSet.equals("Cp850") ? str.getBytes("Cp850") : (charSet.equals("UTF8") ? str.getBytes("UTF8") : str.getBytes());
            return converted;
        } catch (UnsupportedEncodingException err) {
            return str.getBytes();
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public static String decodeFileName(byte[] data, boolean isUTF8) {
        if (isUTF8) {
            try {
                return new String(data, "UTF8");
            } catch (UnsupportedEncodingException e) {
                return new String(data);
            }
        }
        return Zip4jUtil.getCp850EncodedString(data);
    }

    public static String getCp850EncodedString(byte[] data) {
        try {
            String retString = new String(data, "Cp850");
            return retString;
        } catch (UnsupportedEncodingException e) {
            return new String(data);
        }
    }

    public static String getAbsoluteFilePath(String filePath) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(filePath)) {
            throw new ZipException("filePath is null or empty, cannot get absolute file path");
        }
        File file = new File(filePath);
        return file.getAbsolutePath();
    }

    public static boolean checkArrayListTypes(ArrayList sourceList, int type) throws ZipException {
        if (sourceList == null) {
            throw new ZipException("input arraylist is null, cannot check types");
        }
        if (sourceList.size() <= 0) {
            return true;
        }
        boolean invalidFound = false;
        block0 : switch (type) {
            case 1: {
                for (int i = 0; i < sourceList.size(); ++i) {
                    if (sourceList.get(i) instanceof File) continue;
                    invalidFound = true;
                    break block0;
                }
                break;
            }
            case 2: {
                for (int i = 0; i < sourceList.size(); ++i) {
                    if (sourceList.get(i) instanceof String) continue;
                    invalidFound = true;
                    break block0;
                }
                break;
            }
        }
        return !invalidFound;
    }

    public static String detectCharSet(String str) throws ZipException {
        if (str == null) {
            throw new ZipException("input string is null, cannot detect charset");
        }
        try {
            byte[] byteString = str.getBytes("Cp850");
            String tempString = new String(byteString, "Cp850");
            if (str.equals(tempString)) {
                return "Cp850";
            }
            byteString = str.getBytes("UTF8");
            tempString = new String(byteString, "UTF8");
            if (str.equals(tempString)) {
                return "UTF8";
            }
            return InternalZipConstants.CHARSET_DEFAULT;
        } catch (UnsupportedEncodingException e) {
            return InternalZipConstants.CHARSET_DEFAULT;
        } catch (Exception e) {
            return InternalZipConstants.CHARSET_DEFAULT;
        }
    }

    public static int getEncodedStringLength(String str) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(str)) {
            throw new ZipException("input string is null, cannot calculate encoded String length");
        }
        String charset = Zip4jUtil.detectCharSet(str);
        return Zip4jUtil.getEncodedStringLength(str, charset);
    }

    public static int getEncodedStringLength(String str, String charset) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(str)) {
            throw new ZipException("input string is null, cannot calculate encoded String length");
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(charset)) {
            throw new ZipException("encoding is not defined, cannot calculate string length");
        }
        ByteBuffer byteBuffer = null;
        try {
            byteBuffer = charset.equals("Cp850") ? ByteBuffer.wrap(str.getBytes("Cp850")) : (charset.equals("UTF8") ? ByteBuffer.wrap(str.getBytes("UTF8")) : ByteBuffer.wrap(str.getBytes(charset)));
        } catch (UnsupportedEncodingException e) {
            byteBuffer = ByteBuffer.wrap(str.getBytes());
        } catch (Exception e) {
            throw new ZipException(e);
        }
        return byteBuffer.limit();
    }

    public static boolean isSupportedCharset(String charset) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(charset)) {
            throw new ZipException("charset is null or empty, cannot check if it is supported");
        }
        try {
            new String("a".getBytes(), charset);
            return true;
        } catch (UnsupportedEncodingException e) {
            return false;
        } catch (Exception e) {
            throw new ZipException(e);
        }
    }

    public static ArrayList getSplitZipFiles(ZipModel zipModel) throws ZipException {
        if (zipModel == null) {
            throw new ZipException("cannot get split zip files: zipmodel is null");
        }
        if (zipModel.getEndCentralDirRecord() == null) {
            return null;
        }
        ArrayList<String> retList = new ArrayList<String>();
        String currZipFile = zipModel.getZipFile();
        String zipFileName = new File(currZipFile).getName();
        String partFile = null;
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(currZipFile)) {
            throw new ZipException("cannot get split zip files: zipfile is null");
        }
        if (!zipModel.isSplitArchive()) {
            retList.add(currZipFile);
            return retList;
        }
        int numberOfThisDisk = zipModel.getEndCentralDirRecord().getNoOfThisDisk();
        if (numberOfThisDisk == 0) {
            retList.add(currZipFile);
            return retList;
        }
        for (int i = 0; i <= numberOfThisDisk; ++i) {
            if (i == numberOfThisDisk) {
                retList.add(zipModel.getZipFile());
                continue;
            }
            String fileExt = ".z0";
            if (i > 9) {
                fileExt = ".z";
            }
            partFile = zipFileName.indexOf(".") >= 0 ? currZipFile.substring(0, currZipFile.lastIndexOf(".")) : currZipFile;
            partFile = partFile + fileExt + (i + 1);
            retList.add(partFile);
        }
        return retList;
    }

    public static String getRelativeFileName(String file, String rootFolderInZip, String rootFolderPath) throws ZipException {
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(file)) {
            throw new ZipException("input file path/name is empty, cannot calculate relative file name");
        }
        String fileName = null;
        if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderPath)) {
            File tmpFile;
            String tmpFileName;
            File rootFolderFile = new File(rootFolderPath);
            String rootFolderFileRef = rootFolderFile.getPath();
            if (!rootFolderFileRef.endsWith(InternalZipConstants.FILE_SEPARATOR)) {
                rootFolderFileRef = rootFolderFileRef + InternalZipConstants.FILE_SEPARATOR;
            }
            if ((tmpFileName = file.substring(rootFolderFileRef.length())).startsWith(System.getProperty("file.separator"))) {
                tmpFileName = tmpFileName.substring(1);
            }
            if ((tmpFile = new File(file)).isDirectory()) {
                tmpFileName = tmpFileName.replaceAll("\\\\", "/");
                tmpFileName = tmpFileName + "/";
            } else {
                String bkFileName = tmpFileName.substring(0, tmpFileName.lastIndexOf(tmpFile.getName()));
                bkFileName = bkFileName.replaceAll("\\\\", "/");
                tmpFileName = bkFileName + tmpFile.getName();
            }
            fileName = tmpFileName;
        } else {
            File relFile = new File(file);
            fileName = relFile.isDirectory() ? relFile.getName() + "/" : Zip4jUtil.getFileNameFromFilePath(new File(file));
        }
        if (Zip4jUtil.isStringNotNullAndNotEmpty(rootFolderInZip)) {
            fileName = rootFolderInZip + fileName;
        }
        if (!Zip4jUtil.isStringNotNullAndNotEmpty(fileName)) {
            throw new ZipException("Error determining file name");
        }
        return fileName;
    }

    public static long[] getAllHeaderSignatures() {
        long[] allSigs = new long[]{67324752L, 134695760L, 33639248L, 101010256L, 84233040L, 134630224L, 134695760L, 117853008L, 101075792L, 1L, 39169L};
        return allSigs;
    }
}

