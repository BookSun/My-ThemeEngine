
package com.lewa.support.laml;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.*;
import android.os.SystemProperties;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lewa.support.laml.SoundManager.SoundOptions;
import com.lewa.support.laml.data.DateTimeVariableUpdater;
import com.lewa.support.laml.data.VariableBinder;
import com.lewa.support.laml.data.VariableBinderManager;
import com.lewa.support.laml.data.VariableNames;
import com.lewa.support.laml.data.VariableUpdaterManager;
import com.lewa.support.laml.data.Variables;
import com.lewa.support.laml.elements.ButtonScreenElement;
import com.lewa.support.laml.elements.ElementGroup;
import com.lewa.support.laml.elements.FramerateController;
import com.lewa.support.laml.elements.ITicker;
import com.lewa.support.laml.elements.ScreenElement;
import com.lewa.support.laml.elements.ButtonScreenElement.ButtonAction;
import com.lewa.support.laml.util.ConfigFile;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.Task;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.laml.util.ConfigFile.Variable;

import android.os.SystemProperties;

import org.w3c.dom.*;

public class ScreenElementRoot extends ScreenElement implements InteractiveListener {

    public static final int DEFAULT_RES_DENSITY = 240;

    public static final int DEFAULT_SCREEN_WIDTH = 480;

    private static final boolean CALCULATE_FRAME_RATE = true;

    private float DEFAULT_FRAME_RATE = 30;

    private static final int MATRIX_WIDTH = 720;

    private static final String LOG_TAG = "ScreenElementRoot";

    private static final String ROOT_NAME = "__root";

    private static final String VARIABLE_VIEW_HEIGHT = "view_height";

    private static final String VARIABLE_VIEW_WIDTH = "view_width";

    private long mCheckPoint;

    private ConfigFile mConfig;

    private String mConfigPath;

    protected ScreenContext mContext;

    private int mDefaultResourceDensity;

    private int mRawDefaultResourceDensity;

    private int mDefaultScreenWidth;

    public ElementGroup mElementGroup;

    private WeakReference<OnExternCommandListener> mExternCommandListener;

    private ExternalCommandManager mExternalCommandManager;

    private boolean mFinished;

    protected float mFrameRate;

    private IndexedNumberVariable mFrameRateVar;

    private int mFrames;

    private boolean mKeepResource;

    private boolean mNeedDisallowInterceptTouchEvent;

    private IndexedNumberVariable mNeedDisallowInterceptTouchEventVar;

    private boolean mNoAutoScale;

    private boolean mShowFramerate = Constants.CONFIG_SHOW_FPS;

    private float mScale;

    private float mExtraScale;

    protected int mScreenHeight;

    protected int mScreenWidth;

    protected int mRawTargetDensity = Resources.getSystem().getDisplayMetrics().densityDpi;

    protected boolean mScaleByDensity;

    private SoundManager mSoundManager;

    private int mTargetDensity;

    private IndexedNumberVariable mTouchBeginTime;

    private IndexedNumberVariable mTouchBeginX;

    private IndexedNumberVariable mTouchBeginY;

    private IndexedNumberVariable mTouchX;

    private IndexedNumberVariable mTouchY;

    protected VariableBinderManager mVariableBinderManager;

    private VariableUpdaterManager mVariableUpdaterManager;

    private FramerateHelper mFramerateHelper;

    private float mWidth;

    private float mHeight;

    private float mRawWidth;

    private float mRawHeight;

    private ArrayList<FramerateController> mFramerateControllers = new ArrayList<FramerateController>();

    private ArrayList<ITicker> mPreTickers = new ArrayList<ITicker>();

    protected HashMap<String, String> mRawAttrs = new HashMap<String, String>();

    protected float mNormalFrameRate = DEFAULT_FRAME_RATE;

    private Matrix mExtraMatrix;

    private float mMatrixScale = 1;

    private OnExternCommandListener mSystemExternCommandListener;
 
    public boolean mShowDebugLayout;
    public final static int FANCY_SIZE = SystemProperties.getInt("fancyicon_size", 116);

