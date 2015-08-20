
package com.lewa.support.laml;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.MemoryFile;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileDescriptor;
import java.io.IOException;
import java.util.HashMap;

public class SoundManager implements SoundPool.OnLoadCompleteListener {
    private final static String TAG = "SoundManager";

    private static final int MAX_STREAMS = 8;

    private AudioManager mAudioManager;

    private boolean mInitialized;

    private SparseArray<SoundOptions> mPendingSoundMap = new SparseArray<SoundOptions>();

    private SparseArray<SoundOptions> mPlayingSoundMap = new SparseArray<SoundOptions>();

    private ResourceManager mResourceManager;

    private SoundPool mSoundPool;

    private SoundOptions mPendingOption;

    private HashMap<String, Integer> mSoundPoolMap = new HashMap<String, Integer>();

    public SoundManager(Context context, ResourceManager rm) {
        mResourceManager = rm;
        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    private synchronized void init() {
        if (!mInitialized) {
            try {
                mInitialized = true;
                mSoundPool = new SoundPool(MAX_STREAMS, 1, 100);
                mSoundPool.setOnLoadCompleteListener(SoundManager.this);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private synchronized void playSoundImp(int sampleId, SoundOptions options) {
        if (mSoundPool != null) {
            try {
                if (!options.mKeepCur && mPlayingSoundMap.size() != 0) {
                    for (int i = 0, N = mPlayingSoundMap.size(); i < N; i++)
                        mSoundPool.stop(mPlayingSoundMap.keyAt(i));
                    mPlayingSoundMap.clear();
                }
                if (options.mStop) {
                    int streamID = isPlayingSound(options.mSound);
                    if(streamID != -1) {
                        mSoundPool.stop(streamID);
                        mPlayingSoundMap.remove(streamID);
                    }
                } else {
                    int streamID = mSoundPool.play(sampleId, options.mVolume, options.mVolume, 1,
                            options.mLoop ? -1 : 0, 1);
                    mPlayingSoundMap.put(streamID, options);
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    private int isPlayingSound(String sound) {
        for (int i = 0, N = mPlayingSoundMap.size(); i < N; i++)
            if (sound.equals(mPlayingSoundMap.valueAt(i)))
                return mPlayingSoundMap.keyAt(i);
        return -1;
    }

    public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        if (status == 0) {
            SoundOptions options = mPendingSoundMap.get(sampleId);
            if (options == null)
                options = mPendingOption;
            playSoundImp(sampleId, options);
        }
        mPendingSoundMap.remove(sampleId);
    }

    public synchronized void playSound(String sound, SoundOptions options) {
        MemoryFile memoryFile = null;
        if (!mInitialized) {
            init();
        }
        try {
            if (mSoundPool != null && mAudioManager != null
                    && mAudioManager.getMode() == AudioManager.MODE_NORMAL) {
                Integer soundId = mSoundPoolMap.get(sound);
                if (soundId != null) {
                    playSoundImp(soundId, options);
                }
                memoryFile = mResourceManager.getFile(sound);
                if (memoryFile == null) {
                    Log.e(TAG, "the sound does not exist: " + sound);
                } else {
                    try {
                        FileDescriptor fileDescriptor = memoryFile.getFileDescriptor();
                        mPendingOption = options;
                        soundId = mSoundPool.load(fileDescriptor, 0, memoryFile.length(), 1);
                        mSoundPoolMap.put(sound, soundId);
                        mPendingSoundMap.put(soundId, options);
                        memoryFile.close();
                    } catch (IOException e) {
                        Log.e(TAG, "fail to load sound. ");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        } finally {
            if (memoryFile != null)
                memoryFile.close();
        }
    }

    public synchronized void release() {
        if (mInitialized) {
            if (mSoundPool != null) {
                try {
                    for (int i = 0, N = mPlayingSoundMap.size(); i < N; i++)
                        if (mPlayingSoundMap.valueAt(i).mLoop)
                            mSoundPool.stop(mPlayingSoundMap.keyAt(i));
                    mPendingSoundMap.clear();
                    mPlayingSoundMap.clear();
                    mSoundPoolMap.clear();
                    mSoundPool.setOnLoadCompleteListener(null);
                    mSoundPool.release();
                    mSoundPool = null;
                } catch (Exception e) {
                    Log.e(TAG, e.getMessage());
                }
            }
            mInitialized = false;
        }
    }

    public static class SoundOptions {
        public String mSound;

        public boolean mKeepCur;

        public boolean mLoop;

        public boolean mStop;

        public float mVolume;

        public SoundOptions(String name, boolean keepCur, boolean loop, float volume, boolean pause) {
            this(keepCur, loop, volume);
            mStop = pause;
            mSound = name;
        }

        public SoundOptions(boolean keepCur, boolean loop, float volume) {
            mKeepCur = keepCur;
            mLoop = loop;
            if (volume < 0) {
                mVolume = 0;
            } else if (volume > 1) {
                mVolume = 1;
            } else {
                mVolume = volume;
            }

        }
    }
}
