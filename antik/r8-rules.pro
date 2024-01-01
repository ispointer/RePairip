-dontwarn **
-ignorewarnings
-repackageclasses ''
-allowaccessmodification
-dontnote **

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

-keep public class com.repairip.Main {
    public static void main(java.lang.String[]);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}