    public ExecutorService mExecutorService, mListMsgsExecutorService;
    public ScreenElementRoot(ScreenContext c) {
        super(null, null);
        mContext = c;
        super.mRoot = this;
        mVariableUpdaterManager = new VariableUpdaterManager(c);
        mFramerateHelper = new FramerateHelper();
        mTouchX = new IndexedNumberVariable(VariableNames.VAR_TOUCH_X, getContext().mVariables);
        mTouchY = new IndexedNumberVariable(VariableNames.VAR_TOUCH_Y, getContext().mVariables);
        mTouchBeginX = new IndexedNumberVariable(VariableNames.VAR_TOUCH_BEGIN_X,
                getContext().mVariables);
        mTouchBeginY = new IndexedNumberVariable(VariableNames.VAR_TOUCH_BEGIN_Y,
                getContext().mVariables);
        mTouchBeginTime = new IndexedNumberVariable(VariableNames.VAR_TOUCH_BEGIN_TIME,
                getContext().mVariables);
        mNeedDisallowInterceptTouchEventVar = new IndexedNumberVariable(
                VariableNames.VAR_INTECEPT_SYS_TOUCH, getContext().mVariables);
        mSoundManager = new SoundManager(mContext.mContext, mContext.mResourceManager);
        mSystemExternCommandListener = new SystemCommandListener(this);
        createExecutor();
    }
    private void createExecutor(){
        //TODO :This open 10 Thread,for the later ,will be optimized.
        mExecutorService=Executors.newFixedThreadPool(10);
    }
    private void createListExecutor(){
        mListMsgsExecutorService=Executors.newFixedThreadPool(10);
    }

    private void loadConfig(String path) {
        if (path == null)
            return;
        mConfig = new ConfigFile();
        if (!mConfig.load(path)) {
            mConfig = null;
        } else {
            Variables vs = mContext.mVariables;
            for (Variable v : mConfig.getVariables()) {
                if (TextUtils.equals(v.type, "string"))
                    Utils.putVariableString(v.name, vs, v.value);
                else if (TextUtils.equals(v.type, "number"))
                    try {
                        Utils.putVariableNumber(v.name, vs, Double.parseDouble(v.value));
                    } catch (NumberFormatException numberformatexception) {
                    }
            }
            for (Task t : mConfig.getTasks()) {
                Utils.putVariableString(t.id, "name", vs, t.name);
                Utils.putVariableString(t.id, "package", vs, t.packageName);
                Utils.putVariableString(t.id, "class", vs, t.className);
            }
        }
    }

    private void loadRawAttrs(Element root) {
        NamedNodeMap nnm = root.getAttributes();
        for (int i = 0, j = nnm.getLength(); i < j; i++) {
            Node item = nnm.item(i);
            mRawAttrs.put(item.getNodeName(), item.getNodeValue());
        }
    }

    private void processUseVariableUpdater(Element root) {
        String updater = root.getAttribute("useVariableUpdater");
        if (TextUtils.isEmpty(updater)) {
            onAddVariableUpdater(mVariableUpdaterManager);
        } else {
            mVariableUpdaterManager.addFromTag(updater);
        }
    }

    private int resolveExtraResource(Element root, String attr, int target, int def) {
        String extraResources = root.getAttribute(attr);
        if (!TextUtils.isEmpty(extraResources)) {
            String[] resources = extraResources.split(",");
            int minDiff = Integer.MAX_VALUE;
            int closestSw = 0;
            for (String swStr : resources) {
                try {
                    int sw = Integer.parseInt(swStr.trim());
                    int i = Math.abs(target - sw);
                    int diff = i;
                    if (diff < minDiff) {
                        minDiff = diff;
                        closestSw = sw;
                        if (diff == 0) {
                            int defaultDiff = Math.abs(target - def);
                            return defaultDiff >= minDiff ? closestSw : 0;
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "extra resources format error.");
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "extra resources format error.");
                }
            }
        }
        return -1;
    }

