package com.mart.villagemart.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val api: VillageMartApi,
    @ApplicationContext private val context: Context
) {
    suspend fun invoice(id: Long): Uri {
        val response = api.invoice(id)
        val name = "invoice-$id.pdf"
        return save(name) { target ->
            response.use { body -> target.writeBytes(body.bytes()) }
        }
    }

    suspend fun report(report: String, format: String, period: String = "THIS_MONTH"): Uri {
        require(report in EXPORTABLE_REPORTS) { "Unsupported report" }
        require(format in setOf("pdf", "xlsx")) { "Unsupported report format" }
        require(period.matches(Regex("[A-Z0-9_]+"))) { "Unsupported report period" }
        val url = "/api/v1/reports/$report?period=$period&format=$format"
        val body = api.download(url)
        val extension = if (format == "pdf") "pdf" else "xlsx"
        return save("$report-${System.currentTimeMillis()}.$extension") { target ->
            body.use { it.byteStream().use { input -> target.outputStream().use(input::copyTo) } }
        }
    }

    private suspend fun save(name: String, writer: suspend (File) -> Unit): Uri {
        val directory = File(context.cacheDir, "reports")
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not prepare report storage")
        }
        val file = File(directory, name)
        writer(file)
        return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    }

    companion object {
        val EXPORTABLE_REPORTS = setOf("profit", "daily-sales", "product-sales", "village-sales", "stock", "expenses")
    }
}
