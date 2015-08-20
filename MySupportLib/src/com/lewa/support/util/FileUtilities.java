package com.lewa.support.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Tools for managing files.
 *
 * <p>
 * Facilities are provided in the following areas:
 * <ul>
 * <li>writing to a file
 * <li>reading from a file
 * <li>make a directory including parent directories
 * <li>copying files and directories
 * <li>deleting files and directories
 * <li>converting to and from a URL
 * <li>listing files and directories by filter and extension
 * <li>comparing file content
 * <li>file last changed date
 * <li>calculating a checksum
 * </ul>
 * <p>
 *
 * NOTE: Not all of the facilities above are implemented by this time.
 *
 */
public class FileUtilities extends FileUtils {
    /**
     * The number of bytes in a kilobyte.
     */
    public static final long ONE_KB = 1024;

    /**
     * The number of bytes in a megabyte.
     */
    public static final long ONE_MB = ONE_KB * ONE_KB;

    /**
     * The number of bytes in a gigabyte.
     */
    public static final long ONE_GB = ONE_KB * ONE_MB;

    /**
     * An empty array of type <code>File</code>.
     */
    public static final File[] EMPTY_FILE_ARRAY = new File[0];

    /**
     * Replace separators in the given path to '+'
     */
    public static String encodePathSegment(String path) {
        return path.replaceAll(File.separator, "+");
    }

    /**
     * Replace separators in the given path to '+'
     */
    public static String basename(File file) {
        return basename(file.getAbsolutePath());
    }

    /**
     * Remove any prefix up to the last slash ('/') character from the file path
     */
    public static String basename(String path) {
        int index = path.lastIndexOf(File.separatorChar);
        if (index <= 0) {
            return path;
        } else {
            return path.substring(index + 1);
        }
    }

