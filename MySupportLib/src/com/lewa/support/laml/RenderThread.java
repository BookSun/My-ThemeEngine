
package com.lewa.support.laml;

import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import java.util.ArrayList;

public class RenderThread extends Thread {
    private static final String LOG_TAG = "RenderThread";

    private static RenderThread sGlobalThread;

    private static Object sGlobalThreadLock = new Object();

    private long mNextUpdateInterval;

    private boolean mPaused = true;

    private ArrayList<RendererController> mRendererControllerList = new ArrayList<RendererController>();

    private Object mResumeSignal = new Object();

    private boolean mSignaled;

    private Object mSleepSignal = new Object();

    private boolean mStarted;

    private boolean mStop;

    public RenderThread() {
        super("LAML RenderThread");
    }

    public RenderThread(RendererController c) {
        super("LAML RenderThread");
        addRendererController(c);
    }

    private void doFinish() {
        if (mRendererControllerList.size() == 0)
            return;
        synchronized (mRendererControllerList) {
            for (RendererController c : mRendererControllerList) {
                c.finish();
            }
        }
    }

    private void doInit() {
        if (mRendererControllerList.size() == 0)
            return;
        long currentTime = SystemClock.elapsedRealtime();
        synchronized (mRendererControllerList) {
            for (RendererController c : mRendererControllerList) {
                c.setLastUpdateTime(currentTime);
                c.init();
                if (mPaused)
                    c.tick(currentTime);
                c.requestUpdate();
            }
        }
    }

    private void doPause() {
        if (mRendererControllerList.size() == 0)
            return;
        synchronized (mRendererControllerList) {
            for (RendererController c : mRendererControllerList) {
                c.pause();
            }
        }
    }

    private void doResume() {
        if (mRendererControllerList.size() == 0)
            return;
        synchronized (mRendererControllerList) {
            for (RendererController c : mRendererControllerList) {
                c.resume();
            }
        }
    }

    private boolean doUpdateFramerate(long time) {
        if (mRendererControllerList.size() == 0)
            return true;

        boolean allPaused = true;
        mNextUpdateInterval = Long.MAX_VALUE;
        synchronized (mRendererControllerList) {
            for (RendererController c : mRendererControllerList) {
                if (!c.isSelfPaused()) {
                    long l = c.updateFramerate(time);
                    if (l < mNextUpdateInterval) {
                        mNextUpdateInterval = l;
                        allPaused = false;
                    }
                }
            }
        }
        return allPaused;
    }

    public static RenderThread globalThread() {
        return globalThread(false);
    }

    public static RenderThread globalThread(boolean ensureStart) {
        if (sGlobalThread == null) {
            synchronized (sGlobalThreadLock) {
                if (sGlobalThread == null)
                    sGlobalThread = new RenderThread();
            }
        }
        if (ensureStart && !sGlobalThread.isStarted())
            try {
                sGlobalThread.start();
            } catch (IllegalThreadStateException e) {
            }
        return sGlobalThread;
    }

    public static void globalThreadStop() {
        if (sGlobalThread != null)
            synchronized (sGlobalThreadLock) {
                if (sGlobalThread != null)
                    sGlobalThread.setStop();
            }
    }

    private void sleepForFramerate(float framerate, long nextUpdateInterval) {
        long sleepTime = Math.min((long) (1000f / framerate), nextUpdateInterval);
        waitSleep(sleepTime);
        mSignaled = false;
    }

    private void waitSleep(long t) {
        if (!mSignaled && t > 0) {
            synchronized (mSleepSignal) {
                try {
                    if (mSignaled)
                        return;
                    mSleepSignal.wait(t);
                } catch (InterruptedException e) {
                    Log.e(LOG_TAG, e.toString());
                }
            }
        }
    }

