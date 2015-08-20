package com.lewa.themechooser.newmechanism;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * FileContentProvider.java:
 *
 * @author yljiang@lewatek.com 2013-12-1
 */
public class FileContentProvider extends ContentProvider implements ContentProvider.PipeDataWriter<InputStream> {

    private static final String LWT = ".lwt";

    public static String makePath(String dirPath, String name) {
        return Globals.PROVIDER_PATH + "/" + dirPath + "/" + name;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String path = uri.getPath();
        if (NewMechanismUtils.isBlankStr(path))
            return null;
        if (path.startsWith("/" + Globals.OTHER) || path.equals("/" + Globals.FRAMEWORK) || path.equals("/" + Globals.LEWA)) {
            path = Globals.SD_THEME_RES + path;
        }
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        }
        return super.openFile(Uri.parse(path), mode);
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode) throws FileNotFoundException {
        String path = uri.getPath();
        int index = path.indexOf(LWT);
        if (index > 0) {
            int end = LWT.length() + index;
            String lwtPath = path.substring(0, end);
            String entry = path.substring(end + 1);
            InputStream inputStream = getInputStreamForZip(lwtPath, entry);
            if (inputStream != null) {
                ParcelFileDescriptor fileDescriptor = openPipeHelper(uri, null, null, inputStream, this);
                if (fileDescriptor != null)
                    return new AssetFileDescriptor(fileDescriptor, 0, AssetFileDescriptor.UNKNOWN_LENGTH);
            }
        }
        return super.openAssetFile(uri, mode);
    }

    public InputStream getInputStreamForZip(String filePath, String entryName) {
        try {
            ZipFile zip = new ZipFile(filePath);
            if (zip != null) {
                ZipEntry entry = zip.getEntry(entryName);
                if (entry != null) {
                    InputStream inputStream = zip.getInputStream(entry);
                    return inputStream;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        return 0;
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType, Bundle opts, InputStream args) {
        byte[] buffer = new byte[8192];
        int n;
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        try {
            while ((n = args.read(buffer)) >= 0) {
                fout.write(buffer, 0, n);
            }
        } catch (IOException e) {
            Log.i("InstallApk", "Failed transferring", e);
        } finally {
            try {
                args.close();
            } catch (IOException e) {
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }

    }
}
