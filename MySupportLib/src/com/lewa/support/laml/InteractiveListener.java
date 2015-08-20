
package com.lewa.support.laml;

import com.lewa.support.laml.elements.ButtonScreenElement;
import com.lewa.support.laml.elements.ButtonScreenElement.ButtonAction;

public abstract interface InteractiveListener {
    public abstract void onButtonInteractive(ButtonScreenElement ele,
            ButtonAction action);
}
