import java.io.*
import java.util.ArrayList
import java.util.HashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Created by igushs on 4/7/2015.
 */


public fun main(args: Array<String>): Unit {
    if (args.size() < 3) {
        printUsages()
        return
    }
    ZipBatchWriter(File(args[0])).use {
        when (args[1]) {
            "-l" -> {
                it.addBatch(readTasks(File(args[2])))
            }
            "-f" -> {
                for (f in args.drop(2)) {
                    it.add(File(f))
                }
            }
            else -> printUsages()
        }
    }
}


private fun printUsages() {
    println("Usages:\n" +
            "out.zip -l list.txt\n" +
            "out.zip -f file1 file2 file3")
}

fun readTasks(f: File): ArrayList<ZipBatchWriter.BatchTask> {
    val result = ArrayList<ZipBatchWriter.BatchTask>()
    f.forEachLine {
        val parts = it.split("\\s+->\\s+")
        result.add(ZipBatchWriter.BatchTask(File(parts[0]), if (1 in parts.indices) parts[1] else null))
    }
    return result
}

val BUFFER_SIZE = 4096

class ZipBatchWriter(val output: ZipOutputStream) : Closeable {

    constructor(f: File) : this(ZipOutputStream(FileOutputStream(f)))

    val dirs = HashSet<String>()

    private fun writeFile(f: File, path: String): Boolean {
        if (!f.canRead()) {
            return false
        }
        val p = File(path)
        if (p.getParentFile() != null) {
            val dir = p.getParentFile().getPath() + "/"
            if (dirs.add(dir)) {
                output.putNextEntry(ZipEntry(dir))
                output.closeEntry()
            }
        }
        output.putNextEntry(ZipEntry(path))
        val istream = FileInputStream(f)
        val buffer = ByteArray(BUFFER_SIZE)
        var lastRead: Int
        try {
            istream.use {
                while (buffer.let { lastRead = istream.read(buffer); lastRead } > 0) {
                    output.write(buffer)
                }
            }
        } catch (e: Exception) {
            return false
        }
        output.closeEntry()
        return true;
    }

    public fun add(f: File, path: String? = null) {
        writeFile(f, path ?: f.getPath())
    }

    data class BatchTask(val f: File, val path: String? = null)

    public fun addBatch(tasks: List<BatchTask>) {
        for ((f, path) in tasks) {
            add(f, path)
        }
    }

    public override fun close() {
        output.flush()
        output.close()
    }
}