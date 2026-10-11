package com.swiftfaze.veil.ui.widget;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupWidgetTest {

    @Test
    void openWithoutFocusManager() {
        PopupWidget popup = new PopupWidget();
        popup.setFocusManager(null);
        popup.open();
        assertTrue(popup.isVisible());
    }

    @Test
    void openWithFocusManager() {
        PopupWidget popup = new PopupWidget();
        FocusManager manager = new FocusManager();
        popup.setFocusManager(manager);
        popup.open();
        assertTrue(popup.isVisible());
    }

    @Test
    void dismissWithoutFocusManager() {
        PopupWidget popup = new PopupWidget();
        popup.setFocusManager(null);
        popup.open();
        popup.dismiss();
        assertFalse(popup.isVisible());
    }

    @Test
    void dismissWithFocusManager() {
        PopupWidget popup = new PopupWidget();
        FocusManager manager = new FocusManager();
        popup.setFocusManager(manager);
        popup.open();
        popup.dismiss();
        assertFalse(popup.isVisible());
    }

    @Test
    void setOnDismissCallbackExecutes() {
        PopupWidget popup = new PopupWidget();
        boolean[] called = {false};
        popup.setOnDismiss(() -> called[0] = true);
        popup.dismiss();
        assertTrue(called[0]);
    }

    @Test
    void dismissListenersRunAfterOnDismissInRegistrationOrder() {
        PopupWidget popup = new PopupWidget();
        List<String> order = new ArrayList<>();
        popup.setOnDismiss(() -> order.add("onDismiss"));
        popup.addDismissListener(() -> order.add("first"));
        popup.addDismissListener(() -> order.add("second"));

        popup.dismiss();

        assertEquals(List.of("onDismiss", "first", "second"), order);
    }

    @Test
    void dismissListenersRunOnEveryDismiss() {
        PopupWidget popup = new PopupWidget();
        int[] dismissals = {0};
        popup.addDismissListener(() -> dismissals[0]++);

        popup.dismiss();
        popup.dismiss();

        assertEquals(2, dismissals[0]);
    }

    @Test
    void isFullScreenReturnsTrue() {
        PopupWidget popup = new PopupWidget();
        assertTrue(popup.isFullScreen());
    }
}
