package com.example.apkdecompiler

import android.content.Context
import android.net.Uri
import android.os.Environment
import org.jf.baksmali.Baksmali
import org.jf.baksmali.BaksmaliOptions
import org.jf.dexlib2.DexFileFactory
import org.jf.dexlib2.Opcodes
import java.io.File
import java.util.zip.ZipInputStream

object Decompiler {

    data class Progress(val percent: Int, val message: String)

    enum class Engine(val display: String, val advice: String) {
        UNITY_IL2CPP(
            "Unity (IL2CPP)",
            "已提取 global-metadata.dat + libil2cpp.so。\n下一步: 用 Il2CppDumper (电脑) 还原类结构，再用 IDA/Ghidra 读逻辑。"
        ),
        UNITY_MONO(
            "Unity (Mono)",
            "已提取 Assembly-CSharp.dll。\n下一步: 用 dnSpy (电脑) 直接看 C# 源码，几乎等于原始代码。"
        ),
        FLUTTER(
            "Flutter",
            "已提取 libapp.so。\n下一步: 用 blutter (电脑) 反编译 Dart 代码，或解压 flutter_assets。"
        ),
        COCOS(
            "Cocos Creator",
            "已提取 assets/src 下的 JS 脚本 (可能加密)。\n下一步: 如为 .jsc 加密，需找 cc-reverse 解密钥。"
        ),
        GODOT(
            "Godot",
            "已提取 assets 下的 .pck 或 .gdc。\n下一步: 用 gdsdecomp (电脑) 还原 .tscn + .gd 源码。"
        ),
        REACT_NATIVE(
            "React Native",
            "已提取 index.android.bundle。\n下一步: 用 JS 美化工具直接查看, 通常可读。"
        ),
        UNREAL(
            "Unreal Engine",
            "已提取 libUE4.so + Content。\n下一步: 用 FModel (电脑) 解包 .pak, 反编译难度极高。"
        ),
        NATIVE(
            "原生 Android (Java/Kotlin)",
            "已用 baksmali 反编译 dex 为 smali。\n下一步: 用 MT 管理器查看 smali/, 或电脑上用 jadx 转 Java。"
        ),
        UNKNOWN(
            "未知",
            "已通用解包 + smali 反编译。\n请手动分析 res/ 和 assets/。"
        )
    }

    data class Result(
        val engine: Engine,
        val evidence: List<String>,
        val outputDir: File
    )

    fun detect(entries: Set<String>): Pair<Engine, List<String>> {
        val ev = mutableListOf<String>()

        if (entries.any { it.matches(Regex("lib/[^/]+/libil2cpp\\.so")) }) {
            ev.add("libil2cpp.so")
            if (entries.contains("assets/bin/Data/Managed/Metadata/global-metadata.dat"))
                ev.add("global-metadata.dat")
            return Engine.UNITY_IL2CPP to ev
        }
        if (entries.any { it.matches(Regex("assets/bin/Data/Managed/Assembly-CSharp\\.dll")) }) {
            ev.add("Assembly-CSharp.dll")
            return Engine.UNITY_MONO to ev
        }
        if (entries.any { it.matches(Regex("lib/[^/]+/libflutter\\.so")) }) {
            ev.add("libflutter.so")
            if (entries.any { it.matches(Regex("lib/[^/]+/libapp\\.so")) }) ev.add("libapp.so")
            return Engine.FLUTTER to ev
        }
        if (entries.any { it.matches(Regex("lib/[^/]+/libcocos2djs\\.so")) } ||
            entries.any { it.startsWith("assets/src/") }) {
            ev.add("Cocos 标志")
            return Engine.COCOS to ev
        }
        if (entries.any { it.matches(Regex("lib/[^/]+/libgodot_android\\.so")) } ||
            entries.any { it.startsWith("assets/") && it.endsWith(".pck") }) {
            ev.add("Godot 标志")
            return Engine.GODOT to ev
        }
        if (entries.contains("assets/index.android.bundle")) {
            ev.add("index.android.bundle")
            return Engine.REACT_NATIVE to ev
        }
        if (entries.any { it.matches(Regex("lib/[^/]+/libUE4\\.so")) }) {
            ev.add("libUE4.so")
            return Engine.UNREAL to ev
        }
        if (entries.any { it == "classes.dex" }) {
            ev.add("classes.dex")
            return Engine.NATIVE to ev
        }
        return Engine.UNKNOWN to ev
    }

    fun decompile(
        context: Context,
        apkUri: Uri,
        apkName: String,
        onProgress: (Progress) -> Unit
    ): Result {
        val base = apkName.removeSuffix(".apk").replace(" ", "_")
        val root = File(Environment.getExternalStorageDirectory(), "Download/APK反编译/$base")
        if (root.exists()) root.deleteRecursively()
        root.mkdirs()
        val ext = File(root, "_raw")
        ext.mkdirs()

        onProgress(Progress(3, "扫描 APK 结构..."))

        // 1) 收集条目名 (用于检测)
        val entries = mutableSetOf<String>()
        context.contentResolver.openInputStream(apkUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                while (e != null) { entries.add(e.name); e = zis.nextEntry }
            }
        }

        val (engine, evidence) = detect(entries)
        onProgress(Progress(8, "识别引擎: ${engine.display}"))

        // 2) 解压全部
        val total = entries.size.coerceAtLeast(1)
        var done = 0
        val dexFiles = mutableListOf<File>()

