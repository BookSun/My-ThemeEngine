
package com.lewa.support.laml;

import android.util.Log;
import java.util.ArrayList;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ExternalCommandManager {
    private static final String LOG_TAG = "ExternalCommandManager";

    public static final String TAG_NAME = "ExternalCommands";

    private ArrayList<CommandTrigger> mTriggers;

    public ExternalCommandManager(Element node, ScreenElementRoot root)
            throws ScreenElementLoadException {
        mTriggers = new ArrayList<CommandTrigger>();
        if (node != null)
            load(node, root);
    }

    private void load(Element node, ScreenElementRoot root) throws ScreenElementLoadException {
        if (node == null) {
            Log.e(LOG_TAG, "node is null");
            throw new ScreenElementLoadException("node is null");
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
            if (children.item(i).getNodeType() == 1) {
                Element item = (Element) children.item(i);
                if (item.getNodeName().equals("Trigger")) {
                    mTriggers.add(new CommandTrigger(item, root));
                }
            }
    }

    public void finish() {
        for (CommandTrigger t : mTriggers)
            t.finish();
    }

    public void init() {
        for (CommandTrigger t : mTriggers)
            t.init();
    }

    public void onCommand(String command) {
        for (CommandTrigger t : mTriggers)
            if (t.getActionString().equals(command))
                t.perform();
    }

    public void pause() {
        for (CommandTrigger t : mTriggers)
            t.pause();
    }

    public void resume() {
        for (CommandTrigger t : mTriggers)
            t.resume();
    }
}
