
package com.lewa.support.util;

import android.app.*;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.Context;
import android.content.Intent;
import android.content.pm.*;
import android.os.*;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseIntArray;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.lewa.support.util.Process.IS_SYSTEM;
import static com.lewa.support.util.Process.UID_SYSTEM;

public class AudioOutputHelper {

    private static final String DEFAULT_TEMP_FILE = "audio_flinger_%d_%d_%d.dump";

    private static final String TAG = "AudioOutputHelper";

    private static String collectSessions(BufferedReader reader, SparseIntArray sessions)
            throws NumberFormatException, IOException {
        String content;
        while ((content = reader.readLine()) != null) {
            Matcher matcher = DUMP_TAG.SESSIONS_CONTENT_FINDER.matcher(content);
            if (!matcher.find())
                break;
            int sessionId = Integer.valueOf(matcher.group(DUMP_TAG.SESSION_GRP_IDX));
            int pid = Integer.valueOf(matcher.group(DUMP_TAG.PID_GRP_IDX));
            sessions.put(sessionId, pid);
        }
        return content;
    }

    private static String collectTracks(BufferedReader reader, List<AudioOutputClient> clients,
            SparseIntArray sessions, boolean active) throws NumberFormatException, IOException {
        String content;
        while ((content = reader.readLine()) != null) {
            Matcher matcher = DUMP_TAG.TRACK_CONTENT_FINDER.matcher(content);
            if (!matcher.find())
                break;
            int sessionId = Integer.valueOf(matcher.group(DUMP_TAG.TRACK_SESSION_GRP_IDX));
            int proc = sessions.get(sessionId);
            if (proc != 0) {
                int streamType = Integer.valueOf(matcher.group(DUMP_TAG.TRACK_STREAM_TYPE_GRP_IDX))
                        .intValue();
                boolean flag = false;
                if (active) {
                    for (AudioOutputClient c : clients) {
                        if (c.mSessionId == sessionId) {
                            c.mActive = active;
                            flag = true;
                            break;
                        }
                    }
                }
                if (!flag)
                    clients.add(new AudioOutputClient(sessionId, proc, streamType, active));
            }
        }
        return content;
    }

//    public static List<RunningAppProcessInfo> getActiveClientProcessList(
//            List<RunningAppProcessInfo> procs, Context context, boolean addMainProc) {
//        ArrayList<RunningAppProcessInfo> actives = null;
//        if (procs != null) {
//            List<AudioOutputClient> clients = parseAudioFlingerDump(context);
//            if (clients != null) {
//                actives = new ArrayList<RunningAppProcessInfo>();
//                for (AudioOutputClient c : clients) {
//                    if (c.mActive) {
//                        int pid = c.mProcessId;
//                        for (RunningAppProcessInfo proc : procs) {
//                            if (proc.pid == pid && proc.uid != UID_SYSTEM/*should ignore system process*/) {
//                                actives.add(proc);
//                                break;
//                            }
//                        }
//                    }
//                }
//                if (addMainProc) {
//                    List<RunningAppProcessInfo> mainProc = getMainProcessNames(actives, procs);
//                    actives.addAll(mainProc);
//                    return actives;
//                }
//            }
//        }
//        return actives;
//    }

//    public static List<String> getActiveReceiverNameList(Context context) {
//        try {
//            Intent intent = new Intent(Intent.ACTION_MEDIA_BUTTON);
//            List<ResolveInfo> receivers = context.getPackageManager().queryBroadcastReceivers(intent, 0);
//            if (receivers == null || receivers.isEmpty())
//                return null;
//            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE); 
//            List<RunningAppProcessInfo> processes = getActiveClientProcessList(
//                    activityManager.getRunningAppProcesses(), context, true);
//            if (processes == null || processes.isEmpty())
//                return null;
//
//            ArrayList<String> result = new ArrayList<String>();
//            for (RunningAppProcessInfo p : processes) {
//                for (ResolveInfo info : receivers) {
//                    if (info.activityInfo == null
//                            || !p.processName.equals(info.activityInfo.processName)) {
//                        result.add(p.processName);
//                        break;
//                    }
//                }
//            }
//            return result;
//        } catch (Exception e) {
//            return null;
//        }
//    }

    private static List<RunningAppProcessInfo> getMainProcessNames(
            List<RunningAppProcessInfo> actives, List<RunningAppProcessInfo> procs) {
        ArrayList<RunningAppProcessInfo> mainProcs = new ArrayList<RunningAppProcessInfo>();
        for (RunningAppProcessInfo active : actives) {
            int colonIndex = active.processName.indexOf(":");
            if (colonIndex > 0) {
                String mainName = active.processName.substring(0, colonIndex);
                for (RunningAppProcessInfo info : procs) {
                    if (mainName.equals(info.processName)) {
                        mainProcs.add(info);
                        break;
                    }
                }
            }
        }
        return mainProcs;
    }

