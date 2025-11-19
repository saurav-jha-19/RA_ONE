package com.yourapp.bridge;

import android.content.Context;
import android.content.Intent;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

public class WebAppInterface {

    private final Context ctx;

    public WebAppInterface(Context c) {
        this.ctx = c;
    }

    // UI se: Android.saveContacts(JSON.stringify(selectedContacts))
    @JavascriptInterface
    public void saveContacts(String contactsJson) {
        // optional: quick toast for debug
        Toast.makeText(ctx, "Saving contacts via Accessibility...", Toast.LENGTH_SHORT).show();

        Intent i = new Intent("com.yourapp.SAVE_CONTACTS");
        i.putExtra("contacts_json", contactsJson);
        ctx.sendBroadcast(i);
    }

    // UI se: Android.startSend(JSON.stringify(selectedContacts), messageTemplate)
    @JavascriptInterface
    public void startSend(String contactsJson, String messageTemplate) {
        Toast.makeText(ctx, "Starting WhatsApp send flow...", Toast.LENGTH_SHORT).show();

        Intent i = new Intent("com.yourapp.SEND_MESSAGES");
        i.putExtra("contacts_json", contactsJson);
        i.putExtra("msg_template", messageTemplate);
        ctx.sendBroadcast(i);
    }

    // UI se: Android.log("...")  (optional debug)
    @JavascriptInterface
    public void log(String msg) {
        android.util.Log.d("WebAppBridge", msg);
    }
}