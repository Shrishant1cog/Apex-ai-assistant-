package com.aiassistant.mobile;

import android.app.Activity;
import android.content.SharedPreferences;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFS = "assistant_local_settings";
    private SharedPreferences prefs;
    private LinearLayout page;
    private String currentPage = "Home";
    private EditText taskInput, targetInput, exclusionInput;
    private TextView policyStatus;
    private final int ink = Color.rgb(24, 33, 43);
    private final int muted = Color.rgb(95, 108, 121);
    private final int accent = Color.rgb(30, 86, 112);
    private final int paper = Color.rgb(247, 249, 250);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        showPage("Home");
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
        b.setText(label); b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void card(String heading, String body) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16), dp(12), dp(16), dp(12));
        c.setBackgroundColor(Color.WHITE);
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
        TextView top = text("AI MOBILE ASSISTANT", 12, true, accent);
        top.setPadding(dp(20), dp(16), dp(20), dp(8)); root.addView(top);
        ScrollView scroll = new ScrollView(this);
        page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(8), dp(20), dp(20));
        scroll.addView(page); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout nav = new LinearLayout(this); nav.setBackgroundColor(Color.WHITE);
        String[] tabs = {"Home", "Proposal", "Policy", "History"};
        for (String tab : tabs) {
            Button b = new Button(this); b.setText(tab); b.setAllCaps(false); b.setTextSize(11);
            b.setOnClickListener(v -> showPage(tab));
            nav.addView(b, new LinearLayout.LayoutParams(0, dp(52), 1));
        }
        root.addView(nav);
        setContentView(root);

        if ("Home".equals(name)) home();
        else if ("Proposal".equals(name)) proposal();
        else if ("Policy".equals(name)) policy();
        else historyPage();
    }
    private void home() {
        page.addView(text("Your phone. Your rules.", 26, true, ink));
        page.addView(text("A user-controlled assistant shell. Nothing runs silently.", 15, false, muted));
        gap(14);
        card("Security state", stopped() ? "STOPPED • All proposal approvals are blocked." :
            "ACTIVE • Policy checks are on. Action execution is not implemented.");
        card("Special permissions", "None requested. No Accessibility, notification access, microphone, contacts or location permission.");
        card("AI connection", "Not configured. This version does not send your data to an AI provider.");
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
    private void proposal() {
        page.addView(text("Action proposal", 25, true, ink));
        page.addView(text("Write a request and identify its intended target. This creates a review record only.", 14, false, muted));
        gap(10);
        page.addView(text("What do you want the assistant to do?", 14, true, ink));
        taskInput = edit("Describe one specific action");
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
