package com.lewa.support.laml.shader;

import com.lewa.support.laml.ScreenElementRoot;
import com.lewa.support.laml.elements.BitmapProvider;

import org.w3c.dom.Element;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Shader;
/**
 * 
 * BitmapShaderElement.java:
 * @author yljiang@lewatek.com 2014-7-8
 */
public class BitmapShaderElement extends ShaderElement {

    public static final String TAG_NAME = "BitmapShader";
    private Bitmap             mBitmap;
    private BitmapProvider     mBitmapProvider;
    private Shader.TileMode    mTileModeX;
    private Shader.TileMode    mTileModeY;

    public BitmapShaderElement(Element ele, ScreenElementRoot root){
        super(ele, root);
        mBitmapProvider = BitmapProvider.create(root, null);
        mBitmap = mBitmapProvider.getBitmap(ele.getAttribute("src"));
        resolveTileMode(ele);
        mShader = new BitmapShader(mBitmap, mTileModeX, mTileModeY);
    }

    private void resolveTileMode(Element ele) {
        String[] arrayOfString = ele.getAttribute("tile").split(",");
        if (arrayOfString.length >= 2){
            mTileModeX = getTileMode(arrayOfString[0]);
            mTileModeY = getTileMode(arrayOfString[1]);
        } else {
            mTileModeY = mTileMode;
            mTileModeX = mTileMode;
        }
    }

    public void onGradientStopsChanged() {
    }

    public void updateShader() {
    }

    public boolean updateShaderMatrix() {
        return false;
    }
}
