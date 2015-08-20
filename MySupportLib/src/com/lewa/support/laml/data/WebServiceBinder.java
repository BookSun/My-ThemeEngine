package com.lewa.support.laml.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.lewa.support.laml.ScreenContext;
import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.elements.ListScreenElement;
import com.lewa.support.laml.elements.ListScreenElement.ColumnInfo;
import com.lewa.support.laml.util.TextFormatter;
import com.lewa.support.laml.util.Utils;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class WebServiceBinder extends VariableBinder {

    private static final String PREF_LAST_QUERY_TIME = "LastQueryTime";

    public static final String  TAG_NAME             = "WebServiceBinder";

    private static final String LOG_TAG              = TAG_NAME;

    private ScreenContext       mContext;

    private long                mLastQueryTime;

    protected String            mName;

    private TextFormatter       mParamsFormatter;

    private boolean             mQueryInProgress;

    private boolean             mQuerySuccessful     = true;

    private Thread              mQueryThread;

    private int                 mUpdateInterval      = -1;

    private int                 mUpdateIntervalFail  = -1;

    protected TextFormatter     mUriFormatter;

    private List                mList;

    public WebServiceBinder(Element node, ScreenElementRoot root){
        super(root);
        load(node);
    }

    private void load(Element node) {
        if (node == null) {
            Log.e(LOG_TAG, "WebServiceBinder node is null");
            throw new NullPointerException("node is null");
        }

        mName = node.getAttribute("name");
        mUriFormatter = new TextFormatter(node.getAttribute("uri"), node.getAttribute("uriFormat"),
                                          node.getAttribute("uriParas"));
        mParamsFormatter = new TextFormatter(node.getAttribute("params"), node.getAttribute("paramsFormat"),
                                             node.getAttribute("paramsParas"));
        mUpdateInterval = Utils.getAttrAsInt(node, "updateInterval", -1);
        mUpdateIntervalFail = Utils.getAttrAsInt(node, "updateIntervalFail", -1);
        loadVariables(node);
        loadVariableArrays(node);
        try {
            mList = new List(node, mRoot);
        } catch (IllegalArgumentException localIllegalArgumentException) {
            Log.e("WebServiceBinder", "invalid List");
        }
    }

    protected Variable onLoadVariable(Element node) {
        return new Variable(node, getContext().mVariables);
    }

    protected VariableArray onLoadVariableArray(Element node) {
        return new VariableArray(node, mRoot);
    }

    private void onQueryComplete(String result) {
        if (result != null) {
            InputStream is = null;
            try {
                is = new ByteArrayInputStream(result.getBytes("utf-8"));
                XPath xpath = XPathFactory.newInstance().newXPath();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(is);
                for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
                    Variable v = (Variable)variable ;
                    if (v.mStringVar != null) {
                        try {
                            String value = (String)xpath.evaluate(v.mXPath, doc, XPathConstants.STRING);
                            v.mStringVar.set(value);
                        } catch (XPathExpressionException e) {
                            v.mStringVar.set(null);
                            Log.e(LOG_TAG, "fail to get variable: " + v.mName + " :" + e.toString());
                        }
                    }
                }
            } catch (SAXException e) {
                Log.e(LOG_TAG, e.toString());
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, e.toString());
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            } catch (ParserConfigurationException e) {
                Log.e(LOG_TAG, e.toString());
            } finally {
                if (is != null) try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void tryStartQuery() {
        long time = System.currentTimeMillis() - mLastQueryTime;
        if (time < 0) mLastQueryTime = 0;

        if (mLastQueryTime == 0 || (mUpdateInterval > 0 && time > 1000 * mUpdateInterval)
            || (!mQuerySuccessful && mUpdateIntervalFail > 0 && time > 1000 * mUpdateIntervalFail)) startQuery();
    }

    protected void addVariable(Variable v) {
        mVariables.add(v);
    }

    public void finish() {
        SharedPreferences sp = mContext.mContext.getSharedPreferences(ScreenContext.LAML_PREFERENCES, 0);
        Editor ed = sp.edit();
        ed.putLong(mName + PREF_LAST_QUERY_TIME, mLastQueryTime);
        Log.i(LOG_TAG, "persist mLastQueryTime: " + mLastQueryTime);
        for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
            Variable v = (Variable)variable ;
            if (v.mPersist) if (v.mStringVar != null) ed.putString(mName + v.mName, v.mStringVar.get());
            else if (v.mNumberVar != null) ed.putFloat(mName + v.mName, v.mNumberVar.get().floatValue());
        }
        ed.commit();
        super.finish();
    }

    public String getName() {
        return mName;
    }

    public void init() {
        super.init();
        mQuerySuccessful = true;
        SharedPreferences sp = mContext.mContext.getSharedPreferences(ScreenContext.LAML_PREFERENCES, 0);

        mLastQueryTime = sp.getLong(mName + PREF_LAST_QUERY_TIME, 0L);
        Log.i(LOG_TAG, "get persisted mLastQueryTime: " + mLastQueryTime);
        for (com.lewa.support.laml.data.VariableBinder.Variable variable : mVariables) {
            Variable v = (Variable)variable ;
            if (v.mPersist) if (v.mStringVar != null) v.mStringVar.set(sp.getString(mName + v.mName, null));
            else if (v.mNumberVar != null) v.mNumberVar.set(sp.getFloat(mName + v.mName, 0.0F));
        }
        tryStartQuery();
    }

    public void pause() {
        super.pause();
    }

    public void refresh() {
        super.refresh();
        startQuery();
    }

    public void resume() {
        super.resume();
        tryStartQuery();
    }

    public void startQuery() {
        if (mQueryInProgress) return;

        mQueryInProgress = true;
        mQuerySuccessful = false;
        mQueryThread = new QueryThread();
        mQueryThread.start();
    }

    private class QueryThread extends Thread {

        public void run() {
            try {
                ArrayList<BasicNameValuePair> paramsList = null;
                Log.i(LOG_TAG, "QueryThread start");
                Uri uri = Uri.parse(mUriFormatter.getText(mContext.mVariables));
                HttpPost request = new HttpPost(uri.toString());
                String paramsStr = mParamsFormatter.getText(mContext.mVariables);
                if (!TextUtils.isEmpty(paramsStr)) {
                    paramsList = new ArrayList<BasicNameValuePair>();
                    String params[] = paramsStr.split(",");
                    for (String pa : params) {
                        String param[] = pa.split(":");
                        if (param.length == 2) paramsList.add(new BasicNameValuePair(param[0], param[1]));
                    }
                }
                request.setEntity(new UrlEncodedFormEntity(paramsList, "UTF-8"));
                HttpResponse httpResponse = (new DefaultHttpClient()).execute(request);
                int statusCode = httpResponse.getStatusLine().getStatusCode();
                String result = null;
                if (statusCode == 200) result = EntityUtils.toString(httpResponse.getEntity());
                Log.i(LOG_TAG, "QueryThread get result: " + statusCode + " \n" + result);
                onQueryComplete(result);
                mLastQueryTime = System.currentTimeMillis();
                mQueryInProgress = false;
                Log.i(LOG_TAG, "QueryThread end");
            } catch (UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "fail to run query, " + e.toString());
            } catch (ClientProtocolException e) {
                Log.e(LOG_TAG, "fail to run query, " + e.toString());
            } catch (IOException e) {
                Log.e(LOG_TAG, "fail to run query, " + e.toString());
            }
        }

        public QueryThread(){
            super("WebServiceBinder QueryThread");
        }
    }

    public static class Variable extends VariableBinder.Variable {

        public boolean mPersist;

        public String  mXPath;

        public Variable(String name, String type, Variables var){
            super(name, type, var);
        }

        public Variable(Element node, Variables var){
            super(node, var);
        }

        protected void onLoad(Element node) {
            mXPath = node.getAttribute("xpath");
            mPersist = Boolean.parseBoolean(node.getAttribute("persist"));
        }
    }

    private static class VariableArray extends VariableBinder.VariableArray {

        public String mInnerXPath;
        public int    mMaxCount;
        public String mXPath;

        public VariableArray(Element node, ScreenElementRoot root){
            super(node, root);
            mMaxCount = Utils.getAttrAsInt(node, "maxCount", Integer.MAX_VALUE);
            mXPath = node.getAttribute("xpath");
            mInnerXPath = node.getAttribute("innerXpath");
        }

        public void fill(Object object, XPath xPath) {
            if (TextUtils.isEmpty(mXPath)) {
                return;
            }
            NodeList localNodeList;
            try {
                localNodeList = (NodeList)xPath.evaluate(mXPath, object, XPathConstants.NODESET);
                if (localNodeList != null) {
                    int count = localNodeList.getLength();
                    count = Math.min(mMaxCount, count);
                    if (count <= 0) {
                        return;
                    }
                    Object[] arrayOfObject = new Object[count];
                    for (int i = 0; i < count; i++) {
                        Node node2 = localNodeList.item(i);
                        Node node = (Node)xPath.evaluate(mInnerXPath, node2, XPathConstants.NODE);
                        if (node != null) {
                            String str = node.getTextContent();
                            if (str != null) {
                                switch (mType) {
                                    case STRING:
                                        arrayOfObject[i] = str;
                                        break;
                                    case INT:
                                        arrayOfObject[i] = Integer.valueOf(str);
                                        break;
                                    case LONG:
                                        arrayOfObject[i] = Long.valueOf(str);
                                        break;
                                    case FLOAT:
                                        arrayOfObject[i] = Float.valueOf(str);
                                        break;
                                    case DOUBLE:
                                        arrayOfObject[i] = Double.valueOf(str);
                                        break;
                                    case TYPE_BASE:
                                        break;
                                }
                            }
                        }
                    }
                    fillData(arrayOfObject);

                }
            } catch (XPathExpressionException e) {
                e.printStackTrace();
            }
        }
    }

    private static class List {

        private ListScreenElement mList;
        private String            mName;
        private ScreenElementRoot mRoot;
        public String             mXPath;

        public List(Element node, ScreenElementRoot root){
            mXPath = node.getAttribute("xpath");
            mName = node.getAttribute("name");
            mRoot = root;
        }

        public void fill(NodeList nodeList) {
            if (nodeList == null || nodeList.getLength() == 0) {
                return;
            }
            if (mList == null) {
                mList = (ListScreenElement)mRoot.findElement(mName);
                if (mList == null) {
                    Log.e("WebServiceBinder", "fail to find list: " + mName);
                    return;
                }
            }
            mList.removeAllItems();
            ArrayList<ColumnInfo> columnInfos = mList.getColumnInfoList();
            int size = columnInfos.size();
            Object[] arrayOfObject = new Object[size];
            for (int start = 0; start < nodeList.getLength(); start++) {
                Element element = (Element)nodeList.item(start);
                if (element == null) {
                    continue;
                }
                int i = 0;
                for (ColumnInfo columnInfo : columnInfos) {
                    Element elementChild = Utils.getChild(element, columnInfo.mVarName);
                    if (elementChild == null) {
                        continue;
                    }
                    String str = elementChild.getTextContent();
                    if (str == null) {
                        continue;
                    }
                    switch (columnInfo.mType) {
                        case BITMAP:
                            break;
                        case INTEGER:
                            arrayOfObject[i] = Integer.valueOf(str);
                            break;
                        case DOUBLE:
                            arrayOfObject[i] = Double.valueOf(str);
                            break;
                        case LONG:
                            arrayOfObject[i] = Long.valueOf(str);
                            break;
                        case FLOAT:
                            arrayOfObject[i] = Float.valueOf(str);
                            break;
                        case STRING:
                            arrayOfObject[i] = str;
                            break;
                    }
                    i++;
                }
                try {
                    mList.addItem(arrayOfObject);
                } catch (ScreenElementLoadException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
