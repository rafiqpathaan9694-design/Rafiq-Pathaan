package com.example.pdf

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.StudentRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NeetFormPdfGenerator {

    // A4 dimensions in PostScript points (72 dpi): 595 x 842
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 34f
    private const val MARGIN_RIGHT = 34f
    private const val MARGIN_TOP = 32f
    private const val MARGIN_BOTTOM = 38f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    private val blackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 0.85f
    }

    private val thickBlackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
    }

    private val headerBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(236, 236, 236)
        style = Paint.Style.FILL
    }

    private val labelBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(248, 248, 248)
        style = Paint.Style.FILL
    }

    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 15.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val sectionTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 9.5f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(35, 35, 35)
        textSize = 8.2f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 8.6f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val smallPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(65, 65, 65)
        textSize = 7.4f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    suspend fun generatePdfFile(context: Context, record: StudentRecord): File =
        withContext(Dispatchers.IO) {
            val outDir = File(context.filesDir, "generated_pdfs").apply { if (!exists()) mkdirs() }
            val safeName = record.studentName
                .ifBlank { "STUDENT" }
                .replace(Regex("[^A-Za-z0-9]+"), "_")
                .trim('_')
                .take(24)
            val safeForm = record.formNumber.ifBlank { "DRAFT" }.replace(Regex("[^A-Za-z0-9-]+"), "")
            val outFile = File(outDir, "NEET2027_${safeName}_${safeForm}.pdf")

            val document = PdfDocument()
            try {
                renderRecordToPdfDocument(document, record)
                FileOutputStream(outFile).use { stream ->
                    document.writeTo(stream)
                }
            } finally {
                document.close()
            }
            outFile
        }

    suspend fun renderPdfToBitmaps(pdfFile: File): List<Bitmap> = withContext(Dispatchers.IO) {
        val bitmaps = mutableListOf<Bitmap>()
        ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                val scale = 2.0f
                for (i in 0 until renderer.pageCount) {
                    renderer.openPage(i).use { page ->
                        val width = (page.width * scale).toInt()
                        val height = (page.height * scale).toInt()
                        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        bitmaps.add(bmp)
                    }
                }
            }
        }
        bitmaps
    }

    fun savePdfToDownloads(context: Context, sourcePdf: File): Result<Uri> {
        return try {
            val fileName = sourcePdf.name
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(
                        MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/MobileSolution_NEET2027"
                    )
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return Result.failure(IllegalStateException("Could not create Downloads entry"))
                resolver.openOutputStream(uri)?.use { out ->
                    sourcePdf.inputStream().use { input -> input.copyTo(out) }
                } ?: return Result.failure(IllegalStateException("Could not open output stream"))
                Result.success(uri)
            } else {
                val downloadsDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "MobileSolution_NEET2027"
                ).apply { if (!exists()) mkdirs() }
                val dest = File(downloadsDir, fileName)
                sourcePdf.copyTo(dest, overwrite = true)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    dest
                )
                Result.success(uri)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createSharePdfIntent(context: Context, pdfFile: File, studentName: String): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "NEET 2027 Form - ${studentName.ifBlank { "Student" }}")
            putExtra(
                Intent.EXTRA_TEXT,
                "MOBILE SOLUTION - NEET 2027 Form Filling (Contact: 9694705574)\nStudent: ${studentName.ifBlank { "Student" }}"
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun printStudentForm(context: Context, record: StudentRecord) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = "NEET2027_${record.studentName.ifBlank { "Student" }.take(20)}"
        val attributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()
        printManager.print(jobName, NeetPrintDocumentAdapter(context, record), attributes)
    }

    private class NeetPrintDocumentAdapter(
        private val context: Context,
        private val record: StudentRecord
    ) : PrintDocumentAdapter() {

        private var totalPages = 1

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            totalPages = countPagesForRecord(record)
            val info = PrintDocumentInfo.Builder(
                "NEET2027_${record.formNumber.ifBlank { "FORM" }}.pdf"
            )
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(totalPages)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val printAttributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build()

            val pdfDocument = PrintedPdfDocument(context, printAttributes)
            try {
                renderRecordToPdfDocument(pdfDocument, record)
                if (cancellationSignal?.isCanceled == true) {
                    callback.onWriteCancelled()
                    return
                }
                FileOutputStream(destination.fileDescriptor).use { out ->
                    pdfDocument.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument.close()
            }
        }
    }

    private data class FormRow(
        val label: String,
        val value: String,
        val label2: String? = null,
        val value2: String? = null
    )

    private sealed class RenderBlock {
        data class SectionHeader(val title: String) : RenderBlock()
        data class Row(val row: FormRow) : RenderBlock()
        data class DocumentImagesPage(val items: List<Pair<String, String>>) : RenderBlock()
    }

    private fun buildBlocks(record: StudentRecord): List<RenderBlock> {
        val blocks = mutableListOf<RenderBlock>()

        fun addSection(title: String, rows: List<FormRow>) {
            blocks.add(RenderBlock.SectionHeader(title))
            rows.forEach { blocks.add(RenderBlock.Row(it)) }
        }

        // A. Personal Details (First 7 rows share top space with Photo/Sign on Page 1, handled naturally)
        addSection(
            "A. PERSONAL DETAILS",
            listOf(
                FormRow("Student Name", record.studentName.ifBlank { "-" }),
                FormRow("Date of Birth", record.dateOfBirth.ifBlank { "-" }, "Gender", record.gender.ifBlank { "-" }),
                FormRow("Father's Name", record.fatherName.ifBlank { "-" }),
                FormRow("Mother's Name", record.motherName.ifBlank { "-" }),
                FormRow("Aadhaar Number", record.aadhaarNumber.ifBlank { "-" }, "Category", record.category.ifBlank { "-" }),
                FormRow("Student Mobile", record.studentMobile.ifBlank { "-" }, "Religion", record.religion.ifBlank { "-" }),
                FormRow("Student Email ID", record.studentEmail.ifBlank { "-" }),
                FormRow("Emergency Mobile", record.emergencyMobile.ifBlank { "-" }, "PwD (Disability)", record.personWithDisability.ifBlank { "-" }),
                FormRow("Emergency Email ID", record.emergencyEmail.ifBlank { "-" }),
                FormRow("Category Certificate", record.categoryCertificate.ifBlank { "-" }, "Cert. ID / No.", record.categoryCertificateId.ifBlank { "-" }),
                FormRow("Issuing Authority", record.issuingAuthorityName.ifBlank { "-" }, "Issue Date", record.certificateIssueDate.ifBlank { "-" }),
                FormRow("Domicile State", record.domicileState.ifBlank { "-" }, "Annual Income", record.familyAnnualIncome.ifBlank { "-" }),
                FormRow("Question Paper Medium", record.questionPaperMedium.ifBlank { "-" }, "Only Girl Child", record.onlyGirlChild.ifBlank { "-" })
            )
        )

        // B. Matriculation (10th) Details
        addSection(
            "B. MATRICULATION (10TH) DETAILS",
            listOf(
                FormRow("Year of Passing", record.tenthPassingYear.ifBlank { "-" }, "Roll Number", record.tenthRollNumber.ifBlank { "-" }),
                FormRow("Percentage", record.tenthPercentage.ifBlank { "-" }, "Total Marks", record.tenthTotalMarks.ifBlank { "-" }),
                FormRow("Name of Board", record.tenthBoardName.ifBlank { "-" }),
                FormRow("Name of School", record.tenthSchoolName.ifBlank { "-" }),
                FormRow("School Address", record.tenthSchoolAddress.ifBlank { "-" }, "Area PIN Code", record.tenthAreaPinCode.ifBlank { "-" })
            )
        )

        // C. Higher Secondary (12th) Details
        addSection(
            "C. HIGHER SECONDARY (12TH) DETAILS",
            listOf(
                FormRow("Year of Passing", record.twelfthPassingYear.ifBlank { "-" }, "Roll Number", record.twelfthRollNumber.ifBlank { "-" }),
                FormRow("Percentage", record.twelfthPercentage.ifBlank { "-" }, "Total Marks", record.twelfthTotalMarks.ifBlank { "-" }),
                FormRow("Name of Board", record.twelfthBoardName.ifBlank { "-" }),
                FormRow("Name of School", record.twelfthSchoolName.ifBlank { "-" }),
                FormRow("School Address", record.twelfthSchoolAddress.ifBlank { "-" }, "Area PIN Code", record.twelfthAreaPinCode.ifBlank { "-" })
            )
        )

        // D. NEET Exam City Preferences
        addSection(
            "D. NEET EXAM CITY PREFERENCES",
            listOf(
                FormRow("First Choice", record.examCityChoice1.ifBlank { "-" }, "Second Choice", record.examCityChoice2.ifBlank { "-" }),
                FormRow("Third Choice", record.examCityChoice3.ifBlank { "-" })
            )
        )

        // E. Guardian / Parents Details
        addSection(
            "E. GUARDIAN / PARENTS DETAILS",
            listOf(
                FormRow("Father's Qualification", record.fatherQualification.ifBlank { "-" }, "Father's Occupation", record.fatherOccupation.ifBlank { "-" }),
                FormRow("Mother's Qualification", record.motherQualification.ifBlank { "-" }, "Mother's Occupation", record.motherOccupation.ifBlank { "-" })
            )
        )

        // F. Address Details
        addSection(
            "F. ADDRESS DETAILS",
            listOf(
                FormRow("Full Aadhaar Address", record.fullAadhaarAddress.ifBlank { "-" }, "PIN Code", record.addressPinCode.ifBlank { "-" })
            )
        )

        // G. Documents Checklist
        fun statusOf(path: String, optional: Boolean = false): String = when {
            path.isNotBlank() && File(path).exists() -> "ATTACHED"
            optional -> "NOT ATTACHED (OPTIONAL)"
            else -> "MISSING"
        }
        addSection(
            "G. DOCUMENTS CHECKLIST",
            listOf(
                FormRow("Student Photograph", statusOf(record.photoPath), "Student Signature", statusOf(record.signaturePath)),
                FormRow("Aadhaar Card Photo", statusOf(record.aadhaarPhotoPath), "10th Marksheet", statusOf(record.tenthMarksheetPath)),
                FormRow("12th Marksheet", statusOf(record.twelfthMarksheetPath), "Category Certificate", statusOf(record.categoryCertificatePath, optional = true))
            )
        )

        val attachedProofs = listOf(
            "Aadhaar Card Photo" to record.aadhaarPhotoPath,
            "10th Marksheet" to record.tenthMarksheetPath,
            "12th Marksheet" to record.twelfthMarksheetPath,
            "Category Certificate" to record.categoryCertificatePath
        ).filter { (_, p) -> p.isNotBlank() && File(p).exists() }

        if (attachedProofs.isNotEmpty()) {
            blocks.add(RenderBlock.DocumentImagesPage(attachedProofs))
        }

        return blocks
    }

    internal fun countPagesForRecord(record: StudentRecord): Int {
        return computeTotalPages(buildBlocks(record))
    }

    private fun renderRecordToPdfDocument(document: PdfDocument, record: StudentRecord) {
        // Two-pass or buffered canvas approach: render onto pages first?
        // Actually PdfDocument doesn't let you edit a finished page after finishPage().
        // So we dry-run layout pagination to get exact totalPages, then draw cleanly with exact "Page X of Y"!
        val blocks = buildBlocks(record)
        val totalPages = computeTotalPages(blocks)

        var pageNum = 1
        var page = startNewPage(document, pageNum, record, isFirstPage = true)
        var canvas = page.canvas
        var y = MARGIN_TOP + 70f // Aligns cleanly with top-right Photo & Signature block
        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM - 22f

        // Draw Photo & Signature box in top-right of Page 1
        drawPhotoAndSignatureHeader(canvas, record)

        val photoClearY = MARGIN_TOP + 230f // Clears both Photo (110pt) and Signature (40pt)
        val leftTableWidthWithPhoto = CONTENT_WIDTH - 104f

        for (block in blocks) {
            when (block) {
                is RenderBlock.SectionHeader -> {
                    val effectiveWidth = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    val headerH = 19f
                    if (y + headerH + 20f > maxY) {
                        drawFooter(canvas, pageNum, totalPages)
                        document.finishPage(page)
                        pageNum++
                        page = startNewPage(document, pageNum, record, isFirstPage = false)
                        canvas = page.canvas
                        y = MARGIN_TOP + 40f
                    }
                    y += 5f
                    val w = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    drawSectionHeader(canvas, MARGIN_LEFT, y, w, headerH, block.title)
                    y += headerH
                }

                is RenderBlock.Row -> {
                    val w = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    val rowH = measureRowHeight(block.row, w)
                    if (y + rowH > maxY) {
                        drawFooter(canvas, pageNum, totalPages)
                        document.finishPage(page)
                        pageNum++
                        page = startNewPage(document, pageNum, record, isFirstPage = false)
                        canvas = page.canvas
                        y = MARGIN_TOP + 40f
                    }
                    val drawWidth = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    val actualH = measureRowHeight(block.row, drawWidth)
                    drawRow(canvas, MARGIN_LEFT, y, drawWidth, actualH, block.row)
                    y += actualH
                }

                is RenderBlock.DocumentImagesPage -> {
                    drawFooter(canvas, pageNum, totalPages)
                    document.finishPage(page)
                    pageNum++
                    page = startNewPage(document, pageNum, record, isFirstPage = false)
                    canvas = page.canvas
                    y = MARGIN_TOP + 42f

                    drawSectionHeader(canvas, MARGIN_LEFT, y, CONTENT_WIDTH, 20f, "ATTACHED DOCUMENT PROOFS")
                    y += 28f

                    val colGap = 12f
                    val boxW = (CONTENT_WIDTH - colGap) / 2f
                    val boxH = 290f

                    block.items.forEachIndexed { idx, (title, path) ->
                        val col = idx % 2
                        if (idx > 0 && col == 0) {
                            y += boxH + 14f
                        }
                        if (y + boxH > maxY) {
                            drawFooter(canvas, pageNum, totalPages)
                            document.finishPage(page)
                            pageNum++
                            page = startNewPage(document, pageNum, record, isFirstPage = false)
                            canvas = page.canvas
                            y = MARGIN_TOP + 42f
                            drawSectionHeader(canvas, MARGIN_LEFT, y, CONTENT_WIDTH, 20f, "ATTACHED DOCUMENT PROOFS (CONT.)")
                            y += 28f
                        }
                        val x = MARGIN_LEFT + col * (boxW + colGap)
                        drawAttachedDocBox(canvas, x, y, boxW, boxH, title, path)
                    }
                }
            }
        }

        drawFooter(canvas, pageNum, totalPages)
        document.finishPage(page)
    }

    private fun computeTotalPages(blocks: List<RenderBlock>): Int {
        var pageNum = 1
        var y = MARGIN_TOP + 70f
        val maxY = PAGE_HEIGHT - MARGIN_BOTTOM - 22f
        val photoClearY = MARGIN_TOP + 230f
        val leftTableWidthWithPhoto = CONTENT_WIDTH - 104f

        for (block in blocks) {
            when (block) {
                is RenderBlock.SectionHeader -> {
                    val headerH = 19f
                    if (y + headerH + 20f > maxY) {
                        pageNum++
                        y = MARGIN_TOP + 40f
                    }
                    y += 5f + headerH
                }

                is RenderBlock.Row -> {
                    val w = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    val rowH = measureRowHeight(block.row, w)
                    if (y + rowH > maxY) {
                        pageNum++
                        y = MARGIN_TOP + 40f
                    }
                    val drawWidth = if (pageNum == 1 && y < photoClearY) leftTableWidthWithPhoto else CONTENT_WIDTH
                    y += measureRowHeight(block.row, drawWidth)
                }

                is RenderBlock.DocumentImagesPage -> {
                    pageNum++
                    y = MARGIN_TOP + 42f + 28f
                    val boxH = 290f
                    block.items.forEachIndexed { idx, _ ->
                        val col = idx % 2
                        if (idx > 0 && col == 0) {
                            y += boxH + 14f
                        }
                        if (y + boxH > maxY) {
                            pageNum++
                            y = MARGIN_TOP + 42f + 28f
                        }
                    }
                }
            }
        }
        return pageNum
    }

    private fun startNewPage(
        document: PdfDocument,
        pageNum: Int,
        record: StudentRecord,
        isFirstPage: Boolean
    ): PdfDocument.Page {
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        canvas.drawColor(Color.WHITE)

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ENGLISH).format(Date())

        if (isFirstPage) {
            val headerBox = RectF(MARGIN_LEFT, MARGIN_TOP, PAGE_WIDTH - MARGIN_RIGHT, MARGIN_TOP + 62f)
            canvas.drawRect(headerBox, thickBlackPaint)
            canvas.drawText("MOBILE SOLUTION", MARGIN_LEFT + 10f, MARGIN_TOP + 20f, titlePaint)
            canvas.drawText("NEET 2027 FORM FILLING — STUDENT DATA SHEET", MARGIN_LEFT + 10f, MARGIN_TOP + 36f, subtitlePaint)
            canvas.drawText(
                "Contact: 9694705574   |   Form No: ${record.formNumber.ifBlank { "NEW" }}   |   Status: ${record.status}",
                MARGIN_LEFT + 10f,
                MARGIN_TOP + 52f,
                smallPaint
            )

            canvas.drawText("Generated: $dateStr", PAGE_WIDTH - MARGIN_RIGHT - 115f, MARGIN_TOP + 20f, smallPaint)
        } else {
            val headerBox = RectF(MARGIN_LEFT, MARGIN_TOP, PAGE_WIDTH - MARGIN_RIGHT, MARGIN_TOP + 30f)
            canvas.drawRect(headerBox, blackPaint)
            canvas.drawText(
                "MOBILE SOLUTION  |  NEET 2027 FORM FILLING  |  Contact: 9694705574",
                MARGIN_LEFT + 8f,
                MARGIN_TOP + 13f,
                labelPaint
            )
            canvas.drawText(
                "Student: ${record.studentName.ifBlank { "-" }}  |  Form No: ${record.formNumber.ifBlank { "NEW" }}",
                MARGIN_LEFT + 8f,
                MARGIN_TOP + 24f,
                smallPaint
            )
        }

        return page
    }

    private fun drawPhotoAndSignatureHeader(canvas: Canvas, record: StudentRecord) {
        val boxW = 94f
        val x = PAGE_WIDTH - MARGIN_RIGHT - boxW
        val photoTop = MARGIN_TOP + 70f
        val photoH = 110f

        // Photo Frame
        val photoRect = RectF(x, photoTop, x + boxW, photoTop + photoH)
        canvas.drawRect(photoRect, blackPaint)
        val photoBmp = loadBitmapForPdf(record.photoPath, 420)
        if (photoBmp != null) {
            drawBitmapCenterCrop(canvas, photoBmp, RectF(x + 2f, photoTop + 2f, x + boxW - 2f, photoTop + photoH - 14f))
            photoBmp.recycle()
        } else {
            canvas.drawText("STUDENT PHOTO", x + 13f, photoTop + 52f, smallPaint)
        }
        canvas.drawLine(x, photoTop + photoH - 13f, x + boxW, photoTop + photoH - 13f, blackPaint)
        canvas.drawText("STUDENT PHOTO", x + 16f, photoTop + photoH - 3.5f, smallPaint)

        // Signature Frame
        val sigTop = photoTop + photoH + 6f
        val sigH = 40f
        val sigRect = RectF(x, sigTop, x + boxW, sigTop + sigH)
        canvas.drawRect(sigRect, blackPaint)
        val sigBmp = loadBitmapForPdf(record.signaturePath, 360)
        if (sigBmp != null) {
            drawBitmapFitInside(canvas, sigBmp, RectF(x + 2f, sigTop + 2f, x + boxW - 2f, sigTop + sigH - 12f))
            sigBmp.recycle()
        } else {
            canvas.drawText("SIGNATURE", x + 24f, sigTop + 17f, smallPaint)
        }
        canvas.drawLine(x, sigTop + sigH - 11f, x + boxW, sigTop + sigH - 11f, blackPaint)
        canvas.drawText("SIGNATURE", x + 26f, sigTop + sigH - 3f, smallPaint)
    }

    private fun drawSectionHeader(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, title: String) {
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRect(rect, headerBgPaint)
        canvas.drawRect(rect, blackPaint)
        canvas.drawText(title, x + 7f, y + h - 5.5f, sectionTitlePaint)
    }

    private fun measureRowHeight(row: FormRow, totalWidth: Float): Float {
        return if (row.label2 == null) {
            val labelW = totalWidth * 0.34f
            val valW = totalWidth - labelW - 10f
            val valLines = buildStaticLayout(row.value, valuePaint, valW.toInt().coerceAtLeast(40))
            val lblLines = buildStaticLayout(row.label, labelPaint, (labelW - 8f).toInt().coerceAtLeast(40))
            maxOf(valLines.height, lblLines.height).toFloat() + 9f
        } else {
            val halfW = totalWidth / 2f
            val lblW = halfW * 0.42f
            val valW = halfW - lblW - 8f
            val h1 = maxOf(
                buildStaticLayout(row.label, labelPaint, (lblW - 6f).toInt().coerceAtLeast(30)).height,
                buildStaticLayout(row.value, valuePaint, valW.toInt().coerceAtLeast(30)).height
            )
            val h2 = maxOf(
                buildStaticLayout(row.label2, labelPaint, (lblW - 6f).toInt().coerceAtLeast(30)).height,
                buildStaticLayout(row.value2.orEmpty(), valuePaint, valW.toInt().coerceAtLeast(30)).height
            )
            maxOf(h1, h2).toFloat() + 9f
        }.coerceAtLeast(18f)
    }

    private fun drawRow(canvas: Canvas, x: Float, y: Float, totalWidth: Float, rowH: Float, row: FormRow) {
        if (row.label2 == null) {
            val labelW = totalWidth * 0.34f
            canvas.drawRect(x, y, x + labelW, y + rowH, labelBgPaint)
            canvas.drawRect(x, y, x + totalWidth, y + rowH, blackPaint)
            canvas.drawLine(x + labelW, y, x + labelW, y + rowH, blackPaint)

            drawStaticText(canvas, row.label, labelPaint, x + 5f, y + 4.5f, (labelW - 10f).toInt().coerceAtLeast(30))
            drawStaticText(canvas, row.value, valuePaint, x + labelW + 5f, y + 4.5f, (totalWidth - labelW - 10f).toInt().coerceAtLeast(30))
        } else {
            val halfW = totalWidth / 2f
            val lblW = halfW * 0.42f

            canvas.drawRect(x, y, x + lblW, y + rowH, labelBgPaint)
            canvas.drawRect(x + halfW, y, x + halfW + lblW, y + rowH, labelBgPaint)
            canvas.drawRect(x, y, x + totalWidth, y + rowH, blackPaint)
            canvas.drawLine(x + lblW, y, x + lblW, y + rowH, blackPaint)
            canvas.drawLine(x + halfW, y, x + halfW, y + rowH, blackPaint)
            canvas.drawLine(x + halfW + lblW, y, x + halfW + lblW, y + rowH, blackPaint)

            drawStaticText(canvas, row.label, labelPaint, x + 4f, y + 4.5f, (lblW - 8f).toInt().coerceAtLeast(25))
            drawStaticText(canvas, row.value, valuePaint, x + lblW + 4f, y + 4.5f, (halfW - lblW - 8f).toInt().coerceAtLeast(25))

            drawStaticText(canvas, row.label2, labelPaint, x + halfW + 4f, y + 4.5f, (lblW - 8f).toInt().coerceAtLeast(25))
            drawStaticText(canvas, row.value2.orEmpty(), valuePaint, x + halfW + lblW + 4f, y + 4.5f, (halfW - lblW - 8f).toInt().coerceAtLeast(25))
        }
    }

    private fun drawAttachedDocBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        title: String,
        path: String
    ) {
        val headerH = 18f
        canvas.drawRect(x, y, x + w, y + headerH, headerBgPaint)
        canvas.drawRect(x, y, x + w, y + h, blackPaint)
        canvas.drawLine(x, y + headerH, x + w, y + headerH, blackPaint)
        canvas.drawText(title, x + 6f, y + 12.5f, labelPaint)

        val bmp = loadBitmapForPdf(path, 900)
        if (bmp != null) {
            drawBitmapFitInside(canvas, bmp, RectF(x + 4f, y + headerH + 4f, x + w - 4f, y + h - 4f))
            bmp.recycle()
        }
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int) {
        val footerY = PAGE_HEIGHT - MARGIN_BOTTOM + 6f
        canvas.drawLine(MARGIN_LEFT, footerY - 8f, PAGE_WIDTH - MARGIN_RIGHT, footerY - 8f, blackPaint)
        canvas.drawText(
            "MOBILE SOLUTION (9694705574) • Student Form Assistance Sheet (Not an Official NTA Submission)",
            MARGIN_LEFT,
            footerY + 4f,
            smallPaint
        )
        val pageText = "Page $pageNum of $totalPages"
        val tw = smallPaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN_RIGHT - tw, footerY + 4f, labelPaint)
    }

    private fun buildStaticLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.05f)
            .setIncludePad(false)
            .build()
    }

    private fun drawStaticText(canvas: Canvas, text: String, paint: TextPaint, x: Float, y: Float, width: Int) {
        val layout = buildStaticLayout(text, paint, width)
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun loadBitmapForPdf(path: String, maxDim: Int): Bitmap? {
        if (path.isBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var sample = 1
            var maxSide = maxOf(bounds.outWidth, bounds.outHeight)
            while (maxSide / 2 >= maxDim) {
                maxSide /= 2
                sample *= 2
            }
            BitmapFactory.decodeFile(file.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (_: Exception) {
            null
        }
    }

    private fun drawBitmapFitInside(canvas: Canvas, bitmap: Bitmap, dest: RectF) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val scale = minOf(dest.width() / bw, dest.height() / bh)
        val drawW = bw * scale
        val drawH = bh * scale
        val left = dest.left + (dest.width() - drawW) / 2f
        val top = dest.top + (dest.height() - drawH) / 2f
        canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), bitmapPaint)
    }

    private fun drawBitmapCenterCrop(canvas: Canvas, bitmap: Bitmap, dest: RectF) {
        val bw = bitmap.width.toFloat()
        val bh = bitmap.height.toFloat()
        if (bw <= 0f || bh <= 0f) return
        val destRatio = dest.width() / dest.height()
        val bmpRatio = bw / bh
        val srcRect = if (bmpRatio > destRatio) {
            val targetW = bh * destRatio
            val left = ((bw - targetW) / 2f).toInt()
            android.graphics.Rect(left, 0, (left + targetW).toInt(), bitmap.height)
        } else {
            val targetH = bw / destRatio
            val top = ((bh - targetH) / 2f).toInt()
            android.graphics.Rect(0, top, bitmap.width, (top + targetH).toInt())
        }
        canvas.drawBitmap(bitmap, srcRect, dest, bitmapPaint)
    }
}
