package com.skhabibulla.hrassistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Locale;

/** Runs only direct user-issued commands. Never records or uploads window content. */
public class AssistantAccessibilityService extends AccessibilityService {
    private static AssistantAccessibilityService active;

    public static boolean isReady() { return active != null; }

    public static String command(String action, String target) {
        AssistantAccessibilityService svc = active;
        if (svc == null) return "Enable HR HABIB AI Control in Android Accessibility Settings first.";
        switch (action) {
            case "back":
                return svc.performGlobalAction(GLOBAL_ACTION_BACK) ? "Back" : "Back command failed";
            case "home":
                return svc.performGlobalAction(GLOBAL_ACTION_HOME) ? "Home" : "Home command failed";
            case "recents":
                return svc.performGlobalAction(GLOBAL_ACTION_RECENTS) ? "Recent apps" : "Recent apps command failed";
            case "scrollDown": return svc.scroll(true) ? "Scrolled down" : "No scrollable screen found";
            case "scrollUp": return svc.scroll(false) ? "Scrolled up" : "No scrollable screen found";
            case "tap": return svc.tapVisibleText(target) ? "Tapped: " + target : "Visible button not found: " + target;
            default: return "Unknown screen command";
        }
    }

    @Override public void onServiceConnected() {
        super.onServiceConnected();
        active = this;
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) { /* no passive collection */ }
    @Override public void onInterrupt() { }

    @Override public void onDestroy() {
        if (active == this) active = null;
        super.onDestroy();
    }

    private boolean scroll(boolean down) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 250) {
            AccessibilityNodeInfo node = queue.poll();
            int action = down ? AccessibilityNodeInfo.ACTION_SCROLL_FORWARD : AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD;
            if (node.isVisibleToUser() && node.isScrollable() && node.performAction(action)) return true;
            for (int i=0; i<node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) queue.add(child);
            }
        }
        return false;
    }

    private boolean tapVisibleText(String requested) {
        if (requested == null || requested.trim().isEmpty()) return false;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(requested.trim());
        if (nodes == null) return false;
        String want = requested.trim().toLowerCase(Locale.ROOT);
        for (AccessibilityNodeInfo node : nodes) {
            if (!node.isVisibleToUser()) continue;
            String text = node.getText() != null ? node.getText().toString() : "";
            String description = node.getContentDescription() != null ? node.getContentDescription().toString() : "";
            if (!text.toLowerCase(Locale.ROOT).equals(want) && !description.toLowerCase(Locale.ROOT).equals(want)) continue;
            AccessibilityNodeInfo clickable = node;
            for (int i=0; i<5 && clickable != null; i++, clickable=clickable.getParent()) {
                if (clickable.isEnabled() && clickable.isClickable() && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true;
            }
        }
        return false;
    }
}
