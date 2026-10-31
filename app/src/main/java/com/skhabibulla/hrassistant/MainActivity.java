package com.skhabibulla.hrassistant;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Original, licence-free Android app. UI and voice commands are user initiated. */
public class MainActivity extends Activity {
    private static final int REQ_MIC = 710, REQ_VOICE = 711, REQ_CAMERA = 712;
    private static final int BG = 0xFF071225, PANEL = 0xFF13233D, BLUE = 0xFF1D71E8, TEAL = 0xFF09898E;
    private static final String[] LANG = {"en-IN", "hi-IN", "bn-IN"};
    private static final String[] LANG_NAMES = {"English", "हिन्दी", "বাংলা"};
    private final String PREF = "hr_assistant";
    private EditText input;
    private TextView output, subheading;
    private Button languageButton, speakButton;
    private SharedPreferences preferences;
    private TextToSpeech tts;
    private boolean ttsReady = false;
    private boolean requestedTorch = false;
    private final Handler macroHandler = new Handler(Looper.getMainLooper());
    private int macroSequence = 0;
    private int language = 0;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        preferences = getSharedPreferences(PREF, MODE_PRIVATE);
        language = Math.max(0, Math.min(2, preferences.getInt("language", 0)));
        tts = new TextToSpeech(this, status -> {
            ttsReady = status == TextToSpeech.SUCCESS;
            if (ttsReady) updateVoiceLanguage();
        });
        buildLayout();
    }

    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable shape(int color, int radius, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius));
        if (stroke != 0) d.setStroke(dp(1), stroke);
        return d;
    }
    private TextView text(String content, int sp, int color, boolean bold) {
        TextView t=new TextView(this);
        t.setText(content); t.setTextSize(sp); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setLineSpacing(dp(3),1f);
        return t;
    }
    private Button button(String title, int color) {
        Button b = new Button(this);
        b.setText(title); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false);
        b.setBackground(shape(color, 14, 0));
        b.setPadding(dp(10),dp(10),dp(10),dp(10));
        b.setMinHeight(dp(49));
        return b;
    }
    private void gap(LinearLayout layout, int px) {
        View v=new View(this);layout.addView(v, new LinearLayout.LayoutParams(1,dp(px)));
    }
    private LinearLayout vertical() { LinearLayout l=new LinearLayout(this); l.setOrientation(LinearLayout.VERTICAL); return l; }
    private void rowButton(LinearLayout row, Button b) {
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(53),1f);p.setMargins(dp(3),0,dp(3),0);row.addView(b,p);
    }
    private void buildLayout() {
        ScrollView sv=new ScrollView(this);sv.setFillViewport(true);sv.setBackgroundColor(BG);
        LinearLayout root=vertical();root.setPadding(dp(20),dp(24),dp(20),dp(28));sv.addView(root);
        TextView brand=text("🍘  HR HABIB AI", 26, 0xFFF5FAFF, true);root.addView(brand);
        gap(root,4);
        subheading=text("FREE EDITION  •  SK HABIBULLA",11,0xFF6DDAF8,true);root.addView(subheading);
        gap(root,22);

        LinearLayout hero=vertical();hero.setPadding(dp(20),dp(21),dp(20),dp(20));
        GradientDrawable grad=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF194D96,0xFF143449,0xFF10323A});
        grad.setCornerRadius(dp(22));hero.setBackground(grad);root.addView(hero);
        hero.addView(text("Your voice. Your assistant.",22,Color.WHITE,true));
        gap(hero,8);
        hero.addView(text("Voice commands • Android actions • AI chat (optional) • No licence key",13,0xFFDBEAF7,false));
        gap(hero,14);
        LinearLayout h= new LinearLayout(this);h.setOrientation(LinearLayout.HORIZONTAL);hero.addView(h);
        Button mic = button("🎤  Speak now",TEAL);rowButton(h,mic);mic.setOnClickListener(v->startVoice());
        languageButton=button("🌐  "+LANG_NAMES[language],BLUE);rowButton(h,languageButton);
        languageButton.setOnClickListener(v->{language=(language+1)%LANG.length;preferences.edit().putInt("language",language).apply();languageButton.setText("🌐  "+LANG_NAMES[language]);updateVoiceLanguage();});
        gap(root,22);

        root.addView(text("COMMAND CENTER",13,0xFF9DBBD1,true));gap(root,9);
        input=new EditText(this);input.setSingleLine(false);input.setMinLines(2);input.setMaxLines(4);
        input.setHint("Try: open WhatsApp / search cats / note hello");input.setHintTextColor(0xFF8599AC);
        input.setTextColor(Color.WHITE);input.setTextSize(15);input.setPadding(dp(15),dp(13),dp(15),dp(13));
        input.setBackground(shape(PANEL,14,0xFF29465E));root.addView(input);
        gap(root,10);
        LinearLayout actions=new LinearLayout(this);root.addView(actions);
        Button run=button("▶  Run command",BLUE);rowButton(actions,run);
        run.setOnClickListener(v->runCommand(input.getText().toString()));
        Button ask=button("✦  Ask AI",TEAL);rowButton(actions,ask);
        ask.setOnClickListener(v->askAI(input.getText().toString().trim()));
        gap(root,20);
        root.addView(text("QUICK ACTIONS",13,0xFF9DBBD1,true));gap(root,9);
        quickRow(root,"📱  Open apps","open WhatsApp","🔎  Google","search technology");
        gap(root,7);
        quickRow(root,"📝  My notes","notes","⏰  Set alarm","alarm 07:30");
        gap(root,7);
        quickRow(root,"🔦  Torch on","flash on","🔦  Torch off","flash off");
        gap(root,20);
        output=text("Ready. Tap Speak or type a command.",14,0xFFF3F8FF,false);
        output.setPadding(dp(16),dp(16),dp(16),dp(16));output.setBackground(shape(PANEL,16,0xFF2C465D));
        output.setTextIsSelectable(true);root.addView(output);
        gap(root,22);
        root.addView(text("SETTINGS & PRIVACY",13,0xFF9DBBD1,true));gap(root,9);
        LinearLayout settings=new LinearLayout(this);root.addView(settings);
        Button ai=button("⚙  AI Settings",0xFF264361);rowButton(settings,ai);ai.setOnClickListener(v->showAiSettings());
        Button access=button("♿  Screen Control",0xFF264361);rowButton(settings,access);access.setOnClickListener(v->openAccessibilitySettings());
        gap(root,8);
        Button macroButton=button("⚡  My Macros • Custom Automations",0xFF264361);
        root.addView(macroButton, new LinearLayout.LayoutParams(-1,dp(53)));
        macroButton.setOnClickListener(v->showMacros());
        gap(root,8);
        LinearLayout settings2=new LinearLayout(this);root.addView(settings2);
        speakButton=button(preferences.getBoolean("voice",true)?"🔊  Voice ON":"🔇  Voice OFF",0xFF264361);
        rowButton(settings2,speakButton);
        speakButton.setOnClickListener(v->{boolean value=!preferences.getBoolean("voice",true);preferences.edit().putBoolean("voice",value).apply();speakButton.setText(value?"🔊  Voice ON":"🔇  Voice OFF");if (!value && tts!=null)tts.stop();});
        Button privacy=button("🔒  Privacy",0xFF264361);rowButton(settings2,privacy);
        privacy.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Privacy")
                .setMessage("Your notes stay on this device. No telemetry or account is required. Voice recognition may use the Android speech provider. An AI question is sent to your configured provider only when you tap Ask AI. Screen control is optional and only runs your explicit commands.")
                .setPositiveButton("OK",null).show());
        gap(root,19);
        TextView footer=text("Independent app • No Maya code • No licence checks\n© SK HABIBULLA",11,0xFF8CA5BB,false);
        footer.setGravity(Gravity.CENTER);root.addView(footer);
        setContentView(sv);
    }
    private void quickRow(LinearLayout root, String titleA, String commandA, String titleB, String commandB) {
        LinearLayout row=new LinearLayout(this);root.addView(row);
        Button a=button(titleA,0xFF223955),b=button(titleB,0xFF223955);
        rowButton(row,a);rowButton(row,b);
        a.setOnClickListener(v->runCommand(commandA));b.setOnClickListener(v->runCommand(commandB));
    }
    private void updateVoiceLanguage() {
        if (ttsReady && tts != null) tts.setLanguage(Locale.forLanguageTag(LANG[language]));
    }
    private void showReply(String text) {
        if (text == null || text.isEmpty()) return;
        output.setText(text);
        if (ttsReady && preferences.getBoolean("voice",true) && tts != null) {
            String speech=text.length()>1400?text.substring(0,1400):text;
            tts.speak(speech,TextToSpeech.QUEUE_FLUSH,null,"hr_reply");
        }
    }
    private void runCommand(String cmd) {
        if (cmd == null || cmd.trim().isEmpty()) { showReply("Enter a command first.");return; }
        input.setText(cmd);
        try { showReply(CommandEngine.execute(this,cmd)); }
        catch(Exception e) { showReply("Command failed: "+e.getMessage()); }
    }
    private void startVoice() {
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},REQ_MIC);return;
        }
        Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,LANG[language]);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak a command for HR HABIB AI");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,1);
        try { startActivityForResult(i,REQ_VOICE); }
        catch(ActivityNotFoundException e) { showReply("No Android speech recognition service installed."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request,result,data);
        if (request==REQ_VOICE && result==RESULT_OK && data!=null) {
            ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (words!=null && !words.isEmpty())runCommand(words.get(0));
        }
    }
    @Override public void onRequestPermissionsResult(int request,String[] names,int[] grants) {
        super.onRequestPermissionsResult(request,names,grants);
        if (request==REQ_MIC) {
            if (grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED)startVoice();
            else showReply("Microphone permission is required only for voice commands. Typed commands still work.");
        }
        if (request==REQ_CAMERA) {
            if (grants.length>0 && grants[0]==PackageManager.PERMISSION_GRANTED) setFlashlight(requestedTorch);
            else showReply("Camera permission is required to use the flashlight on this device.");
        }
    }
    boolean launch(Intent i,String error) {
        try { startActivity(i);return true; }
        catch(Exception e) { showReply(error);return false; }
    }
    String openApp(String name) {
        if (name.trim().isEmpty()) return "Say open, followed by an app name.";
        PackageManager pm=getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN);query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<android.content.pm.ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        String target=name.toLowerCase(Locale.ROOT).trim();
        android.content.pm.ResolveInfo best=null;
        for (android.content.pm.ResolveInfo info: apps) {
            String label=info.loadLabel(pm).toString().toLowerCase(Locale.ROOT).trim();
            if (label.equals(target)) { best=info;break; }
            if (best==null && label.contains(target)) best=info;
        }
        if (best==null) return "App not found: " + name;
        Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
        i.setClassName(best.activityInfo.packageName,best.activityInfo.name);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return launch(i,"App could not be opened")?"Opening " + best.loadLabel(pm):"App launch failed";
    }
    void searchWeb(String words) {
        if (words.trim().isEmpty()) { showReply("What should I search?");return; }
        String q=URLEncoder.encode(words,StandardCharsets.UTF_8);
        launch(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="+q)),"Browser unavailable");
    }
    void searchYouTube(String words) {
        if (words.trim().isEmpty()) { showReply("What should I search on YouTube?");return; }
        String q=URLEncoder.encode(words,StandardCharsets.UTF_8);
        launch(new Intent(Intent.ACTION_VIEW,Uri.parse("https://www.youtube.com/results?search_query="+q)),"Browser unavailable");
    }
    String dial(String raw) {
        String number=raw.replaceAll("[^0-9+]", "");
        if (number.length()<3) return "Use: call 1234567890. This opens the dialer; it won't place the call automatically.";
        return launch(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+Uri.encode(number))),"No dialer available")?"Opening dialer for " + number:"Cannot open dialer";
    }
    String composeSms(String raw) {
        String[] pair=raw.trim().split("\\s+",2);
        if (pair.length<2 || !pair[0].matches("[+0-9]{3,16}")) return "Use: sms 1234567890 Hello there";
        Intent i=new Intent(Intent.ACTION_SENDTO,Uri.parse("smsto:"+Uri.encode(pair[0])));
        i.putExtra("sms_body",pair[1]);
        return launch(i,"No SMS app available")?"Opening SMS draft — please review and send manually":"Cannot open SMS draft";
    }
    void setFlashlight(boolean on) {
        requestedTorch=on;
        if (checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA},REQ_CAMERA);return;
        }
        try {
            CameraManager mgr=(CameraManager)getSystemService(CAMERA_SERVICE);
            if (mgr==null) throw new Exception("Camera manager not found");
            for(String id:mgr.getCameraIdList()) {
                Boolean available=mgr.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if(Boolean.TRUE.equals(available)) { mgr.setTorchMode(id,on);showReply(on?"Flashlight on":"Flashlight off");return; }
            }
            showReply("No flashlight found on this phone.");
        }catch(Exception e){showReply("Flashlight unavailable: "+e.getMessage());}
    }
    void addNote(String content) {
        if(content==null || content.trim().isEmpty()) return;
        try {
            JSONArray array=new JSONArray(preferences.getString("notes","[]"));
            array.put(new JSONObject().put("text",content.trim()).put("date",new SimpleDateFormat("dd MMM HH:mm",Locale.getDefault()).format(new Date())));
            preferences.edit().putString("notes",array.toString()).apply();
        }catch(Exception e){showReply("Could not save the note.");}
    }
    void showNotes() {
        try {
            JSONArray a=new JSONArray(preferences.getString("notes","[]"));
            StringBuilder buf=new StringBuilder();
            for(int i=a.length()-1;i>=0;i--){
                JSONObject n=a.getJSONObject(i);
                buf.append("• ").append(n.optString("text")).append("  (" ).append(n.optString("date")).append(")\n\n");
            }
            new AlertDialog.Builder(this).setTitle("Your local notes")
                    .setMessage(buf.length()==0?"No notes yet.":buf.toString())
                    .setPositiveButton("Close",null)
                    .setNeutralButton("Delete all",(d,w)->new AlertDialog.Builder(this).setTitle("Delete all notes?")
                            .setMessage("This cannot be undone.")
                            .setNegativeButton("Cancel",null)
                            .setPositiveButton("Delete",(x,y)->preferences.edit().remove("notes").apply()).show())
                    .show();
        }catch(Exception e){showReply("Could not display notes.");}
    }
    void openAccessibilitySettings() {
        launch(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),"Accessibility Settings unavailable");
    }
    private EditText field(LinearLayout box,String hint,String existing,boolean secret) {
        EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(15);e.setText(existing);e.setHint(hint);
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        box.addView(e,new LinearLayout.LayoutParams(-1,dp(56)));return e;
    }
    private void showAiSettings() {
        LinearLayout box=vertical();box.setPadding(dp(20),dp(8),dp(20),dp(3));
        box.addView(text("Use an HTTPS OpenAI-compatible chat-completions API. Provider fees or limits are separate from this free app.",13,0xFF334455,false));
        EditText url=field(box,"HTTPS endpoint",preferences.getString("ai_url",""),false);
        EditText model=field(box,"Model name",preferences.getString("ai_model",""),false);
        EditText key=field(box,"API key (leave blank to keep saved key)","",true);
        new AlertDialog.Builder(this).setTitle("Optional AI Connection")
                .setView(box)
                .setPositiveButton("Save",(d,w)->{
                    String u=url.getText().toString().trim(),m=model.getText().toString().trim(),k=key.getText().toString().trim();
                    if(!u.isEmpty() && !u.startsWith("https://")){showReply("AI endpoint must start with https://");return;}
                    preferences.edit().putString("ai_url",u).putString("ai_model",m).apply();
                    if(!k.isEmpty()) try{SecretStore.save(this,k);}catch(Exception e){showReply("Failed to save encrypted API key: "+e.getMessage());return;}
                    showReply("AI settings saved. Use Ask AI when ready.");
                })
                .setNeutralButton("Remove key",(d,w)->{
                    try {SecretStore.save(this,"");showReply("Saved AI API key removed.");}
                    catch(Exception e){showReply("Could not clear key: "+e.getMessage());}
                })
                .setNegativeButton("Cancel",null).show();
    }
    void askAI(String prompt) {
        if (prompt==null || prompt.trim().isEmpty()) {showReply("Type your question first.");return;}
        String url=preferences.getString("ai_url","").trim();
        String model=preferences.getString("ai_model","").trim();
        if(url.isEmpty() || model.isEmpty()) {showReply("Configure an optional AI provider in AI Settings first. Local commands work without one.");return;}
        String key;
        try {key=SecretStore.read(this);}catch(Exception e){showReply("Could not read encrypted API key: "+e.getMessage());return;}
        showReply("Contacting your configured AI provider...");
        AiClient.ask(url,model,key,prompt,(result,success)->showReply(result));
    }
    /** User-created, manually triggered macros: app launch, visible tap, scroll, back, wait. */
    void showMacros() {
        JSONArray macros;
        try { macros=new JSONArray(preferences.getString("macros","[]")); }
        catch(Exception ex) { macros=new JSONArray(); }
        final JSONArray stored=macros;
        String[] names=new String[stored.length()+1];
        names[0]="＋ Create new macro";
        for(int i=0;i<stored.length();i++) names[i+1]=stored.optJSONObject(i).optString("name","Unnamed");
        new AlertDialog.Builder(this).setTitle("Your automations")
                .setItems(names,(d,index)->{
                    if (index==0) createMacro();
                    else showMacroActions(stored.optJSONObject(index-1),index-1);
                })
                .setNegativeButton("Close",null).show();
    }
    private void createMacro() {
        LinearLayout fields=vertical();fields.setPadding(dp(20),dp(5),dp(20),dp(2));
        EditText name=field(fields,"Name, e.g. morning","",false);
        EditText steps= new EditText(this);
        steps.setSingleLine(false);steps.setMinLines(5);steps.setTextSize(14);
        steps.setHint("One command per line:\nopen Settings\nwait 2\ntap Display\nscroll down");
        fields.addView(steps);
        new AlertDialog.Builder(this).setTitle("Create your macro")
                .setView(fields)
                .setPositiveButton("Save",(d,w)->{
                    String n=name.getText().toString().trim();
                    String script=steps.getText().toString().trim();
                    String error=validateMacro(script);
                    if(n.isEmpty() || n.length()>40 || error!=null){showReply(error!=null?error:"Macro name must be 1–40 characters.");return;}
                    try {
                        JSONArray a=new JSONArray(preferences.getString("macros","[]"));
                        for(int i=0;i<a.length();i++)if(a.getJSONObject(i).optString("name").equalsIgnoreCase(n)){
                            showReply("Macro name already exists.");return;
                        }
                        a.put(new JSONObject().put("name",n).put("steps",script));
                        preferences.edit().putString("macros",a.toString()).apply();
                        showReply("Macro saved: "+n+". Run it with: macro "+n);
                    }catch(Exception e){showReply("Cannot save macro: "+e.getMessage());}
                })
                .setNegativeButton("Cancel",null).show();
    }
    private void showMacroActions(JSONObject macro,int index) {
        if(macro==null)return;
        String name=macro.optString("name","Unnamed"), script=macro.optString("steps","");
        new AlertDialog.Builder(this).setTitle(name).setMessage(script)
                .setPositiveButton("Run",(d,w)->runNamedMacro(name))
                .setNeutralButton("Delete",(d,w)->new AlertDialog.Builder(this)
                        .setTitle("Delete "+name+"?")
                        .setNegativeButton("Cancel",null)
                        .setPositiveButton("Delete",(x,y)->{
                            try {
                                JSONArray old=new JSONArray(preferences.getString("macros","[]")),next=new JSONArray();
                                for(int i=0;i<old.length();i++)if(i!=index)next.put(old.get(i));
                                preferences.edit().putString("macros",next.toString()).apply();
                                showReply("Macro deleted: "+name);
                            }catch(Exception e){showReply("Cannot delete macro.");}
                        }).show())
                .setNegativeButton("Close",null).show();
    }
    private String validateMacro(String script) {
        String[] lines=script.split("\\r?\\n");
        if(lines.length<1 || lines.length>15)return "A macro needs 1–15 lines.";
        int nonempty=0;
        for(String s:lines) {
            String t=s.trim().toLowerCase(Locale.ROOT);
            if(!t.isEmpty())nonempty++;
            if(t.isEmpty())continue;
            if(t.matches("wait [0-9]{1,2}")) {
                int sec=Integer.parseInt(t.substring(5));
                if(sec>20)return "Each wait must be 0–20 seconds.";
            } else if(!(t.startsWith("open ") || t.startsWith("tap ") || t.equals("back") || t.equals("home") ||
                    t.equals("scroll up") || t.equals("scroll down"))) {
                return "Unsupported macro step: "+s+". Use open, tap, back, home, scroll or wait.";
            }
        }
        return nonempty==0?"Add at least one macro command.":null;
    }
    String runNamedMacro(String name) {
        try {
            JSONArray macros=new JSONArray(preferences.getString("macros","[]"));
            for(int i=0;i<macros.length();i++) {
                JSONObject m=macros.getJSONObject(i);
                if(m.optString("name").equalsIgnoreCase(name.trim())) {
                    String script=m.optString("steps","");
                    String error=validateMacro(script);
                    if(error!=null)return error;
                    String[] lines=script.split("\\r?\\n");
                    int run=++macroSequence;
                    performMacroStep(lines,0,run);
                    return "Running macro: "+name;
                }
            }
        } catch(Exception e){return "Could not load macro: "+e.getMessage();}
        return "Macro not found: "+name;
    }
    private void performMacroStep(String[] lines,int position,int run) {
        if(run!=macroSequence || isFinishing() || isDestroyed())return;
        if(position>=lines.length){output.setText("Macro finished.");return;}
        String command=lines[position].trim();
        if(command.isEmpty()){performMacroStep(lines,position+1,run);return;}
        long nextDelay=1100;
        if(command.toLowerCase(Locale.ROOT).startsWith("wait ")) {
            nextDelay=1000L*Integer.parseInt(command.substring(5).trim());
        } else {
            try {String result=CommandEngine.execute(this,command);output.setText("Macro: "+command+" → "+result);}
            catch(Exception e){output.setText("Macro step failed: "+e.getMessage());return;}
        }
        macroHandler.postDelayed(()->performMacroStep(lines,position+1,run),nextDelay);
    }
    @Override protected void onDestroy() {
        macroSequence++;
        macroHandler.removeCallbacksAndMessages(null);
        if(tts!=null){tts.stop();tts.shutdown();tts=null;}
        super.onDestroy();
    }
}
