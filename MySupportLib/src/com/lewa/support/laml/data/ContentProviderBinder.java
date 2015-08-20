package com.lewa.support.laml.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import org.w3c.dom.Element;

import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteFullException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.provider.ContactsContract.PhoneLookup;
import android.text.TextUtils;
import android.util.Log;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.elements.ImageScreenElement;
import com.lewa.support.laml.elements.ListScreenElement;
import com.lewa.support.laml.elements.ScreenElement;
import com.lewa.support.laml.elements.ListScreenElement.ColumnInfo;
import com.lewa.support.laml.elements.ListScreenElement.DataWrapper;
import com.lewa.support.laml.elements.ListScreenElement.DeleteExtra;
import com.lewa.support.laml.util.IndexedNumberVariable;
import com.lewa.support.laml.util.TextFormatter;
import com.lewa.support.laml.util.Utils;
import com.lewa.support.util.LocationUtil;

public class ContentProviderBinder extends VariableBinder {

    private static final boolean  DBG              = false;

    private static final int      QUERY_TOKEN      = 100;

    public static final String    TAG_NAME         = "ContentProviderBinder";

    private static final String   LOG_TAG          = TAG_NAME;

    protected String              mArgs[];

    public ChangeObserver         mChangeObserver  = new ChangeObserver();

    protected String              mColumns[];

    protected String              mCountName;

    protected String              mNotifyUri;

    private IndexedNumberVariable mCountVar;

    private Cursor                mCursor;

    private Object                mCursorLock      = new Object();

    private DataSetObserver       mDataSetObserver = new MyDataSetObserver();

    private String                mDependency;

    private Handler               mHandler;

    private long                  mLastQueryTime;

    private String                mLastUri;

    protected String              mName;

    private boolean               mNeedsRequery;

    protected String              mOrder;

    private QueryCompleteListener mQueryCompletedListener;

    private QueryHandler          mQueryHandler;

    private int                   mUpdateInterval  = -1;

    private Runnable              mUpdater;

    protected TextFormatter       mUriFormatter;

    protected TextFormatter       mWhereFormatter;

    private List                  mList;

    public ContentProviderBinder(ScreenElementRoot root){
        this(root, null);
    }

    public ContentProviderBinder(ScreenElementRoot root, QueryCompleteListener l){
        super(root);
        Context context = getContext().mContext;
        mHandler = new Handler(context.getMainLooper());
        try {
            mQueryHandler = new QueryHandler(getContext().mContext);
        } catch (Exception e) {
        }
        mQueryCompletedListener = l;
    }

    public ContentProviderBinder(Element node, ScreenElementRoot root) throws ScreenElementLoadException{
        this(node, root, null);
    }

    public ContentProviderBinder(Element node, ScreenElementRoot root, QueryCompleteListener l)
                                                                                               throws ScreenElementLoadException{
        this(root, l);
        load(node);
    }

    private void checkUpdate() {
        if (mUpdateInterval > 0) {
            mHandler.removeCallbacks(mUpdater);
            long elapsedTime = System.currentTimeMillis() - mLastQueryTime;
            if (elapsedTime >= (long)(1000 * mUpdateInterval)) {
                startQuery();
                elapsedTime = 0;
            }
            mHandler.postDelayed(mUpdater, (long)(1000 * mUpdateInterval) - elapsedTime);
        }
    }

    private void closeCursor() {
        synchronized (mCursorLock) {
            if (mCursor != null) {
                if (mUpdateInterval == -1) registerObserver(mCursor, false);
                mCursor.close();
                mCursor = null;
            }
        }
    }