    /**
     * Remove suffix from the file name
     */
    public static String removeExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index <= 0) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    /**
     * Open a FileInputStream for the specified file.
     *
     * @see #openOutputStream(File)
     */
    public static FileInputStream openInputStream(String fileName) throws IOException {
        return openInputStream(new File(fileName));
    }

    /**
     * Open a {@link FileInputStream} for the specified file, providing better
     * error messages than simply calling <code>new FileInputStream(file)</code>.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * <p>
     * An exception is thrown if the file does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be read.
     *
     * @param file  the file to open for input, must not be <code>null</code>
     * @return a new {@link FileInputStream} for the specified file
     * @throws FileNotFoundException if the file does not exist
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be read
     */
    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canRead() == false) {
                throw new IOException("File '" + file + "' cannot be read");
            }
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    /**
     * Open a FileOutputStream for the specified file.
     *
     * @see #openOutputStream(File)
     */
    public static FileOutputStream openOutputStream(String fileName) throws IOException {
        return openOutputStream(new File(fileName));
    }

    /**
     * Open a {@link FileOutputStream} for the specified file, checking and
     * creating the parent directory if it does not exist.
     * <p>
     * At the end of the method either the stream will be successfully opened,
     * or an exception will have been thrown.
     * <p>
     * The parent directory will be created if it does not exist.
     * The file will be created if it does not exist.
     * An exception is thrown if the file object exists but is a directory.
     * An exception is thrown if the file exists but cannot be written to.
     * An exception is thrown if the parent directory cannot be created.
     *
     * @param file  the file to open for output, must not be <code>null</code>
     * @return a new {@link FileOutputStream} for the specified file
     * @throws IOException if the file object is a directory
     * @throws IOException if the file cannot be written to
     * @throws IOException if a parent directory needs creating but that fails
     */
    public static FileOutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (file.canWrite() == false) {
                throw new IOException("File '" + file + "' cannot be written to");
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && parent.exists() == false) {
                if (parent.mkdirs() == false) {
                    throw new IOException("File '" + file + "' could not be created");
                }
            }
        }
        return new FileOutputStream(file);
    }

    /**
     * Return a human-readable version of the file size, where the input
     * represents a specific number of bytes.
     *
     * @param size  the number of bytes
     * @return a human-readable display value (includes units)
     */
    public static String byteCountToDisplaySize(long size) {
        String displaySize;

        if (size / ONE_GB > 0) {
            displaySize = String.valueOf(size / ONE_GB) + " GB";
        } else if (size / ONE_MB > 0) {
            displaySize = String.valueOf(size / ONE_MB) + " MB";
        } else if (size / ONE_KB > 0) {
            displaySize = String.valueOf(size / ONE_KB) + " KB";
        } else {
            displaySize = String.valueOf(size) + " bytes";
        }
        return displaySize;
    }

    /**
     * Delete a directory recursively.
     *
     * @param directory  directory to delete
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectory(directory);
        if (!directory.delete()) {
            String message =
                "Unable to delete directory " + directory + ".";
            throw new IOException(message);
        }
    }

    /**
     * Delete a file, never throwing an exception.
     * If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>No exceptions are thrown when a file or directory cannot be deleted.</li>
     * </ul>
     *
     * @param file  file or directory to delete, can be <code>null</code>
     * @return <code>true</code> if the file or directory was deleted, otherwise
     * <code>false</code>
     */
    public static boolean deleteQuietly(File file) {
        if (file == null) {
            return false;
        }
        try {
            if (file.isDirectory()) {
                cleanDirectory(file);
            }
        } catch (Exception e) {
        }

        try {
            return file.delete();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clean a directory without deleting the directory.
     *
     * @param directory directory to clean
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory) throws IOException {
        cleanDirectory(directory, null);
    }

    /**
     * Delete files whose names match the given filter in a directory.
     *
     * @param directory directory to clean
     * @param filter only files whose names match will be deleted
     * @throws IOException in case cleaning is unsuccessful
     */
    public static void cleanDirectory(File directory, FilenameFilter filter) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = ((filter == null) ? directory.listFiles() : directory.listFiles(filter));
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * <p>
     * The difference between File.delete() and this method are:
     * <ul>
     * <li>A directory to be deleted does not have to be empty.</li>
     * <li>You get exceptions when a file or directory cannot be deleted.
     *      (java.io.File methods returns a boolean)</li>
     * </ul>
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws FileNotFoundException if the file was not found
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent){
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message =
                    "Unable to delete file: " + file;
                throw new IOException(message);
            }
        }
    }

    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * No exception will be thrown if the file doesn't exist.
     *
     * @see #deleteIfExists(File)
     */
    public static void deleteIfExists(String fileName) throws IOException {
        deleteIfExists(new File(fileName));
    }

    /**
     * Delete a file. If file is a directory, delete it and all sub-directories.
     * No exception will be thrown if the file doesn't exist.
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws IOException in case deletion is unsuccessful
     */
    public static void deleteIfExists(File file) throws IOException {
        if (!file.exists()) {
            return;
        }
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else if (!file.delete()) {
            String message = "Unable to delete file: " + file;
            throw new IOException(message);
        }
    }

    /**
     * Create a new, empty file named by fileName.
     *
     * <p>
     * The difference between File.createNewFile() and this method is:
     * <ul>
     * <li>This method doesn't throw any exception.  </li>
     * </ul>
     */
    public static boolean createNewFile(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.createNewFile();
                return true;
            } catch (IOException e) {
            }
        }
        return false;
    }

    /**
     * Schedule a file to be deleted when JVM exits.
     * If file is directory delete it and all sub-directories.
     *
     * @param file  file or directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the file is <code>null</code>
     * @throws IOException in case deletion is unsuccessful
     */
    public static void forceDeleteOnExit(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectoryOnExit(file);
        } else {
            file.deleteOnExit();
        }
    }

    /**
     * Schedule a directory recursively for deletion on JVM exit.
     *
     * @param directory  directory to delete, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException in case deletion is unsuccessful
     */
    private static void deleteDirectoryOnExit(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        cleanDirectoryOnExit(directory);
        directory.deleteOnExit();
    }

    /**
     * Schedule a directory clean on JVM exit.
     *
     * @param directory  directory to clean, must not be <code>null</code>
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException in case cleaning is unsuccessful
     */
    private static void cleanDirectoryOnExit(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {  // null if security restricted
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            try {
                forceDeleteOnExit(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception) {
            throw exception;
        }
    }

    /**
     * Make a directory, including any necessary but nonexistent parent
     * directories. If there already exists a file with specified name or
     * the directory cannot be created then an exception is thrown.
     *
     * @see #forceMkdir(File, boolean)
     */
    public static void forceMkdir(File directory) throws IOException {
        forceMkdir(directory, false);
    }

    /**
     * Make a directory, including any necessary but nonexistent parent
     * directories. If there already exists a file with specified name or
     * the directory cannot be created then an exception is thrown.
     *
     * @param directory  directory to create, must not be <code>null</code>
     * @param successIfExists  when this is true, do not throw an exception if the directory exists already
     * @throws NullPointerException if the directory is <code>null</code>
     * @throws IOException if the directory cannot be created
     */
    public static void forceMkdir(File directory, boolean successIfExists) throws IOException {
        if (directory.exists() && !successIfExists) {
            if (directory.isFile()) {
                String message = "File "
                        + directory
                        + " exists and is "
                        + "not a directory. Unable to create directory.";
                throw new IOException(message);
            }
        } else if(!directory.exists()){
            if (!directory.mkdirs()) {
                String message =
                    "Unable to create directory " + directory;
                throw new IOException(message);
            }
        }
    }

    /**
     * Set file permissions to 755.
     *
     * @see #setPermissions(String, String)
     */
    public static void setPermissions(File file) {
        setPermissions(file.getAbsolutePath(), "755");
    }

    /**
     * Set file permissions.
     *
     * @see #setPermissions(String, String)
     */
    public static void setPermissions(File file, String permissions) {
        setPermissions(file.getAbsolutePath(), permissions);
    }

    /**
     * Set file permissions to 755.
     *
     * @see #setPermissions(String, String)
     */
    public static void setPermissions(String fileName) {
        setPermissions(fileName, "755");
    }

    /**
     * Set file permissions.
     *
     * @param fileName file whose permissions will be set/modified
     * @param permissions in format of "xxx", each character is an octal number (0-7)
     * representing the bit pattern for the new mode bits.
     */
    public static void setPermissions(String fileName, String permissions) {
        if (permissions == null || permissions.length() != 3) {
            return;
        }

        int u = Character.digit(permissions.charAt(0), 10);
        int g = Character.digit(permissions.charAt(1), 10);
        int o = Character.digit(permissions.charAt(2), 10);

        int mode = 0;
        switch (u) {
            case 6:
                mode = S_IRUSR | S_IWUSR;
                break;
            case 5:
                mode = S_IRUSR | S_IXUSR;
                break;
            case 3 :
                mode = S_IWUSR | S_IXUSR;
                break;
            case 7 :
                mode = S_IRWXU;
                break;
            case 4 :
                mode = S_IRUSR;
                break;
            case 2 :
                mode = S_IWUSR;
                break;
            case 1 :
                mode = S_IXUSR;
                break;
            default:
                return;
        }

        switch (g) {
            case 6:
                mode = mode | S_IRGRP | S_IWGRP;
                break;
            case 5:
                mode = mode | S_IRGRP | S_IXGRP;
                break;
            case 3 :
                mode = mode | S_IWGRP | S_IXGRP;
                break;
            case 7 :
                mode = mode | S_IRWXG;
                break;
            case 4 :
                mode = mode | S_IRGRP;
                break;
            case 2 :
                mode = mode | S_IWGRP;
                break;
            case 1 :
                mode = mode | S_IXGRP;
                break;
            default:
                return;
        }

        switch (o) {
            case 6:
                mode = mode | S_IROTH | S_IWOTH;
                break;
            case 5:
                mode = mode | S_IROTH | S_IXOTH;
                break;
            case 3 :
                mode = mode | S_IWOTH | S_IXOTH;
                break;
            case 7 :
                mode = mode | S_IRWXO;
                break;
            case 4 :
                mode = mode | S_IROTH;
                break;
            case 2 :
                mode = mode | S_IWOTH;
                break;
            case 1 :
                mode = mode | S_IXOTH;
                break;
            default:
                return;
        }

        setPermissions(fileName, mode, -1, -1);
        // Woody Guo @ 2012/08/27:
        // Use super.setPermissions instead of running the shell command to set file mode bits.
        /*
         * if (file.exists()) {
         *     String[] cmds = new String[]
         *             {"sh", "-c", "chmod " + permission + " " + file.getAbsolutePath()};
         *     try {
         *         Process process = Runtime.getRuntime().exec(cmds);
         *         process.waitFor();
         *     } catch (Exception e) {
         *         e.printStackTrace();
         *     }
         * }
         */
    }

    /**
     * Extract a signle file from a zip archive
     *
     * @see #extractFromZip(File, String, String, String)
     */
    public static void extractFromZip(
            File zipFile, String fileName, String destFileName) throws ZipException, IOException {
        extractFromZip(zipFile, fileName, destFileName, null);
    }

    /**
     * Extract a signle file from a zip archive, and set it's permissions as specified
     *
     * @param zipFile is the zip archive
     * @param fileName is the name of the file to be extracted from the zip
     * @param destFileName is the path of the extracted file
     * @param permissions of the extracted file
     */
    public static void extractFromZip(
            File zipFile, String fileName, String destFileName, String permissions)
            throws ZipException, FileNotFoundException, IOException {
        ZipFile zfile = new ZipFile(zipFile);
        Enumeration zList = zfile.entries();
        ZipEntry ze = null;
        boolean found = false;

        while (zList.hasMoreElements()) {
            ze = (ZipEntry) zList.nextElement();
            if (ze.isDirectory() || !ze.getName().endsWith(fileName)) {
                continue;
            }
            found = true;
            OutputStream os = null;
            InputStream is = null;
            File f = new File(destFileName);
            try {
                os = new BufferedOutputStream(new FileOutputStream(f));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                connectIO(is, os);
                setPermissions(destFileName, permissions);
            } finally {
                if (null != is) close(is);
                if (null != os) close(os);
            }
        }
        zfile.close();
        if (!found) throw new FileNotFoundException();
    }

    /**
     * Extract all contents from a zip archive
     *
     * @see #unzip(File, String, String)
     */
    public static void unzip(File zipFile, String directory) throws ZipException, IOException {
        unzip(zipFile, directory, null);
    }

    /**
     * Extract all contents from a zip archive, and set permissions as specified
     *
     * @param zipFile the zip archive to be extracted
     * @param directory the destination directory to store files extracted
     * @param permissions of the files/dirs extracted
     *
     * @throws ZipException in case of failure zip operations
     * @throws IOException in case of failure file/io operations
     */
    public static void unzip(
            File zipFile, String directory, String permissions) throws ZipException, IOException {
        ZipFile zfile = new ZipFile(zipFile);
        Enumeration zList = zfile.entries();
        ZipEntry ze = null;

        if (false) android.util.Log.d("FileUtilities", zipFile.getAbsolutePath() + " -> " + directory);
        while (zList.hasMoreElements()) {
            ze = (ZipEntry) zList.nextElement();
            /*
             * if (ze.isDirectory()) {
             *     Log.d("upZipFile", "ze.getName() = "+ze.getName());
             *     String dirstr = folderPath + ze.getName();
             *     dirstr = new String(dirstr.getBytes("8859_1"), "GB2312");
             *     Log.d("upZipFile", "str = "+dirstr);
             *     File f=new File(dirstr);
             *     f.mkdir();
             *     continue;
             * }
             */
            if (false) android.util.Log.d("FileUtilities", ze.getName());
            if (ze.isDirectory()) {
                continue;
            }
            OutputStream os = null;
            InputStream is = null;
            File f = getRealFileName(directory, ze.getName());
            try {
                os = new BufferedOutputStream(new FileOutputStream(f));
                is = new BufferedInputStream(zfile.getInputStream(ze));
                connectIO(is, os);
                setPermissions(f, permissions);
            } finally {
                if (null != is) close(is);
                if (null != os) close(os);
            }
        }
        zfile.close();
    }

    private static File getRealFileName(String zipPath, String absFileName) {
        String[] dirs = absFileName.split("/", absFileName.length());

        File ret = new File(zipPath);
        if (dirs.length > 1) {
            for (int i = 0; i < dirs.length - 1; i++) {
                ret = new File(ret, dirs[i]);
            }
        }

        if (!ret.exists()) {
            ret.mkdirs();
        }

        ret = new File(ret, dirs[dirs.length - 1]);

        return ret;
    }

    /**
     * Close a closeable.
     */
    public static void close(Closeable stream) {
        try {
            stream.close();
        } catch (IOException e) {}
    }

    /**
     * @see #connectIO(InputStream, OutputStream, int)
     */
    public static int connectIO(InputStream in, OutputStream out) throws IOException {
        return connectIO(in, out, 4096);
    }

    /**
     * Connect an input and output stream together to copy all bytes from the
     * input into the output.
     * <p>
     * Does not close either stream in any case. The inputstream is left fully
     * exhausted on return.
     *
     * @param in
     * @param out
     * @param bufSize buffer size used during copy.
     *
     * @return number of bytes copied.
     */
    public static int connectIO(InputStream in, OutputStream out, int bufSize)
            throws IOException {
        int total = 0;
        int n;
        byte[] buf = new byte[bufSize];

        while ((n = in.read(buf)) >= 0) {
            out.write(buf, 0, n);
            total += n;
        }

        return total;
    }

    /**
     * Call {@link File#renameTo(File)}, but throw an IOException on failure.
     *
     * @throws IOException Thrown if the rename attempt fails.
     */
    public static void renameExplodeOnFail(File src, File dst) throws IOException {
        if (!src.renameTo(dst)) {
            throw new IOException("Unable to rename '" + src + "' to '" + dst + "'");
        }
    }
    
    public static void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (RuntimeException rethrown) {
                throw rethrown;
            } catch (Exception ignored) {
            }
        }
    }

}
