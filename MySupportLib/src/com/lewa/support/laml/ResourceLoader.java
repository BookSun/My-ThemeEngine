
package com.lewa.support.laml;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Rect;
import android.os.MemoryFile;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;

import com.lewa.support.laml.ResourceManager.BitmapInfo;
import com.lewa.support.laml.util.Utils;

public abstract class ResourceLoader {
    private static final String IMAGES_FOLDER_NAME = "images";

    private static final String LOG_TAG = "ResourceLoader";

    protected static final String MANIFEST_FILE_NAME = "manifest.xml";

    protected String mLanguageCountrySuffix;

    protected String mLanguageSuffix;

    protected String mManifestName = MANIFEST_FILE_NAME;

    private String getPathForLanguage(String src, String folder) {
        if (!TextUtils.isEmpty(mLanguageCountrySuffix)) {
            String path = folder + "_" + mLanguageCountrySuffix + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!TextUtils.isEmpty(mLanguageSuffix)) {
            String path = folder + "_" + mLanguageSuffix + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!TextUtils.isEmpty(folder)) {
            String path = folder + "/" + src;
            if (resourceExists(path))
                return path;
        }
        if (!resourceExists(src))
            return null;
        return src;
    }

    public BitmapInfo getBitmapInfo(String src, Options opts) {
        String path = getPathForLanguage(src, IMAGES_FOLDER_NAME);
        InputStream is = null;
        try {
            is = getInputStream(path);
            Rect padding = new Rect();
            Bitmap bm = BitmapFactory.decodeStream(is, padding, opts);
            if (bm == null)
                return null;
            return new BitmapInfo(bm, padding);
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString(), e);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }

        return null;
    }

    public MemoryFile getFile(String src) {
        final int COUNT = 4096;
        InputStream is = null;
        try {
            long[] length = new long[1];
            is = getInputStream(src, length);
            if (is == null)
                return null;
            MemoryFile mf = new MemoryFile(null, (int) length[0]);
            OutputStream os = mf.getOutputStream();
            int read;
            byte[] buffer = new byte[COUNT];
            while ((read = is.read(buffer)) > 0) {
                os.write(buffer, 0, read);
            }
            os.flush();
            os.close();
            return mf;
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString(), e);
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString(), e);
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    protected final InputStream getInputStream(String path) {
        return getInputStream(path, null);
    }

    protected abstract InputStream getInputStream(String path, long[] ids);

    public Element getManifestRoot() {
        String manifestName = null;
        if (!TextUtils.isEmpty(mLanguageCountrySuffix)) {
            manifestName = Utils.addFileNameSuffix(mManifestName, mLanguageCountrySuffix);
            if (!resourceExists(manifestName))
                manifestName = null;
        }
        if (manifestName == null && !TextUtils.isEmpty(mLanguageSuffix)) {
            manifestName = Utils.addFileNameSuffix(mManifestName, mLanguageSuffix);
            if (!resourceExists(manifestName))
                manifestName = null;
        }

        if (manifestName == null)
            manifestName = mManifestName;

        InputStream is = null;
        try {
            is = getInputStream(manifestName);
            if (is != null)
                return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is)
                        .getDocumentElement();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString());
        } finally {
            if (is != null)
                try {
                    is.close();
                } catch (IOException e) {
                }
        }
        return null;
    }

    protected abstract boolean resourceExists(String path);

    public ResourceLoader setLocal(Locale locale) {
        if (locale != null) {
            mLanguageSuffix = locale.getLanguage();
            mLanguageCountrySuffix = locale.toString();
            if (TextUtils.equals(mLanguageSuffix, mLanguageCountrySuffix))
                mLanguageSuffix = null;

        }
        return this;
    }
}
