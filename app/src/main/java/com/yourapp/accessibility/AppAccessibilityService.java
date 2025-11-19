package com.yourapp.accessibility;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.util.Log;

import com.yourapp.engine.EngineBridge;

public class AppAccessibilityService extends AccessibilityService {

    private static final String TAG = "AppA11yService";
    private static AppAccessibilityService instance;

    public static AppAccessibilityService getInstance() {
        return instance;
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility service connected");
        // Initialize the EngineBridge to start listening for commands
        EngineBridge.initialize(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Sirf WhatsApp ke events yahan aayenge (config XML me packageNames="com.whatsapp")
        if (event == null) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            // Kuch devices / moments pe null normal hai (UI not ready)
            return;
        }

        // Saara actual action EngineBridge + Engine decide karega
        EngineBridge.onWhatsAppEvent(event, root, this);
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Accessibility service interrupted.");
        // Required override; yahan kuch nahi karna
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up the EngineBridge
        EngineBridge.destroy(this);
        instance = null;
        Log.d(TAG, "Accessibility service destroyed");
    }
}
