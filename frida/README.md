# Frida capture — no CorePatch, no patched install

PairIP's VM dies on a modified APK, so the classic flow installs an unsigned
logging build with signature verification disabled (CorePatch/LSPosed). Frida
avoids that: keep the Play-signed app, launch it so PairIP fills the junk-class
fields, dump them to `pairip.json`, then translate and sign the clean APK.

Root is still required — `frida-server` must run as root to attach to the app.
This guide removes CorePatch and the patched install, not root.

## Requirements

- Rooted device with **frida-server 16.x** and a matching **Frida Python 16.x**
  client. Frida 17 removed the built-in Java bridge; `capture_pairip.py` exits
  on 17.x. Verified with 16.7.19 on both sides.
- `adb`, Java 17+, Python 3.
- **RePairip 1.8.30+** from
  [Releases](https://github.com/ispointer/RePairip/releases/latest). It writes
  `junk_classes.txt` during the merge step.

## Get RePairip

```bash
curl -L -o RePairip.jar \
  https://github.com/ispointer/RePairip/releases/download/1.8.30/RePairip.1.8.30.jar
java -jar RePairip.jar -v
```

Adjust the version for newer releases; `RePairip.jar` is the local name used
below.

## 1. Pull the installed splits

```bash
PKG=com.example.app
mkdir -p work
for p in $(adb shell pm path "$PKG" | sed 's/package://;s/\r//'); do adb pull "$p" work/; done

python3 - <<'EOF'
import zipfile, glob, os
with zipfile.ZipFile("work/app.apks", "w", zipfile.ZIP_STORED) as z:
    for f in ["work/base.apk"] + sorted(glob.glob("work/split_config*.apk")):
        z.write(f, os.path.basename(f))
EOF
```

No `zip` binary is needed; the snippet builds a stored `.apks`.

## 2. Merge and get the junk class list

```bash
java -jar RePairip.jar -i work/app.apks
```

Writes:

- `work/app_merged.apk` — clean merged APK
- `work/app_patched.apk` — logging build (not used by this flow)
- `work/junk_classes.txt` — PairIP junk class descriptors, generated automatically

The list should be non-empty for a protected app.

## 3. Start frida-server

```bash
adb shell 'kill $(pidof frida-server) 2>/dev/null'
adb shell 'nohup /data/local/tmp/frida-server -D >/dev/null 2>&1 &'
adb shell '/data/local/tmp/frida-server --version'
```

Prefix with `su -c` if the adb shell is not root. The server version must match
the Python client (16.7.19).

## 4. Launch the stock app

Install the Play-signed APK normally, launch it, and leave it in the foreground:

```bash
adb shell monkey -p "$PKG" -c android.intent.category.LAUNCHER 1
```

PairIP populates the junk-class fields during startup.

## 5. Capture `pairip.json`

```bash
python3 -m venv .venv
.venv/bin/pip install 'frida==16.7.19'
.venv/bin/python frida/capture_pairip.py -p "$PKG" -c work/junk_classes.txt -o work/pairip.json
```

Expected output (sample app):

```text
attach pid 12345
dump: {'event': 'dump', 'bytes': 73194, 'classes': 36, 'errors': []}
wrote work/pairip.json
```

- With several devices connected, pass `-d <serial>`.
- Prefer attach. `--spawn` resumes the app and waits 10 seconds, but some
  Samsung builds reject spawning with `ptrace pokedata`; `--wait N` changes the
  delay.
- A partial capture still writes the JSON, prints a `WARNING` naming the
  missing classes, and exits 1.

## 6. Translate

```bash
java -jar RePairip.jar -i work/app.apks -t work/pairip.json
# -> work/app_translated.apk
```

This removes `com.pairip.*`, the VM assets, and `libpairipcore.so`, and restores
the captured classes. Passing the `.apks` re-merges it first; `-i` also accepts
an `.apk`.

## 7. Align and sign

```bash
BT=${ANDROID_HOME:-$HOME/Android/Sdk}/build-tools/35.0.0
keytool -genkeypair -keystore key.jks -storetype pkcs12 -alias key \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass android -keypass android -dname "CN=RePairip"

$BT/zipalign -p -f 4 work/app_translated.apk app-aligned.apk
$BT/apksigner sign --ks key.jks --ks-pass pass:android \
  --key-pass pass:android --out app-signed.apk app-aligned.apk
$BT/apksigner verify --verbose app-signed.apk
```

On Android 15+ devices with 16 KB pages, use `zipalign -P 16`.

## 8. Install

The stock app is Play-signed, so `adb install -r app-signed.apk` fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Either disable signature verification
(CorePatch/LSPosed) and install over the stock app, or uninstall the package
(app data is lost) and install fresh:

```bash
adb uninstall "$PKG"
adb install app-signed.apk
```

If installation fails with `Can't find service: package`, the package service is
restarting; wait and retry:

```bash
adb push app-signed.apk /data/local/tmp/app-signed.apk
adb shell pm install -r /data/local/tmp/app-signed.apk
```

## Troubleshooting

- `ReferenceError: 'Java' is not defined` — server is 17.x; use 16.x on both
  sides.
- `unable to find process '<package>'` — app not running or wrong device;
  launch it or pass `-d <serial>`.
- `cannot reach frida-server` — start the matching 16.x server.
- `frida-server is not privileged` — not rooted (or the server runs as shell);
  root is required.
- `no dictionary received` — app not running, PairIP not initialized, or the
  class list belongs to a different APK.
- Empty field values — attached too early; wait for the main UI and recapture.
- `spawn failed (... ptrace pokedata)` — launch the app normally and drop
  `--spawn`.
- `junk_classes.txt` missing or empty — the APK is not PairIP-protected, or the
  JAR is older than 1.8.30 (use `frida/JunkClasses.java` to generate the list).

## Notes

- The capture reads the same static `String` / `reflect.Method` fields that the
  injected `PairipLog` reads, so `pairip.json` stays compatible with the root
  flow.
- The translated build injects the captured values back (`SPUT_OBJECT` in
  `MethodMaker`), so recapturing from it returns the same dictionary.
