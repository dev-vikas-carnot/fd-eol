package com.carnot.fd.eol.utils

import android.content.Context
import android.os.Environment
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.property.TextAlignment
import java.io.File
import java.io.FileOutputStream

object PdfHelper {

    fun createSamplePdf(context: Context,vin:String,imei:String,status:String): String? {
        return try {
            // Path where the PDF will be saved
            val pdfFolder = File(
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "MyPdf"
            )
            if (!pdfFolder.exists()) {
                pdfFolder.mkdirs()
            }

            // Create a unique name for the PDF file
            val pdfFile = File(pdfFolder, "sample.pdf")

            // Create the PdfWriter instance
            val writer = PdfWriter(FileOutputStream(pdfFile))

            // Initialize the PDF document
            val pdfDocument = PdfDocument(writer)

            pdfDocument.defaultPageSize =   PageSize(255.12f, 113.39f) // 90mm x 40mm

            // Create a document to add content
            val document = Document(pdfDocument)

            // Set margin
            document.setMargins(5f, 5f, 5f, 5f)

            // Add centered content
            document.add(Paragraph("VIN: $vin")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("IMEI: $imei")
                .setFontSize(10f)
                .setTextAlignment(TextAlignment.CENTER))
            document.add(Paragraph("EOL Status: $status")
                .setFontSize(10f)
                .setFontColor(ColorConstants.BLACK)
                .setTextAlignment(TextAlignment.CENTER))


            // Close the document
            document.close()

            // Return the file path
            pdfFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()

//            logCrashError(
//                apiName = "Error while creating sample pdf function",
//                error = e,
//                message = e.message.toString()
//            )


            null
        }
    }
}
