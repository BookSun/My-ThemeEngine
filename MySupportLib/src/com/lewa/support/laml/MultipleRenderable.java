
package com.lewa.support.laml;

import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import com.lewa.support.laml.RendererController.IRenderable;

public class MultipleRenderable implements IRenderable {
    private static final String LOG_TAG = "MultipleRenderable";

    private int mActiveCount;

    private ArrayList<RenderableInfo> mList = new ArrayList<RenderableInfo>();

    private RenderableInfo find(IRenderable r) {
        for (int i = 0, N = mList.size(); i < N; i++) {
            RenderableInfo ri = mList.get(i);
            if (ri.r.get() == r)
                return ri;
        }
        return null;
    }

    private int setPause(IRenderable r, boolean pause) {
        Log.d(LOG_TAG, "setPause: " + pause + " " + r);
        RenderableInfo ri = find(r);

        if (ri == null)
            return mActiveCount;

        if (ri.paused != pause) {
            ri.paused = pause;
            mActiveCount += pause ? -1 : 1;
        }
        return mActiveCount;
    }

    public synchronized void add(IRenderable r) {
        RenderableInfo ri = find(r);
        if (ri == null) {
            Log.d("MultipleRenderable", "add: " + r);
            mList.add(new RenderableInfo(r));
            mActiveCount++;
        }
    }

    public synchronized void doRender() {
        mActiveCount = 0;
        for (int i = mList.size() - 1; i >= 0; i--) {
            RenderableInfo ri = mList.get(i);
            IRenderable r = ri.r.get();
            if (r != null) {
                if (!ri.paused) {
                    r.doRender();
                    mActiveCount++;
                }
            } else {
                mList.remove(i);
            }
        }
    }

    public synchronized void remove(IRenderable r) {
        for (int i = mList.size() - 1; i >= 0; i--) {
            RenderableInfo ri = mList.get(i);
            IRenderable ren = ri.r.get();
            if (ren != null && r == ren && !ri.paused) {
                mActiveCount--;
                mList.remove(i);
                return;
            }
        }
    }

    public synchronized int pause(IRenderable r) {
        return setPause(r, true);
    }

    public synchronized int resume(IRenderable r) {
        return setPause(r, false);
    }

    public synchronized int size() {
        return mList.size();
    }

    private static class RenderableInfo {
        public boolean paused;

        public WeakReference<IRenderable> r;

        public RenderableInfo(IRenderable re) {
            r = new WeakReference<IRenderable>(re);
        }
    }
}
