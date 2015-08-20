package com.lewa.support.laml.shader;

import java.util.ArrayList;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.util.ColorParser;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import android.graphics.Matrix;
import android.graphics.Shader;
import android.text.TextUtils;
import android.util.Log;
/**
 * 
 * ShaderElement.java:
 * @author yljiang@lewatek.com 2014-7-8
 */
public abstract class ShaderElement {

    private static final String LOG_TAG        = "ShaderElement";
    protected GradientStops     mGradientStops = new GradientStops();
    protected ScreenElementRoot mRoot;
    protected Shader            mShader;
    protected Matrix            mShaderMatrix  = new Matrix();
    protected Shader.TileMode   mTileMode;
    protected float             mX;
    protected Expression        mXExp;
    protected float             mY;
    protected Expression        mYExp;

    public ShaderElement(Element ele, ScreenElementRoot root){
        mRoot = root;
        mXExp = Expression.build(ele.getAttribute("x"));
        mYExp = Expression.build(ele.getAttribute("y"));
        mTileMode = getTileMode(ele.getAttribute("tile"));
        if (!ele.getTagName().equalsIgnoreCase("BitmapShader")){
            loadGradientStops(ele, root);
        }
    }

    public static Shader.TileMode getTileMode(String paramString) {
        if (TextUtils.isEmpty(paramString))
            return Shader.TileMode.CLAMP;
        if (paramString.equalsIgnoreCase("mirror")) 
            return Shader.TileMode.MIRROR;
        if (paramString.equalsIgnoreCase("repeat"))
            return Shader.TileMode.REPEAT;
        return Shader.TileMode.CLAMP;
    }

    private void loadGradientStops(Element ele, ScreenElementRoot root) {
        NodeList localNodeList = ele.getElementsByTagName("GradientStop");
        for (int i = 0; i < localNodeList.getLength(); i++) {
            Element localElement = (Element)localNodeList.item(i);
            mGradientStops.add(new GradientStop(localElement, root));
        }
        if (mGradientStops.size() <= 0) {
            Log.e(LOG_TAG, "lost gradient stop.");
            return;
        }
        mGradientStops.init();
    }

    public Shader getShader() {
        return mShader;
    }

    public float getX() {
        return mXExp != null ? (float)(mXExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    public float getY() {
        return mYExp != null ? (float)(mYExp.evaluate(mRoot.getVariables()) * mRoot.getScale()):0;
    }

    public abstract void onGradientStopsChanged();

    public void updateShader() {
        mGradientStops.update();
        if (updateShaderMatrix()){
            mShader.setLocalMatrix(mShaderMatrix);
        }
    }

    public abstract boolean updateShaderMatrix();

    private final class GradientStop {

        public static final String TAG_NAME = "GradientStop";
        private ColorParser        mColorParser;
        private Expression         mPositionExp;
        public  boolean            noDefPosition = false;

        public GradientStop(Element root, ScreenElementRoot arg3){
            mColorParser = ColorParser.fromElement(root);
            mPositionExp = Expression.build(root.getAttribute("position"));
            if (mPositionExp == null) {
                noDefPosition = true ;
                Log.e(TAG_NAME, "lost position attribute.");
            }
        }

        public int getColor() {
            return mColorParser.getColor(mRoot.getVariables());
        }

        public float getPosition() {
            return mPositionExp != null ? (float)mPositionExp.evaluate(mRoot.getVariables()):0 ;
        }
    }

    protected final class GradientStops {

        private int[]                     mColors;
        protected ArrayList<GradientStop> stops = new ArrayList<GradientStop>();
        private float[]                   mPositions;
        public  boolean                   noDefPosition ;

        protected GradientStops(){
        }

        public void add(GradientStop stop) {
            if(stop.noDefPosition){
                noDefPosition = true ;
            }
            stops.add(stop);
        }

        public int[] getColors() {
            return mColors;
        }


        public float[] getPositions() {
            if(noDefPosition){
                return null;
            }
            return mPositions;
        }
        public void init() {
            mColors = new int[size()];
            mPositions = new float[size()];
        }

        public int size() {
            return stops.size();
        }

        public void update() {
            boolean change = false ;
            for (int index = 0; index < size(); index++) {
                int color = stops.get(index).getColor();
                if (color != mColors[index]) {
                    mColors[index] = color;
                    change = true ;
                }
                float position = (stops.get(index)).getPosition();
                if (position != mPositions[index]) {
                    mPositions[index] = position;
                    change = true ;
                }
            }
            if (change) {
                onGradientStopsChanged();
            } 
        }
    }
}
