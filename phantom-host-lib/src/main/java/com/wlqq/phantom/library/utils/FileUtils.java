/*
 * Copyright (C) 2017-2018 Manbang Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.wlqq.phantom.library.utils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipFile;

public final class FileUtils {

    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The file copy buffer size (30 MB)
     */
    private static final long FILE_COPY_BUFFER_SIZE = ONE_MB * 30;
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
    private static final int EOF = -1;

    private FileUtils() {
        // prevent instantiate
    }

    public static boolean isDirectoryEmpty(@NonNull File directory) {
        if (directory.isDirectory()) {
            final String[] files = directory.list();
            return files == null || files.length == 0;
        } else {
            return true;
        }
    }

    public static byte[] toByteArray(InputStream inStream) throws IOException {
        ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
        byte[] buff = new byte[100];
        int rc;
        while ((rc = inStream.read(buff, 0, 100)) > 0) {
            swapStream.write(buff, 0, rc);
        }
        return swapStream.toByteArray();
    }

    public static File ensureDirectoryCreated(@NonNull File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            VLog.w("Unable to create the directory: %s.", folder.getAbsolutePath());
        }
        return folder;
    }

    /**
     * Deletes a directory recursively.
     *
     * @param dir directory to delete
     * @return true if success
     */
    public static boolean deleteDir(@Nullable File dir) {
        if (dir == null) {
            return false;
        }
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String file : children) {
                    boolean success = deleteDir(new File(dir, file));
                    if (!success) {
                        return false;
                    }
                }
            }
        }
        return dir.delete();
    }

    /**
     * Cleans a directory without deleting it.
     *
     * @param dir directory to clean
     */
    public static void cleanDir(File dir) {
        if (dir.isDirectory()) {
            final File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        cleanDir(file);
                    }
                    if (!file.delete()) {
                        VLog.w("delete %s error", file.getName());
                    }
                }
            }
        }
    }

    /**
     * Copies bytes from an java.io.InputStream source to a file destination. The directories up to destination will be
     * created if they don't already exist. destination will be overwritten if it already exists.
     *
     * @param source      the InputStream to copy bytes from, must not be null
     * @param destination the non-directory File to write bytes to (possibly overwriting), must not be null
     * @throws IOException if an IO error occurs during copying
     */
    public static void copyInputStreamToFile(@NonNull InputStream source, @NonNull File destination)
            throws IOException {
        try {
            FileOutputStream output = new FileOutputStream(destination);
            try {
                copyStream(source, output);
                output.close(); // don't swallow close Exception if copy completes normally
            } finally {
                IoUtils.closeQuietly(output);
            }
        } finally {
            IoUtils.closeQuietly(source);
        }
    }

    public static long copyStream(InputStream input, OutputStream output) throws IOException {
        return copyStream(input, output, new byte[DEFAULT_BUFFER_SIZE]);
    }

    public static long copyStream(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
        long count = 0;
        int n;
        while ((n = input.read(buffer)) != EOF) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copies a file to a new location.
     *
     * @param srcFile  the validated source file, must not be {@code null}
     * @param destFile the validated destination file, must not be {@code null}
     * @throws IOException if an error occurs
     */
    public static void copyFile(File srcFile, File destFile) throws IOException {
        if (destFile.exists() && destFile.isDirectory()) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0;
            long count = 0;
            while (pos < size) {
                count = size - pos > FILE_COPY_BUFFER_SIZE ? FILE_COPY_BUFFER_SIZE : size - pos;
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            IoUtils.closeQuietly(output);
            IoUtils.closeQuietly(fos);
            IoUtils.closeQuietly(input);
            IoUtils.closeQuietly(fis);
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '"
                    + srcFile + "' to '" + destFile + "'");
        }
    }

    public static void closeZipFileQuietly(@Nullable ZipFile zipFile) {
        if (zipFile != null) {
            try {
                zipFile.close();
            } catch (Exception ignored) {
                // ignore it
            }
        }
    }

    public static int readFileToInt(final File file) throws IOException {
        DataInputStream inputStream = null;
        try {
            inputStream = new DataInputStream(new FileInputStream(file));
            return inputStream.readInt();
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    public static void writeIntToFile(final File file, int data) throws IOException {
        DataOutputStream outputStream = null;
        try {
            outputStream = new DataOutputStream(new FileOutputStream(file));
            outputStream.writeInt(data);
        } finally {
            IoUtils.closeQuietly(outputStream);
        }
    }

    /**
     * Calculate the md5 string for the given inputStream
     *
     * @param inputStream the given inputStream
     * @return the md5 string for the given inputStream or <code>null</code> if error
     */
    @Nullable
    static String calculateMd5(@NonNull InputStream inputStream) {
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            VLog.w(e, "Exception while getting digest");
            return null;
        }

        byte[] buffer = new byte[8192];
        int read;
        try {
            while ((read = inputStream.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            // Fill to 32 chars
            output = String.format("%32s", output).replace(' ', '0');
            return output;
        } catch (IOException e) {
            VLog.w(e, "Unable to process file for MD5");
            return null;
        } finally {
            IoUtils.closeQuietly(inputStream);
        }
    }

    /**
     * Calculate the md5 string for the given file
     *
     * @param file the given file
     * @return the md5 string for the given file or <code>null</code> if error
     */
    @Nullable
    public static String calculateMd5(@NonNull File file) {
        try {
            return calculateMd5(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            VLog.w(e, "Exception while getting FileInputStream");
            return null;
        }
    }
}
