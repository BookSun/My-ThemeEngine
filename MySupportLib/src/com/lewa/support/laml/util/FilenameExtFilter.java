
package com.lewa.support.laml.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;

public class FilenameExtFilter implements FilenameFilter {

    private HashSet<String> mExts;

    public FilenameExtFilter(String exts[]) {
        mExts = new HashSet<String>();
        if (exts != null)
            mExts.addAll(Arrays.asList(exts));
    }

    public boolean accept(File dir, String filename) {
        File file = new File((new StringBuilder()).append(dir).append(File.separator)
                .append(filename).toString());
        if (file.isDirectory())
            return true;
        int dotPosition = filename.lastIndexOf('.');
        if (dotPosition != -1) {
            String ext = (String) filename.subSequence(dotPosition + 1, filename.length());
            return contains(ext.toLowerCase(Locale.US));
        } else {
            return false;
        }
    }

    public boolean contains(String ext) {
        return mExts.contains(ext.toLowerCase(Locale.US));
    }
}
