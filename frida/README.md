# Frida capture — no CorePatch, no root reinstall

The stock root flow needs a patched APK installed with signature verification
disabled (CorePatch / LSPosed), because the logging build is unsigned and the
PairIP VM dies on a modified APK. A Frida capture removes that whole dance:

- install the app normally from Play (original signature, license works)
- launch it once so the PairIP VM populates every junk class
- attach and dump the populated static fields straight to `pairip.json`
- translate the clean merged APK and sign it normally

No CorePatch, no unsigned install, no base.apk swap, no ART dex-cache tricks.

## Steps

```bash
# 1. merged APK + junk_classes.txt (logging APK is fine to ignore)
java -jar RePairip.jar -i app.apks
#    -> app_merged.apk, app_merged_pairip.apk, junk_classes.txt

# 2. launch the app (Play-installed, pairip healthy), leave it in foreground
adb shell monkey -p <package> -c android.intent.category.LAUNCHER 1

# 3. dump
python3 frida/capture_pairip.py -p <package> -c junk_classes.txt -o pairip.json

# 4. translate and sign as usual
java -jar RePairip.jar -i app.apks -t pairip.json
zipalign -p 4 app_merged_translated.apk app-aligned.apk
apksigner sign --ks key.jks --out app-signed.apk app-aligned.apk
```

Requirements: `pip install frida-tools` (or frida bindings) and a running
`frida-server` on the device. Attach mode assumes the app is already running;
`--spawn` starts it under Frida instead.

## Notes

- `junk_classes.txt` is emitted by the logging build step (DexPatcher), one
  class descriptor per line.
- The capture reads the same static `String` / `reflect.Method` fields that the
  injected `PairipLog` writes, so the resulting JSON is format-compatible with
  the root-mode capture.
- Works on Play-installed apps where the license passes, so PairIP's native
  init and VM stay healthy while fields are populated.
