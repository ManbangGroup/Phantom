/*
 * Copyright (C) 2017-2019 Manbang Group
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

package com.wlqq.phantom.gradle.utils

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 完成jar的解压和重新压缩
 */

final class JarUtils {

    /**
     * 压缩文件
     *
     * @param filePath 待压缩的文件路径
     * @param outPath jar输出路径
     * @return 压缩后的文件
     */
    public static File makeJar(String filePath, String outPath) {
        File target = null;
        File source = new File(filePath);
        if (source.exists()) {
            createMetaFile(filePath);
            String zipName = outPath;
            target = new File(zipName);
            if (target.exists()) {
                target.delete(); // 删除旧的文件
            }

            if (null == target.getParentFile() || !target.getParentFile().exists()) {
                target.getParentFile().mkdirs();
            }

            FileOutputStream fos = null;
            ZipOutputStream zos = null;
            try {
                fos = new FileOutputStream(target);
                zos = new ZipOutputStream(new BufferedOutputStream(fos));
                // 添加对应的文件Entry
                for (File file : source.listFiles()) {
                    // 递归列出目录下的所有文件，添加文件Entry
                    addEntry("", file, zos);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(zos, fos);
            }
        }
        return target;
    }

    /**
     * 扫描添加文件Entry
     *
     * @param base   基路径
     * @param source 源文件
     * @param zos    jar文件输出流
     * @throws IOException
     */
    private static void addEntry(String base, File source, ZipOutputStream zos)
            throws IOException {
        String entry = base + source.getName();
        if (source.isDirectory()) {
            for (File file : source.listFiles()) {
                // 递归列出目录下的所有文件，添加文件Entry
                addEntry(entry + File.separator, file, zos);
            }
        } else {
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            try {
                byte[] buffer = new byte[1024 * 10];
                fis = new FileInputStream(source);
                bis = new BufferedInputStream(fis, buffer.length);
                int read = 0;
                String newEntryName = entry.replace((char)'\\', (char)'/');
                zos.putNextEntry(new ZipEntry(newEntryName));
                while ((read = bis.read(buffer, 0, buffer.length)) != -1) {
                    zos.write(buffer, 0, read);
                }
                zos.flush();
                zos.closeEntry();
            } finally {
                IOUtils.closeQuietly(bis, fis);
            }
        }
    }

    private static void createMetaFile(String path) throws FileNotFoundException, IOException {
        File metaDir = new File(path, "META-INF");
        if (!metaDir.exists() || !metaDir.isDirectory()) {
            metaDir.mkdirs();
        }

        File metaFile = new File(metaDir, "MANIFEST.MF");
        if (metaFile.exists()) {
            return;
        }

        metaFile.createNewFile();

        FileOutputStream fos = new FileOutputStream(metaFile);
        String metaData = "Manifest-Version: 1.0\n";
        fos.write(metaData.getBytes());
        IOUtils.closeQuietly(fos);

    }

    /**
     * 解压文件
     *
     * @param filePath 压缩文件路径
     * @param outPath 解压到的目录
     */
    public static void unzipJar(String filePath, String outPath) {
        File source = new File(filePath);
        File outDir = new File(outPath);
        if (!outDir.exists()) {
            outDir.mkdirs();
        }
        if (source.exists()) {
            ZipInputStream zis = null;
            BufferedOutputStream bos = null;
            try {
                zis = new ZipInputStream(new FileInputStream(source));
                ZipEntry entry = null;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    File target = new File(outPath, entry.getName());
                    if (!target.getParentFile().exists()) {
                        // 创建文件父目录
                        target.getParentFile().mkdirs();
                    }
                    if (!target.exists()) {
                        target.createNewFile();
                    }

                    // 写入文件
                    bos = new BufferedOutputStream(new FileOutputStream(target));
                    int read = 0;
                    byte[] buffer = new byte[1024 * 50];
                    while ((read = zis.read(buffer, 0, buffer.length)) != -1) {
                        bos.write(buffer, 0, read);
                    }
                    bos.flush();
                    bos.close();
                }
                zis.closeEntry();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                IOUtils.closeQuietly(zis, bos);
            }
        }
    }

    public static void transformJar(String filePath, File outJarPath, JarVisitor visitor) throws FileNotFoundException {
        File source = new File(filePath);
        if (!source.exists()) {
            throw new FileNotFoundException(filePath);
        }
        if (!outJarPath.getParentFile().exists()) {
            outJarPath.getParentFile().mkdirs();
        }


        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(outJarPath)));

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(new FileInputStream(source));
            ZipEntry entry = null;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                if (visitor.visitEntry(entry, zis, zos)) {
                    transformEntry(entry, zis, zos);
                    zos.closeEntry();
                    zis.closeEntry();
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(zis, zos);
        }

    }

    public static void transformEntry(ZipEntry entry, ZipInputStream inputStream, ZipOutputStream outputStream) throws IOException{
        byte[] buffer = new byte[1024 * 50];
        int readLen = 0;
        outputStream.putNextEntry(new ZipEntry(entry.getName()));
        while ((readLen = inputStream.read(buffer, 0, buffer.length)) != -1) {
            outputStream.write(buffer, 0, readLen);
        }
    }

    public static byte[] getEntryData(ZipInputStream inputStream)  throws IOException{
        byte[] buffer = new byte[1024 * 50];
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int readLen = 0;
        while ((readLen = inputStream.read(buffer, 0, buffer.length)) != -1) {
            bos.write(buffer, 0, readLen);
        }
        inputStream.closeEntry();
        bos.close();
        return bos.toByteArray();
    }

    public static void saveEntry(String entryName, byte[] entryData, ZipOutputStream outputStream) throws IOException{
        outputStream.putNextEntry(new ZipEntry(entryName));
        outputStream.write(entryData, 0, entryData.length);
        outputStream.closeEntry();
    }
}
