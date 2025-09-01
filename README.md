# Intent Tester

A small utility app to experiment with **Android Intents**.  
You can dynamically add/remove intent actions at runtime, and the app will register a `BroadcastReceiver` for those actions.  
When a matching broadcast is received, it shows up in the log with timestamp, action, sender, and extras.

---

## Features
- Add/remove custom actions at runtime
- Import actions from a `.json` or `.txt` file
- Export current actions to JSON
- View a live log of received broadcasts (action, package, extras)
- Share or clear the log easily
- Minimal Material3 + Jetpack Compose UI

---

## Usage

1. Enter an action (e.g. `com.example.PING`) and tap **➕** to start listening.
2. Send a broadcast from another app or from ADB:
   ```bash
   adb shell am broadcast -a com.example.PING --es msg "Hello from ADB"
   ```
3. The broadcast appears in the **Received intents** list with any extras.

---

## Import/Export

- **Import**: Pick a `.json` file (array of strings) or `.txt` file with one action per line.  
- **Export**: Saves current actions as a JSON array.

---

## Notes

- Since Android 8.0+, some system broadcasts cannot be received dynamically by third-party apps.  
- Custom app broadcasts (from ADB or other apps you control) work fine.  

---

## License
MIT License — feel free to use, modify, and share.

---
