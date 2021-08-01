import org.apache.commons.codec.binary.Base64
import org.apache.pdfbox.pdmodel.PDDocument
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import kotlin.system.exitProcess

fun Int.toHexString(): String = Integer.toHexString(this).uppercase()

fun main() {
    // JV - YK
    val htmlFile = File("end.html")
    val innerPDF = "Crypt.pdf"
    val decryptedInnerPDF = "Decrypt.pdf"
    val htmlSize = htmlFile.length()
    val ras = RandomAccessFile(htmlFile, "r")
    val pdfHead = byteArrayOf(
        0x76, 0x61, 0x72, 0x20, 0x50, 0x44, 0x46, 0x44, 0x61, 0x74, 0x61, 0x20, 0x3D, 0x20, 0x22
    )
    val pdfTail = byteArrayOf(
        0x22, 0x3B, 0x0D, 0x0A
    )
    val pdfTailSize = pdfTail.size

    var isFindPdfHead = false
    var pdfHeadOffset = 0L

    var isFindPdfTail = false
    var pdfTailOffset: Long
    var pdfTailSeekTime = 1
    println("正在查找 PDF 文件头")
    rasLoop@ while (true) {
        for ((index, headByte) in pdfHead.withIndex()) {
            val nextByte = ByteArray(1)
            val isEOF = ras.read(nextByte)
            if (isEOF == -1) break@rasLoop
            pdfHeadOffset += 1
            if (nextByte[0] != headByte) break
            if (index == pdfHead.size - 1) {
                isFindPdfHead = true
                break@rasLoop
            }
        }
    }
    if (isFindPdfHead) {
        println("成功找到 PDF 文件头，偏移量为 : " + pdfHeadOffset.toInt().toHexString())
    } else {
        println("查找 PDF 文件头失败，建议手动操作！")
        exitProcess(-1)
    }
    println("正在查找 PDF 文件尾")
    rasLoop@ while (true) {
        // 将文件指针移动到 距离文件尾部还剩 pdfTailSize * pdfTailSeekTime 的位置
        pdfTailOffset = htmlSize - pdfTailSize * pdfTailSeekTime
        ras.seek(pdfTailOffset)
        pdfTailSeekTime += 1
        // 遍历 pdfTail
        for ((index, tailByte) in pdfTail.withIndex()) {
            val nextByte = ByteArray(1)
            val isEOF = ras.read(nextByte)
            // 如果到了文件尾，则退出循环
            if (isEOF == -1) break@rasLoop
            if (nextByte[0] != tailByte) break
            if (index == pdfTail.size - 1) {
                isFindPdfTail = true
                break@rasLoop
            }
        }
    }
    if (isFindPdfTail) {
        println("成功找到 PDF 文件尾，偏移量为 : " + pdfTailOffset.toInt().toHexString())
    } else {
        println("查找 PDF 文件尾失败，建议手动操作！")
        exitProcess(-1)
    }
    println("正在尝试获取 PDF 文件")

    val pdfSize = pdfTailOffset - pdfHeadOffset
    val pdfContent = ByteArray(pdfSize.toInt())
    ras.seek(pdfHeadOffset)
    ras.read(pdfContent)
    println("PDF 文件读取成功\n正在写出 PDF 文件")
    val decodedPDF = Base64.decodeBase64(pdfContent)
    val encryptedFile = FileOutputStream(innerPDF)
    encryptedFile.write(decodedPDF)
    encryptedFile.flush()
    encryptedFile.close()
    println("PDF 文件写出成功\n正在加载带密码的 PDF 文件")
    val passwd = "%qwerASDF.+"
    val doc = PDDocument.load(File(innerPDF), passwd)
    //加载带密码保护的PDF文件
    println("带密码的 PDF 文件加载成功\n正在尝试去除密码")
    //解除文档中的密码保护
    doc.isAllSecurityToBeRemoved = true
    println("PDF 文件密码去除成功！")
    //保存文件
    doc.save(decryptedInnerPDF)

    doc.close()
    ras.close()

    println("PDF 文件写出成功!")
    println("如果密码错误，建议手动从 html 里提取密码")
}