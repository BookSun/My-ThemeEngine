
package com.lewa.support.laml.data;

import java.io.File;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.FilenameExtFilter;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.IndexedStringVariable;
import com.lewa.support.laml.util.TextFormatter;

import org.w3c.dom.Element;

import android.text.TextUtils;
import android.util.Log;

public class FileBinder extends VariableBinder {

    public static final String TAG_NAME = "FileBinder";

    private static final String LOG_TAG = TAG_NAME;

    private IndexedNumberVariable mCountVar;

    protected TextFormatter mDirFormatter;

    private String mFiles[];

    private String mFilters[];

    protected String mName;


    public FileBinder(Element node, ScreenElementRoot root) {
        super(root);
        load(node);
    }

    private void load(Element node) {
        if (node == null) {
            Log.e(LOG_TAG, "FileBinder node is null");
            return;
        }
        mName = node.getAttribute("name");
        String filter = node.getAttribute("filter").trim();
        mFilters = TextUtils.isEmpty(filter) ? null : filter.split(",");
        mDirFormatter = new TextFormatter(node.getAttribute("dir"), Expression.build(node
                .getAttribute("dirExp")));
        if (!TextUtils.isEmpty(mName))
            mCountVar = new IndexedNumberVariable(mName, "count", getContext().mVariables);
        loadVariables(node);
    }

    protected Variable onLoadVariable(Element node) {
        return  new Variable(node, getContext().mVariables);
    }

    private void updateVariables() {
        int count = mFiles == null ? 0 : mFiles.length;
        for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
            Variable v= (Variable)variable ;
            if (v.mIndex != null) {
                int index = (int) v.mIndex.evaluate(getContext().mVariables);
                v.mVar.set(count == 0 ? null : mFiles[index % count]);
            }
        }
    }

    protected void addVariable(Variable v) {
        mVariables.add(v);
    }

    public String getName() {
        return mName;
    }

    public void init() {
        super.init();
        refresh();
    }

    public void refresh() {
        super.refresh();
        File dir = new File(mDirFormatter.getText(getContext().mVariables));
        mFiles = mFilters == null ? dir.list() : dir.list(new FilenameExtFilter(mFilters));
        int count = mFiles == null ? 0 : mFiles.length;
        if (mCountVar != null)
            mCountVar.set(count);
        Log.i(LOG_TAG, "file count: " + count);
        updateVariables();
    }

    public void tick() {
        super.tick();
        updateVariables();
    }

    private static class Variable extends VariableBinder.Variable {

        public static final String TAG_NAME = "Variable";

        public Expression mIndex;

        public IndexedStringVariable mVar;

        public Variable(Element node, Variables var) {
            super(node, var);
            mVar = new IndexedStringVariable(mName, var);
            mIndex = Expression.build(node.getAttribute("index"));
            if (mIndex == null)
                Log.e(TAG_NAME, "fail to load file index expression");
        }
    }
}
