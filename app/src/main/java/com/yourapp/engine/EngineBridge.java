package com.yourapp.engine;

import android.accessibilityservice.AccessibilityService;
import static android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.yourapp.model.Contact;
import com.yourapp.util.AccessibilityNodeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Queue;

public class EngineBridge {

    private static final String TAG = "EngineBridge";
    private static CommandReceiver commandReceiver;
    private static final Handler handler = new Handler(Looper.getMainLooper());

    // Engine State
    private static String currentMode = "idle";
    private static String currentStep = "idle";

    private static Queue<Contact> contactsQueue = new LinkedList<>();
    private static String messageTemplate = "";
    private static Contact currentContact = null;

    public static void onWhatsAppEvent(AccessibilityEvent event, AccessibilityNodeInfo root, AccessibilityService service) {
        if (root == null || !currentMode.equals("send_messages")) return;

        // Only process events when the window content changes, to avoid rapid-fire processing.
        if (event.getEventType() != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            return;
        }

        Log.d(TAG, "Processing event. Current step: " + currentStep);

        // Simple state machine for sending a message
        switch (currentStep) {
            case "start_sending":
                Log.d(TAG, "Step: start_sending. Finding Search button.");
                // Find the search icon (often described as "Search")
                AccessibilityNodeInfo searchButton = AccessibilityNodeUtils.findNodeByContentDescription(root, "Search");
                if (AccessibilityNodeUtils.performClick(searchButton)) {
                    currentStep = "typing_number";
                }
                break;

            case "typing_number":
                Log.d(TAG, "Step: typing_number. Finding search input field.");
                // The search input field in WhatsApp often has the resource ID "com.whatsapp:id/search_input"
                AccessibilityNodeInfo searchInput = AccessibilityNodeUtils.findNodeByViewId(root, "com.whatsapp:id/search_src_text");
                if (searchInput != null) {
                     // Format number: remove + and spaces, etc. (basic example)
                    String cleanNumber = currentContact.number.replaceAll("[\\s-]+", "");
                    if (AccessibilityNodeUtils.performPaste(searchInput, cleanNumber)) {
                        currentStep = "clicking_chat";
                    }
                }
                break;

            case "clicking_chat":
                Log.d(TAG, "Step: clicking_chat. Finding chat result for " + currentContact.number);
                // After searching, the chat result should be on screen. We click the first one.
                // The chat result might be identifiable by text or a more complex structure.
                // Let's try to find a node with the contact's number.
                 AccessibilityNodeInfo chatResult = AccessibilityNodeUtils.findNodeByText(root, currentContact.number);
                if (AccessibilityNodeUtils.performClick(chatResult)) {
                    currentStep = "typing_message";
                }
                break;

            case "typing_message":
                Log.d(TAG, "Step: typing_message. Finding message input box.");
                // WhatsApp message box resource ID is typically "com.whatsapp:id/entry"
                AccessibilityNodeInfo messageBox = AccessibilityNodeUtils.findNodeByViewId(root, "com.whatsapp:id/entry");
                if (messageBox != null) {
                    String personalizedMessage = messageTemplate.replace("{name}", currentContact.name);
                    if (AccessibilityNodeUtils.performPaste(messageBox, personalizedMessage)) {
                        currentStep = "clicking_send";
                    }
                }
                break;

            case "clicking_send":
                Log.d(TAG, "Step: clicking_send. Finding Send button.");
                // The send button is usually described as "Send"
                AccessibilityNodeInfo sendButton = AccessibilityNodeUtils.findNodeByContentDescription(root, "Send");
                if (AccessibilityNodeUtils.performClick(sendButton)) {
                    // Message sent! Now go back to get ready for the next one.
                    currentStep = "going_back";
                    // Use a delay before going back to allow message to send
                    handler.postDelayed(() -> service.performGlobalAction(GLOBAL_ACTION_BACK), 1000);
                }
                break;

            case "going_back":
                 Log.d(TAG, "Step: going_back. Checking if we are on main screen.");
                // After pressing back, we should land on the main screen.
                // A reliable way to check is to look for the "Search" button again.
                 if (AccessibilityNodeUtils.findNodeByContentDescription(root, "Search") != null) {
                    Log.d(TAG, "Back on the main screen.");
                    // Ready for the next contact
                    currentStep = "idle";
                    checkAndProcessNextContact(); // Process next in queue
                 }
                break;
        }
    }

    private static void checkAndProcessNextContact() {
        if (currentMode.equals("send_messages") && currentStep.equals("idle")) {
            if (!contactsQueue.isEmpty()) {
                currentContact = contactsQueue.poll();
                Log.d(TAG, "Starting process for next contact: " + currentContact.name);
                currentStep = "start_sending"; // Kick off the state machine for the new contact
                // We need to trigger an event to get the state machine running again.
                // A small delay helps.
                handler.postDelayed(() -> {
                    Log.d(TAG, "Triggering new event cycle.");
                     // The next AccessibilityEvent will pick up the new step.
                }, 500);

            } else {
                Log.d(TAG, "Contact queue is empty. Automation finished.");
                currentMode = "idle"; // All done!
            }
        }
    }

    public static class CommandReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null || !currentMode.equals("idle")) return;

            if ("com.yourapp.SEND_MESSAGES".equals(action)) {
                messageTemplate = intent.getStringExtra("msg_template");
                try {
                    contactsQueue.clear();
                    JSONArray jsonArray = new JSONArray(intent.getStringExtra("contacts_json"));
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject jsonObj = jsonArray.getJSONObject(i);
                        Contact contact = new Contact();
                        contact.name = jsonObj.optString("name", "");
                        contact.number = jsonObj.getString("number");
                        contactsQueue.add(contact);
                    }

                    if (!contactsQueue.isEmpty()) {
                        Log.d(TAG, "SEND_MESSAGES command received. Starting automation.");
                        currentMode = "send_messages";
                        currentStep = "idle";
                        checkAndProcessNextContact(); // Start with the first contact
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to parse contacts JSON", e);
                }
            }
        }
    }

    public static void initialize(Context context) {
        if (commandReceiver == null) {
            commandReceiver = new CommandReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("com.yourapp.SAVE_CONTACTS");
            filter.addAction("com.yourapp.SEND_MESSAGES");
            context.registerReceiver(commandReceiver, filter);
        }
    }

    public static void destroy(Context context) {
        if (commandReceiver != null) {
            context.unregisterReceiver(commandReceiver);
            commandReceiver = null;
            contactsQueue.clear();
            currentMode = "idle";
            currentStep = "idle";
        }
    }
}
