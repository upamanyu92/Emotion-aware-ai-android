#!/usr/bin/env python3
"""
Screenshot automation for Emotion-Aware AI (Tara) — Samsung SM-N980F
Captures every major screen and saves to docs/user_guide/screenshots/
"""
import subprocess, os, time, sys

ADB    = os.path.expanduser("~/Library/Android/sdk/platform-tools/adb")
SERIAL = "RZ8R704RKCW"
PKG    = "com.example.emotionawareai.debug"
MAIN   = "com.example.emotionawareai.MainActivity"
OUT    = os.path.join(os.path.dirname(__file__), "..", "docs", "user_guide", "screenshots")
OUT    = os.path.abspath(OUT)

# ── helpers ────────────────────────────────────────────────────────────────
def adb(*args, timeout=30):
    r = subprocess.run([ADB, "-s", SERIAL] + list(args),
                       capture_output=True, text=True, timeout=timeout)
    return (r.stdout + r.stderr).strip()

def screenshot(name, wait=2.5):
    time.sleep(wait)
    remote = f"/sdcard/ss_{name}.png"
    local  = os.path.join(OUT, f"{name}.png")
    adb("shell", "screencap", "-p", remote)
    adb("pull", remote, local)
    adb("shell", "rm", "-f", remote)
    size = os.path.getsize(local) if os.path.exists(local) else 0
    print(f"  [OK] {name}.png  ({size//1024} KB)")
    return local

def tap(x, y, label=""):
    print(f"  TAP ({x},{y})  {label}")
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.4)

def swipe(x1, y1, x2, y2, dur=400):
    adb("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(dur))
    time.sleep(0.5)

def press_back():
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.5)

def type_text(text):
    # Escape spaces for adb input
    escaped = text.replace(" ", "%s")
    adb("shell", "input", "text", escaped)
    time.sleep(0.3)

# ── setup ─────────────────────────────────────────────────────────────────
os.makedirs(OUT, exist_ok=True)
subprocess.run([ADB, "start-server"], capture_output=True, timeout=10)
time.sleep(1)

print(f"\nDevice  : {adb('shell','getprop','ro.product.model')} — Android {adb('shell','getprop','ro.build.version.release')}")
print(f"Output  : {OUT}\n")

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 1 — Splash Screen (fresh launch)
# ─────────────────────────────────────────────────────────────────────────────
print("=== [1/12] Splash Screen — fresh launch ===")
adb("shell", "pm", "clear", PKG)
time.sleep(1)
adb("shell", "am", "start", "-n", f"{PKG}/{MAIN}")
screenshot("01_splash_screen", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 2 — LLM Setup Screen
# ─────────────────────────────────────────────────────────────────────────────
print("=== [2/12] LLM Setup Screen ===")
screenshot("02_llm_setup_screen", wait=3)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 3 — LLM Setup expanded (model options visible — scroll down slightly)
# ─────────────────────────────────────────────────────────────────────────────
print("=== [3/12] LLM Setup – model list ===")
swipe(540, 1400, 540, 900)
screenshot("03_llm_setup_models", wait=1.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 4 — Skip setup → Login / Onboarding
# ─────────────────────────────────────────────────────────────────────────────
print("=== [4/12] Skip setup → Onboarding / Login ===")
# Scroll back to top then tap Skip
swipe(540, 900, 540, 1400)
time.sleep(0.5)
tap(540, 1850, "Skip LLM setup")
screenshot("04_login_screen", wait=3)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 5 — Enter user name
# ─────────────────────────────────────────────────────────────────────────────
print("=== [5/12] Onboarding — enter name ===")
tap(540, 850, "Name field")
type_text("Alex")
screenshot("05_onboarding_name", wait=1.5)

# Continue
tap(540, 1850, "Continue")
screenshot("06_onboarding_goals", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 6 — Main Chat Screen (stub mode)
# ─────────────────────────────────────────────────────────────────────────────
print("=== [6/12] Main Chat Screen — empty / stub mode ===")
# Possibly tap another Continue
tap(540, 1850, "Finish onboarding")
screenshot("07_chat_screen_empty", wait=3)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 7 — Chat: type a message
# ─────────────────────────────────────────────────────────────────────────────
print("=== [7/12] Chat — type a message ===")
tap(480, 2200, "Message input")
time.sleep(0.6)
type_text("Hello%20Tara,%20how%20are%20you%20today?")
screenshot("08_chat_typing", wait=1.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 8 — Chat: send & AI response
# ─────────────────────────────────────────────────────────────────────────────
print("=== [8/12] Chat — AI response ===")
tap(970, 2200, "Send button")
screenshot("09_chat_response", wait=5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 9 — Navigate to Diary tab
# ─────────────────────────────────────────────────────────────────────────────
print("=== [9/12] Diary Screen ===")
tap(216, 2380, "Diary tab")
screenshot("10_diary_screen", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 10 — Insights Screen
# ─────────────────────────────────────────────────────────────────────────────
print("=== [10/12] Insights Screen ===")
tap(432, 2380, "Insights tab")
screenshot("11_insights_screen", wait=2.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 11 — Settings Screen
# ─────────────────────────────────────────────────────────────────────────────
print("=== [11/12] Settings Screen ===")
tap(864, 2380, "Settings tab")
screenshot("12_settings_screen", wait=2.5)

# Scroll down in settings to show more sections
swipe(540, 1400, 540, 600)
screenshot("13_settings_ai_model", wait=1.5)

swipe(540, 1400, 540, 600)
screenshot("14_settings_tts_camera", wait=1.5)

# ─────────────────────────────────────────────────────────────────────────────
# SCREEN 12 — Back to Chat, show Voice input button
# ─────────────────────────────────────────────────────────────────────────────
print("=== [12/12] Chat — Voice input UI ===")
tap(108, 2380, "Chat tab")
screenshot("15_chat_voice_button", wait=2)

# Tap voice button (mic icon, usually left side of input row)
tap(100, 2200, "Voice mic button")
screenshot("16_voice_listening", wait=2)

# Stop voice input
press_back()
screenshot("17_chat_final", wait=1.5)

print(f"\n{'='*55}")
total = len([f for f in os.listdir(OUT) if f.endswith(".png")])
print(f"DONE — {total} screenshots saved to:\n  {OUT}")
print("="*55)

