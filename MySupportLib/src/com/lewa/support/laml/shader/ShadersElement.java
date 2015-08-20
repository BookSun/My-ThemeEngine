package com.lewa.support.laml.shader;

import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.graphics.Shader;

/**
 * ShadersElement.java:
 * 
 * @author yljiang@lewatek.com 2014-7-8
 */
public final class ShadersElement {

    public static final String FILL_TAG_NAME       = "FillShaders";
    public static final String tagNameOKE_TAG_NAME = "tagNameokeShaders";
    private ShaderElement      mShaderElement;

    public ShadersElement(Element ele, ScreenElementRoot root){
        loadShaderElements(ele, root);
    }

    private void loadShaderElements(Element ele, ScreenElementRoot root) {
        NodeList children = ele.getChildNodes();
        for (int i = 0, N = children.getLength(); i < N; i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node;
                String tagName = element.getTagName();
                if (tagName.equalsIgnoreCase(LinearGradientElement.TAG_NAME)) {
                    mShaderElement = new LinearGradientElement(element, root);
                    return;
                }
                if (tagName.equalsIgnoreCase(RadialGradientElement.TAG_NAME)) {
                    mShaderElement = new RadialGradientElement(element, root);
                    return;
                }
                if (tagName.equalsIgnoreCase(SweepGradientElement.TAG_NAME)) {
                    mShaderElement = new SweepGradientElement(element, root);
                    return;
                }
                if (tagName.equalsIgnoreCase(BitmapShaderElement.TAG_NAME)) {
                    mShaderElement = new BitmapShaderElement(element, root);
                    return;
                }
            }
        }
    }

    public Shader getShader() {
        return mShaderElement != null ? mShaderElement.getShader() : null;
    }

    public void updateShader() {
        if (mShaderElement != null){
            mShaderElement.updateShader();
        }
    }
}
