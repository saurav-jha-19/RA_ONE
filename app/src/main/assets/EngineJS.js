// EngineJS: single brain object
var EngineJS = (function () {
  // Private state
  let mode = "idle";        // "idle" | "save" | "send"
  let queue = [];           // [{ name, number, ... }, ...]
  let index = 0;            // current contact index
  let messageTemplate = "";
  let startedAt = null;     // session start time (ms)
  let sentCount = 0;        // how many messages sent in this session
  let maxMessagesPerDay = 200;   // configurable safe cap
  let maxWindowMs = 10 * 60 * 60 * 1000; // 10 hours window

  // Pause / resume flags (for tiny floating UI future)
  let paused = false;
  let stopRequested = false;

  // -------------- Helpers --------------

  function now() {
    return Date.now();
  }

  function randomDelay(baseMs, jitterMs) {
    // base + [0..jitter]
    return baseMs + Math.floor(Math.random() * jitterMs);
  }

  function log(msg) {
    // UI side pe logArea ya console
    if (typeof console !== "undefined") console.log("[EngineJS]", msg);
    if (typeof logArea !== "undefined") {
      logArea.textContent += "\n" + msg;
    }
    // Android log bridge (optional)
    if (window.Android && Android.log) {
      Android.log(msg);
    }
  }

  function checkWindowLimit() {
    if (!startedAt) return true;
    const elapsed = now() - startedAt;
    if (elapsed > maxWindowMs) {
      log("Session window (10h) over; stopping automation.");
      return false;
    }
    return true;
  }

  function checkDailyCap() {
    if (sentCount >= maxMessagesPerDay) {
      log("Max messages per day reached (" + maxMessagesPerDay + "); stopping.");
      return false;
    }
    return true;
  }

  function currentContact() {
    return queue[index] || null;
  }

  function personalizeMessage(contact, template) {
    if (!template) return "";
    let msg = template;
    if (contact) {
      msg = msg.replace(/\{name\}/gi, contact.name || "");
      msg = msg.replace(/\{number\}/gi, contact.number || "");
    }
    return msg;
  }

  // -------------- Public API --------------

  function startSave(contacts) {
    if (!Array.isArray(contacts) || contacts.length === 0) {
      alert("No contacts to save.");
      return;
    }
    mode = "save";
    queue = contacts.slice();
    index = 0;
    paused = false;
    stopRequested = false;
    startedAt = startedAt || now(); // first time only
    log("Starting SAVE for " + queue.length + " contacts.");

    // For now: batch handover to Android
    if (window.Android && Android.saveContacts) {
      Android.saveContacts(JSON.stringify(queue));
      log("Sent SAVE batch to Android (accessibility will handle contact saving flow).");
    } else {
      alert("Open inside Android app to auto-save contacts.");
    }
  }

  function startSend(contacts, template) {
    if (!Array.isArray(contacts) || contacts.length === 0) {
      alert("No contacts selected.");
      return;
    }
    if (!template || template.trim() === "") {
      alert("Please enter a message template.");
      return;
    }

    mode = "send";
    queue = contacts.slice();
    index = 0;
    messageTemplate = template;
    paused = false;
    stopRequested = false;
    sentCount = 0;
    startedAt = startedAt || now();

    log("Starting SEND for " + queue.length + " contacts.");
    log("Daily cap: " + maxMessagesPerDay + " messages; window: 10 hours.");

    // For Android bridge: full batch
    if (window.Android && Android.startSend) {
      Android.startSend(JSON.stringify(queue), messageTemplate);
      log("Sent SEND batch to Android (engine + accessibility will drive per-contact flow).");
    } else {
      // Fallback: in-browser simulation (existing sendNextMessage)
      log("Android bridge not available; using browser-only simulation.");
      if (typeof startBrowserSimulation === "function") {
        startBrowserSimulation(queue, template);
      }
    }
  }

  function pause() {
    paused = true;
    log("Engine paused by user.");
  }

  function resume() {
    if (!paused) return;
    paused = false;
    log("Engine resumed.");
    // future: continue per-contact flow if internal state machine is here
  }

  function stop() {
    stopRequested = true;
    paused = false;
    mode = "idle";
    log("Engine stop requested; current session ended.");
  }

  // Expose config setters (later UI se tune kar सकते ho)
  function setMaxMessagesPerDay(n) {
    maxMessagesPerDay = n || maxMessagesPerDay;
  }

  function setMaxWindowHours(h) {
    if (!h) return;
    maxWindowMs = h * 60 * 60 * 1000;
  }

  // For future: EngineBridge agar JS ko ping kare to yahan se state le sake
  function getCurrentMode() {
    return mode;
  }

  function getCurrentIndex() {
    return index;
  }

  function getCurrentStep() {
    // Future: "SAVE_CONTACT" / "OPEN_CHAT" / "TYPE_MESSAGE" / "PRESS_SEND"
    // abhi placeholder
    return "IDLE";
  }

  function getPendingMessage() {
    const c = currentContact();
    return personalizeMessage(c, messageTemplate);
  }

  return {
    // main actions
    startSave,
    startSend,
    pause,
    resume,
    stop,

    // config
    setMaxMessagesPerDay,
    setMaxWindowHours,

    // status (for EngineBridge / tiny UI)
    getCurrentMode,
    getCurrentIndex,
    getCurrentStep,
    getPendingMessage
  };
})();