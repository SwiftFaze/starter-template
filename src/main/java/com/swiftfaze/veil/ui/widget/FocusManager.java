package com.swiftfaze.veil.ui.widget;

public class FocusManager {

    private boolean popupOpen = false;

    public void captureModally() {
        popupOpen = true;
    }

    public void restoreFocus() {
        popupOpen = false;
    }

    public boolean isPopupFocused() {
        return popupOpen;
    }
}
