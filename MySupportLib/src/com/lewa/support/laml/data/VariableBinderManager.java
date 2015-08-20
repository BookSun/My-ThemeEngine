
package com.lewa.support.laml.data;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.util.TextFormatter;

import org.w3c.dom.*;

public class VariableBinderManager implements ContentProviderBinder.QueryCompleteListener {

    private static final String LOG_TAG = "VariableBinderManager";

    public static final String TAG_NAME = "VariableBinders";

    private ScreenElementRoot mRoot;

    private ArrayList<VariableBinder> mVariableBinders = new ArrayList<VariableBinder>();

    public VariableBinderManager(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        mRoot = root;
        if (node != null)
            load(node, root);
    }

    private static VariableBinder createBinder(Element ele, ScreenElementRoot root,
            VariableBinderManager m) throws ScreenElementLoadException {
        String tag = ele.getTagName();
        try {
            if (tag.equalsIgnoreCase(ContentProviderBinder.TAG_NAME))
                return new ContentProviderBinder(ele, root, m);
            else if (tag.equalsIgnoreCase(WebServiceBinder.TAG_NAME))
                return new WebServiceBinder(ele, root);
            else if (tag.equalsIgnoreCase(SensorBinder.TAG_NAME))
                return new SensorBinder(ele, root);
            else if (tag.equalsIgnoreCase(BroadcastBinder.TAG_NAME))
                return new BroadcastBinder(ele, root);
            else if (tag.equalsIgnoreCase(FileBinder.TAG_NAME))
                return new FileBinder(ele, root);
        } catch (ScreenElementLoadException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return null;
    }

    private void load(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            throw new NullPointerException("node is null");
        } else {
            loadBinders(node, root);
        }
    }

    private void loadBinders(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        NodeList children = node.getChildNodes();
        for (int i = 0, N = children.getLength(); i < N; i++) {
            Node n = children.item(i);
            if (n.getNodeType() == Node.ELEMENT_NODE){
                VariableBinder vb = createBinder((Element)n, root, this);
                if (vb != null)
                    mVariableBinders.add(vb);
            }
        }

    }

    public ContentProviderBinder.Builder addContentProviderBinder(String uri) {
        return addContentProviderBinder(new TextFormatter(uri));
    }

    public ContentProviderBinder.Builder addContentProviderBinder(String uriFormat, String uriParas) {
        return addContentProviderBinder(new TextFormatter(uriFormat, uriParas));
    }

    public ContentProviderBinder.Builder addContentProviderBinder(TextFormatter uri) {
        ContentProviderBinder binder = new ContentProviderBinder(mRoot, this);
        binder.mUriFormatter = uri;
        mVariableBinders.add(binder);
        return new ContentProviderBinder.Builder(binder);
    }

    public VariableBinder findBinder(String name) {
        for (VariableBinder binder : mVariableBinders) {
            if (TextUtils.equals(name, binder.getName()))
                return binder;
        }
        return null;
    }

    public void finish() {
        for (VariableBinder binder : mVariableBinders) {
            binder.finish();
        }
    }

    public void init() {
        for (VariableBinder binder : mVariableBinders) {
            binder.init();
        }
    }

    public void onQueryCompleted(String name) {
        if(name != null)
        for (VariableBinder binder : mVariableBinders) {
            if (binder instanceof ContentProviderBinder) {
                ContentProviderBinder cp = (ContentProviderBinder) binder;
                String dependency = cp.getDependency();
                if (!TextUtils.isEmpty(dependency) && dependency.equals(name))
                    cp.startQuery();
            }
        }
    }

    public void pause() {
        for (VariableBinder binder : mVariableBinders) {
            binder.pause();
        }
    }

    public void resume() {
        for (VariableBinder binder : mVariableBinders) {
            binder.resume();
        }
    }

    public void refresh() {
        for (VariableBinder binder : mVariableBinders) {
            binder.refresh();
        }
    }

    public void tick() {
        for (VariableBinder binder : mVariableBinders) {
            binder.tick();
        }
    }
}