    private float resolveExtraScale(Element root, String attr, int target) {
        String extraScales = root.getAttribute(attr);
        if (!TextUtils.isEmpty(extraScales)) {
            String[] as = extraScales.split(",");
            for (String s : as) {
                try {
                    String strNums[] = s.split(":");
                    int extra = Integer.parseInt(strNums[0].trim());
                    if (extra == target)
                        return Float.parseFloat(strNums[1].trim());
                } catch (IndexOutOfBoundsException e) {
                    Log.e(LOG_TAG, "extra scale format error.");
                } catch (NumberFormatException e) {
                    Log.e(LOG_TAG, "extra scale format error.");
                }
            }
        }
        return -1;
    }

    public void addElement(ScreenElement newElement) {
        if (mElementGroup != null && newElement != null)
            mElementGroup.addElement(newElement);
    }

    public void addFramerateController(FramerateController framerateController) {
        mFramerateControllers.add(framerateController);
    }

    public void addPreTicker(ITicker ticker) {
        mPreTickers.add(ticker);
    }

    public FramerateTokenList.FramerateToken createFramerateToken(String name) {
        return mContext.createToken(name);
    }

    public void doRender(Canvas c) {
        if (mFinished) {
            return;
        }
        try {
            if (mExtraMatrix != null) {
                c.setMatrix(mExtraMatrix);
            }
            if (mElementGroup != null) {
                mElementGroup.doRender(c);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.toString());
        } catch (OutOfMemoryError e) {
            Log.e(LOG_TAG, e.toString());
        }
        if (mShowFramerate) {
            mFramerateHelper.draw(c);
        }
        mFrames++;
        doneRender();
    }

    public void doneRender() {
        mContext.doneRender();
    }

    public VariableBinder findBinder(String name) {
        if (mVariableBinderManager != null) {
            return mVariableBinderManager.findBinder(name);
        }
        return null;
    }

    public ScreenElement findElement(String name) {
        if (!ROOT_NAME.equals(name)) {
            return mElementGroup == null ? null : mElementGroup.findElement(name);
        } else {
            return this;
        }
    }

    public Task findTask(String id) {
        return null;
    }

