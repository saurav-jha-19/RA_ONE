package com.yourapp.util;

import android.os.Bundle;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AccessibilityNodeUtils {

    /**
     * Finds the first node that contains the given text (case-insensitive).
     */
    public static AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo rootNode, String text) {
        if (rootNode == null || text == null) return null;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        // Prefer exact matches
        for (AccessibilityNodeInfo node : nodes) {
            if (node != null && node.getText() != null && node.getText().toString().equalsIgnoreCase(text)) {
                return node;
            }
        }
        // Fallback for partial matches if exact match fails
        if (!nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    /**
     * Finds the first node with a matching View ID.
     */
    public static AccessibilityNodeInfo findNodeByViewId(AccessibilityNodeInfo rootNode, String viewId) {
        if (rootNode == null || viewId == null) return null;
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId);
        if (!nodes.isEmpty()) {
            return nodes.get(0);
        }
        return null;
    }

    /**
     * Finds a node by its content description using BFS.
     */
    public static AccessibilityNodeInfo findNodeByContentDescription(AccessibilityNodeInfo rootNode, String description) {
        if (rootNode == null || description == null) return null;

        Queue<AccessibilityNodeInfo> queue = new LinkedList<>();
        queue.add(rootNode);

        while (!queue.isEmpty()) {
            AccessibilityNodeInfo current = queue.poll();
            if (current == null) continue;

            if (current.getContentDescription() != null && description.equalsIgnoreCase(current.getContentDescription().toString())) {
                return current;
            }

            for (int i = 0; i < current.getChildCount(); i++) {
                AccessibilityNodeInfo child = current.getChild(i);
                if (child != null) {
                    queue.add(child);
                }
            }
        }
        return null;
    }


    /**
     * Performs a click action on the given node. If the node is not clickable,
     * it travels up the hierarchy to find a clickable parent.
     * Returns true if the action was successful.
     */
    public static boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }
        // If not clickable, try clicking its parent
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Pastes the given text into an editable node.
     * Returns true if the action was successful.
     */
    public static boolean performPaste(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null) return false;
        
        // For some systems, we need to focus before pasting
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);

        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
    }
}
