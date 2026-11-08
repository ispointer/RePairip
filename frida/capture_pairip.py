#!/usr/bin/env python3
"""Capture pairip.json from an unmodified, already-running app via Frida.

No CorePatch, no root reinstall, no signature spoofing.
pairip.json is the translation map consumed by:
    java -jar RePairip.jar -i app.apks -t pairip.json

Usage:
  1) java -jar RePairip.jar -i app.apks            # merged APK (+ junk_classes.txt on main builds)
     java -cp RePairip.jar frida/JunkClasses.java app_merged.apk > junk_classes.txt
  2) launch the app normally (Play-installed stock APK, pairip VM runs at startup)
  3) python3 frida/capture_pairip.py -p <package> -c <junk_classes.txt> -o pairip.json
  4) java -jar RePairip.jar -i app.apks -t pairip.json
"""
import argparse, json, subprocess, sys, time

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

def resolve_pid(dev, package, serial=None):
    try:
        return dev.get_process(package).pid
    except Exception:
        pass
    if package.isdigit():
        try:
            return dev.get_process(int(package)).pid
        except Exception:
            pass
    cmd = ["adb"] + (["-s", serial] if serial else []) + ["shell", "pidof", package]
    try:
        out = subprocess.run(cmd, capture_output=True, text=True, timeout=5).stdout.strip()
    except Exception:
        out = ""
    if out:
        return int(out.split()[0])
    raise RuntimeError("unable to find process '%s' (is the app running?)" % package)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("-p", "--package", required=True)
    ap.add_argument("-c", "--classes", required=True,
                    help="junk_classes.txt (use frida/JunkClasses.java for release JARs)")
    ap.add_argument("-o", "--out", default="pairip.json")
    ap.add_argument("-d", "--device", default=None, help="frida device id (default: usb)")
    ap.add_argument("--spawn", action="store_true",
                    help="spawn the app instead of attaching (waits before reading fields)")
    ap.add_argument("--wait", type=float, default=0.0,
                    help="seconds to wait before reading fields (default: 0 attach, 10 spawn)")
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
    major = int(frida.__version__.split(".")[0])
    if major >= 17:
        sys.exit("frida %s has no built-in Java bridge; use frida 16.x "
                 "(verified with 16.7.19) and a matching frida-server" % frida.__version__)

    try:
        dev = frida.get_device(args.device, timeout=10) if args.device else frida.get_usb_device(timeout=10)
    except Exception as exc:
        sys.exit("cannot reach frida device: %s" % exc)
    if args.spawn:
        try:
            pid = dev.spawn([args.package])
        except frida.NotSupportedError as exc:
            sys.exit("spawn failed (%s); launch the app normally and omit --spawn" % exc)
        except frida.ServerNotRunningError:
            sys.exit("cannot reach frida-server; start frida-server 16.x and retry")
        session = dev.attach(pid)
        dev.resume(pid)
        wait = args.wait if args.wait > 0 else 10.0
        print("attach pid", pid, "(spawned)")
        if wait > 0:
            time.sleep(wait)
    else:
        pid = resolve_pid(dev, args.package, dev.id)
        print("attach pid", pid)
        try:
            session = dev.attach(pid)
        except frida.ServerNotRunningError:
            sys.exit("cannot reach frida-server; start frida-server 16.x and retry")
        if args.wait > 0:
            time.sleep(args.wait)

    script = session.create_script(build_js(classes))
    result = {"payload": None, "info": None}

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
                result["info"] = info
                print("dump:", info)
                return
        if isinstance(payload, str) and payload.strip().startswith("{"):
            result["payload"] = payload

    script.on("message", on_message)
    script.load()
    deadline = time.time() + 25
    while time.time() < deadline and result["payload"] is None:
        time.sleep(0.2)
    session.detach()
    if result["payload"] is None:
        sys.exit("no dictionary received (is the app running and pairip initialized?)")
    info = result["info"] or {}
    got = info.get("classes")
    errors = info.get("errors") or []
    open(args.out, "w").write(result["payload"])
    print("wrote", args.out)
    if errors or got != len(classes):
        print("WARNING: captured %s/%d classes, %d errors" % (got, len(classes), len(errors)))
        for e in errors[:5]:
            print(" -", e)
        sys.exit(1)

if __name__ == "__main__":
    main()
