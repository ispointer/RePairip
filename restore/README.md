# restore.dex

`antik/src/main/resources/restore.dex` is built from `RePairipRestore.java` and injected into
translated APKs. It resolves `java.lang.reflect.Method` values captured in `pairip.json`.

## Build

Use `javac` and `d8` from Android build-tools (d8 is part of build-tools 35):

```sh
mkdir -p /tmp/restore/classes /tmp/restore/dex
javac -g --release 11 -d /tmp/restore/classes restore/RePairipRestore.java
d8 --min-api 32 --output /tmp/restore/dex \
  /tmp/restore/classes/com/aantik/vmrunnerlogger/RePairipRestore.class
cp /tmp/restore/dex/classes.dex antik/src/main/resources/restore.dex
```

The generated dex must contain:

- class `Lcom/aantik/vmrunnerlogger/RePairipRestore;`
- static `get(Ljava/lang/String;)Ljava/lang/reflect/Method;`
- `Ljava/lang/Class;->getDeclaredMethods()[Ljava/lang/reflect/Method;`
- both `Ljava/lang/reflect/Method;->toString()` and `toGenericString()` checks
- a non-initializing `Class.forName(String, boolean, ClassLoader)` call

`CheckLogDexTest` asserts these references and the dex header checksum/signature.
