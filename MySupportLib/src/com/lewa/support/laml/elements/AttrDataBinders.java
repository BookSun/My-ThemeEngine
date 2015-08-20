package com.lewa.support.laml.elements;

import java.util.ArrayList;
import java.util.Iterator;

import com.lewa.support.laml.data.ContextVariables;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.Element;

import android.text.TextUtils;
import android.util.Log;

public class AttrDataBinders {

    private static final String       ATTR_BITMAP = "bitmap";
    private static final String       ATTR_NAME   = "name";
    private static final String       ATTR_SRC    = "src";
    private static final String       ATTR_SRCID  = "srcid";
    private static final String       ATTR_TEXT   = "text";
    private static final String       LOG_TAG     = "AttrDataBinders";
    public static final String        TAG         = "AttrDataBinders";

    private ArrayList<AttrDataBinder> mBinders    = new ArrayList<AttrDataBinder>();
    protected ContextVariables        mVars;

    public AttrDataBinders(Element node, ContextVariables variables){
       mVars = variables;
        Utils.traverseXmlElementChildren(node, "AttrDataBinder", new Utils.XmlTraverseListener() {

            public void onChild(Element node) {
                try {
                    mBinders.add(new AttrDataBinder(node, mVars));
                    return;
                } catch (IllegalArgumentException localIllegalArgumentException) {
                    Log.e(LOG_TAG, localIllegalArgumentException.toString());
                }
            }
        });
    }

    public void bind(ElementGroup nodeGroup) {
        Iterator<AttrDataBinder> localIterator = mBinders.iterator();
        while (localIterator.hasNext()) {
            localIterator.next().bind(nodeGroup);
        }
    }

    // <AttrDataBinder target="text1111" attr="text(srcid ,src ,name,bitmap)" data="name" >

    // <AttrDataBinder target="text1111" attr="text" data="name" >
    // <AttrDataBinder target="bitmap1111" attr="bitmap" data="data.png" >
    public static class AttrDataBinder {

        protected String           mAttr;
        protected Binder           mBinder;
        protected String           mData;
        protected String           mTarget;
        protected ContextVariables mVars;

        public AttrDataBinder(Element node, ContextVariables variables){
            mTarget = node.getAttribute("target");
            mAttr = node.getAttribute("attr");
            mData = node.getAttribute("data");
            mVars = variables;
            mBinder = createBinder(mAttr);
            if ((TextUtils.isEmpty(mTarget)) || (TextUtils.isEmpty(mAttr)) || (TextUtils.isEmpty(mData))
                || (mBinder == null)) throw new IllegalArgumentException("invalid AttrDataBinder");
        }

        private Binder createBinder(String paramString) {
            if (TextUtils.isEmpty(paramString)) return null;
            if (ATTR_TEXT.equals(paramString)) {
                return new TextBinder();
            } else if (ATTR_NAME.equals(paramString)) {
                return new NameBinder();
            } else if (ATTR_BITMAP.equals(paramString)) {
                return new BitmapBinder();
            } else if (ATTR_SRC.equals(paramString)) {
                return new SrcBinder();
            } else if (ATTR_SRCID.equals(paramString)) {
                return new SrcIdBinder();
            }
            return null;
        }

        public boolean bind(ElementGroup node) {
            try {
                ScreenElement element = node.findElement(mTarget);
                if (element != null) {
                    mBinder.bind(element);
                    return true;
                }
            } catch (Exception localException) {
                localException.printStackTrace();
            }
            return false;
        }

        private abstract class Binder {

            public abstract void bind(ScreenElement element);
        }

        private class BitmapBinder extends Binder {

            public void bind(ScreenElement element) {
                ((ImageScreenElement)element).setBitmap(mVars.getBmp(mData));
            }
        }

        private class NameBinder extends Binder {

            public void bind(ScreenElement element) {
                element.setName(mVars.getString(mData));
            }
        }

        private class SrcBinder extends Binder {

            public void bind(ScreenElement element) {
                ((AnimatedScreenElement)element).setSrc(mVars.getString(mData));
            }
        }

        private class SrcIdBinder extends Binder {

            public void bind(ScreenElement element) {
                ((AnimatedScreenElement)element).setSrcId((float)mVars.getDouble(mData).doubleValue());
            }
        }

        private class TextBinder extends Binder {

            public void bind(ScreenElement element) {
                ((TextScreenElement)element).setText(mVars.getString(mData));
            }
        }
    }
}
