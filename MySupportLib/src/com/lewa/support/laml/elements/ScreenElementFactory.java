
package com.lewa.support.laml.elements;

import com.lewa.support.laml.ScreenElementLoadException;
import com.lewa.support.laml.ScreenElementRoot;

import org.w3c.dom.Element;

public class ScreenElementFactory {
    public ScreenElement createInstance(Element ele, ScreenElementRoot root)
            throws ScreenElementLoadException {
        String tag = ele.getTagName();
        if (tag.equalsIgnoreCase(ImageScreenElement.TAG_NAME))
            return new ImageScreenElement(ele, root);

        if (tag.equalsIgnoreCase(PathScreenElement.TAG_NAME))
            return new PathScreenElement(ele, root);

        if (tag.equalsIgnoreCase(TimepanelScreenElement.TAG_NAME))
            return new TimepanelScreenElement(ele, root);

        if (tag.equalsIgnoreCase(ImageNumberScreenElement.TAG_NAME))
            return new ImageNumberScreenElement(ele, root);

        if (tag.equalsIgnoreCase(TextScreenElement.TAG_NAME))
            return new TextScreenElement(ele, root);

        if (tag.equalsIgnoreCase(DateTimeScreenElement.TAG_NAME))
            return new DateTimeScreenElement(ele, root);

        if (tag.equalsIgnoreCase(ButtonScreenElement.TAG_NAME))
            return new ButtonScreenElement(ele, root);

        if (tag.equalsIgnoreCase(MusicControlScreenElement.TAG_NAME))
            return new MusicControlScreenElement(ele, root);

        if (tag.equalsIgnoreCase(ElementGroup.TAG_NAME)
                || tag.equalsIgnoreCase(ElementGroup.TAG_NAME1))
            return new ElementGroup(ele, root);

        if (tag.equalsIgnoreCase(VariableElement.TAG_NAME))
            return new VariableElement(ele, root);

        if (tag.equalsIgnoreCase(VariableArrayElement.TAG_NAME))
            return new VariableArrayElement(ele, root);

        if (tag.equalsIgnoreCase(SpectrumVisualizerScreenElement.TAG_NAME))
            return new SpectrumVisualizerScreenElement(ele, root);

        if (tag.equalsIgnoreCase(AdvancedSlider.TAG_NAME))
            return new AdvancedSlider(ele, root);

        if (tag.equalsIgnoreCase(FramerateController.TAG_NAME))
            return new FramerateController(ele, root);

        if (tag.equalsIgnoreCase(VirtualScreen.TAG_NAME))
            return new VirtualScreen(ele, root);

        if (tag.equalsIgnoreCase(LineScreenElement.TAG_NAME))
            return new LineScreenElement(ele, root);

        if (tag.equalsIgnoreCase(RectangleScreenElement.TAG_NAME))
            return new RectangleScreenElement(ele, root);
         
        if (tag.equalsIgnoreCase(EllipseScreenElement.TAG_NAME))
            return new EllipseScreenElement(ele, root);
         
        if (tag.equalsIgnoreCase(CircleScreenElement.TAG_NAME))
            return new CircleScreenElement(ele, root);
         
        if (tag.equalsIgnoreCase(ArcScreenElement.TAG_NAME))
            return new ArcScreenElement(ele, root);
 
        if (tag.equalsIgnoreCase(ListScreenElement.TAG_NAME))
            return new ListScreenElement(ele, root);

        if (tag.equalsIgnoreCase(MirrorScreenElement.TAG_NAME))
            return new MirrorScreenElement(ele, root);

        if (tag.equalsIgnoreCase(PaintScreenElement.TAG_NAME))
            return new PaintScreenElement(ele, root);
        return null;
    }
}
