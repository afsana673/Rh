package com.skhabibulla.hrassistant;

import android.content.Intent;
import android.net.Uri;
import android.provider.AlarmClock;
import android.provider.Settings;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Explicit, local voice/text commands. No other app's proprietary behavior is reused. */
final class CommandEngine {
    private CommandEngine() {}

    static String execute(MainActivity app, String raw) {
        String cmd = raw == null ? "" : raw.trim();
        if (cmd.isEmpty()) return "Enter a command or press the microphone.";
        String lower = cmd.toLowerCase(Locale.ROOT).trim();

        if (starts(lower, "ai ", "ask ", "এআই ", "एआई ")) {
            int i=cmd.indexOf(' ');
            app.askAI(i < 0 ? "" : cmd.substring(i + 1).trim());
            return "";
        }
        if (starts(lower, "macro ", "automation ")) return app.runNamedMacro(tail(cmd));
        if (eq(lower, "macros", "automations")) { app.showMacros(); return "Choose a macro to run or edit."; }
        if (eq(lower, "help", "commands", "সাহায্য", "मदद")) return
                "Try: open WhatsApp | search cats | youtube music | call 1234567890 | sms 1234567890 hello | " +
                "note buy milk | notes | alarm 07:30 | flash on | flash off | home | back | scroll down | tap Settings | macro morning | ai hello";
        if (eq(lower, "hello", "hi", "hey", "হ্যালো", "হাই", "नमस्ते"))
            return "Hello! I am HR HABIB AI, your free voice assistant. 🍘";
        if (eq(lower, "time", "what time is it", "সময়", "সময়", "समय"))
            return new java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new java.util.Date());
        if (eq(lower, "date", "today date", "তারিখ", "तारीख"))
            return new java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new java.util.Date());

        if (eq(lower, "accessibility", "enable accessibility")) {
            app.openAccessibilitySettings();
            return "Enable HR HABIB AI Control manually to use screen commands.";
        }
        if (eq(lower, "home", "হোম", "होम")) return AssistantAccessibilityService.command("home", "");
        if (eq(lower, "back", "পিছনে", "पीछे")) return AssistantAccessibilityService.command("back", "");
        if (eq(lower, "recent apps", "recents")) return AssistantAccessibilityService.command("recents", "");
        if (eq(lower, "scroll down", "নিচে স্ক্রল", "नीचे स्क्रॉल")) return AssistantAccessibilityService.command("scrollDown", "");
        if (eq(lower, "scroll up", "উপরে স্ক্রল", "ऊपर स्क्रॉल")) return AssistantAccessibilityService.command("scrollUp", "");
        if (starts(lower, "tap ", "click ", "ট্যাপ ", "टैप "))
            return AssistantAccessibilityService.command("tap", tail(cmd));

        if (eq(lower, "flash on", "torch on", "light on", "টর্চ অন", "टॉर्च ऑन")) {
            app.setFlashlight(true); return "";
        }
        if (eq(lower, "flash off", "torch off", "light off", "টর্চ অফ", "टॉर्च ऑफ")) {
            app.setFlashlight(false); return "";
        }
        if (starts(lower, "note ", "save note ", "নোট ", "नोट ")) {
            String note = lower.startsWith("save note ") ? cmd.substring(10) : tail(cmd);
            app.addNote(note);
            return "Note saved on this device.";
        }
        if (eq(lower, "notes", "show notes", "আমার নোট", "मेरे नोट")) {
            app.showNotes(); return "Your local notes are open.";
        }
        if (starts(lower, "alarm ", "অ্যালার্ম ", "अलार्म ")) {
            Matcher match = Pattern.compile("(\\d{1,2})[:.](\\d{2})").matcher(cmd);
            if (!match.find()) return "Use a 24-hour time, e.g. alarm 07:30";
            int h = Integer.parseInt(match.group(1)), m = Integer.parseInt(match.group(2));
            if (h > 23 || m > 59) return "Invalid alarm time.";
            Intent alarm = new Intent(AlarmClock.ACTION_SET_ALARM);
            alarm.putExtra(AlarmClock.EXTRA_HOUR, h);
            alarm.putExtra(AlarmClock.EXTRA_MINUTES, m);
            alarm.putExtra(AlarmClock.EXTRA_MESSAGE, "HR HABIB AI reminder");
            return app.launch(alarm, "Alarm app unavailable") ? "Opening alarm setup" : "Alarm app unavailable";
        }

        if (starts(lower, "open ", "launch ", "খোলো ", "খুলে দাও ", "ओपन ", "खोलो ")) {
            String appName = tail(cmd);
            if (lower.startsWith("খুলে দাও ")) appName = cmd.substring("খুলে দাও ".length());
            if (eq(appName.toLowerCase(Locale.ROOT), "settings", "সেটিংস", "सेटिंग्स")) {
                app.launch(new Intent(Settings.ACTION_SETTINGS), "Settings unavailable");
                return "Opening Settings";
            }
            return app.openApp(appName);
        }
        if (lower.endsWith(" kholo") || lower.endsWith(" khol do")) {
            String appName = cmd.replaceFirst("(?i)\\s+(kholo|khol do)$", "").trim();
            return app.openApp(appName);
        }
        if (starts(lower, "search ", "google ", "গুগল ", "সার্চ ", "सर्च ")) {
            app.searchWeb(tail(cmd)); return "Opening web search";
        }
        if (starts(lower, "youtube ", "ইউটিউব ", "यूट्यूब ")) {
            app.searchYouTube(tail(cmd)); return "Opening YouTube search";
        }
        if (starts(lower, "call ", "dial ", "কল ", "कॉल ")) {
            return app.dial(tail(cmd));
        }
        if (starts(lower, "sms ", "message ", "মেসেজ ", "मैसेज ")) {
            return app.composeSms(tail(cmd));
        }
        if (eq(lower, "privacy")) return "No app licence, analytics or telemetry. Notes are local. AI text is sent only when you choose Ask AI.";
        return "I didn't recognize that local command. Say 'help', or use 'ai your question' after configuring an optional AI provider.";
    }
    private static boolean starts(String s, String... prefixes) {
        for (String prefix : prefixes) if (s.startsWith(prefix)) return true;
        return false;
    }
    private static boolean eq(String s, String... values) {
        for (String value : values) if (s.equals(value)) return true;
        return false;
    }
    private static String tail(String s) {
        int n = s.indexOf(' ');
        return n < 0 ? "" : s.substring(n+1).trim();
    }
}
