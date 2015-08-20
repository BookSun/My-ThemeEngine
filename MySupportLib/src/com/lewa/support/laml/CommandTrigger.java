
package com.lewa.support.laml;

import android.text.TextUtils;

import java.util.ArrayList;

import com.lewa.support.laml.data.Expression;
import com.lewa.support.laml.elements.ButtonScreenElement.ButtonAction;
import com.lewa.support.laml.util.Utils;

import org.w3c.dom.*;

import static com.lewa.support.laml.elements.ButtonScreenElement.ButtonAction.*;

public class CommandTrigger {

    public static final String TAG_NAME = "Trigger";

    private ButtonAction mAction = ButtonAction.Other;

    private String mActionString;

    private ArrayList<ActionCommand> mCommands = new ArrayList<ActionCommand>();

    private Expression mCondition;

    private ActionCommand.PropertyCommand mPropertyCommand;

    private ScreenElementRoot mRoot;

    public CommandTrigger(Element ele, ScreenElementRoot root) throws ScreenElementLoadException {
        load(ele, root);
    }

    public static CommandTrigger fromElement(Element ele, ScreenElementRoot root) {
        if (ele != null) {
            try {
                return new CommandTrigger(ele, root);
            } catch (ScreenElementLoadException e) {
            }
        }
        return null;
    }

    public static CommandTrigger fromParentElement(Element parent, ScreenElementRoot root) {
        return fromElement(Utils.getChild(parent, TAG_NAME), root);
    }

    private void load(Element ele, ScreenElementRoot root) throws ScreenElementLoadException {
        if (ele != null) {
            mRoot = root;
            String target = ele.getAttribute("target");
            String property = ele.getAttribute("property");
            String value = ele.getAttribute("value");
            if (!TextUtils.isEmpty(property) && !TextUtils.isEmpty(target)
                    && !TextUtils.isEmpty(value))
                mPropertyCommand = ActionCommand.PropertyCommand.create(root, target + "."
                        + property, value);
            String action = ele.getAttribute("action");
            mActionString = action;
            if (!TextUtils.isEmpty(action))
                if (action.equalsIgnoreCase("down"))
                    mAction = Down;
                else if (action.equalsIgnoreCase("up"))
                    mAction = Up;
                else if (action.equalsIgnoreCase("double"))
                    mAction = Double;
                else if (action.equalsIgnoreCase("long"))
                    mAction = Long;
                else if (action.equalsIgnoreCase("cancel"))
                    mAction = Cancel;
                else
                    mAction = Other;
            mCondition = Expression.build(ele.getAttribute("condition"));
            NodeList nodelist = ele.getChildNodes();
            for (int j = 0, N = nodelist.getLength(); j < N; j++) {
                if (nodelist.item(j).getNodeType() == Node.ELEMENT_NODE) {
                    Element item = (Element) nodelist.item(j);
                    ActionCommand command = ActionCommand.create(item, root);
                    if (command != null)
                        mCommands.add(command);
                }
            }

        }
    }

    public void finish() {
        for (ActionCommand cmd : mCommands)
            cmd.finish();

    }

    public ButtonAction getAction() {
        return mAction;
    }

    public String getActionString() {
        return mActionString;
    }

    public void init() {
        for (ActionCommand cmd : mCommands)
            cmd.init();

    }

    public void pause() {
        for (ActionCommand cmd : mCommands)
            cmd.pause();

    }

    public void perform() {
        if (mCondition == null || mCondition.evaluate(mRoot.getVariables()) > 0) {
            if (mPropertyCommand != null)
                mPropertyCommand.perform();
            for (ActionCommand cmd : mCommands)
                cmd.perform();

        }
    }

    public void resume() {
        for (ActionCommand cmd : mCommands)
            cmd.resume();

    }
}
