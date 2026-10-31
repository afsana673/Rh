# 🍘 HR HABIB AI — Free Android Assistant

**Developer:** SK HABIBULLA  
**Version:** 1.0.0 (source starter project)  
**Requirements:** Android 8.0+ / Android Studio / JDK 17

> Independent Android app made from scratch. This is **not** the Maya APK, a Maya licence patch, or a copy of Maya's code, back end, proprietary voice engine, or branding. **There is no in-app licence check, subscription, trial timer, or activation key.**

## ✅ Implemented features

- 🎤 Voice input using Android's installed speech recognition provider
- 🌐 English, Hindi and Bangla voice recognition selection
- 🔊 Text-to-Speech replies through the installed Android TTS engine
- 📱 Launch apps by visible app name (e.g. `open WhatsApp`)
- 🔍 Google Search and YouTube Search
- 📞 Open dialer and create **unsent** SMS drafts (user confirms sending)
- 🔦 Turn torch on/off (permission required)
- ⏰ Open alarm creation in Android's alarm app
- 📝 Local notes, view and delete all notes
- 📲 Optional Accessibility service for explicitly requested back, home, recents, scroll and tapping **visible exact text**
- ⚡ Custom manual macro sequences using `open`, `wait`, `tap`, `scroll`, `back` and `home`
- 🤖 Optional **OpenAI-compatible** chat completions (enter your own HTTPS endpoint/model/API key)
- 🔐 API keys encrypted in Android Keystore. No analytics, account, licence server or embedded secrets.

## 🔧 Quick build using GitHub Actions (no Android Studio needed)

1. Unzip this project on your PC/phone.
2. Create a **new GitHub repository**. Upload **the contents of the extracted folder** (not the ZIP as a single file). Include `.github/workflows/build-apk.yml`.
3. On GitHub, open the **Actions** tab, choose **Build free Android APK**, then **Run workflow**. A push also starts a build.
4. When the workflow completes, open its run and download the **HR-HABIB-AI-Free-debug** artifact.
5. Extract the downloaded artifact and install `app-debug.apk` on your Android device. Android may request permission to install from that source.

The debug APK is signed for development/testing by the Android build tools. For public distribution, generate your own release signing key and make a signed release build.

## 🖥️ Build using Android Studio

Open the unzipped project directory, let Gradle sync, and choose **Build → Build APK(s)**. APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## 💬 Try commands

```text
open WhatsApp
search cute animals
youtube funny videos
call 9876543210
sms 9876543210 hello there
note my new idea
notes
alarm 07:30
flash on
flash off
home
back
scroll down
scroll up
tap Settings
macros
macro morning
ai hello, how are you?
```

## ⚡ Macro example

Open **My Macros → Create new macro** and choose name `morning`:

```text
open Settings
wait 2
tap Display
scroll down
```

Run it with `macro morning`. Macros are **explicitly launched by the user** and can run up to 15 steps. `wait N` has a 20-second maximum; steps only support the documented local controls. Android may stop an automation if the app process is killed or the target app blocks accessibility.

**Accessibility:** enable **HR HABIB AI Control** manually in Android Accessibility Settings, only if you need screen controls. It does not passively collect or transmit screen text. Android and other apps may restrict particular actions. Do not automate payments, private data entry, or permission dialogs.

## 🤖 Optional AI chat

This app's built-in commands require **no AI API key**. To use the optional chat API:

1. Tap **AI Settings**.
2. Enter an HTTPS endpoint that implements the OpenAI-style `/chat/completions` request/response format, a model name, and optionally an API key.
3. Tap **Ask AI** or type `ai [question]`.

The AI provider (not HR HABIB AI) may require an account and may charge fees or enforce a quota. This project does not include a free unlimited AI model, backend or API token. There is no AI service dependency for core local features.

## 🔒 Privacy & limitations

- No telemetry, advertising SDK, paid licence check, or account registration.
- Notes and macros are stored locally in app-private preferences. AI key is encrypted with Android Keystore.
- Voice recognition is provided by the Android speech recognition provider and **may** send recorded speech to that provider. It is not guaranteed to work offline.
- For AI chat, the prompt is sent to your configured HTTPS provider only after you explicitly request it.
- No always-on hotword, cloned Maya voice, speaker guardian, background autonomous agent, remote server infrastructure, or Maya feature parity. Those require their own implementation, permissions and sometimes compute/API charges.

## 📦 Project structure

```text
app/src/main/java/com/skhabibulla/hrassistant/
  MainActivity.java                     UI, voice, shortcuts, macros, notes
  CommandEngine.java                    text command parser
  AssistantAccessibilityService.java    optional user-triggered on-screen actions
  AiClient.java                         optional AI endpoint client
  SecretStore.java                      encrypted credential storage
app/src/main/AndroidManifest.xml
app/src/main/res/xml/accessibility_service_config.xml
.github/workflows/build-apk.yml
```

## বাংলা নির্দেশনা

এটি Maya-র লাইসেন্স বাইপাস করা APK নয়। **HR HABIB AI** আলাদা, নতুন Android প্রজেক্ট। এতে কোনো লাইসেন্স কী/অ্যাক্টিভেশন লাগে না। ZIP আনজিপ করে GitHub repo-তে ফাইলগুলো আপলোড করো। GitHub **Actions → Build free Android APK → Run workflow** চালিয়ে `app-debug.apk` নাও। Android speech service থাকলে Voice Command কাজ করবে। স্ক্রিন কন্ট্রোলের জন্য চাইলে Accessibility Settings-এ নিজে পারমিশন দিতে হবে। AI Chat সম্পূর্ণ ফ্রি হওয়ার নিশ্চয়তা নেই; নিজের API লাগে।
