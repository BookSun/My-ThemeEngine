
package com.lewa.support.laml.elements;

import java.util.ArrayList;

import android.content.*;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.RemoteException;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.lewa.support.laml.*;
import com.lewa.support.laml.data.VariableNames;
import com.lewa.support.laml.util.*;
import com.lewa.support.util.AudioOutputHelper;
import com.lewa.support.util.ImageUtils;
import com.lewa.support.util.InputStreamLoader;
import android.media.IAudioService;
import android.os.ServiceManager;

import org.w3c.dom.Element;

public class MusicControlScreenElement extends ElementGroup implements
        ButtonScreenElement.ButtonActionListener {
    private static final boolean SHOW_ALBUM_COVER_INIT = false;

    private static final String BUTTON_MUSIC_NEXT = "music_next";

    private static final String BUTTON_MUSIC_PAUSE = "music_pause";

    private static final String BUTTON_MUSIC_PLAY = "music_play";

    private static final String BUTTON_MUSIC_PREV = "music_prev";

    private static final int CHECK_STREAM_MUSIC_DELAY = 1000;

    private static final String ELE_MUSIC_ALBUM_COVER = "music_album_cover";

    private static final String ELE_MUSIC_DISPLAY = "music_display";

    private static final int FRAMERATE_PLAYING = 30;

    private static final String LOG_TAG = "MusicControlScreenElement";

    private static final int MAX_ALBUM_COVER_PIXEL_SIZE = 0x400000;

    private static final int MUSIC_NONE = 0;

    private static final int MUSIC_PLAY = 2;

    private static final int MUSIC_STOP = 1;

    public static final String TAG_NAME = "MusicControl";

    private boolean isPaused;

    private Bitmap mAlbumCoverBm;

    private String mAlbumName;

    private String mArtistName;

    private IndexedStringVariable mArtistVar;

    private AudioManager mAudioManager;

    private boolean mAutoShow;

    private ButtonScreenElement mButtonNext;

    private ButtonScreenElement mButtonPause;

    private ButtonScreenElement mButtonPlay;

    private ButtonScreenElement mButtonPrev;

    private Bitmap mDefaultAlbumCoverBm;

    private IntentFilter mFilter;

    private ImageScreenElement mImageAlbumCover;

    private boolean mIsOnlineSongBlocking;

    private IndexedNumberVariable mMusicStateVar;

    private int mMusicStatus;

    private boolean mRegistered;

    private SpectrumVisualizerScreenElement mSpectrumVisualizer;

    private TextScreenElement mTextDisplay;

    private IndexedStringVariable mTitleVar;

    public static final String EXTRA_IS_PLAYING = "playing";
 // Begin, added by yljiang@lewatek.com 2014-06-10
    private static final boolean DEBUG = true ;
    private static final String TAG ="MusicControlScreenElement" ;
    public static final String LYRIC_AFTER = "lyric_after";
    public static final String LYRIC_BEFORE = "lyric_before";
    public static final String LYRIC_CURRENT = "lyric_current";
    public static final String LYRIC_CURRENT_LINE_PROGRESS = "lyric_current_line_progress";
    public static final String LYRIC_LAST = "lyric_last";
    public static final String LYRIC_NEXT = "lyric_next";
    public static final String MUSIC_POSITION = "music_position";
    public static final String MUSIC_DURATION = "music_duration";

    private boolean mEnableLyric;
    private boolean mIsPlaying;
    private int mUpdateMusicProgressInterval;
    private long mPosition;
    private long mDuration;
    private long mLastUpdateTime;
    private String[] mLyricArray;
    private int[] mTimeArray;

    private int mCurrentLineNumber ;

    private StringBuilder mLyricAfterBuilder;
    private StringBuilder mLyricBeforeBuilder;
    private IndexedNumberVariable mDurationVar;
    private IndexedNumberVariable mPositionVar;
    private IndexedStringVariable mLyricBeforeVar;
    private IndexedNumberVariable mLyricCurrentLineProgressVar;
    private IndexedStringVariable mLyricCurrentVar;
    private IndexedStringVariable mLyricLastVar;
    private IndexedStringVariable mLyricNextVar;
    private IndexedStringVariable mLyricAfterVar;

    private void initLyric(Element ele, ScreenElementRoot root){
        mEnableLyric = Boolean.parseBoolean(ele.getAttribute("enableLyric"));
        mUpdateMusicProgressInterval = Utils.getAttrAsInt(ele, "updateLyricInterval", 1000);
        if (mEnableLyric){
            mDurationVar = new IndexedNumberVariable(this.mName, MUSIC_DURATION, getVariables());
            mPositionVar = new IndexedNumberVariable(this.mName, MUSIC_POSITION, getVariables());
            mLyricCurrentLineProgressVar = new IndexedNumberVariable(this.mName, LYRIC_CURRENT_LINE_PROGRESS, getVariables());
            mLyricCurrentVar = new IndexedStringVariable(this.mName, LYRIC_CURRENT, getVariables());
            mLyricBeforeVar = new IndexedStringVariable(this.mName, LYRIC_BEFORE, getVariables());
            mLyricAfterVar = new IndexedStringVariable(this.mName, LYRIC_AFTER, getVariables());
            mLyricLastVar = new IndexedStringVariable(this.mName, LYRIC_LAST, getVariables());
            mLyricNextVar = new IndexedStringVariable(this.mName, LYRIC_NEXT, getVariables());
        }
    }

    private void resetLyric() {
        if(mEnableLyric && mHasName){
            mLyricArray = null;
            mTimeArray = null;
            mLyricBeforeBuilder = null;
            mLyricAfterBuilder = null;
            mLyricBeforeVar.set(null);
            mLyricAfterVar.set(null);
            mLyricLastVar.set(null);
            mLyricNextVar.set(null);
            mLyricCurrentVar.set(null);
        }
    }

    private void updateLyric(Intent paramIntent) {
        int status = paramIntent.getIntExtra("lyric_status", 0);
        if (paramIntent.getIntExtra("lyric_status", 0) != 3 || mLyricArray == null) {
            resetLyric();
            ArrayList<CharSequence> localArrayList = paramIntent.getCharSequenceArrayListExtra("lyric");
            if (localArrayList!= null && !localArrayList.isEmpty()){
                mLyricArray = new String[localArrayList.size()];
                localArrayList.toArray(this.mLyricArray);
                mTimeArray = paramIntent.getIntArrayExtra("lyric_time");
            }
        }
    }

    private void requestLyric() {
        Intent localIntent = new Intent("com.miui.player.requestlyric");
        getContext().mContext.sendBroadcast(localIntent);
        localIntent.setAction("com.lewa.player.requestlyric");
        getContext().mContext.sendBroadcast(localIntent);
    }

    private void resetMusicProgress() {
        if(mEnableLyric && mHasName){
            mPositionVar.set(0);
            mDurationVar.set(0);
            mPosition = 0;
            mDuration = 0 ;
            mCurrentLineNumber = -1;
            mLyricCurrentLineProgressVar.set(0);
        }
    }

    private void updateMusicProgress(Intent intent) {
        resetMusicProgress();
        long duration = intent.getLongExtra("duration", -1);
        long position = intent.getLongExtra("position", -1);
        long time_stamp = intent.getLongExtra("time_stamp", -1);
        if(duration >= 0 && position >= 0 && time_stamp >= 0){
            mDuration = duration ;
            mDurationVar.set(duration);
            mPosition = position + System.currentTimeMillis() - time_stamp ;
            mPosition = Math.min(mDuration, mPosition);
            mPositionVar.set(mPosition);
            mLastUpdateTime = System.currentTimeMillis();
        }
        if(mLyricArray != null && mLyricArray.length > 0 && mTimeArray!= null && mTimeArray.length > 0){
            mCurrentLineNumber = -1;
            mLyricBeforeBuilder = new StringBuilder();
            mLyricAfterBuilder = new StringBuilder();
            int i = 0;
            for(;i < mLyricArray.length;i++){
                if(mTimeArray[i] > mPosition){
                    if(mCurrentLineNumber == -1){
                        mCurrentLineNumber = i - 1;
                    }
                    mLyricAfterBuilder.append(mLyricArray[i] + "\n");
                } else {
                   mLyricBeforeBuilder.append(mLyricArray[i] + "\n");
                   if(mTimeArray[i] == mPosition){
                       mCurrentLineNumber = i;
                   }
                }
                if(mCurrentLineNumber != -1 && mLyricBeforeBuilder != null && mLyricBeforeBuilder.length()>0){
                    int start =mLyricBeforeBuilder.length()-1- mLyricArray[mCurrentLineNumber].length() ;
                    mLyricBeforeBuilder.delete(Math.max(0, start), mLyricBeforeBuilder.length()-1);
                }
            }
            setLyricVar();
            setLyricCurrentLineProgressVar();
            getContext().getHandler().removeCallbacks(mUpdateMusicProgress);
            getContext().getHandler().postDelayed(mUpdateMusicProgress,mUpdateMusicProgressInterval);
        }
    }

    private void setLyricVar(){
        if(mLyricArray == null || mLyricArray.length == 0 || mLyricBeforeVar == null || mLyricAfterVar == null)
            return;
        if (mCurrentLineNumber >= 0){
            mLyricCurrentVar.set(mLyricArray[mCurrentLineNumber]);

            if (mCurrentLineNumber > 0){
                mLyricLastVar.set(mLyricArray[(mCurrentLineNumber-1)]);
            }
        }
        if(mCurrentLineNumber < mLyricArray.length-1 ){
            mLyricNextVar.set(mLyricArray[(mCurrentLineNumber+1)]);
        }
        mLyricBeforeVar.set(mLyricBeforeBuilder.toString());
        mLyricAfterVar.set(mLyricAfterBuilder.toString());
    }

    private void setLyricCurrentLineProgressVar(){
        if(mCurrentLineNumber >= 0 && mCurrentLineNumber < mLyricArray.length-1 ){
            double d = (double)(mPosition - mTimeArray[mCurrentLineNumber]) / (double)(mTimeArray[mCurrentLineNumber+1] - mTimeArray[mCurrentLineNumber]);
            d =  Math.min(1, Math.max(0, d));
            mLyricCurrentLineProgressVar.set(d);
        }else{
            mLyricCurrentLineProgressVar.set(0);
        }
    }

    private void needRequestLyric(){
        needRequestLyric(false);
    }

    private void needRequestLyric(boolean reset){
        if(mIsPlaying  && mEnableLyric && mHasName){
            if(mLyricArray == null || reset){
                requestLyric();
            }
        }
    }

    private Runnable mUpdateMusicProgress = new Runnable(){

        @Override
        public void run() {
            if(!mIsPlaying || mIsOnlineSongBlocking ||mCurrentLineNumber == -1 ||mLyricArray == null || mTimeArray == null || mLyricArray.length == 0 || mTimeArray.length == 0)
                return;
            long currentTime = System.currentTimeMillis();
            mPosition += currentTime - mLastUpdateTime ;
            mPosition = Math.max(0, Math.min(mDuration, mPosition)) ;
            mLastUpdateTime = currentTime ;
            mPositionVar.set(mPosition);
            if(mCurrentLineNumber >= 0 && mCurrentLineNumber < mTimeArray.length -1 && mPosition >= mTimeArray[mCurrentLineNumber+1]){
                String appendStr = mLyricArray[mCurrentLineNumber] + "\n" ;
                mLyricBeforeBuilder.append(appendStr);
                if(DEBUG){
                    Log.d(TAG, "append-----:"+appendStr);
                }
                mCurrentLineNumber++ ;
                String deleteStr = mLyricArray[mCurrentLineNumber]+ "\n" ;
                mLyricAfterBuilder.delete(0,deleteStr.length());
                setLyricVar();
                if(DEBUG){
                    Log.d(TAG, "delete---:"+ deleteStr);
                    Log.d(TAG, "mLyricAfterBuilder--delete---:"+mLyricAfterBuilder.toString());
                }
            }
            setLyricCurrentLineProgressVar();
            getContext().getHandler().postDelayed(this, mUpdateMusicProgressInterval);
        }
    };
//end
    private Runnable mCheckStreamMusicRunnable = new Runnable() {

        public void run() {
            updateMusic();
            updateSpectrumVisualizer();
            getContext().getHandler().postDelayed(this, CHECK_STREAM_MUSIC_DELAY);
        }
    };

    private Runnable mEnableSpectrumVisualizerRunnable = new Runnable() {

        public void run() {
            getContext().getHandler().removeCallbacks(mEnableSpectrumVisualizerRunnable);
            updateSpectrumVisualizer();
        }
    };

    private BroadcastReceiver mPlayerStatusListener = new BroadcastReceiver() {

        private void setTrackInfo(Intent intent) {
            String title = intent.getStringExtra("track");
            String artist = intent.getStringExtra("artist");
            if (!mIsPlaying) {
                title = null;
                artist = null;
            }
            if (mHasName) {
                if (mTitleVar == null)
                    mTitleVar = new IndexedStringVariable(mName, VariableNames.VAR_MUSIC_TITLE,
                            getVariables());
                mTitleVar.set(title);
                if (mArtistVar == null)
                    mArtistVar = new IndexedStringVariable(mName, VariableNames.VAR_MUSIC_ARTIST,
                            getVariables());
                mArtistVar.set(artist);
            }
            if (mTextDisplay != null) {
                if (TextUtils.isEmpty(title) && TextUtils.isEmpty(artist)) {
                    mTextDisplay.show(false);
                } else {
                    if (TextUtils.isEmpty(title))
                        mTextDisplay.setText(artist);
                    else if (TextUtils.isEmpty(artist))
                        mTextDisplay.setText(title);
                    else
                        mTextDisplay.setText(String.format("%s   %s", title, artist));
                    mTextDisplay.show(true);
                }
            }
        }

        public void onReceive(Context context, Intent intent) {
            try {
                String action = intent.getAction();
                mIsPlaying = intent.getBooleanExtra(EXTRA_IS_PLAYING, false);
                if(DEBUG)
                    Log.d(TAG, "mPlayerStatusListener--action:"+action+"  mIsPlaying:"+mIsPlaying);
                if (action.endsWith("metachanged")) {
                    String extra = intent.getStringExtra("other");
                    if(DEBUG)
                        Log.d(TAG, "metachanged  extra:"+extra);
                    if ("meta_changed_track".equals(extra)) {
                        setTrackInfo(intent);
                        requestAlbum(intent);
                        if(mIsPlaying  && mEnableLyric && mHasName ){
                            MusicControlScreenElement.this.getContext().getHandler().removeCallbacks(mUpdateMusicProgress);
                            resetLyric();
                            resetMusicProgress();
                        }
                    } else if ("meta_changed_album".equals(extra)) {
                        requestAlbum(intent, true);
                    }
//                    else {
//                        requestAlbum();
//                    }
                    if ("meta_changed_buffer".equals(extra)){
                        if(mEnableLyric && mHasName ){
                            updateMusicProgress(intent);
                        }
                    }
                } else if (action.endsWith("refreshprogress")) {
                    mIsOnlineSongBlocking = intent.getBooleanExtra("blocking", false);
                    if (!mIsOnlineSongBlocking && mEnableLyric && mHasName){
                        updateMusicProgress(intent);
                    }
                } else if (action.endsWith("playstatechanged")) {
                    if (mTextDisplay != null && !mTextDisplay.isVisible())
                        setTrackInfo(intent);
                    requestAlbum(intent);
                    needRequestLyric(true);
                } else if ("lockscreen.action.SONG_METADATA_UPDATED".equals(action) || action.endsWith("lockscreen")) {
                    setTrackInfo(intent);
                    setAlbumCover(intent);
                    needRequestLyric();
                }else if (action.endsWith("responselyric") && mEnableLyric && mHasName ){
                    updateLyric(intent);
                    updateMusicProgress(intent);
                    needRequestLyric();
                }
            } catch(Exception e){
                e.printStackTrace() ;
            }
        }
    };

    public MusicControlScreenElement(Element ele, ScreenElementRoot root)
            throws ScreenElementLoadException {
        super(ele, root);
        mButtonPrev = (ButtonScreenElement) findElement(BUTTON_MUSIC_PREV);
        mButtonNext = (ButtonScreenElement) findElement(BUTTON_MUSIC_NEXT);
        mButtonPlay = (ButtonScreenElement) findElement(BUTTON_MUSIC_PLAY);
        mButtonPause = (ButtonScreenElement) findElement(BUTTON_MUSIC_PAUSE);
        mTextDisplay = (TextScreenElement) findElement(ELE_MUSIC_DISPLAY);
        mImageAlbumCover = (ImageScreenElement) findElement(ELE_MUSIC_ALBUM_COVER);
        mSpectrumVisualizer = findSpectrumVisualizer(this);
        if (mButtonPrev != null && mButtonNext != null && mButtonPlay != null
                && mButtonPause != null) {
            setupButton(mButtonPrev);
            setupButton(mButtonNext);
            setupButton(mButtonPlay);
            setupButton(mButtonPause);
            mButtonPause.show(false);
            if (mImageAlbumCover != null) {
                String strDefAlbumCoverBmp = ele.getAttribute("defAlbumCover");
                if (!TextUtils.isEmpty(strDefAlbumCoverBmp))
                    mDefaultAlbumCoverBm = getContext().mResourceManager
                            .getBitmap(strDefAlbumCoverBmp);
                if (mDefaultAlbumCoverBm != null)
                    mDefaultAlbumCoverBm.setDensity(mRoot.getResourceDensity());
            }
            mAutoShow = Boolean.parseBoolean(ele.getAttribute("autoShow"));
            mAudioManager = (AudioManager) getContext().mContext
                    .getSystemService(Context.AUDIO_SERVICE);
            if (mHasName)
                mMusicStateVar = new IndexedNumberVariable(mName, VariableNames.MUSIC_STATE,
                        getVariables());
        } else {
            throw new ScreenElementLoadException("invalid music control");
        }
        initLyric(ele,root);
    }

    private SpectrumVisualizerScreenElement findSpectrumVisualizer(ElementGroup g) {
        for (ScreenElement se : g.getElements()) {
            if (se instanceof SpectrumVisualizerScreenElement) {
                return (SpectrumVisualizerScreenElement) se;
            } else if (se instanceof ElementGroup) {
                SpectrumVisualizerScreenElement ret = findSpectrumVisualizer((ElementGroup) se);
                if (ret != null)
                    return ret;
            }
        }
        return null;
    }

    private int getKeyCode(String name) {
        if (BUTTON_MUSIC_PREV.equals(name))
            return KeyEvent.KEYCODE_MEDIA_PREVIOUS;
        if (BUTTON_MUSIC_NEXT.equals(name))
            return KeyEvent.KEYCODE_MEDIA_NEXT;
        return BUTTON_MUSIC_PLAY.equals(name) || BUTTON_MUSIC_PAUSE.equals(name) ? KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                : KeyEvent.KEYCODE_UNKNOWN;
    }

    private void requestAlbum() {
        Intent requestIntent = new Intent("lockscreen.action.SONG_METADATA_REQUEST");
        getContext().mContext.sendBroadcast(requestIntent);
    }

    private void requestAlbum(Intent intent) {
        requestAlbum(intent, false);
    }

    private void requestAlbum(Intent intent, boolean forceRequest) {
        if (mImageAlbumCover != null) {
            if (mIsPlaying) {
                if(!forceRequest && mAlbumCoverBm != null){
                    mImageAlbumCover.setBitmap(mAlbumCoverBm);
                    return;
                }
                String albumName = intent.getStringExtra("album");
                String artistName = intent.getStringExtra("artist");
                if (forceRequest || !Utils.equals(albumName, mAlbumName)
                        || !Utils.equals(artistName, mArtistName) || mAlbumCoverBm == null) {
                    Uri uri = (Uri) intent.getParcelableExtra("album_uri");
                    String albumPath = intent.getStringExtra("album_path");
                    mAlbumCoverBm = null;
                    if (uri == null && albumPath == null) {
                        mImageAlbumCover.setBitmap(mDefaultAlbumCoverBm);
                    } else {
                        requestAlbum();
                    }
                }
            } else {
                mImageAlbumCover.setBitmap(mDefaultAlbumCoverBm);
            }
        }
    }

    private void safeRegisterReceiver() {
        if (mRegistered)
            return;
        mRegistered = true;
        try {
            getContext().mContext.registerReceiver(mPlayerStatusListener, mFilter, null,
                    getContext().getHandler());
            if (SHOW_ALBUM_COVER_INIT) {
                Intent requestIntent = new Intent("lockscreen.action.SONG_METADATA_REQUEST");
                getContext().mContext.sendBroadcast(requestIntent);
            }
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, e.toString());
        } catch (IllegalStateException e) {
            Log.e(LOG_TAG, e.toString());
        }
    }

    private void safeUnregisterReceiver() {
        if (!mRegistered)
            return;
        mRegistered = false;
        try {
            getContext().mContext.unregisterReceiver(mPlayerStatusListener);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "mMusicStatus: " + mMusicStatus + ", " + e.toString());
        }
    }

    private void sendMediaButtonBroadcast(int action, int keyCode) {
        long eventtime = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(eventtime, eventtime, action, keyCode, 0);
        Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        intent.putExtra(Intent.EXTRA_KEY_EVENT,
                KeyEvent.changeFlags(event, KeyEvent.FLAG_FROM_SYSTEM));
        intent.putExtra("forbid_double_click", true);
        getContext().mContext.sendOrderedBroadcast(intent, null);
    }
    
    private void dispatchMediaKey(int action, int keyCode) {
        KeyEvent key = new KeyEvent(action, keyCode);
        key.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        IAudioService audioService = android.media.IAudioService.Stub.asInterface(ServiceManager.checkService("audio"));
        if (audioService != null) {
            // NingYi For Launcher
            /*
            try {
                audioService.dispatchMediaKeyEvent(key);
                return;
            } catch (RemoteException e) {
                Log.e("MusicControlScreenElement",
                        (new StringBuilder()).append("dispatchMediaKeyEvent threw exception ").append(e).toString());
            }
            */
            return;
        } else {
            Log.w("MusicControlScreenElement", "Unable to find IAudioService for media key event");
            return;
        }
    }

    private void setAlbumCover(Intent intent) {
        boolean isPlaying = intent.getBooleanExtra("playing", false);
        if (mImageAlbumCover == null || !isPlaying)
            return;
        mAlbumName = intent.getStringExtra("album");
        mArtistName = intent.getStringExtra("artist");
        String albumPath = intent.getStringExtra("tmp_album_path");
        try {
            mAlbumCoverBm = ImageUtils.getBitmap(
                    albumPath != null ? new InputStreamLoader(albumPath) : new InputStreamLoader(
                            getContext().mContext, Uri.parse("content://com.lewa.player/"
                                    + mArtistName + ".jpg")), MAX_ALBUM_COVER_PIXEL_SIZE);
            if (mAlbumCoverBm != null)
                mAlbumCoverBm.setDensity(mRoot.getResourceDensity());
            mImageAlbumCover
                    .setBitmap(mAlbumCoverBm != null ? mAlbumCoverBm : mDefaultAlbumCoverBm);
            requestUpdate();
        } catch (Throwable e) {
            mAlbumCoverBm = null;
            Log.e(LOG_TAG, "failed to load album cover bitmap: " + e.toString());
        }
    }

    private void setupButton(ButtonScreenElement button) {
        if (button != null) {
            button.setListener(this);
            button.setParent(this);
        }
    }
    private void updateMusic() {
        boolean isMusicActive = mAudioManager.isMusicActive();
        boolean play =(isMusicActive && !mIsOnlineSongBlocking);
        mButtonPlay.show(!play);
        mButtonPause.show(play);
        if (isMusicActive) {
            if (mAlbumCoverBm == null)
                requestAlbum();
            else if (mImageAlbumCover.getBitmap() == null)
                mImageAlbumCover.setBitmap(mAlbumCoverBm);
        }
        if (mHasName)
            mMusicStateVar.set(isMusicActive ? VariableNames.MUSIC_STATE_PLAY
                    : VariableNames.MUSIC_STATE_STOP);
        requestFramerate(isMusicActive ? FRAMERATE_PLAYING : 0);
        mMusicStatus = play ? MUSIC_PLAY : MUSIC_STOP;
    }

    public void finish() {
        getContext().getHandler().removeCallbacks(mCheckStreamMusicRunnable);
        getContext().getHandler().removeCallbacks(mUpdateMusicProgress);
        safeUnregisterReceiver();
        if (mSpectrumVisualizer != null)
            mSpectrumVisualizer.enableUpdate(false);
    }

    public void init() {
        super.init();
        mButtonPause.show(false);
        mFilter = new IntentFilter();
        mFilter.addAction("lockscreen.action.SONG_METADATA_UPDATED");
        mFilter.addAction("com.miui.player.metachanged");
        mFilter.addAction("com.miui.player.refreshprogress");
        mFilter.addAction("com.miui.player.playstatechanged");
        mFilter.addAction("com.miui.player.responselyric");

        // add for Lewa Player
        mFilter.addAction("com.lewa.player.metachanged");
        mFilter.addAction("com.lewa.player.lockscreen");
        mFilter.addAction("com.lewa.player.UpdateAtristBG");
        mFilter.addAction("com.lewa.player.playstatechanged");
        mFilter.addAction("com.lewa.player.playStatus");
        mFilter.addAction("com.lewa.player.refreshprogress");
        mFilter.addAction("com.lewa.player.responselyric");
        isPaused = true;
        resume();
    }

    public boolean onButtonDoubleClick(String name) {
        return false;
    }

    public boolean onButtonDown(String name) {
        int keyCode = getKeyCode(name);
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false;
        } else {
//            final Handler handler = getContext().getHandler();
//            handler.removeCallbacks(mCheckStreamMusicRunnable);
//            handler.removeCallbacks(mEnableSpectrumVisualizerRunnable);
            dispatchMediaKey(KeyEvent.ACTION_DOWN, keyCode);
            return true;
        }
    }

    public boolean onButtonLongClick(String name) {
        return false;
    }

    public boolean onButtonUp(final String name) {
        int keyCode = getKeyCode(name);
        if (keyCode == KeyEvent.KEYCODE_UNKNOWN) {
            return false;
        } else {
        dispatchMediaKey(KeyEvent.ACTION_UP, keyCode);
        final Handler handler = getContext().getHandler();
        handler.removeCallbacks(mCheckStreamMusicRunnable);
        handler.removeCallbacks(mEnableSpectrumVisualizerRunnable);
            handler.post(new Runnable() {
                public void run() {
                    if (BUTTON_MUSIC_PAUSE.equals(name)) {
                        mButtonPause.show(false);
                        mButtonPlay.show(true);
                        mMusicStatus = MUSIC_STOP;
                        if (mHasName)
                            mMusicStateVar.set(VariableNames.MUSIC_STATE_STOP);
                        requestFramerate(0);
                    } else if (BUTTON_MUSIC_PLAY.equals(name)) {
                        mButtonPlay.show(false);
                        mButtonPause.show(true);
                        mMusicStatus = MUSIC_PLAY;
                        if (mHasName)
                            mMusicStateVar.set(VariableNames.MUSIC_STATE_PLAY);
                        requestFramerate(FRAMERATE_PLAYING);
                        // Begin, change by yljiang@lewatek.com 2014-06-10
//                        requestAlbum();
                        // End
                    }
                }
            });
            handler.postDelayed(mCheckStreamMusicRunnable, CHECK_STREAM_MUSIC_DELAY * 2);
            handler.postDelayed(mEnableSpectrumVisualizerRunnable, CHECK_STREAM_MUSIC_DELAY / 2);
            return true;
        }
    }

    protected void onVisibilityChange(boolean visible) {
        super.onVisibilityChange(visible);
        if (visible) {
            resume();
        } else {
            pause();
        }
    }

    public void pause() {
        if (!isPaused) {
            super.pause();
            isPaused = true;
            getContext().getHandler().removeCallbacks(mCheckStreamMusicRunnable);
            getContext().getHandler().removeCallbacks(mUpdateMusicProgress);
            mAlbumCoverBm = null;
            safeUnregisterReceiver();
            if (mSpectrumVisualizer != null) {
                mSpectrumVisualizer.enableUpdate(false);
            }
        }
    }

    public void resume() {
        if (isPaused) {
            super.resume();
            isPaused = false;
            boolean isMusicActive = AudioOutputHelper.hasActiveReceivers(getContext().mContext);
            if (isMusicActive) {
                mMusicStatus = MUSIC_PLAY;
            }
            if (mHasName) {
                mMusicStateVar.set(isMusicActive ? VariableNames.MUSIC_STATE_PLAY
                        : VariableNames.MUSIC_STATE_STOP);
            }
            boolean flagshow=(isVisible() || mAutoShow && isMusicActive && mAudioManager.isMusicActive());
            if (flagshow) {
                show(true);
            }
            if(mEnableLyric && mHasName)
                requestLyric();
        }
    }

    public void show(boolean show) {
        super.show(show);
        if (show) {
            getContext().getHandler().postDelayed(mCheckStreamMusicRunnable,
                    CHECK_STREAM_MUSIC_DELAY);
            safeRegisterReceiver();
            updateMusic();
        } else {
            mMusicStatus = MUSIC_NONE;
            getContext().getHandler().removeCallbacks(mCheckStreamMusicRunnable);
            getContext().getHandler().removeCallbacks(mUpdateMusicProgress);
            safeUnregisterReceiver();
            if (mSpectrumVisualizer != null)
                mSpectrumVisualizer.enableUpdate(false);
            requestFramerate(0);
        }
    }

    protected void updateSpectrumVisualizer() {
        boolean isMusicActive = mAudioManager.isMusicActive();
        if (mSpectrumVisualizer != null) {
            mSpectrumVisualizer.enableUpdate(isMusicActive && isVisible() && !isPaused);
        }
    }
}
