package com.example.semesterticket

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.semesterticket.ui.theme.SemesterticketTheme
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.FileKit
import io.github.vinceglb.filekit.core.PickerMode
import io.github.vinceglb.filekit.core.PickerType
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FileKit.init(this)
        enableEdgeToEdge()
        setContent {
            SemesterticketTheme {
                Box(Modifier.fillMaxSize()) {
                    QRCodeScreen(
                        context = LocalContext.current
                    )
                }
            }
        }
    }
}

@Composable
fun QRCodeScreen(context: Context) {
    val launcher = rememberFilePickerLauncher(
        type = PickerType.File(extensions = listOf("pdf")),
        mode = PickerMode.Single,
    ) { file ->
        if (file != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(file.uri)
                val newPdf = File(context.filesDir, "ticket.pdf")
                val outputStream = FileOutputStream(newPdf)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                generateBitmap(context)
            } catch (e: Exception) {
                // TODO: Error handling
                e.printStackTrace()
            }
        }
    }
    Scaffold (
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingOpenPDF {
                context.startActivity (
                    PdfViewerActivity.launchPdfFromPath(
                        context = context,
                        path = context.filesDir.path + "/ticket.pdf",
                        pdfTitle = "Semesterticket",
                        saveTo = saveTo.ASK_EVERYTIME,
                    )
                )
            }
        }
    ) { innerPadding ->
        Box (modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            Box (modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 80.dp)) {
                Text(
                    text= "Semesterticket",
                    fontSize = 30.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                )
                ElevatedCard (
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 6.dp
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .wrapContentHeight()
                        .offset(y = 50.dp)
                    ) {

                    val fileName = context.filesDir.path + "/qrcode"
                    if (File(fileName).exists()) {
                        val bitmap = BitmapFactory.decodeFile(fileName)
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "QR code of the ticket",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(400.dp)
                                .padding(start = 20.dp, top = 20.dp, bottom = 40.dp, end = 20.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                        )
                    }
                }
            }
            IconButton (
                onClick = {launcher.launch()},
                modifier = Modifier
                    .align(Alignment.TopEnd),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.upload),
                    contentDescription = "Icon",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}

@Composable
fun FloatingOpenPDF(onClick: () -> Unit) {
    ExtendedFloatingActionButton (
        onClick = onClick,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.file_open),
            contentDescription = "Open PDF")
        Text(text = " PDF öffnen")
    }
}


var resolutionModifier = 18

fun generateBitmap(context: Context) {
    val fileDescriptor = ParcelFileDescriptor.open(
        File(context.filesDir.path + "/ticket.pdf"),
        ParcelFileDescriptor.MODE_READ_ONLY
    )
    val pdfRenderer = PdfRenderer(fileDescriptor)
    val page = pdfRenderer.openPage(0)

    val bitmap = Bitmap.createBitmap(
        page.width * resolutionModifier,
        page.height * resolutionModifier,
        Bitmap.Config.ARGB_8888
    )
    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

    val croppedBitmap = Bitmap.createBitmap(
        bitmap,
        226 * resolutionModifier,
        98 * resolutionModifier,
        115 * resolutionModifier,
        115 * resolutionModifier
    )

    val newBitmapFile = File(context.filesDir, "qrcode")
    val bos = ByteArrayOutputStream()
    croppedBitmap.compress(Bitmap.CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
    val bitmapdata = bos.toByteArray()
    val fos = FileOutputStream(newBitmapFile)
    fos.write(bitmapdata)
    fos.flush()
    fos.close()

    page.close()
    pdfRenderer.close()
}