#!/usr/bin/env python3
"""Capture pairip.json from an unmodified, already-running app via Frida.

No CorePatch, no root reinstall, no signature spoofing.
pairip.json is the translation map consumed by:
    java -jar RePairip.jar -i app.apks -t pairip.json

Usage:
  1) java -jar RePairip.jar -i app.apks            # builds merged APK + junk_classes.txt
  2) launch the app normally (Play-installed stock APK, pairip VM runs at startup)
  3) python3 frida/capture_pairip.py -p <package> -c <junk_classes.txt> -o pairip.json
  4) java -jar RePairip.jar -i app.apks -t pairip.json
"""
import argparse, json, sys, time

def build_js(classes):
    return r'''
send("capture-ready");
Java.perform(function () {
    var classes = %s;
    var JSONObject = Java.use("org.json.JSONObject");
    var Modifier = Java.use("java.lang.reflect.Modifier");
    var root = JSONObject.$new();
    var ok = 0, errors = [];
    classes.forEach(function (cn) {
        try {
            var C = Java.use(cn);
            var cls = C.class;
            var fields = cls.getDeclaredFields();
            var obj = JSONObject.$new();
            var fobj = JSONObject.$new();
            var typeName = null;
            for (var i = 0; i < fields.length; i++) {
                var f = fields[i];
                if (!Modifier.isStatic(f.getModifiers())) continue;
                f.setAccessible(true);
                if (typeName === null) typeName = f.getType().getName();
                var v = f.get(null);
                var out;
                if (v === null) out = JSONObject.NULL;
                else { try { out = v.toString(); } catch (e2) { out = "<err>"; } }
                fobj.put(f.getName(), out);
            }
            obj.put("type", typeName);
            obj.put("fields", fobj);
            root.put(cls.getName(), obj);
            ok++;
        } catch (e) { errors.push(cn + ": " + e); }
    });
    var s = root.toString(2);
    send(JSON.stringify({event: "dump", bytes: s.length, classes: ok, errors: errors}));
    send(s);
});
''' % json.dumps(classes)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-p", "--package", required=True)
    ap.add_argument("-c", "--classes", required=True, help="junk_classes.txt written by RePairip")
    ap.add_argument("-o", "--out", default="pairip.json")
    ap.add_argument("-d", "--device", default=None, help="frida device id (default: usb)")
    ap.add_argument("--spawn", action="store_true", help="spawn instead of attaching to a running app")
    args = ap.parse_args()

    raw = [l.strip() for l in open(args.classes) if l.strip()]
    classes = []
    for c in raw:
        c = c.replace("/", ".")
        if c.startswith("L") and c.endswith(";"):
            c = c[1:-1]
        classes.append(c)
    if not classes:
        sys.exit("empty class list")

    import frida
    dev = frida.get_device(args.device, timeout=10) if args.device else frida.get_usb_device(timeout=10)
    if args.spawn:
        pid = dev.spawn([args.package])
    else:
        pid = dev.get_process(args.package).pid
    print("attach pid", pid)
    session = dev.attach(pid)
    script = session.create_script(build_js(classes))
    result = {"payload": None}

    def on_message(message, data):
        if message.get("type") != "send":
            print(message)
            return
        payload = message["payload"]
        if isinstance(payload, str) and payload.startswith("{"):
            try:
                info = json.loads(payload)
            except Exception:
                info = None
            if info and info.get("event") == "dump":
                print("dump:", info)
                return
        if isinstance(payload, str) and payload.strip().startswith("{"):
            result["payload"] = payload

    script.on("message", on_message)
    script.load()
    if args.spawn:
        dev.resume(pid)
    deadline = time.time() + 25
    while time.time() < deadline and result["payload"] is None:
        time.sleep(0.2)
    session.detach()
    if result["payload"] is None:
        sys.exit("no dictionary received (is the app running and pairip initialized?)")
    open(args.out, "w").write(result["payload"])
    print("wrote", args.out)

if __name__ == "__main__":
    main()
