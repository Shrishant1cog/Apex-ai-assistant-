package com.aiassistant.mobile;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.Manifest;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "assistant_local_settings";
    private ApexKeyStore keyStore;
    private ApexAiClient aiClient;
    private static final int VOICE_REQUEST = 7201;
    private static final int MIC_PERMISSION_REQUEST = 7202;
    private SharedPreferences prefs;
    private LinearLayout page;
    private String currentPage = "Home";
    private EditText taskInput, targetInput, exclusionInput, chatInput;
    private TextToSpeech speech;
    private String voiceDestination = "chat";
    private TextView policyStatus;
    private final int ink = Color.rgb(38, 54, 48);
    private final int muted = Color.rgb(105, 119, 111);
    private final int accent = Color.rgb(15, 133, 111);
    private final int paper = Color.rgb(246, 250, 242);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        keyStore = new ApexKeyStore(this);
        aiClient = new ApexAiClient(keyStore);
        speech = new TextToSpeech(this, status -> { if (status == TextToSpeech.SUCCESS) speech.setLanguage(Locale.getDefault()); });
        showPage("Home");
    }

    private void startVoiceCapture() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is unavailable on this device.", Toast.LENGTH_LONG).show();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, MIC_PERMISSION_REQUEST);
            return;
        }
        launchVoiceRecognizer();
    }

    private void launchVoiceRecognizer() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak one action you want Apex to propose");
            intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
            startActivityForResult(intent, VOICE_REQUEST);
        } catch (Exception e) {
            Toast.makeText(this, "Could not start speech recognition.", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_REQUEST && resultCode == RESULT_OK && data != null) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (matches != null && !matches.isEmpty()) {
                String spoken = matches.get(0);
                if ("proposal".equals(voiceDestination) && taskInput != null) {
                    taskInput.setText(spoken); taskInput.setSelection(taskInput.length());
                    Toast.makeText(this, "Transcript added to proposal. Review before saving.", Toast.LENGTH_LONG).show();
                } else if (chatInput != null) {
                    chatInput.setText(spoken); chatInput.setSelection(chatInput.length());
                    sendChatMessage(spoken);
                } else Toast.makeText(this, "Open Chat and try voice again.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MIC_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) launchVoiceRecognizer();
            else Toast.makeText(this, "Microphone permission denied. You can type instead.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + .5f); }
    private TextView text(String s, int size, boolean bold, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(0, dp(4), 0, dp(4));
        return t;
    }
    private void gap(int h) {
        View v = new View(this);
        page.addView(v, new LinearLayout.LayoutParams(1, dp(h)));
    }
    private Button button(String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label); b.setAllCaps(false); b.setTextColor(ink); b.setTextSize(15);
        android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
        shape.setColor(Color.rgb(229, 245, 235)); shape.setCornerRadius(dp(14));
        b.setBackground(shape); b.setPadding(dp(12), dp(8), dp(12), dp(8));
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void card(String heading, String body) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.WHITE); bg.setCornerRadius(dp(18)); bg.setStroke(dp(1), Color.rgb(229, 237, 228));
        c.setBackground(bg);
        c.addView(text(heading, 17, true, ink));
        c.addView(text(body, 14, false, muted));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(10); page.addView(c, lp);
    }
    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextSize(15); e.setSingleLine(false);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8); page.addView(e, lp);
        return e;
    }
    private boolean stopped() { return prefs.getBoolean("kill", false); }
    private Set<String> exclusions() {
        return new HashSet<>(prefs.getStringSet("excluded", new HashSet<>()));
    }
    private JSONArray history() {
        try { return new JSONArray(prefs.getString("history", "[]")); }
        catch (JSONException e) { return new JSONArray(); }
    }
    private void addHistory(String line) {
        JSONArray h = history();
        h.put(System.currentTimeMillis() + " • " + line);
        while (h.length() > 100) h.remove(0);
        prefs.edit().putString("history", h.toString()).apply();
    }
    private void showPage(String name) {
        currentPage = name;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(paper);
        LinearLayout masthead=new LinearLayout(this); masthead.setGravity(Gravity.CENTER_VERTICAL); masthead.setPadding(dp(20),dp(13),dp(20),dp(13));
        LinearLayout brand=new LinearLayout(this); brand.setOrientation(LinearLayout.VERTICAL); brand.addView(text("APEX",22,true,ink)); brand.addView(text("YOUR PERSONAL AI",10,true,accent)); masthead.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        TextView live=text(keyStore.list().isEmpty()?"SETUP":"READY",10,true,accent); android.graphics.drawable.GradientDrawable pill=new android.graphics.drawable.GradientDrawable(); pill.setColor(Color.rgb(226,242,234)); pill.setCornerRadius(dp(20)); live.setPadding(dp(12),dp(7),dp(12),dp(7)); live.setBackground(pill); masthead.addView(live); root.addView(masthead);
        ScrollView scroll = new ScrollView(this);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(8), dp(20), dp(20));
        scroll.addView(page); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER); nav.setPadding(dp(5),dp(5),dp(5),dp(5)); nav.setBackgroundColor(Color.WHITE);
        String[] tabs = {"Home", "Chat", "Settings", "Policy", "History"};
        for (String tab : tabs) { TextView b = new TextView(this); b.setText(tab); b.setGravity(Gravity.CENTER); b.setTextSize(11); b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL)); b.setTextColor(tab.equals(name)?accent:muted); if(tab.equals(name)){android.graphics.drawable.GradientDrawable selected=new android.graphics.drawable.GradientDrawable();selected.setColor(Color.rgb(232,245,237));selected.setCornerRadius(dp(13));b.setBackground(selected);} b.setPadding(dp(3),dp(12),dp(3),dp(12)); b.setOnClickListener(v -> showPage(tab)); nav.addView(b,new LinearLayout.LayoutParams(0,dp(50),1)); }
        root.addView(nav);
        setContentView(root);

        if ("Home".equals(name)) home();
        else if ("Chat".equals(name)) chatPage();
        else if ("Proposal".equals(name)) proposal();
        else if ("Settings".equals(name)) settingsPage();
        else if ("Policy".equals(name)) policy();
        else historyPage();
    }
    private void home() {
        page.addView(text("Meet Apex.", 32, true, ink));
        page.addView(text("A calmer, smarter way to shape your next action.", 16, false, muted));
        gap(10);
        card("Your assistant, your choice", "Start with your voice or type a request. You stay in control at every step.");
        page.addView(button("🎙  Talk to Apex", () -> { voiceDestination = "chat"; showPage("Chat"); startVoiceCapture(); }));
        page.addView(text("Tap to talk. For wake-word listening, enable it separately when a supported wake engine is configured. Microphone capture is not secretly active.", 12, false, muted));
        page.addView(button("Open conversation", () -> showPage("Chat")));
        gap(14);
        card("Security state", stopped() ? "STOPPED • All proposal approvals are blocked." :
            "ACTIVE • Policy checks are on. Action execution is not implemented.");
        card("Voice commands", "Tap Talk to Apex and speak. Dictated messages are sent to the chat locally; an AI backend is still needed for answers.");
        card("AI connection", keyStore.list().isEmpty() ? "No provider configured. Open Settings to add one or more keys." : "Configured AI keys: " + keyStore.list().size() + ". Open Settings to manage providers and connection checks.");
        page.addView(button("AI provider settings", () -> showPage("Settings")));
        card("Action executor", "Disabled. Approvals are recorded locally only. No other app is opened or controlled.");
        page.addView(button(stopped() ? "Resume proposal review" : "Activate kill switch", () -> {
            prefs.edit().putBoolean("kill", !stopped()).apply();
            addHistory(stopped() ? "Kill switch deactivated" : "Kill switch activated");
            showPage("Home");
        }));
        gap(8);
        page.addView(button("Create a proposal", () -> showPage("Proposal")));
        page.addView(button("Review private-zone exclusions", () -> showPage("Policy")));
    }

    private void chatPage() {
        page.addView(text("Talk to Apex.", 30, true, ink));
        page.addView(text("A calmer place to ask, explore and learn.", 15, false, muted));
        gap(8);
        card("Assistant status", keyStore.list().isEmpty() ? "No AI key configured. Add a provider in Settings." : "Apex will attempt configured providers and keys with automatic failover.");
        chatInput = edit("Message Apex…");
        page.addView(button("🎙  Speak and send", () -> { voiceDestination = "chat"; startVoiceCapture(); }));
        page.addView(button("Send message", () -> {
            String message = chatInput == null ? "" : chatInput.getText().toString().trim();
            sendChatMessage(message);
        }));
        gap(8);
        page.addView(text("RECENT CONVERSATION", 12, true, muted));
        JSONArray h = history();
        boolean found = false;
        for (int i = h.length() - 1; i >= 0; i--) {
            String line = h.optString(i, "");
            if (line.startsWith("Chat message: ")) {
                card("You", line.substring("Chat message: ".length()));
                found = true;
                break;
            }
        }
        if (!found) card("Start here", "Ask anything by typing or using the microphone. Dictated messages send automatically.");
        page.addView(text("Wake phrase: not available in this build. Voice is activated only when you tap the microphone control.", 12, false, muted));
    }

    private void sendChatMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            Toast.makeText(this, "Say or type a message first.", Toast.LENGTH_SHORT).show();
            return;
        }
        message = message.trim();
        if (message.length() > 4000) {
            Toast.makeText(this, "Please keep messages under 4,000 characters.", Toast.LENGTH_SHORT).show();
            return;
        }
        addHistory("Chat message: " + message);
        if (chatInput != null) chatInput.setText("");
        // Store user turn locally, then request response asynchronously.
        addHistory("Apex is thinking…");
        showPage("Chat");
        aiClient.ask(message, new ApexAiClient.Callback() {
            @Override public void onSuccess(String answer, String providerLabel) {
                runOnUiThread(() -> { addHistory("Apex: " + answer); addHistory("AI route: " + providerLabel); if (speech != null) speech.speak(answer, TextToSpeech.QUEUE_FLUSH, null, "apex-reply"); showPage("Chat"); });
            }
            @Override public void onFailure(String reason) {
                runOnUiThread(() -> { addHistory("Apex response unavailable: " + reason); Toast.makeText(MainActivity.this, reason, Toast.LENGTH_LONG).show(); showPage("Chat"); });
            }
        });
    }

    @Override protected void onDestroy() {
        if (speech != null) { speech.stop(); speech.shutdown(); }
        super.onDestroy();
    }

    private void proposal() {
        page.addView(text("Make it happen.", 27, true, ink));
        page.addView(text("Write a request and identify its intended target. This creates a review record only.", 14, false, muted));
        gap(10);
        page.addView(text("What do you want the assistant to do?", 14, true, ink));
        taskInput = edit("Describe one specific action");
        page.addView(button("🎙  Speak your request", () -> { voiceDestination = "proposal"; startVoiceCapture(); }));
        page.addView(text("Voice creates a draft only. Check the transcript and target, then save for review. Android speech recognition may use your device/provider settings.", 12, false, muted));
        page.addView(text("Target package / identifier", 14, true, ink));
        targetInput = edit("Example: com.example.app");
        page.addView(button("Save proposal for review", () -> {
            String task = taskInput.getText().toString().trim();
            String target = targetInput.getText().toString().trim();
            if (task.isEmpty() || target.isEmpty()) {
                Toast.makeText(this, "Enter both a request and target.", Toast.LENGTH_SHORT).show(); return;
            }
            if (task.length() > 500 || target.length() > 200) {
                Toast.makeText(this, "Please shorten the request or target.", Toast.LENGTH_SHORT).show(); return;
            }
            JSONObject p = new JSONObject();
            try { p.put("task", task); p.put("target", target); p.put("time", System.currentTimeMillis()); }
            catch (JSONException ignored) {}
            prefs.edit().putString("proposal", p.toString()).putBoolean("has_proposal", true).apply();
            addHistory("Proposal created for target " + target);
            Toast.makeText(this, "Proposal saved locally.", Toast.LENGTH_SHORT).show();
            showPage("Proposal");
        }));
        gap(14);
        page.addView(text("Current proposal", 18, true, ink));
        if (prefs.getBoolean("has_proposal", false)) {
            try {
                JSONObject p = new JSONObject(prefs.getString("proposal", "{}"));
                String task = p.optString("task");
                String target = p.optString("target");
                card("Target: " + target, task);
                boolean blocked = stopped() || exclusions().contains(target);
                card("Policy result", stopped() ? "BLOCKED: kill switch is active." :
                    (blocked ? "BLOCKED: this target is excluded." : "Eligible for human review only. Execution remains disabled."));
                page.addView(button("Approve proposal (record only)", () -> {
                    if (stopped()) { addHistory("Approval denied by policy: kill switch active"); Toast.makeText(this, "Blocked: kill switch is active.", Toast.LENGTH_SHORT).show(); showPage("Proposal"); return; }
                    if (exclusions().contains(target)) { addHistory("Approval denied by policy for excluded target " + target); Toast.makeText(this, "Blocked: target is excluded.", Toast.LENGTH_SHORT).show(); showPage("Proposal"); return; }
                    addHistory("User approved proposal (not executed): " + target + " — " + task);
                    prefs.edit().putBoolean("has_proposal", false).apply();
                    Toast.makeText(this, "Approval recorded. No action was executed.", Toast.LENGTH_LONG).show();
                    showPage("History");
                }));
                page.addView(button("Reject proposal", () -> {
                    addHistory("User rejected proposal for " + target);
                    prefs.edit().putBoolean("has_proposal", false).apply();
                    showPage("History");
                }));
            } catch (JSONException e) { card("Proposal", "Saved proposal could not be read."); }
        } else {
            card("No pending proposal", "Create a request above. The assistant does not generate proposals until an AI provider is deliberately configured.");
        }
    }
    private void policy() {
        page.addView(text("Policy & privacy", 25, true, ink));
        page.addView(text("Rules are local to this app. Android does not let an ordinary app comprehensively hide or protect other apps.", 14, false, muted));
        gap(10);
        card("Fail-closed baseline", "No execution adapter is available. Approving a proposal never triggers device control.");
        card("Kill switch", stopped() ? "ON • Approvals are blocked." : "OFF • Policy checks still apply; execution is unavailable.");
        page.addView(button(stopped() ? "Turn kill switch off" : "Turn kill switch on", () -> {
            prefs.edit().putBoolean("kill", !stopped()).apply();
            addHistory(stopped() ? "Kill switch turned off" : "Kill switch turned on");
            showPage("Policy");
        }));
        gap(12);
        page.addView(text("Private-zone exclusions", 18, true, ink));
        page.addView(text("Add exact app package identifiers or target strings. Excluded targets are blocked by this app's review policy.", 14, false, muted));
        exclusionInput = edit("Package / target identifier");
        page.addView(button("Add exclusion", () -> {
            String value = exclusionInput.getText().toString().trim();
            if (value.isEmpty() || value.length() > 200) { Toast.makeText(this, "Enter a valid identifier.", Toast.LENGTH_SHORT).show(); return; }
            Set<String> set = exclusions(); set.add(value);
            prefs.edit().putStringSet("excluded", set).apply();
            addHistory("Private-zone exclusion added: " + value);
            showPage("Policy");
        }));
        page.addView(text("Excluded identifiers", 16, true, ink));
        Set<String> set = exclusions();
        if (set.isEmpty()) page.addView(text("No targets excluded yet.", 14, false, muted));
        for (String value : set) {
            LinearLayout line = new LinearLayout(this); line.setGravity(Gravity.CENTER_VERTICAL);
            line.addView(text(value, 13, false, ink), new LinearLayout.LayoutParams(0, -2, 1));
            line.addView(button("Remove", () -> {
                Set<String> copy = exclusions(); copy.remove(value);
                prefs.edit().putStringSet("excluded", copy).apply();
                addHistory("Private-zone exclusion removed: " + value);
                showPage("Policy");
            }));
            page.addView(line);
        }
        gap(10);
        card("Important limitation", "These exclusions only guard this assistant's proposal workflow. They cannot stop you or other apps from accessing excluded apps, their notifications, content or data.");
    }

    private void settingsPage() {
        page.addView(text("Apex AI settings", 28, true, ink));
        page.addView(text("Connect Apex by pasting your API key. Apex detects supported key formats and chooses a compatible default model automatically.", 14, false, muted));
        gap(8);
        card("Key safety", "Keys are encrypted with Android Keystore-backed AES-GCM and kept in private app storage. Keys are sent only to the detected provider. Use keys from the provider's official website.");
        page.addView(text("Connect your AI", 18, true, ink));
        EditText secret = edit("Paste your AI API key");
        secret.setSingleLine(true);
        secret.setInputType(129);
        page.addView(button("Connect key", () -> {
            String k = secret.getText().toString().trim();
            if (k.isEmpty()) { Toast.makeText(this, "Paste an API key first.", Toast.LENGTH_SHORT).show(); return; }
            String label, endpoint, model;
            if (k.startsWith("sk-or-")) {
                label="OpenRouter"; endpoint="https://openrouter.ai/api/v1/chat/completions"; model="openai/gpt-4o-mini";
            } else if (k.startsWith("gsk_")) {
                label="Groq"; endpoint="https://api.groq.com/openai/v1/chat/completions"; model="llama-3.3-70b-versatile";
            } else if (k.startsWith("sk-ant-")) {
                Toast.makeText(this, "This looks like an Anthropic key. Direct Anthropic API uses a different format. For now, connect through a compatible gateway such as OpenRouter using its key.", Toast.LENGTH_LONG).show(); return;
            } else if (k.startsWith("AIza")) {
                Toast.makeText(this, "This looks like a Google AI key. Automatic Gemini connection is not supported by this chat-completions build yet. Use an OpenRouter or Groq key, or configure a compatible gateway.", Toast.LENGTH_LONG).show(); return;
            } else if (k.startsWith("sk-")) {
                label="OpenAI"; endpoint="https://api.openai.com/v1/chat/completions"; model="gpt-4o-mini";
            } else {
                Toast.makeText(this, "Apex couldn't identify this key format. Use an OpenAI, Groq (gsk_), or OpenRouter (sk-or-) API key.", Toast.LENGTH_LONG).show(); return;
            }
            try {
                keyStore.add(label, endpoint, model, k);
                addHistory("AI key connected: " + label + " (default model " + model + ")");
                Toast.makeText(this, "Key saved securely. Testing provider…", Toast.LENGTH_SHORT).show();
                java.util.List<ApexKeyStore.Entry> saved=keyStore.list();
                ApexKeyStore.Entry newest=saved.get(saved.size()-1);
                aiClient.test(newest.id,new ApexAiClient.Callback(){
                    public void onSuccess(String msg,String route){runOnUiThread(()->{keyStore.status(newest.id,"Connected • "+model);addHistory("AI connection test passed: "+label);Toast.makeText(MainActivity.this,"Apex connected to "+label+".",Toast.LENGTH_LONG).show();showPage("Settings");});}
                    public void onFailure(String err){runOnUiThread(()->{keyStore.status(newest.id,"Needs attention • "+err);addHistory("AI connection test failed: "+label);Toast.makeText(MainActivity.this,"Key saved, but connection failed: "+err,Toast.LENGTH_LONG).show();showPage("Settings");});}
                });
            } catch(Exception e) { Toast.makeText(this,"Could not securely save key: "+e.getMessage(),Toast.LENGTH_LONG).show(); }
        }));
        page.addView(text("Provider and model are detected from recognizable key prefixes. The default model is selected automatically; some providers may change model availability. If the key format isn't recognized, Apex asks for a supported key type rather than guessing.",12,false,muted));
        gap(12); page.addView(text("Connected keys",18,true,ink));
        java.util.List<ApexKeyStore.Entry> entries=keyStore.list();
        if(entries.isEmpty()) page.addView(text("No keys connected yet.",14,false,muted));
        for(ApexKeyStore.Entry entry:entries){
            card(entry.label+" • "+entry.model, "Status: "+entry.status+"\nEndpoint: "+entry.endpoint+"\nKey: "+entry.masked);
            page.addView(button("Test connection • "+entry.label, () -> { Toast.makeText(this,"Testing "+entry.label+"…",Toast.LENGTH_SHORT).show(); aiClient.test(entry.id,new ApexAiClient.Callback(){public void onSuccess(String msg,String route){runOnUiThread(()->{keyStore.status(entry.id,"OK • tested");addHistory("Provider test passed: "+entry.label);Toast.makeText(MainActivity.this,"Connection succeeded.",Toast.LENGTH_LONG).show();showPage("Settings");});}public void onFailure(String err){runOnUiThread(()->{keyStore.status(entry.id,"Failed • "+err);addHistory("Provider test failed: "+entry.label);Toast.makeText(MainActivity.this,err,Toast.LENGTH_LONG).show();showPage("Settings");});}}); }));
            page.addView(button("Remove key • "+entry.label, () -> { keyStore.remove(entry.id); addHistory("Provider key removed: "+entry.label); showPage("Settings"); }));
        }
        gap(8); card("Failover policy", "Apex tries eligible keys in order. Authentication, rate-limit, quota and server errors can move to the next key/provider. Bad-request errors stop. Offline failures stop. Respect provider quotas; extra keys do not create extra entitlement.");
        card("Current limitation", "Automatic setup currently recognizes OpenAI, Groq, and OpenRouter key formats and assigns a compatible default model. It does not query a provider model catalog yet, and a recognized key can still lack access to the selected model. This app calls provider endpoints directly; treat device compromise as a risk.");
    }

    private void historyPage() {
        page.addView(text("Local activity history", 25, true, ink));
        page.addView(text("Stored on this device. Entries are simple records, not tamper-proof security auditing.", 14, false, muted));
        gap(10);
        JSONArray h = history();
        if (h.length() == 0) page.addView(text("Nothing recorded yet.", 15, false, muted));
        for (int i = h.length() - 1; i >= 0; i--) {
            card("Activity " + (h.length() - i), h.optString(i));
        }
        page.addView(button("Clear history", () -> {
            prefs.edit().putString("history", "[]").apply();
            showPage("History");
        }));
    }
}
