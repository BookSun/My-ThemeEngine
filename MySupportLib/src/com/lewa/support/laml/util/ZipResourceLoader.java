
package com.lewa.support.laml.util;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.lewa.support.laml.ResourceLoader;

public class ZipResourceLoader extends ResourceLoader {

    private static final String LOG_TAG = "ZipResourceLoader";

    private String mInnerPath;

    private String mResourcePath;

    private ZipFile mZipFile;

    public ZipResourceLoader(String zipPath) {
        this(zipPath, null, null);
    }

    public ZipResourceLoader(String zipPath, String innerPath) {
        this(zipPath, innerPath, null);
    }

    public ZipResourceLoader(String zipPath, String innerPath, String manifestName) {
        if (TextUtils.isEmpty(zipPath))
            throw new IllegalArgumentException("empty zip path");
        mResourcePath = zipPath;
        if (innerPath == null)
            innerPath = "";
        mInnerPath = innerPath;
        if (manifestName != null)
            mManifestName = manifestName;
        try {
            mZipFile = new ZipFile(mResourcePath);
        } catch (IOException e) {
            Log.e(LOG_TAG, "fail to init zip file: " + mResourcePath);
        }
    }

    protected void finalize() throws Throwable {
        if (mZipFile != null) {
            try {
                mZipFile.close();
            } catch (IOException ioexception) {
            }
            mZipFile = null;
        }
        super.finalize();
    }

    protected InputStream getInputStream(String path, long size[]) {
        if (mZipFile != null && path != null) {
            try {
                ZipEntry entry = mZipFile.getEntry(mInnerPath + path);
                if (entry != null) {
                    if (size != null)
                        size[0] = entry.getSize();
                    return mZipFile.getInputStream(entry);
                }
            } catch (IOException e) {
            }
        }
        return null;
    }

    protected boolean resourceExists(String path) {
        return mZipFile != null && path != null && mZipFile.getEntry(mInnerPath + path) != null;
    }

    public String toString() {
        return mResourcePath;
    }
}