        context.contentResolver.openInputStream(apkUri)?.use { input ->
            ZipInputStream(input).use { zis ->
                var e = zis.nextEntry
                val buf = ByteArray(8192)
                while (e != null) {
                    val name = e.name
                    val out = File(ext, name)
                    if (e.isDirectory) out.mkdirs()
                    else {
                        out.parentFile?.mkdirs()
                        out.outputStream().use { o ->
                            var n: Int
                            while (zis.read(buf).also { n = it } > 0) o.write(buf, 0, n)
                        }
                        if (name.matches(Regex("classes\\d*\\.dex"))) dexFiles.add(out)
                    }
                    done++
                    val pct = 10 + (done * 40 / total)
                    if (done % 8 == 0 || done == total)
                        onProgress(Progress(pct, "解压: $name"))
                    e = zis.nextEntry
                }
            }
        }

        // 3) 移动关键文件到顶层
        onProgress(Progress(52, "整理输出目录..."))
        val keep = listOf(
            "res", "assets", "lib", "META-INF", "AndroidManifest.xml",
            "resources.arsc", "classes.dex", "classes2.dex", "classes3.dex"
        )
        keep.forEach { n ->
            val s = File(ext, n)
            if (s.exists()) {
                val d = File(root, n)
                try {
                    if (s.isDirectory) s.copyRecursively(d, overwrite = true)
                    else s.copyTo(d, overwrite = true)
                } catch (_: Exception) {}
            }
        }

        // 4) 引擎专属提取
        when (engine) {
            Engine.UNITY_IL2CPP -> {
                listOf(
                    "assets/bin/Data/Managed/Metadata/global-metadata.dat",
                    "lib/arm64-v8a/libil2cpp.so",
                    "lib/armeabi-v7a/libil2cpp.so"
                ).forEach { n ->
                    val s = File(ext, n)
                    if (s.exists()) {
                        val d = File(root, File(n).name)
                        try { s.copyTo(d, overwrite = true) } catch (_: Exception) {}
                    }
                }
            }
            Engine.UNITY_MONO -> {
                val s = File(ext, "assets/bin/Data/Managed/Assembly-CSharp.dll")
                if (s.exists()) s.copyTo(File(root, "Assembly-CSharp.dll"), overwrite = true)
            }
            Engine.FLUTTER -> {
                listOf("lib/arm64-v8a/libapp.so", "lib/armeabi-v7a/libapp.so")
                    .forEach { n ->
                        val s = File(ext, n)
                        if (s.exists()) s.copyTo(File(root, "libapp.so"), overwrite = true)
                    }
            }
            Engine.GODOT -> {
                File(ext, "assets").listFiles()?.forEach { f ->
                    if (f.name.endsWith(".pck") || f.name.endsWith(".gdc"))
                        f.copyTo(File(root, f.name), overwrite = true)
                }
            }
            else -> {}
        }

        // 5) baksmali 反编译所有 dex
        if (dexFiles.isNotEmpty()) {
            onProgress(Progress(60, "共 ${dexFiles.size} 个 dex, 开始反编译..."))
            dexFiles.forEachIndexed { i, dex ->
                val sm = File(root, if (i == 0) "smali" else "smali_$i")
                sm.mkdirs()
                try {
                    val df = DexFileFactory.loadDexFile(dex, Opcodes.forApi(27))
                    val o = BaksmaliOptions()
                    o.jobs = 1
                    o.outputDirectory = sm
                    Baksmali.disassembleDexFile(df, 1, o)
                } catch (e: Exception) {
                    File(root, "smali_error_${dex.name}.txt")
                        .writeText(e.stackTraceToString())
                }
                val pct = 60 + ((i + 1) * 30 / dexFiles.size)
                onProgress(Progress(pct, "反编译 ${dex.name} 完成"))
            }
        }

        // 6) 生成分析报告
        onProgress(Progress(94, "生成分析报告..."))
        val report = buildString {
            appendLine("APK 分析报告")
            appendLine("=".repeat(40))
            appendLine("文件名: $apkName")
            appendLine("识别引擎: ${engine.display}")
            appendLine("识别依据: ${evidence.joinToString(", ")}")
            appendLine()
            appendLine("应对方案:")
            appendLine(engine.advice)
            appendLine()
            appendLine("输出结构:")
            appendLine("  smali*/     反编译后的 smali 源码")
            appendLine("  res/        资源文件")
            appendLine("  assets/     应用资源")
            appendLine("  lib/        native 库")
            appendLine("  AndroidManifest.xml")
            appendLine("  resources.arsc")
            if (engine == Engine.UNITY_IL2CPP) {
                appendLine("  global-metadata.dat  IL2CPP 元数据")
                appendLine("  libil2cpp.so         IL2CPP native 库")
            }
            if (engine == Engine.UNITY_MONO) {
                appendLine("  Assembly-CSharp.dll  Mono 程序集")
            }
            if (engine == Engine.FLUTTER) {
                appendLine("  libapp.so            Flutter AOT 库")
            }
            appendLine()
            appendLine("提示:")
            appendLine("  · 本应用离线运行, 不联网, 不上传")
            appendLine("  · smali 源码可用 MT 管理器直接查看")
            appendLine("  · 需要 Java 源码请复制到电脑用 jadx 转")
        }
        File(root, "分析报告.txt").writeText(report)

        // 7) 清理临时
        ext.deleteRecursively()

        onProgress(Progress(100, "完成 · ${engine.display}"))
        return Result(engine, evidence, root)
    }
}
