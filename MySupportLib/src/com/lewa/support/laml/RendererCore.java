
package com.lewa.support.laml;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;

import com.lewa.support.laml.RendererController.IRenderable;
import com.lewa.support.laml.util.ZipResourceLoader;

public class RendererCore {
    private static final String LOG_TAG = "RendererCore";

    private boolean mCleaned;

    private SingleRootListener mListener;

    private MultipleRenderable mMultipleRenderable = new MultipleRenderable();

    private WeakReference<OnReleaseListener> mOnReleaseListener;

    private RendererController mRendererController;

    private ScreenElementRoot mRoot;

    private RenderThread mThread;

    private boolean mReleased;

    public RendererCore(ScreenElementRoot root) {
        attach(root);
    }

    public RendererCore(ScreenElementRoot root, RenderThread t) {
        attach(root, t);
    }

    private void attach(ScreenElementRoot root) {
        attach(root, RenderThread.globalThread(true));
    }

    private void attach(ScreenElementRoot root, RenderThread t) {
        mThread = t;
        mRoot = root;
        mListener = new SingleRootListener(mRoot, mMultipleRenderable);
        mRendererController = new RendererController(mListener);
        mRendererController.selfPause();
        mRoot.setRenderController(mRendererController);
        mThread.addRendererController(mRendererController);
    }

    public static RendererCore create(Context context, ResourceLoader loader) {
        return create(context, loader, new Handler());
    }

    public static RendererCore create(Context context, ResourceLoader loader, Handler h) {
        return create(context, loader, RenderThread.globalThread(true), h);
    }

    public static RendererCore create(Context context, ResourceLoader loader, RenderThread t) {
        return create(context, loader, t, new Handler());
    }

    public static RendererCore create(Context context, ResourceLoader loader, RenderThread t,
            Handler h) {
        LifecycleResourceManager rm = new LifecycleResourceManager(loader,
                LifecycleResourceManager.TIME_HOUR * 10, LifecycleResourceManager.TIME_HOUR);
        ScreenContext mElementContext = new ScreenContext(context, rm, h);
        ScreenElementRoot root = new ScreenElementRoot(mElementContext);
        root.setDefaultFramerate(0);
        return root.load() ? new RendererCore(root, t) : null;
    }

    public static RendererCore createFromZipFile(Context context, String path) {
        return createFromZipFile(context, path, new Handler());
    }

    public static RendererCore createFromZipFile(Context context, String path, Handler h) {
        return createFromZipFile(context, path, RenderThread.globalThread(true), h);
    }

    public static RendererCore createFromZipFile(Context context, String path, RenderThread t) {
        return createFromZipFile(context, path, t, new Handler());
    }

    public static RendererCore createFromZipFile(Context context, String path, RenderThread t,
            Handler h) {
        if (context != null && t != null) {
            if (path != null && new File(path).exists()) {
                ResourceLoader loader = new ZipResourceLoader(path).setLocal(context.getResources()
                        .getConfiguration().locale);
                return create(context, loader, t, h);
            }
            return null;
        }
        throw new NullPointerException();
    }

    public synchronized void addRenderable(IRenderable r) {
        if (!mCleaned) {
            mMultipleRenderable.add(r);
            Log.d(LOG_TAG, "add: " + r + " size:" + mMultipleRenderable.size());
            mRendererController.selfResume();
            mReleased = false;
        }
    }

    public void cleanUp() {
        mCleaned = true;
        Log.d(LOG_TAG, "cleanUp: " + toString());
        if (mRoot != null) {
            mRoot.setRenderController(null);
            mRoot = null;
        }

        if (mThread != null) {
            mThread.removeRendererController(mRendererController);
            mRendererController.finish();
            mRendererController = null;
            mThread = null;
        }
    }

    protected void finalize() throws Throwable {
        cleanUp();
        super.finalize();
    }

    public ScreenElementRoot getRoot() {
        return mRoot;
    }

    public synchronized void pauseRenderable(IRenderable r) {
        if (!mCleaned) {
            int active = mMultipleRenderable.pause(r);
            if (active == 0) {
                Log.d(LOG_TAG, "self pause: " + toString());
                mRendererController.selfPause();
            }
        }
    }

    public synchronized void removeRenderable(IRenderable r) {
        if (!mCleaned) {
            mMultipleRenderable.remove(r);
            Log.d(LOG_TAG, "remove: " + r + " size:" + mMultipleRenderable.size());
            if (mMultipleRenderable.size() == 0) {
                mRendererController.selfPause();
                if (!mReleased && mOnReleaseListener != null) {
                    OnReleaseListener listener = mOnReleaseListener.get();
                    if (listener != null)
                        listener.OnRendererCoreReleased(this);
                }
                mReleased = true;
            }
        }
    }

    public float getFramerate() {
        return mRendererController.getFramerate();
    }

    public void render(Canvas c) {
        if (!mCleaned && mThread.isStarted()) {
            mRoot.render(c);
        }
    }

    public synchronized void resumeRenderable(IRenderable r) {
        if (!mCleaned) {
            mMultipleRenderable.resume(r);
            Log.d(LOG_TAG, "self resume: " + toString());
            mRendererController.selfResume();
        }
    }

    public void setOnReleaseListener(OnReleaseListener l) {
        mOnReleaseListener = new WeakReference<OnReleaseListener>(l);
    }

    public static abstract interface OnReleaseListener {
        public abstract void OnRendererCoreReleased(RendererCore paramRendererCore);
    }
}
