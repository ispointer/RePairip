<p align="center">
  <img src="preview/re.png" width="100" height="100" alt="RePairip logo"/>
</p>

<h1 align="center">RePairip</h1>

<p align="center">
  RePairip is a Google加固 reverse engineering tool that can dump all VMRunner snapshots and fully repack Android applications, removing protection layers completely
</p>

<p align="center">
  <b>Current version:</b> 1.7.10 &nbsp;|&nbsp;
  <b>Language:</b> Java 17 &nbsp;|&nbsp;
  <b>Build:</b> Gradle + Shadow Jar
</p>


## Tutorial

[Watch Video on RevDex.Re](https://revdex.re/input.mp4)


[Watch Video on Telegram](https://t.me/RevDex/557)

| App UI | Dump Style |
| --- | --- |
| ![App UI](preview/a1.jpg) | ![Dump Style](preview/a2.jpg) |


## License

This project is distributed under the Apache License 2.0.

```text
Apache License
Version 2.0, January 2004
https://www.apache.org/licenses/
```

Copyright (C) 2026 HighCapable


## Capture without CorePatch

The classic root flow installs the logging APK with signature verification
disabled. On Android 15 the runtime logging patch also needs the AOT/dex swap
dance, and any modified APK kills the PairIP VM before it populates classes.

The Frida path skips all of it: keep the Play-signed APK installed, launch it,
and dump the populated junk-class fields to `pairip.json`. Then translate the
clean merged APK and sign it normally.

```bash
java -jar RePairip.jar -i app.apks                       # merged APK
java -cp RePairip.jar frida/JunkClasses.java app_merged.apk > junk_classes.txt  # 1.7.10 release
# launch the Play-installed app, then:
python3 frida/capture_pairip.py -p <package> -c junk_classes.txt -o pairip.json
java -jar RePairip.jar -i app.apks -t pairip.json        # translated APK
```

Requires frida-server and a matching Frida Python client on 16.x (Frida 17
removed the Java bridge). Root is still required for frida-server to attach to
the app; the Frida path only removes CorePatch and the patched-APK install.
Builds from `main` write `junk_classes.txt` themselves; the `JunkClasses.java`
step is only needed for the 1.7.10 release JAR. No CorePatch, no `su` install,
no base.apk replacement. See `frida/README.md` for the full procedure and
troubleshooting.