    public static boolean hasActiveReceivers(Context context) {
        if(!IS_SYSTEM && Build.VERSION.SDK_INT <19)
            return false;
        try {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE); 
            List<RunningAppProcessInfo> runningAppProcessInfos = activityManager.getRunningAppProcesses() ;
            List<RunningAppProcessInfo> processes = null ;
            if(BINDER_LEWAMUSIC) {
                processes = getLewaMusicRunningProcess(runningAppProcessInfos);
            } 
//            else {
//                 processes =getActiveClientProcessList(runningAppProcessInfos, context, false);
//            }
            return processes != null && !processes.isEmpty();
        } catch (Exception e) {
            Log.e(TAG, e.toString(), e);
        }
        return false;
    }
    //mdf by huzeyin for Bug63184
    private static final boolean BINDER_LEWAMUSIC = true ;

    private static final  String LEWA_PLAYER [] = {"com.lewa.player"};

    private static List<RunningAppProcessInfo>  getLewaMusicRunningProcess (List<RunningAppProcessInfo> procs){
        ArrayList<RunningAppProcessInfo> actives = new ArrayList<RunningAppProcessInfo>();
        for(RunningAppProcessInfo info :procs) {
            for(String packageName:LEWA_PLAYER){
                if(info.processName .equals(packageName)){
                    actives.add(info);
                }
            }
        }
        return actives ;
    }

//    public static List<AudioOutputClient> parseAudioFlingerDump(Context context) {
//        File dir = Process.myUid() == 1000 ? new File("/cache") : context.getCacheDir();
//        int pid = Process.myPid();
//        long tid = Thread.currentThread().getId();
//        int i = 0;
//        File dumpFile;
//        while ((dumpFile = new File(dir, String.format(DEFAULT_TEMP_FILE, pid, tid, i))).exists()) {
//            i++;
//        }
//        List<AudioOutputClient> result = parseAudioFlingerDumpInternal(dumpFile);
//        dumpFile.delete();
//        return result;
//    }

//    private static List<AudioOutputClient> parseAudioFlingerDumpInternal(File file) {
//        ArrayList<AudioOutputClient> clients = new ArrayList<AudioOutputClient>();
//        FileOutputStream os = null;
//        InputStream in = null;
//        try {
//            os = new FileOutputStream(file);
//            ServiceManager.getService("media.audio_flinger").dump(os.getFD(), null);
//            os.close();
//            in = new FileInputStream(file);
//            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
//            SparseIntArray sessions = new SparseIntArray();
//            String skipped;
//            while ((skipped = reader.readLine()) != null) {
//                if(TextUtils.isEmpty(skipped))
//                    continue;
//                
//                Matcher standbyMatcher = DUMP_TAG.STANDBY_FINDER.matcher(skipped);
//                if (standbyMatcher.matches()
//                        && Boolean.valueOf(standbyMatcher.group(DUMP_TAG.STANDBY_GRP_IDX)))
//                    return null;
//
//                if (DUMP_TAG.SESSIONS_HEAD_FINDER.matcher(skipped).matches())
//                    skipped = collectSessions(reader, sessions);
//
//                if (skipped == null)
//                    break;
//
//                Matcher trackMatcher = DUMP_TAG.TRACKS_FINDER.matcher(skipped);
//                if (trackMatcher.matches() && (skipped = reader.readLine()) != null)
//                    skipped = collectTracks(reader, clients, sessions, false);
//
//                if (skipped == null)
//                    break;
//
//                Matcher activeTrackMatcher = DUMP_TAG.ACTIVE_TRACKS_FINDER.matcher(skipped);
//                if (activeTrackMatcher.matches() && (skipped = reader.readLine()) != null)
//                    skipped = collectTracks(reader, clients, sessions, true);
//
//                if (skipped == null)
//                    break;
//            }
//        } catch (Exception e) {
//            Log.e(TAG, e.toString(), e);
//        } finally {
//            if (os != null)
//                try {
//                    os.close();
//                } catch (IOException ex) {
//                }
//            if (in != null)
//                try {
//                    in.close();
//                } catch (IOException ex) {
//                }
//        }
//        return clients;
//    }

    public static class AudioOutputClient {

        public boolean mActive;

        public final int mProcessId;

        public final int mSessionId;

        public final int mStreamType;

        public AudioOutputClient(int sessionId, int processId, int streamType) {
            this(sessionId, processId, streamType, false);
        }

        public AudioOutputClient(int sessionId, int processId, int streamType, boolean active) {
            mSessionId = sessionId;
            mProcessId = processId;
            mStreamType = streamType;
            mActive = active;
        }
    }

    private static class DUMP_TAG {

        public static final Pattern ACTIVE_TRACKS_FINDER = Pattern
                .compile("^Output thread 0x[\\w]+ active tracks");

        public static final int PID_GRP_IDX = 2;

        public static final Pattern SESSIONS_CONTENT_FINDER = Pattern
                .compile("^\\s+(\\d+)\\s+(\\d+)\\s+\\d+$");

        public static final Pattern SESSIONS_HEAD_FINDER = Pattern
                .compile("^ session pid (cnt|count)");

        public static final int SESSION_GRP_IDX = 1;

        public static final Pattern STANDBY_FINDER = Pattern.compile("^standby: (\\w+)");

        public static final int STANDBY_GRP_IDX = 1;

        public static final Pattern TRACKS_FINDER = Pattern
                .compile("^Output thread 0x[\\w]+ tracks");

        public static final Pattern TRACK_CONTENT_FINDER = Pattern
                .compile("^(\\s|F)+\\d+\\s+\\d+\\s+(\\d+)\\s+\\d+\\s+\\w+\\s+(\\d+)\\s.+");

        public static final int TRACK_SESSION_GRP_IDX = 3;

        public static final int TRACK_STREAM_TYPE_GRP_IDX = 2;
    }
}
