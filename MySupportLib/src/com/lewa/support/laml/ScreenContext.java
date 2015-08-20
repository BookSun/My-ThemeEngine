
package com.lewa.support.laml;

import com.lewa.support.laml.data.ContextVariables;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.elements.ScreenElementFactory;

import android.content.Context;
import android.os.Handler;

public class ScreenContext {

    public static final String LAML_PREFERENCES = "LamlPreferences";

    public final Context mContext;

    private RendererController mController;

    public final ScreenElementFactory mFactory;

    private final Handler mHandler;

    private ScreenContext mParent;

    public ScreenElementRoot mRoot;

    public final ResourceManager mResourceManager;

    public final Variables mVariables;
    
    public static final String[] DEBUG_NAMES = {"battery_low", "battery_charging", "battery_full"}; 
    public ContextVariables mContextVariables;

    public ScreenContext(Context context, ResourceLoader loader) {
        this(context, loader, new Handler());
    }

    public ScreenContext(Context context, ResourceLoader loader, Handler h) {
        this(context, new ResourceManager(loader), new ScreenElementFactory(), h);
    }

    public ScreenContext(Context context, ResourceLoader loader, ScreenElementFactory factory) {
        this(context, loader, factory, new Handler());
    }

    public ScreenContext(Context context, ResourceLoader loader, ScreenElementFactory factory,
            Handler h) {
        this(context, new ResourceManager(loader), factory, h);
    }

    public ScreenContext(Context context, ResourceManager resourceMgr) {
        this(context, resourceMgr, new ScreenElementFactory(), new Handler());
    }

    public ScreenContext(Context context, ResourceManager resourceMgr, Handler h) {
        this(context, resourceMgr, new ScreenElementFactory(), h);
    }

    public ScreenContext(Context context, ResourceManager resourceMgr, ScreenElementFactory factory) {
        this(context, resourceMgr, factory, new Handler());
    }

    public ScreenContext(Context context, ResourceManager resourceMgr,
            ScreenElementFactory factory, Handler h) {
        this(context, resourceMgr, factory, h, new Variables());
    }

    public ScreenContext(Context context, ResourceManager resourceMgr,
            ScreenElementFactory factory, Handler h, Variables v) {
        Context rawContext = context.getApplicationContext();
        if (rawContext == null)
            rawContext = context;
        mContext = rawContext;
        mResourceManager = resourceMgr;
        mFactory = factory;
        mHandler = h;
        mVariables = v;
        mContextVariables = new ContextVariables();
    }

    public FramerateTokenList.FramerateToken createToken(String name) {
        if (mController != null)
            return mController.createToken(name);
        if (mParent != null)
            return mParent.createToken(name);
        else
            return null;
    }

    public void doneRender() {
        if (mController != null)
            mController.doneRender();
        else if (mParent != null) {
            mParent.doneRender();
        }
    }

    public Handler getHandler() {
        return mHandler;
    }

    public RendererController getRenderController() {
        return mController;
    }

    public boolean isGlobalThread() {
        final RendererController oldRendererController=getRenderController();
        return oldRendererController!=null&&oldRendererController.mGlobal;
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        return mHandler.postDelayed(r, delayMillis);
    }

    public void requestUpdate() {
        if (mController != null)
            mController.requestUpdate();
        else if (mParent != null) {
            mParent.requestUpdate();
        }
    }

    public void setParentContext(ScreenContext parent) {
        mParent = parent;
    }

    public void setRenderController(RendererController controller) {
        mController = controller;
    }

    public boolean isDebugingName(String name) {
        if (DEBUG_NAMES != null && DEBUG_NAMES.length > 0) {
            for (String debugName : DEBUG_NAMES) {
                if (debugName.equals(name)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    public boolean shouldUpdate() {
        if (mController != null)
            return mController.shouldUpdate();
        if (mParent != null)
            return mParent.shouldUpdate();
        else
            return false;
    }
}