    public synchronized void finish() {
        if (!mFinished)
            try {
                if (mConfig != null) {
                    mConfig.save();
                }
                if (mElementGroup != null) {
                    mElementGroup.finish();
                }
                if (mVariableBinderManager != null) {
                    mVariableBinderManager.finish();
                }
                if (mExternalCommandManager != null) {
                    mExternalCommandManager.finish();
                }
                if (mVariableUpdaterManager != null) {
                    mVariableUpdaterManager.finish();
                }
                mSoundManager.release();
                mContext.mResourceManager.finish(mKeepResource);
                mFinished = true;
                mKeepResource = false;
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
    }

    public ScreenContext getContext() {
        return mContext;
    }

    public int getDefaultScreenWidth() {
        return mDefaultScreenWidth;
    }

    public ArrayList<ScreenElement> getElements() {
        return mElementGroup == null ? null : mElementGroup.getElements();
    }

    public float getHeight() {
        return mHeight;
    }

    public String getRawAttr(String name) {
        return mRawAttrs.get(name);
    }

    public int getResourceDensity() {
        return mDefaultResourceDensity;
    }

    public float getScale() {
        if (mScale == 0) {
            Log.w(LOG_TAG, "scale not initialized!");
            return 1;
        } else {
            return mScale;
        }
    }

    public float getMatrixScale() {
        return mMatrixScale;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getTargetDensity() {
        return mTargetDensity;
    }

    public float getWidth() {
        return mWidth;
    }

    public void haptic(int i) {
    }

    private void initExecutorsIfNeed(){
        if(null==mExecutorService||mExecutorService.isShutdown()){
            createExecutor();
        }
    }
    private void initListExecutorsIfNeed(){
        if(null==mListMsgsExecutorService||mListMsgsExecutorService.isShutdown()){
            createListExecutor();
        }
    }
    public void init() {
        mFinished = false;
        super.init();
        initExecutorsIfNeed();
        initListExecutorsIfNeed();
        mShowDebugLayout = SystemProperties.getBoolean("debug.layout", false);
        LanguageHelper.load(mContext.mContext.getResources().getConfiguration().locale,
                mContext.mResourceManager, mContext.mVariables);
        Utils.putVariableNumber(VariableNames.RAW_SCREEN_WIDTH, mContext.mVariables,
                Double.valueOf(mScreenWidth * mMatrixScale));
        Utils.putVariableNumber(VariableNames.RAW_SCREEN_HEIGHT, mContext.mVariables,
                Double.valueOf(mScreenHeight * mMatrixScale));
        Utils.putVariableNumber(VariableNames.SCREEN_WIDTH, mContext.mVariables,
                Double.valueOf((float) mScreenWidth / mScale));
        Utils.putVariableNumber(VariableNames.SCREEN_HEIGHT, mContext.mVariables,
                Double.valueOf((float) mScreenHeight / mScale));
        Utils.putVariableNumber(VariableNames.SCREEN_DENSITY, mContext.mVariables,
                Double.valueOf(mRawTargetDensity));
        loadConfig(mConfigPath);
        if (mVariableUpdaterManager != null) {
            mVariableUpdaterManager.init();
        }
        if (mVariableBinderManager != null) {
            mVariableBinderManager.init();
        }
        if (mExternalCommandManager != null) {
            mExternalCommandManager.init();
        }
        if (mElementGroup != null) {
            mElementGroup.init();
        }
        reset();
        if (CALCULATE_FRAME_RATE) {
            requestFramerate(mFrameRate);
        }
        initCamera(mContext.mContext);
    }
    
    private void initCamera(Context context) {
        Intent intent = new Intent("lewa.media.action.STILL_IMAGE_CAMERA");
        PackageManager pm = context.getPackageManager();
        ResolveInfo info = pm.resolveActivity(intent, 0);
        if (info != null && info.activityInfo != null && info.activityInfo.packageName.contains("lewa")
                && Build.VERSION.SDK_INT >= 17) {
            Utils.putVariableNumber(VariableNames.NATIVE_CAMERA, mContext.mVariables,
                    Double.valueOf(0));
        } else {
            Utils.putVariableNumber(VariableNames.NATIVE_CAMERA, mContext.mVariables,
                    Double.valueOf(1));
        }
    }

    public void setOnExternCommandListener(OnExternCommandListener l) {
        mExternCommandListener = l == null ? null : new WeakReference<OnExternCommandListener>(l);
    }

    public void issueExternCommand(String command, Double numPara, String strPara) {
        if(mSystemExternCommandListener != null){
            mSystemExternCommandListener.onCommand(command, numPara, strPara);
        }
        if (mExternCommandListener != null) {
            OnExternCommandListener l = mExternCommandListener.get();
            if (l != null) {
                l.onCommand(command, numPara, strPara);
                Log.d(LOG_TAG, "issueExternCommand: " + command + " " + numPara + " " + strPara);
            }
        }
    }

    public boolean load() {
        Element root = mContext.mResourceManager.getManifestRoot();
        if (root == null) {
            return false;
        }
        mName = root.getNodeName();
        setupScale(root);
        processUseVariableUpdater(root);
        
        loadRawAttrs(root);
        mFrameRate = mNormalFrameRate = Utils.getAttrAsFloat(root, "frameRate", DEFAULT_FRAME_RATE);
        String showFramerate = root.getAttribute("showFramerate");
        if (!TextUtils.isEmpty(showFramerate)) {
            mShowFramerate = Boolean.parseBoolean(showFramerate);
        }
        try {
            mElementGroup = new ElementGroup(root, this);
            Element binders = Utils.getChild(root, VariableBinderManager.TAG_NAME);
            mVariableBinderManager = new VariableBinderManager(binders, this);
            Element commands = Utils.getChild(root, ExternalCommandManager.TAG_NAME);
            if (commands != null) {
                mExternalCommandManager = new ExternalCommandManager(commands, this);
            }
        } catch (ScreenElementLoadException e) {
            Log.e(LOG_TAG, e.toString());
        }
        return onLoad(root);
    }

    @SuppressWarnings("deprecation")
    private void setupScale(Element root) {
        String scaleByDensity = root.getAttribute("scaleByDensity");
        if (!TextUtils.isEmpty(scaleByDensity)) {
            mScaleByDensity = Boolean.parseBoolean(scaleByDensity);
        }
        mDefaultScreenWidth = Utils.getAttrAsInt(root, "screenWidth", 0);
        mRawDefaultResourceDensity = Utils.getAttrAsInt(root, "resDensity", 0);
        mDefaultResourceDensity = ResourceManager.translateDensity(mRawDefaultResourceDensity);

        if (mDefaultScreenWidth == 0 && mDefaultResourceDensity == 0) {
            mDefaultScreenWidth = DEFAULT_SCREEN_WIDTH;
            mDefaultResourceDensity = DEFAULT_RES_DENSITY;
        } else if (mDefaultResourceDensity == 0) {
            mDefaultResourceDensity = (DEFAULT_RES_DENSITY * mDefaultScreenWidth) / DEFAULT_SCREEN_WIDTH;
        } else if (mDefaultScreenWidth == 0) {
            mDefaultScreenWidth = (DEFAULT_SCREEN_WIDTH * mDefaultResourceDensity) / DEFAULT_RES_DENSITY;
        }

        mContext.mResourceManager.setResourceDensity(mDefaultResourceDensity);
        // boolean isIconScale = "Icon".equals(mName) && mRawTargetDensity == DisplayMetrics.DENSITY_HIGH;
        boolean isLockScreen = "Lockscreen".equals(mName);
				
        Display display = ((WindowManager) mContext.mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int tmpW = display.getWidth();
        int tmpH = display.getHeight();
        int rotation = display.getRotation();
        if (!mNoAutoScale && isLockScreen && tmpW > MATRIX_WIDTH) {
            tmpH = (int) (tmpH / (mMatrixScale = (float) tmpW / MATRIX_WIDTH));
            tmpW = MATRIX_WIDTH;
        }
        boolean rotated = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270;
        if (rotated) {
            mScreenHeight = tmpW;
            mScreenWidth = tmpH;
        } else {
            mScreenHeight = tmpH;
            mScreenWidth = tmpW;
        }

        if (mNoAutoScale) {
            mScale = 1;
        } else {
            if (mTargetDensity != 0) {
                mScale = (float) mTargetDensity / mDefaultResourceDensity;
            } else {
                mScale = (float) mScreenWidth / mDefaultScreenWidth;
                mTargetDensity = Math.round((float) mDefaultResourceDensity * mScale);
                Log.i(LOG_TAG, "init target density: " + mTargetDensity);
                mContext.mResourceManager.setTargetDensity(mTargetDensity);
            }
        }

        int extraDensity = resolveExtraResource(root, "extraResourcesDensity", mRawTargetDensity,
                mRawDefaultResourceDensity);
        if (extraDensity > 0) {
            mContext.mResourceManager.setExtraResourceDensity(extraDensity);
            mExtraScale = resolveExtraScale(root, "extraScaleByDensity", extraDensity);
            mExtraScale *= (float) mRawTargetDensity / extraDensity;
        } else {
            int extraWidth = resolveExtraResource(root, "extraResourcesScreenWidth", mScreenWidth,
                    mDefaultScreenWidth);
            if (extraWidth > 0) {
                mContext.mResourceManager.setExtraResourceScreenWidth(extraWidth);
                mExtraScale = resolveExtraScale(root, "extraScaleByScreenWidth", extraWidth);
                mExtraScale *= (float) mScreenWidth / extraWidth;
            }
        }
        mRawWidth = Utils.getAttrAsInt(root, "width", 0);
        mRawHeight = Utils.getAttrAsInt(root, "height", 0);

        if (mRawWidth > 0) {
            Utils.putVariableNumber(VARIABLE_VIEW_WIDTH, mContext.mVariables, Double.valueOf(mRawWidth));
        }
        if (mRawHeight > 0) {
            Utils.putVariableNumber(VARIABLE_VIEW_HEIGHT, mContext.mVariables, Double.valueOf(mRawHeight));
        }
        
//        if (isIconScale) {
//            // auto scale smaller icon for hdpi in 84dp
//            mRawTargetDensity = DisplayMetrics.DENSITY_HIGH;
//            mDefaultResourceDensity = Math.round(DisplayMetrics.DENSITY_HIGH / 0.67f);
//            mScaleByDensity = true;
//        } else            
       if (isLockScreen && mMatrixScale != 1) {            
            mExtraMatrix = new Matrix();
            mExtraMatrix.setScale(mMatrixScale, mMatrixScale);
        }
        setScaleByDensity();
    }

    public void setScaleByDensity() {
        if (mDefaultScreenWidth != 0 && mDefaultResourceDensity != 0) {
            if (mScaleByDensity) {
                mTargetDensity = ResourceManager.translateDensity(mRawTargetDensity);
                mScale = (float) mTargetDensity / (float) mDefaultResourceDensity;
                if ("Icon".equals(mName)) {
                    mScale *= com.lewa.support.util.Constants.ICON_SCALE;
                }
            } else {
                mScale = (float) mScreenWidth / (float) mDefaultScreenWidth;
                if ("Icon".equals(mName)) {
                    // use mRawIconScale instead of mIconScale because 480 / 720 * 128 = 85.
                    mScale = com.lewa.support.util.Constants.CUSTOM_ICON_WIDTH / Float.parseFloat(FANCY_SIZE+"");
                }
                mTargetDensity = (int) ((float) mDefaultResourceDensity * mScale);
            }
            mContext.mResourceManager.setTargetDensity(mTargetDensity);
            if (mExtraScale > 0) {
                mScale = mExtraScale;
            }
            mWidth = Math.round(mRawWidth * mScale);
            mHeight = Math.round(mRawHeight * mScale);
        }
    }

    public boolean needDisallowInterceptTouchEvent() {
        return mNeedDisallowInterceptTouchEvent;
    }

    protected void onAddVariableUpdater(VariableUpdaterManager m) {
        m.add(new DateTimeVariableUpdater(m));
    }

    public void onButtonInteractive(ButtonScreenElement element, ButtonAction action) {
    }

    public void onCommand(String command) {
        if (mExternalCommandManager != null)
            try {
                mExternalCommandManager.onCommand(command);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.toString());
            }
    }

    protected boolean onLoad(Element root) {
        return true;
    }

    public boolean onTouch(MotionEvent event) {
        if (!mFinished && mElementGroup != null) {
            float x = descale(event.getX() / mMatrixScale);
            float y = descale(event.getY() / mMatrixScale);
            mTouchX.set(x);
            mTouchY.set(y);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_UP:
                    mNeedDisallowInterceptTouchEvent = false;
                    break;
                case MotionEvent.ACTION_DOWN:
                    mTouchBeginX.set(x);
                    mTouchBeginY.set(y);
                    mTouchBeginTime.set(System.currentTimeMillis());
                    mNeedDisallowInterceptTouchEvent = false;
                    break;
            }
            boolean ret = mElementGroup.onTouch(event);
            requestUpdate();
            return ret;
        }
        return false;
    }

    public void pause() {
        super.pause();
        if (mElementGroup != null)
            mElementGroup.pause();
        if (mVariableBinderManager != null)
            mVariableBinderManager.pause();
        if (mExternalCommandManager != null)
            mExternalCommandManager.pause();
        if (mVariableUpdaterManager != null)
            mVariableUpdaterManager.pause();
        mContext.mResourceManager.pause();
    }

    public void playSound(String sound) {
        playSound(sound, new SoundOptions(false, false, 1));
    }

    public void playSound(String sound, SoundManager.SoundOptions options) {
        if (!TextUtils.isEmpty(sound) && shouldPlaySound()) {
            mSoundManager.playSound(sound, options);
        }
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        return mContext.postDelayed(r, delayMillis);
    }

    public void removePreTicker(ITicker ticker) {
        mPreTickers.remove(ticker);
    }

    public void reset(long time) {
        super.reset(time);
        if (mElementGroup != null)
            mElementGroup.reset(time);
    }

    public void resume() {
        super.resume();
        mShowDebugLayout = SystemProperties.getBoolean("debug.layout", false);
        if (mElementGroup != null)
            mElementGroup.resume();
        if (mVariableBinderManager != null)
            mVariableBinderManager.resume();
        if (mExternalCommandManager != null)
            mExternalCommandManager.resume();
        if (mVariableUpdaterManager != null)
            mVariableUpdaterManager.resume();
        mContext.mResourceManager.resume();
    }

    public void saveVar(String name, Double value) {
        if (mConfig == null) {
            Log.w("ScreenElementRoot", "fail to saveVar, config file is null");
        } else {
            if (value == null) {
                mConfig.putNumber(name, "null");
            } else {
                mConfig.putNumber(name, value.doubleValue());
            }
        }
    }

    public void saveVar(String name, String value) {
        if (mConfig == null) {
            Log.w("ScreenElementRoot", "fail to saveVar, config file is null");
        } else {
            mConfig.putString(name, value);
        }
    }

    public void setConfig(String path) {
        mConfigPath = path;
    }

    public void setDefaultFramerate(float f) {
        DEFAULT_FRAME_RATE = f;
    }

    public final void setKeepResource(boolean b) {
        mKeepResource = b;
    }

    public void setRenderController(RendererController controller) {
        mContext.setRenderController(controller);
    }

    public void setTargetDensity(int targetDensity) {
        mTargetDensity = targetDensity;
        mContext.mResourceManager.setTargetDensity(targetDensity);
    }

    protected boolean shouldPlaySound() {
        return true;
    }

    public boolean shouldUpdate() {
        return mContext.shouldUpdate();
    }

    public void showFramerate(boolean show) {
        mShowFramerate = show;
    }

    public void tick(long currentTime) {
        if (!mFinished) {
            if (mVariableBinderManager != null)
                mVariableBinderManager.tick();
            mVariableUpdaterManager.tick(currentTime);
            for (ITicker ticker : mPreTickers)
                ticker.tick(currentTime);

            if (mElementGroup != null)
                mElementGroup.tick(currentTime);

            Double d = mNeedDisallowInterceptTouchEventVar.get();
            if (d != null)
                mNeedDisallowInterceptTouchEvent = d.doubleValue() > 0;
        }
    }

    public long updateFramerate(long time) {
        long nextUpdateInterval = Long.MAX_VALUE;
        for (FramerateController c : mFramerateControllers) {
            long l = c.updateFramerate(time);
            if (l < nextUpdateInterval) {
                nextUpdateInterval = l;
            }
        }

        if (mFrameRateVar == null) {
            mFrameRateVar = new IndexedNumberVariable(VariableNames.FRAME_RATE, mContext.mVariables);
            mCheckPoint = 0;
        }

        if (DEFAULT_FRAME_RATE > 0 && nextUpdateInterval == Long.MAX_VALUE) {
            nextUpdateInterval = (long) ((float) 1000 / DEFAULT_FRAME_RATE);
        }

        if (mCheckPoint == 0) {
            mCheckPoint = time;
        } else {
            long t = time - mCheckPoint;
            if (t >= 1000) {
                int r = (int) ((float) 1000 * mFrames / t);
                mFramerateHelper.set(r);
                mFrameRateVar.set(r);
                mFrames = 0;
                mCheckPoint = time;
            }
        }
        return nextUpdateInterval;
    }

    private static class FramerateHelper {

        private String mFramerateText;

        private TextPaint mPaint;

        private int mRealFrameRate;

        private int mShowingFramerate;

        private int mTextX;

        private int mTextY;

        public void draw(Canvas c) {
            if (mFramerateText == null || mShowingFramerate != mRealFrameRate) {
                mShowingFramerate = mRealFrameRate;
                mFramerateText = String.format("FPS %d", mShowingFramerate);
            }
            c.drawText(mFramerateText, mTextX, mTextY, mPaint);
        }

        public void set(int framerate) {
            mRealFrameRate = framerate;
        }

        public FramerateHelper() {
            this(Color.RED, 14, 10, 12);
        }

        public FramerateHelper(int color, int size, int x, int y) {
            mPaint = new TextPaint();
            mPaint.setColor(color);
            mPaint.setTextSize(size);
            mTextX = x;
            mTextY = y;
        }
    }

    public  void unlocked(Intent intent,String enterResName ,String exitResName){

    }

    public static interface OnExternCommandListener {

        public abstract void onCommand(String command, Double para1, String para2);
    }
}
