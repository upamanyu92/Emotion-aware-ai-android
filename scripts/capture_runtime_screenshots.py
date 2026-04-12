#!/usr/bin/env python3
"""
Runtime screenshot capture for Tara – Emotion-Aware AI
Device : Samsung Galaxy Note20 Ultra (SM-N980F) — Android 13
Method : ADB exec-out screencap (app rendered over lock screen via setShowWhenLocked)
Output : docs/user_guide/screenshots/runtime_*.png
"""
import subprocess, os, time, sys

ADB    = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
SERIAL = "RZ8R704RKCW"
PKG    = "com.example.emotionawareai.debug"
MAIN   = "com.example.emotionawareai.MainActivity"
OUT    = os.path.abspath(
    os.path.join(os.path.dirname(__file__), "..", "docs", "user_guide", "screenshots")
)

# ── helpers ──────────────────────────────────────────────────────────────────
def adb(*args, timeout=30):
    r = subprocess.run([ADB, "-s", SERIAL] + list(args),
                       capture_output=True, text=True, timeout=timeout)
    return (r.stdout + r.stderr).strip()

def screencap(name, wait=2.8):
    """Stream screenshot directly via exec-out and save locally."""
    time.sleep(wait)
    r = subprocess.run(
        [ADB, "-s", SERIAL, "exec-out", "screencap", "-p"],
        capture_output=True, timeout=30
    )
    path = os.path.join(OUT, f"{name}.png")
    with open(path, "wb") as f:
        f.write(r.stdout)
    sz = os.path.getsize(path)
    status = "✅" if sz > 50_000 else ("⚠️  SMALL" if sz > 5_000 else "❌ BLACK")
    print(f"  {status}  {name}.png  ({sz//1024} KB)")
    return sz

def tap(x, y, label=""):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.45)

def swipe(x1, y1, x2, y2, ms=400):
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(ms))
    time.sleep(0.55)

def key(code):
    adb("shell", "input", "keyevent", str(code))
    time.sleep(0.35)

def text(s):
    """Type text; spaces via swipe-right trick."""
    safe = s.replace(" ", "%s").replace("'", "")
    adb("shell", "input", "text", safe)
    time.sleep(0.3)

def wake():
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    time.sleep(0.6)

def force_stop():
    adb("shell", "am", "force-stop", PKG)
    time.sleep(0.5)

def launch():
    adb("shell", "am", "start", "-n", f"{PKG}/{MAIN}")
    time.sleep(0.3)

# ── main ─────────────────────────────────────────────────────────────────────
os.makedirs(OUT, exist_ok=True)
subprocess.run([ADB, "start-server"], capture_output=True, timeout=10)
time.sleep(1)

model  = adb("shell", "getprop", "ro.product.model")
android= adb("shell", "getprop", "ro.build.version.release")
print(f"\n{'='*60}")
print(f"  Device : {model} — Android {android}")
print(f"  Output : {OUT}")
print(f"{'='*60}\n")

# ─────────────────────────────────────────────────────────────────────────────
# 0. CLEAR DATA → fresh first-launch flow
# ─────────────────────────────────────────────────────────────────────────────
print("── Clearing app data for fresh launch ──────────────────────")
force_stop()
adb("shell", "pm", "clear", PKG)
time.sleep(1.2)
wake()

