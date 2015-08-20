
package com.lewa.support.util;

import android.content.Context;
import android.net.Uri;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipFile;

public class InputStreamLoader {
    ByteArrayInputStream mByteArrayInputStream;

    private Context mContext;

    private InputStream mInputStream;

    private String mPath;

    private Uri mUri;

    private ZipFile mZipFile;

    private String mZipPath;

    public InputStreamLoader(Context context, Uri uri) {
        if ("file".equals(uri.getScheme())) {
            mPath = uri.getPath();
        } else {
            mContext = context;
            mUri = uri;
        }
    }

    public InputStreamLoader(String path) {
        mPath = path;
    }

    public InputStreamLoader(String zipPath, String entry) {
        mZipPath = zipPath;
        mPath = entry;
    }

    public InputStreamLoader(byte[] data) {
        mByteArrayInputStream = new ByteArrayInputStream(data);
    }

    public void close() {
        try {
            if (mInputStream != null)
                mInputStream.close();

            if (mZipFile != null)
                mZipFile.close();
            return;
        } catch (IOException e) {
        }
    }

    public InputStream get() {
        close();
        try {
            if (mUri != null) {
                mInputStream = mContext.getContentResolver().openInputStream(mUri);
            } else if (mZipPath != null) {
                mZipFile = new ZipFile(mZipPath);
                mInputStream = mZipFile.getInputStream(mZipFile.getEntry(mPath));
            } else if (mPath != null) {
                mInputStream = new FileInputStream(mPath);
            } else if (mByteArrayInputStream != null) {
                mByteArrayInputStream.reset();
                mInputStream = mByteArrayInputStream;
            }
        } catch (Exception e) {
        }
        if (mInputStream != null && !(mInputStream instanceof ByteArrayInputStream))
            mInputStream = new BufferedInputStream(mInputStream, 0x4000);
        return mInputStream;
    }
}