    private void load(Element node) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "ContentProviderBinder node is null");
            throw new ScreenElementLoadException("node is null");
        }
        mName = node.getAttribute("name");
        mDependency = node.getAttribute("dependency");
        mUriFormatter = getUriFTextFormatter(node);

        String tmp;
        mColumns = getColumns(node);
        mWhereFormatter = new TextFormatter(node.getAttribute("where"), node.getAttribute("whereFormat"),
                                            node.getAttribute("whereParas"),
                                            Expression.build(node.getAttribute("whereExp")),
                                            Expression.build(node.getAttribute("whereFormatExp")));

        tmp = node.getAttribute("args");
        mArgs = TextUtils.isEmpty(tmp) ? null : tmp.split(",");

        tmp = node.getAttribute("order");
        mOrder = TextUtils.isEmpty(tmp) ? null : tmp;

        tmp = node.getAttribute("countName");
        mCountName = TextUtils.isEmpty(tmp) ? null : tmp;

        tmp = node.getAttribute("notifyUri");
        mNotifyUri = TextUtils.isEmpty(tmp) ? null : tmp;

        if (mCountName != null) mCountVar = new IndexedNumberVariable(mCountName, getContext().mVariables);

        mUpdateInterval = Utils.getAttrAsInt(node, "updateInterval", -1);
        if (mUpdateInterval > 0) mUpdater = new Runnable() {

            public void run() {
                checkUpdate();
            }
        };
        loadVariables(node);
        loadVariableArrays(node);
        mList = new List(node, mRoot);
    }

    public static String[] getColumns(Element node) {
        String tmp = node.getAttribute("columns");
        return TextUtils.isEmpty(tmp) ? null : tmp.split(",");
    }

    public static TextFormatter getUriFTextFormatter(Element node) {
        Expression uriExp = Expression.build(node.getAttribute("uriExp"));
        Expression uriFormatExp = Expression.build(node.getAttribute("uriFormatExp"));
        TextFormatter mUriFormatter = new TextFormatter(node.getAttribute("uri"), node.getAttribute("uriFormat"),
                                                        node.getAttribute("uriParas"), uriExp, uriFormatExp);
        return mUriFormatter;

    }

    protected Variable onLoadVariable(Element node) {
        return new Variable(node, getContext().mVariables);
    }

    protected VariableArray onLoadVariableArray(Element node) {
        return new VariableArray(node, mRoot);
    }

    private void onQueryComplete(Cursor cursor) {
        if (!mFinished) {
            if (cursor != null) {
                synchronized (mCursorLock) {
                    try {
                        closeCursor();
                        mCursor = cursor;
                        if (mUpdateInterval == -1) registerObserver(mCursor, true);
                        new AsyncUpdateVariables(true,false).execute();
//                        updateVariables();
//                        if (mUpdateInterval != -1) {
//                            mCursor.close();
//                            mCursor = null;
//                        }
//                        mRoot.requestUpdate();
                    } catch (Exception e) {
                        Log.e(LOG_TAG, e.toString());
                    }
                }
            }
            if (mQueryCompletedListener != null) mQueryCompletedListener.onQueryCompleted(mName);
        } else {
            if (cursor != null) cursor.close();
        }
    }

    private void registerObserver(Cursor c, boolean reg) {
        if (c != null) {
            if (reg) {
                if (mNotifyUri != null) c.setNotificationUri(getContext().mContext.getContentResolver(),
                                                             Uri.parse(mNotifyUri));
                c.registerContentObserver(mChangeObserver);
                c.registerDataSetObserver(mDataSetObserver);
            } else {
                c.unregisterContentObserver(mChangeObserver);
                c.unregisterDataSetObserver(mDataSetObserver);
            }
        }
    }
    private final class AsyncUpdateVariables extends AsyncTask<Void, Void, Boolean>{
        private boolean isNeedCallback=false;
        private boolean isNeedUpdate=false;
        public AsyncUpdateVariables(boolean update,boolean callback){
           isNeedUpdate=update;
           isNeedCallback=callback;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            return updateVariables();
        }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(isNeedCallback){
                if (mQueryCompletedListener != null) {
                    mQueryCompletedListener.onQueryCompleted(mName);
                }
            }
            if(isNeedUpdate){
                if (mUpdateInterval != -1) {
                    mCursor.close();
                    mCursor = null;
                }
                mRoot.requestUpdate();
            }
        }
    }
    private boolean updateVariables() {
        synchronized (mCursorLock) {
            int count;
            if(DBG)Log.d(LOG_TAG, "start to updateVariables...");
            if (mCursor != null && mList != null) {
                final boolean listDataIsNull=mList.isListDataNull();
                if(!listDataIsNull){
                    if(DBG)Log.d(LOG_TAG, mName+"List Data is not null, start to reload data...");
                    mList.fill(mCursor);
                }else {
                    if(DBG)Log.d(LOG_TAG, mName+"List Data is  null, don't need to reload data...");
                }
            }
            if (mCursor == null || (count = mCursor.getCount()) == 0) {
                if (mCountVar != null) mCountVar.set(0);
                for (com.lewa.support.laml.data.VariableBinder.Variable v : mVariables) {
                    ((Variable)v).setNull(mRoot);
                }
            } else {
                if (DBG) Log.i(LOG_TAG, "query result count: " + count + " " + mLastUri);

                Iterator iterator = mVariableArrays.iterator();
                while (iterator.hasNext()) {
                    VariableArray variableArray = (VariableArray)iterator.next();
                    if (!variableArray.mBlocked) {
                        variableArray.fill(mCursor);
                    }
                }
                if (mCountVar != null) {
                    mCountVar.set(count);
                }
                for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
                    Variable v = (Variable)variable;
                    if (mCursor.moveToPosition(v.mRow)) {
                        String column = v.mColumn;
                        try {
                            int col = mCursor.getColumnIndexOrThrow(column);
                            if (DBG) Log.d(LOG_TAG,
                                           "updateVariables: "
                                                   + String.format(Locale.US,
                                                                   "name:%s type:%s row:%d column:%s value:%s",
                                                                   v.mName, v.mType, v.mRow, v.mColumn,
                                                                   mCursor.getString(col)));
                            switch (v.mType) {
                                case Variable.STRING:
                                    v.mStringVar.set(mCursor.getString(col));
                                    break;
                                case Variable.INT:
                                    v.mNumberVar.set(mCursor.getInt(col));
                                    break;
                                case Variable.LONG:
                                    v.mNumberVar.set(mCursor.getLong(col));
                                    break;
                                case Variable.FLOAT:
                                    v.mNumberVar.set(mCursor.getFloat(col));
                                    break;
                                case Variable.DOUBLE:
                                    v.mNumberVar.set(mCursor.getDouble(col));
                                    break;
                                case Variable.BLOB_BITMAP:
                                    byte[] valueBytes = mCursor.getBlob(col);
                                    ImageScreenElement image = v.getImageElement(mRoot);
                                    Bitmap bitmap = BitmapFactory.decodeByteArray(valueBytes, 0, valueBytes.length);
                                    image.setBitmap(bitmap);
                                default:
                                    Log.w(LOG_TAG, "invalide type" + v.mTypeStr);
                                    v.setNull(mRoot);
                                    break;
                            }
                        } catch (OutOfMemoryError e) {
                            Log.e(LOG_TAG, "image blob column decode error: " + column);
                        } catch (NumberFormatException e) {
                            Log.e(LOG_TAG, "column does not exist: " + column);
                        } catch (IllegalArgumentException e) {
                            Log.w(LOG_TAG, "failed to get value from cursor");
                        }
                    }
                }
            }
            return true;
        }
    }

    protected void addVariable(Variable v) {
        mVariables.add(v);
    }

    public void createCountVar() {
        mCountVar = mCountName == null ? null : new IndexedNumberVariable(mCountName, getContext().mVariables);
    }

    public void finish() {
        closeCursor();
        mHandler.removeCallbacks(mUpdater);
        setBlockedColumns(null);
        super.finish();
    }

    public final void setBlockedColumns(String[] strings) {
        HashSet<String> hashSet = null;
        if (strings != null && strings.length > 0) {
            hashSet = new HashSet<String>();
            for (String str : strings) {
                hashSet.add(str);
            }
        }
        Iterator iteratorVar = mVariables.iterator();
        while (iteratorVar.hasNext()) {
            Variable variable = (Variable)iteratorVar.next();
            variable.mBlocked = (hashSet == null ? false : hashSet.contains(variable.mColumn));
        }

        Iterator iteratorArray = mVariableArrays.iterator();
        while (iteratorArray.hasNext()) {
            VariableArray array = (VariableArray)iteratorArray.next();
            array.mBlocked = (hashSet == null ? false : hashSet.contains(array.mColumn));
        }
    }

    public String getDependency() {
        return mDependency;
    }

    public String getName() {
        return mName;
    }

    public void init() {
        super.init();
	    if (TextUtils.isEmpty(getDependency())) startQuery();
    }

    public void onContentChanged() {
        if (DBG) Log.i(LOG_TAG, "ChangeObserver: content changed.");
        if (!mFinished) {
            if (mPaused && !mRoot.getContext().isGlobalThread()) {
                mNeedsRequery = true;
            } else {
                startQuery();
            }
        }
    }

    public void pause() {
        super.pause();
        mHandler.removeCallbacks(mUpdater);
    }

    public void refresh() {
        super.refresh();
        startQuery();
    }

    public void resume() {
        super.resume();
        if (mNeedsRequery) startQuery();
        checkUpdate();
    }

    public void startQuery() {
       new AsyncQuery().execute();
    }
    //modify by huzeyin for Bug63984 2014.12.25, async query database,async update data
    private  final Object mLock=new Object();
    private boolean isWaitting;
    private class AsyncQuery extends AsyncTask<Void,Void, Boolean>{
         @Override
         protected Boolean doInBackground(Void... params) {
            synchronized (mLock) {
             while (isWaitting) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                 }
             }
            return startQueryData();
          }
        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if(result){
              checkUpdate();
           }
          isWaitting=false;
          synchronized (mLock) {
            mLock.notifyAll();
          }
       }
    }
    private boolean startQueryData(){
        isWaitting=true;
        synchronized (mLock) {
           mLock.notifyAll();
        }
        mNeedsRequery = false;
        if (mQueryHandler != null) mQueryHandler.cancelOperation(QUERY_TOKEN);
        Uri uri = Uri.parse(mUriFormatter.getText(getContext().mVariables));
        if (uri.getHost().equals("datahub")) // ignore miui datahub
        return false;
        mLastUri = uri.toString();
        String where = mWhereFormatter.getText(getContext().mVariables);
        if (DBG) Log.d(LOG_TAG, "start query: " + mLastUri + "\n where:" + where);
        if (mQueryHandler != null) mQueryHandler.startQuery(100, null, uri, mColumns, where, mArgs, mOrder);
        else onQueryComplete(getContext().mContext.getContentResolver().query(uri, mColumns, where, mArgs, mOrder));
        mLastQueryTime = System.currentTimeMillis();
        return true;
    }
    public static class Builder {

        private ContentProviderBinder mBinder;

        public void addVariable(String name, String type, String column, int row, Variables var) {
            Variable v = new Variable(name, type, var);
            v.mColumn = column;
            v.mRow = row;
            mBinder.addVariable(v);
        }

        public Builder setArgs(String args[]) {
            mBinder.mArgs = args;
            return this;
        }

        public Builder setColumns(String columns[]) {
            mBinder.mColumns = columns;
            return this;
        }

        public Builder setCountName(String countName) {
            mBinder.mCountName = countName;
            mBinder.createCountVar();
            return this;
        }

        public Builder setName(String name) {
            mBinder.mName = name;
            return this;
        }

        public Builder setOrder(String order) {
            mBinder.mOrder = order;
            return this;
        }

        public Builder setWhere(String where) {
            mBinder.mWhereFormatter = new TextFormatter(where);
            return this;
        }

        public Builder setWhere(String whereFormat, String whereParas) {
            mBinder.mWhereFormatter = new TextFormatter(whereFormat, whereParas);
            return this;
        }

        public Builder setNotifyUri(String uri) {
            mBinder.mNotifyUri = uri;
            return this;
        }

        protected Builder(ContentProviderBinder binder){
            mBinder = binder;
        }
    }

    private class ChangeObserver extends ContentObserver {

        public boolean deliverSelfNotifications() {
            return true;
        }

        public void onChange(boolean selfChange) {
            onContentChanged();
        }

        public ChangeObserver(){
            super(mHandler);
        }
    }

    private class MyDataSetObserver extends DataSetObserver {

        public void onChanged() {
            if (!mFinished) {
                  new AsyncUpdateVariables(false,true).execute();
//                updateVariables();
//                if (mQueryCompletedListener != null) {
//                    mQueryCompletedListener.onQueryCompleted(mName);
//                }
            }
        }

        public void onInvalidated() {
            if (!mFinished) {
                  new AsyncUpdateVariables(false,true).execute();
//                updateVariables();
//                if (mQueryCompletedListener != null) {
//                    mQueryCompletedListener.onQueryCompleted(mName);
//                }
            }
        }
    }

    public static interface QueryCompleteListener {

        public abstract void onQueryCompleted(String s);
    }

    private final class QueryHandler extends AsyncQueryHandler {

        protected Handler createHandler(Looper looper) {
            return new CatchingWorkerHandler(looper);
        }

        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
            ContentProviderBinder.this.onQueryComplete(cursor);
        }

        public QueryHandler(Context context){
            super(context.getContentResolver());
        }

        protected class CatchingWorkerHandler extends WorkerHandler {

            public void handleMessage(Message msg) {
                try {
                    super.handleMessage(msg);
                } catch (SQLiteDiskIOException e) {
                    Log.w(LOG_TAG, "Exception on background worker thread", e);
                } catch (SQLiteFullException e) {
                    Log.w(LOG_TAG, "Exception on background worker thread", e);
                } catch (SQLiteDatabaseCorruptException e) {
                    Log.w(LOG_TAG, "Exception on background worker thread", e);
                }
            }

            public CatchingWorkerHandler(Looper looper){
                super(looper);
            }
        }
    }

    private static class Variable extends VariableBinder.Variable {

        public static final int    BLOB_BITMAP = 1001;

        private ImageScreenElement mBlobVar;

        public String              mColumn;

        public int                 mRow;

        public boolean             mBlocked;

        protected void onLoad(Element node) {
            mColumn = node.getAttribute("column");
            mRow = Utils.getAttrAsInt(node, "row", 0);
        }

        public Variable(String name, String type, Variables var){
            super(name, type, var);
        }

        public Variable(Element node, Variables var){
            super(node, var);
        }

        public ImageScreenElement getImageElement(ScreenElementRoot root) {
            if (mBlobVar == null) mBlobVar = (ImageScreenElement)root.findElement(mName);
            return mBlobVar;
        }

        protected int parseType(String string) {
            int type = super.parseType(string);
            if ("blob.bitmap".equalsIgnoreCase(string)) {
                type = BLOB_BITMAP;
            }
            return type;
        }

        public void setNull(ScreenElementRoot root) {
            if (mType == STRING) {
                mStringVar.set(null);
            } else if (isNumber()) {
                mNumberVar.set(null);
            } else if (getImageElement(root) != null) {
                getImageElement(root).setBitmap(null);
            }
        }
    }

    private static class List {

        private int                            mMaxCount;
        private boolean                        mIsMixedLista;
        private String                         mName;
        private String                         mMixedItemName;
        private String[]                       mColumns;
        private HashMap<String, String>        mRemoveExtraHashMap;
        private ArrayList<DeleteExtra>         mDeleteExtras;

        private ListScreenElement              mList;
        private ScreenElementRoot              mRoot;
        private TextFormatter                  mUriFormatter;
        private ContentResolver                mContentResolver;
        private String                         mReplaceUri, mReplaceColumn, mReplaceOldeColumnr;

        private HashMap<String, TextFormatter> mDefaultHashMap;
        private static final String            DATE = "date";                                   // In order to sort

        public List(Element node, ScreenElementRoot root){
            mRoot = root;
            mName = node.getAttribute("name");
            mContentResolver = root.getContext().mContext.getContentResolver();
            mUriFormatter = ContentProviderBinder.getUriFTextFormatter(node);
            mColumns = ContentProviderBinder.getColumns(node);
            mMaxCount = Utils.getAttrAsInt(node, "maxCount", Integer.MAX_VALUE);
            loadListData(node, root);
        }
        public boolean isListDataNull(){
            return TextUtils.isEmpty(mMixedItemName);
        }
        private void loadListData(Element node, ScreenElementRoot root) {
            Utils.traverseXmlElementChildren(node, "ListData", new Utils.XmlTraverseListener() {

                @Override
                public void onChild(Element node) {
                    mIsMixedLista = Boolean.parseBoolean(node.getAttribute("binderMixedList"));
                    mMixedItemName = node.getAttribute("mixedItemName");
                    mReplaceUri = node.getAttribute("replaceUri");
                    loadData(node.getAttribute("removeExtra"), new DataLoading() {

                        @Override
                        public void load(String key, String value) {
                            if (!mIsMixedLista && mDeleteExtras == null) {
                                mDeleteExtras = new ArrayList<ListScreenElement.DeleteExtra>();
                            }
                            if (mRemoveExtraHashMap == null) {
                                mRemoveExtraHashMap = new HashMap<String, String>();
                            }
                            mRemoveExtraHashMap.put(key, value);

                        }
                    });
                    loadData(node.getAttribute("defaultData"), new DataLoading() {

                        @Override
                        public void load(String key, String value) {
                            if (mDefaultHashMap == null) {
                                mDefaultHashMap = new HashMap<String, TextFormatter>();
                            }
                            mDefaultHashMap.put(key, new TextFormatter(value, "", ""));

                        }
                    });
                    loadData(node.getAttribute("replace"), new DataLoading() {

                        @Override
                        public void load(String key, String value) {
                            mReplaceOldeColumnr = key;
                            mReplaceColumn = value;
                        }
                    });
                }
            });
        }

        interface DataLoading {

            void load(String key, String value);
        }

        private static final String STR_SPLIT_ELEMNT = "," ;
        private static final String STR_SPLIT = ":" ;

        private void loadData(String data, DataLoading loading) {
            if (!TextUtils.isEmpty(data)) {
                String[] strings = data.split(STR_SPLIT_ELEMNT);
                for (String str : strings) {
                    int i = str.indexOf(STR_SPLIT);
                    if (i == -1) {
                        continue;
                    }
                    loading.load(str.substring(0, i), str.substring(i + 1));
                }
            }
        }
        /**
         * Get name from YellowPage
         * @param defaultData
         * @param phoneNumber
         * @return Name for Special Number
         */
        private String getYellowPageNumberName(String defaultData,String phoneNumber){
            final String specilaPhoneName =LocationUtil.getSpecialPhone(mRoot.getContext().mContext,phoneNumber,true);
            return TextUtils.isEmpty(specilaPhoneName)?defaultData:specilaPhoneName;
        }
        private static final Uri PHONE_LOOKUP=PhoneLookup.CONTENT_FILTER_URI;
        private boolean isContacts(String phoneNumber){
            Uri uri = Uri.withAppendedPath(PHONE_LOOKUP, Uri.encode(phoneNumber));
            Cursor cursor=null;
            cursor=mContentResolver.query(uri, new String [] {PhoneLookup.DISPLAY_NAME},null,null, null);
            if(null!=cursor&&cursor.getCount()>0){
                cursor.close();
                return true ;
              }
           return false;
        }
        private String getReplaceData(String data, String uriStr, String replaceColumnName) {
            Cursor cursor = null;
            try {
                Uri uri = Uri.withAppendedPath(Uri.parse(uriStr), Uri.encode(data));
                if (uri != null) {
                    cursor = mContentResolver.query(uri, new String[] { replaceColumnName }, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(replaceColumnName);
                        if (index != -1) {
                            String string = cursor.getString(index);
                            if (!TextUtils.isEmpty(string)) {
                                data = string;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    try {
                        cursor.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return data;
        }
        public void fill(final Cursor cursor) {
            if (getListScreenElement() != null) {
               final boolean isRemoveItem=List.this.mList.getIsRemoveItem();
               if(isRemoveItem){
                   List.this.mList.setIsRemoveItem(false);
                   return ;
                }
                if (mIsMixedLista) {
                    new AsyncLoadListData(List.this,mMixedItemName).execute(cursor);
                } else {
                    new AsyncLoadListDataForDeleteExtras(List.this,mDeleteExtras).execute(cursor);
                }
            }
        }
        private static class AsyncLoadListData extends AsyncTask<Cursor,Void ,Object>{
            private String mMixedItemName;
            private List mParent ;
            public AsyncLoadListData(List parent ,String name){
                mMixedItemName=name;
                mParent=parent;
            }
            @Override
            protected Object doInBackground(Cursor... params) {
                Cursor cursor=params[0];
                if(cursor.isClosed()){
                    Log.d(LOG_TAG, "AsyncLoadListData Cursor is closed...");
                    return null;
                }
                return mParent.getDataList(cursor);
            }
            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                mParent.mList.fillList((ArrayList<DataWrapper>)result, mMixedItemName);
            }
        }
        private static class AsyncLoadListDataForDeleteExtras extends AsyncTask<Cursor, Void, Object>{
            private  ArrayList<DeleteExtra>         mDeleteExtras;
            private List mParent ;
            public AsyncLoadListDataForDeleteExtras(List parent,ArrayList<DeleteExtra>  deleteExtras){
                mParent=parent;
                mDeleteExtras=deleteExtras;
            }
            @Override
            protected Object doInBackground(Cursor... params) {
                Cursor cursor=params[0];
                if(cursor.isClosed()){
                    Log.d(LOG_TAG, "AsyncLoadListDataForDeleteExtras Cursor is closed...");
                   return null;
                }
                return mParent.getDataList(cursor);
            }
            @Override
            protected void onPostExecute(Object result) {
                super.onPostExecute(result);
                mParent.mList.fillList((ArrayList<Object[]>)result, mDeleteExtras);
            }
        }
        private ListScreenElement getListScreenElement() {
            if (mList == null) {
                ScreenElement element = mRoot.findElement(mName);
                if (element instanceof ListScreenElement) {
                    mList = (ListScreenElement)element;
                }
                if(mList == null){
                    Log.e("ContentProviderBinder", "fail to find list: " + mName);
                }
            }
            return mList;
        }

        private Object getDataList(Cursor cursor) {
            try {
                if (cursor == null || cursor.getCount() <= 0 || cursor.isClosed()||getListScreenElement() == null) {
                    return null;
                }
                ArrayList<ColumnInfo> columnInfos = mList.getColumnInfoList(mIsMixedLista ? mMixedItemName : null);
                int size = columnInfos.size();
                int[] arrayOfInt = new int[size];
                String[] defalutData = null;
                if (mDefaultHashMap != null) {
                    defalutData = new String[size];
                }
                int replaceIndex = -1;
                for (int i = 0; i < size; i++) {
                    String columnName = ((ColumnInfo)columnInfos.get(i)).mVarName;
                    try {
                        if (replaceIndex == -1 && mReplaceOldeColumnr != null && mReplaceOldeColumnr.equals(columnName)) {
                            replaceIndex = i;
                        }
                        arrayOfInt[i] = cursor.getColumnIndex(columnName);
                        if (defalutData != null) {
                            TextFormatter formatter = mDefaultHashMap.get(columnName);
                            if (formatter != null) {
                                defalutData[i] = formatter.getText(mRoot.getVariables());
                            }
                        }
                    } catch (Exception exception) {
                        Log.e("ContentProviderBinder", "illegal column:" + columnName + "  " + exception.toString());
                        return null;
                    }
                }
                boolean needExtra = mIsMixedLista || mDeleteExtras != null;
                int firstColumnIndex = needExtra ? cursor.getColumnIndex(mColumns[0]) : 0;
                String string = needExtra ? mColumns[0] + " = " : null;
                String uri = needExtra ? mUriFormatter.getText(mRoot.getVariables()) : null;
                ArrayList<Object> arrayList = new ArrayList<Object>(cursor.getCount());
                if (mDeleteExtras != null) {
                    mDeleteExtras.clear();
                }
                for (int start = 0, Max = Math.min(mMaxCount, cursor.getCount()); start < Max; start++) {
                    cursor.moveToPosition(start);
                    Object[] objects = new Object[size];
                    Object[] oldObjects = new Object[size];
                    long date = -1;
                    String where = string + (needExtra ? cursor.getString(firstColumnIndex) : null);
                    int strPostion=-1;
                    int nullStrPostion=-1;
                    for (int position = 0; position < size; position++) {
                        ColumnInfo info = (ColumnInfo)columnInfos.get(position);
                        int index = arrayOfInt[position];
                        if (mIsMixedLista && date == -1 && info.mVarName.equals(DATE)) {
                            date = Long.valueOf(cursor.getLong(index));
                        }
                        switch (info.mType) {
                            case BITMAP:
                                byte[] arrayOfByte = cursor.getBlob(index);
                                if (arrayOfByte != null) {
                                    objects[position] = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                                    oldObjects[position] = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                                }
                                break;
                            case INTEGER:
                                objects[position] = Integer.valueOf(cursor.getInt(index));
                                oldObjects[position] = Integer.valueOf(cursor.getInt(index));
                                break;
                            case DOUBLE:
                                objects[position] = Double.valueOf(cursor.getDouble(index));
                                oldObjects[position] = Double.valueOf(cursor.getDouble(index));
                                break;
                            case LONG:
                                objects[position] = Long.valueOf(cursor.getLong(index));
                                oldObjects[position] = Long.valueOf(cursor.getLong(index));
                                break;
                            case FLOAT:
                                objects[position] = Float.valueOf(cursor.getFloat(index));
                                oldObjects[position] = Float.valueOf(cursor.getFloat(index));
                                break;
                            case STRING:
                                objects[position] = cursor.getString(index);
                                oldObjects[position] = cursor.getString(index);
                                if(objects[position]!=null&&date==-1){
                                    strPostion=position;
                                }
                                if (objects[position] == null && defalutData != null && defalutData[position] != null) {
                                    nullStrPostion=position;
                                    //oldObjects[position] = defalutData[position];
                                    //objects[position] = defalutData[position];
                                }
                                break;
                        }
                        if(strPostion!=-1&&nullStrPostion!=-1){
                            objects[nullStrPostion]=getYellowPageNumberName(defalutData[nullStrPostion],(String)objects[strPostion]);
                        }
                        if (replaceIndex != -1 && position == replaceIndex && objects[position] instanceof String) {
                            final String phoneNumber =(String)objects[position];
                            if(isContacts(phoneNumber)){
                                objects[position] =getReplaceData(phoneNumber, mReplaceUri, mReplaceColumn);
                              }else {
                                   String yellowName =getYellowPageNumberName(null, phoneNumber);
                                   if(TextUtils.isEmpty(yellowName)){
                                      objects[position] =getReplaceData(phoneNumber, mReplaceUri, mReplaceColumn);
                                      }else {
                                        objects[position]=yellowName;
                                     }
                              }
                        }
                    }
                    Object object = null;
                    DeleteExtra extra = new DeleteExtra(uri, where, mRemoveExtraHashMap);
                    if (mIsMixedLista) {
                        DataWrapper data = new DataWrapper(objects,oldObjects, date, mMixedItemName);
                        data.setDelete(extra);
                        object = data;
                    } else {
                        if (mDeleteExtras != null) {
                            mDeleteExtras.add(extra);
                        }
                        object = objects;
                    }
                    arrayList.add(object);
                }
                return arrayList;
            } catch (Exception e) {
            }
            return null;
        }
    }

    private static class VariableArray extends VariableBinder.VariableArray {

        public static final int BLOB_BITMAP = 1001;
        public boolean          mBlocked;
        public String           mColumn;
        public int              mMaxCount;

        public VariableArray(Element node, ScreenElementRoot root){
            super(node, root);
            mColumn = node.getAttribute("column");
            mMaxCount = Utils.getAttrAsInt(node, "maxCount", Integer.MAX_VALUE);
        }

        protected int parseType(String str) {
            int type = super.parseType(str);
            if ("blob.bitmap".equalsIgnoreCase(str)) {
                type = BLOB_BITMAP;
            }
            return type;
        }

        public void fill(Cursor cursor) {
            Object[] objects = null;
            if (cursor != null) {
                int count = cursor.getCount();
                if (count > mMaxCount) {
                    count = mMaxCount;
                }
                objects = new Object[count];
                int columnIndex = cursor.getColumnIndex(mColumn);
                if (columnIndex < 0) {
                    return;
                }
                for (int i = 0; i < count; i++) {
                    if (cursor.moveToPosition(i) && !cursor.isNull(columnIndex)) {
                        switch (mType) {
                            case STRING:
                                objects[i] = cursor.getString(columnIndex);
                                break;
                            case INT:
                                objects[i] = Integer.valueOf(cursor.getInt(columnIndex));
                                break;
                            case LONG:
                                objects[i] = Long.valueOf(cursor.getLong(columnIndex));
                                break;
                            case FLOAT:
                                objects[i] = Float.valueOf(cursor.getFloat(columnIndex));
                                break;
                            case DOUBLE:
                                objects[i] = Double.valueOf(cursor.getDouble(columnIndex));
                                break;
                            case TYPE_BASE:
                                byte[] arrayOfByte = cursor.getBlob(columnIndex);
                                if (arrayOfByte != null) {
                                    objects[i] = BitmapFactory.decodeByteArray(arrayOfByte, 0, arrayOfByte.length);
                                }
                                break;
                        }
                    }
                }
                fillData(objects);
            }
        }
    }
}