# ─────────────────────────────────────────────────────────────────────────────
# 1. SPLASH SCREEN
# ─────────────────────────────────────────────────────────────────────────────
print("\n[1/15] Splash Screen")
launch()
sz = screencap("runtime_01_splash", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# 2. LLM SETUP SCREEN — top (model name + status)
# ─────────────────────────────────────────────────────────────────────────────
print("[2/15] LLM Setup Screen — top")
sz = screencap("runtime_02_llm_setup_top", wait=3.5)

# ─────────────────────────────────────────────────────────────────────────────
# 3. LLM SETUP SCREEN — scroll down to show model list
# ─────────────────────────────────────────────────────────────────────────────
print("[3/15] LLM Setup Screen — model list")
swipe(540, 1500, 540, 700, 500)
sz = screencap("runtime_03_llm_setup_models", wait=1.8)

# ─────────────────────────────────────────────────────────────────────────────
# 4. SKIP LLM SETUP → Onboarding / Login screen
#    The "Skip" button is near the bottom of the screen.
#    Scroll back to top first, then tap Skip.
# ─────────────────────────────────────────────────────────────────────────────
print("[4/15] Skip LLM setup → Onboarding screen")
swipe(540, 700, 540, 1500, 500)   # scroll back up
time.sleep(0.8)
# Tap "Skip" button — typically at the bottom of the LLM setup screen
tap(540, 1900, "Skip LLM setup")
sz = screencap("runtime_04_onboarding_welcome", wait=3.0)

# ─────────────────────────────────────────────────────────────────────────────
# 5. ONBOARDING — enter user name
# ─────────────────────────────────────────────────────────────────────────────
print("[5/15] Onboarding — enter name")
# Tap name text field (upper-center of screen)
tap(540, 820, "Name field")
time.sleep(0.7)
text("Alex")
sz = screencap("runtime_05_onboarding_name", wait=1.8)

# ─────────────────────────────────────────────────────────────────────────────
# 6. ONBOARDING — growth area chips
# ─────────────────────────────────────────────────────────────────────────────
print("[6/15] Onboarding — growth areas")
# Scroll down slightly to show chips
swipe(540, 1400, 540, 1000, 400)
sz = screencap("runtime_06_onboarding_areas", wait=1.5)

# ─────────────────────────────────────────────────────────────────────────────
# 7. TAP CONTINUE → Chat screen
# ─────────────────────────────────────────────────────────────────────────────
print("[7/15] Tap Continue → Chat screen")
swipe(540, 1000, 540, 1400, 400)  # scroll back
time.sleep(0.5)
tap(540, 1900, "Continue")
sz = screencap("runtime_07_chat_empty", wait=3.5)

# ─────────────────────────────────────────────────────────────────────────────
# 8. CHAT — type a message
# ─────────────────────────────────────────────────────────────────────────────
print("[8/15] Chat — typing a message")
# Tap the message input field at the bottom
tap(480, 2180, "Message input")
time.sleep(0.8)
text("Hello%20Tara,%20I%20am%20feeling%20a%20bit%20stressed%20today")
sz = screencap("runtime_08_chat_typing", wait=1.8)

# ─────────────────────────────────────────────────────────────────────────────
# 9. CHAT — send & AI response streaming
# ─────────────────────────────────────────────────────────────────────────────
print("[9/15] Chat — AI response")
# Tap Send button (right side of input row)
tap(980, 2180, "Send")
sz = screencap("runtime_09_chat_response", wait=5.5)

# ─────────────────────────────────────────────────────────────────────────────
# 10. DIARY TAB
# ─────────────────────────────────────────────────────────────────────────────
print("[10/15] Diary screen")
# Bottom nav: Home(108) Diary(324) Insights(540) Goals(756) Eval(864) Profile(972)
tap(324, 2380, "Diary tab")
sz = screencap("runtime_10_diary", wait=2.8)

# ─────────────────────────────────────────────────────────────────────────────
# 11. INSIGHTS TAB
# ─────────────────────────────────────────────────────────────────────────────
print("[11/15] Insights screen")
tap(540, 2380, "Insights tab")
sz = screencap("runtime_11_insights", wait=2.8)

# ─────────────────────────────────────────────────────────────────────────────
# 12. GOALS TAB
# ─────────────────────────────────────────────────────────────────────────────
print("[12/15] Goals screen")
tap(756, 2380, "Goals tab")
sz = screencap("runtime_12_goals", wait=2.8)

# ─────────────────────────────────────────────────────────────────────────────
# 13. SETTINGS / PROFILE TAB  (top of settings)
# ─────────────────────────────────────────────────────────────────────────────
print("[13/15] Settings screen — top (profile + AI model)")
tap(972, 2380, "Profile tab")
sz = screencap("runtime_13_settings_top", wait=2.8)

# Scroll down to show Conversation + Camera sections
swipe(540, 1600, 540, 600, 500)
sz = screencap("runtime_14_settings_mid", wait=1.8)

# Scroll more to show Data & Privacy + Upgrade button
swipe(540, 1600, 540, 600, 500)
sz = screencap("runtime_15_settings_bottom", wait=1.5)

# ─────────────────────────────────────────────────────────────────────────────
# 14. BACK TO CHAT — show voice button and camera controls
# ─────────────────────────────────────────────────────────────────────────────
print("[14/15] Chat — with voice/camera controls visible")
tap(108, 2380, "Chat tab")
sz = screencap("runtime_16_chat_controls", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# 15. CHAT — tap mic to show voice listening UI
# ─────────────────────────────────────────────────────────────────────────────
print("[15/15] Chat — voice input active")
tap(108, 2180, "Mic button")
sz = screencap("runtime_17_voice_listening", wait=2.5)
key("KEYCODE_BACK")

# ─────────────────────────────────────────────────────────────────────────────
# SUMMARY
# ─────────────────────────────────────────────────────────────────────────────
pngs = sorted(f for f in os.listdir(OUT) if f.startswith("runtime_") and f.endswith(".png"))
good = sum(1 for f in pngs if os.path.getsize(os.path.join(OUT, f)) > 50_000)
bad  = len(pngs) - good

print(f"\n{'='*60}")
print(f"  Captured : {len(pngs)} screenshots  ✅ {good} real  ❌ {bad} blank")
for f in pngs:
    sz = os.path.getsize(os.path.join(OUT, f))
    mark = "✅" if sz > 50_000 else "❌"
    print(f"    {mark} {f}  ({sz//1024} KB)")
print(f"  Saved to : {OUT}")
print(f"{'='*60}\n")