    private void waiteForResume() {
        try {
            mResumeSignal.wait();
        } catch (InterruptedException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    public void addRendererController(RendererController c) {
        synchronized (mRendererControllerList) {
            mRendererControllerList.add(c);
            c.setRenderThread(this);
            setPaused(false);
        }
    }

    public boolean isStarted() {
        return mStarted;
    }

    public void removeRendererController(RendererController c) {
        synchronized (mRendererControllerList) {
            mRendererControllerList.remove(c);
            c.setRenderThread(null);
        }
    }

    public void run() {
        Log.i(LOG_TAG, "RenderThread started");
        doInit();
        mStarted = true;
        int pendingCounter = 0;
        while (true) {
            if (mStop || mPaused) {
                synchronized (mResumeSignal) {
                    if (mPaused) {
                        doPause();
                        Log.i(LOG_TAG, "RenderThread paused, waiting for signal");
                        waiteForResume();
                        Log.i(LOG_TAG, "RenderThread resumed");
                        doResume();
                    }
                    if (mStop) {
                        doFinish();
                        Log.i(LOG_TAG, "RenderThread stopped");
                        return;
                    }
                }
            }
            long currentTime = SystemClock.elapsedRealtime();
            if (doUpdateFramerate(currentTime)) {
                mPaused = true;
            } else {
                float maxFramerate = 0;
                synchronized (mRendererControllerList) {
                    int N = mRendererControllerList.size();
                    for (int i = 0; i < N; i++) {
                        try {
                            RendererController c = mRendererControllerList.get(i);
                            if (c.isSelfPaused())
                                continue;
                            if (!c.hasInited()) {
                                c.init();
                            }

                            boolean isFramerateDive = false;

                            float framerate = c.getFramerate();
                            if (framerate > maxFramerate) {
                                maxFramerate = framerate;
                            }
                            float curFramerate = c.getCurFramerate();
                            if (curFramerate != framerate) {
                                if (curFramerate > 1 && framerate < 1) {
                                    isFramerateDive = true;
                                }
                                c.setCurFramerate(framerate);
                                Log.d(LOG_TAG, "framerate changed: " + framerate + " at time: "
                                        + currentTime);

                                int frameTime = (int) (framerate == 0 ? 0 : (float) 1000
                                        / framerate);
                                c.setFrameTime(frameTime);
                            }

                            if (isFramerateDive
                                    || ((currentTime - c.getLastUpdateTime() >= (long) c
                                            .getFrameTime() || c.shouldUpdate() || c.hasMessage()))) {
                                MotionEvent event = c.getMessage();
                                if (event != null)
                                    c.onTouch(event);
                                c.tick(currentTime);
                                c.doRender();
                                c.setLastUpdateTime(currentTime);
                                pendingCounter = 0;
                            }
                            if (c.shouldUpdate() || c.hasMessage()) {
                                mNextUpdateInterval = 5;
                                if (c.pendingRender() && ++pendingCounter > 20) {
                                    Log.i(LOG_TAG, "detected waiting too long for pending render.");
                                    c.doneRender();
                                    pendingCounter = 0;
                                }
                            }
                        } catch (Exception e) {
                            Log.e(LOG_TAG, e.toString());
                        }
                    }
                }
                if (!mPaused) {
                    sleepForFramerate(maxFramerate, mNextUpdateInterval);
                }
            }
        }
    }

    public void setPaused(boolean pause) {
        if (mPaused == pause)
            return;
        synchronized (mResumeSignal) {
            if (!(mPaused = pause))
                mResumeSignal.notify();
        }
    }

    public void setPausedSafety(boolean pause) {
        if (!mSignaled)
            signal();
        setPaused(pause);
    }

    public void setStop() {
        mStop = true;
        signal();
        setPaused(false);
    }

    public void signal() {
        if (!mSignaled) {
            synchronized (mSleepSignal) {
                mSignaled = true;
                mSleepSignal.notify();
            }
        }
    }

    public static boolean isGlobal(RenderThread renderThread) {
        return sGlobalThread == null ? false : sGlobalThread == renderThread;
    }
}
