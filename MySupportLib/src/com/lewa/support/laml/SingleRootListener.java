
package com.lewa.support.laml;

import android.util.Log;
import android.view.MotionEvent;

import com.lewa.support.laml.RendererController.IRenderable;

public class SingleRootListener implements RendererController.Listener {
    private static final String LOG_TAG = "SingleRootListener";

    private IRenderable mRenderable;

    private ScreenElementRoot mRoot;

    public SingleRootListener(ScreenElementRoot root,
            IRenderable renderable) {
        if (renderable == null)
            throw new NullPointerException("renderable is null");
        mRenderable = renderable;
        setRoot(root);
    }

    public void doRender() {
        mRenderable.doRender();
    }

    public void finish() {
        mRoot.finish();
    }

    public void init() {
        mRoot.init();
    }

    public void onTouch(MotionEvent event) {
        try {
            mRoot.onTouch(event);
            event.recycle();
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    public void pause() {
        mRoot.pause();
    }

    public void resume() {
        mRoot.resume();
    }

    public void setRoot(ScreenElementRoot root) {
        if (root == null)
            throw new NullPointerException("root is null");
        mRoot = root;
    }

    public void tick(long currentTime) {
        mRoot.tick(currentTime);
    }

    public long updateFramerate(long time) {
        return mRoot.updateFramerate(time);
    }
}
