# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.bawabat.alorood.**$$serializer { *; }
-keepclassmembers class com.bawabat.alorood.** { *** Companion; }
-keepclasseswithmembers class com.bawabat.alorood.** { kotlinx.serialization.KSerializer serializer(...); }
