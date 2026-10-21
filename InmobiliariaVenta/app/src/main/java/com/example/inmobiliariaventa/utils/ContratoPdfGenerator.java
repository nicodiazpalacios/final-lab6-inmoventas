package com.example.inmobiliariaventa.utils;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.inmobiliariaventa.R;
import com.example.inmobiliariaventa.data.repository.EmpresaRepository;
import com.example.inmobiliariaventa.domain.entity.Contrato;
import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ContratoPdfGenerator {

    // A4 dimensions in PostScript points (72 dpi)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 40;

    public static void mostrarDialogoExportarPdf(Context context, Contrato contrato) {
        if (context == null || contrato == null) return;

        File pdfFile = generarPdf(context, contrato);
        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(context, "No se pudo generar el PDF del contrato", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog viewerDialog = new Dialog(context, android.R.style.Theme_Material_Light_NoActionBar);
        viewerDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_visor_pdf_contrato, null);
        viewerDialog.setContentView(dialogView);
        viewerDialog.setCancelable(true);

        if (viewerDialog.getWindow() != null) {
            Window window = viewerDialog.getWindow();
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setStatusBarColor(Color.parseColor("#00164E"));
            window.setNavigationBarColor(Color.WHITE);
            WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(window, dialogView);
            if (controller != null) {
                controller.setAppearanceLightStatusBars(false);
                controller.setAppearanceLightNavigationBars(true);
            }
        }

        View headerLayout = dialogView.findViewById(R.id.layout_visor_pdf_header);
        View bottomLayout = dialogView.findViewById(R.id.layout_visor_pdf_bottom);
        final float density = context.getResources().getDisplayMetrics().density;
        final int pad12 = Math.round(12 * density);
        final int pad14 = Math.round(14 * density);

        ViewCompat.setOnApplyWindowInsetsListener(dialogView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()
            );
            if (headerLayout != null) {
                headerLayout.setPadding(
                        pad12 + insets.left,
                        pad12 + insets.top,
                        pad12 + insets.right,
                        pad12
                );
            }
            if (bottomLayout != null) {
                bottomLayout.setPadding(
                        pad14 + insets.left,
                        pad12,
                        pad14 + insets.right,
                        pad12 + insets.bottom
                );
            }
            return windowInsets;
        });

        TextView tvTitulo = dialogView.findViewById(R.id.tv_titulo_visor_pdf);
        ImageView ivPreview = dialogView.findViewById(R.id.iv_pdf_page_preview);
        ImageButton btnCerrar = dialogView.findViewById(R.id.btn_cerrar_visor_pdf);
        MaterialButton btnDescargar = dialogView.findViewById(R.id.btn_pdf_descargar);
        MaterialButton btnImprimir = dialogView.findViewById(R.id.btn_pdf_imprimir);
        MaterialButton btnCompartir = dialogView.findViewById(R.id.btn_pdf_compartir);

        if (tvTitulo != null) {
            tvTitulo.setText(pdfFile.getName());
        }

        // Renderizar el archivo PDF directamente en el visor con alta definición
        Bitmap renderedBitmap = renderizarPaginaPdf(pdfFile);
        if (ivPreview != null && renderedBitmap != null) {
            ivPreview.setImageBitmap(renderedBitmap);
        }

        if (btnCerrar != null) {
            btnCerrar.setOnClickListener(v -> viewerDialog.dismiss());
        }

        if (btnDescargar != null) {
            btnDescargar.setOnClickListener(v -> {
                String savedName = guardarEnDescargas(context, pdfFile);
                if (savedName != null) {
                    Toast.makeText(context, "PDF guardado en Descargas: " + savedName, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, "Error al guardar el PDF en Descargas", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnImprimir != null) {
            btnImprimir.setOnClickListener(v -> imprimirPdf(context, pdfFile, contrato));
        }

        if (btnCompartir != null) {
            btnCompartir.setOnClickListener(v -> compartirPdf(context, pdfFile, contrato));
        }

        viewerDialog.show();
    }

    private static Bitmap renderizarPaginaPdf(File pdfFile) {
        try (ParcelFileDescriptor pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
             PdfRenderer renderer = new PdfRenderer(pfd);
             PdfRenderer.Page page = renderer.openPage(0)) {

            int width = page.getWidth() * 2;
            int height = page.getHeight() * 2;
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.WHITE);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
            return bitmap;
        } catch (Exception e) {
            return null;
        }
    }

    public static void imprimirPdf(Context context, File pdfFile, Contrato contrato) {
        if (context == null || pdfFile == null || !pdfFile.exists()) return;
        try {
            PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);
            if (printManager == null) {
                Toast.makeText(context, "Servicio de impresión no disponible", Toast.LENGTH_SHORT).show();
                return;
            }
            String jobName = pdfFile.getName();
            printManager.print(jobName, new PrintDocumentAdapter() {
                @Override
                public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                                     CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {
                    if (cancellationSignal.isCanceled()) {
                        callback.onLayoutCancelled();
                        return;
                    }
                    PrintDocumentInfo info = new PrintDocumentInfo.Builder(pdfFile.getName())
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build();
                    callback.onLayoutFinished(info, true);
                }

                @Override
                public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                                    CancellationSignal cancellationSignal, WriteResultCallback callback) {
                    try (FileInputStream in = new FileInputStream(pdfFile);
                         FileOutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
                        byte[] buf = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = in.read(buf)) > 0 && !cancellationSignal.isCanceled()) {
                            out.write(buf, 0, bytesRead);
                        }
                        if (cancellationSignal.isCanceled()) {
                            callback.onWriteCancelled();
                        } else {
                            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                        }
                    } catch (Exception e) {
                        callback.onWriteFailed(e.getMessage());
                    }
                }
            }, null);
        } catch (Exception e) {
            Toast.makeText(context, "No se pudo iniciar la impresión", Toast.LENGTH_SHORT).show();
        }
    }

    public static File generarPdf(Context context, Contrato contrato) {
        if (context == null || contrato == null) return null;

        EmpresaRepository empresaRepo = new EmpresaRepository(context);
        String nombreEmpresa = empresaRepo.getNombreEmpresaInmediato();
        String telefonoEmpresa = empresaRepo.getTelefonoWhatsappInmediato();
        String emailEmpresa = empresaRepo.getEmailInmediato();
        String direccionEmpresa = empresaRepo.getDireccionInmediata();

        NumberFormat currencyFormat = NumberFormat.getNumberInstance(Locale.US);
        currencyFormat.setMaximumFractionDigits(0);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int contentWidth = PAGE_WIDTH - (MARGIN * 2);
        float y = MARGIN;

        // 1. Header Banner (Primary Navy Blue)
        paint.setColor(Color.parseColor("#00164E"));
        RectF headerRect = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 86);
        canvas.drawRoundRect(headerRect, 12f, 12f, paint);

        // Company Name in Header
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(18f);
        canvas.drawText(nombreEmpresa != null ? nombreEmpresa.toUpperCase(Locale.ROOT) : "INMOBILIARIA VENTA", MARGIN + 20, y + 32, paint);

        // Subtitle in Header
        paint.setColor(Color.parseColor("#B6C4FF"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(10.5f);
        canvas.drawText("DETALLE Y CONSTANCIA DE ACUERDO INMOBILIARIO", MARGIN + 20, y + 50, paint);

        String contactoEmpresa = (direccionEmpresa != null ? direccionEmpresa : "")
                + (telefonoEmpresa != null && !telefonoEmpresa.isEmpty() ? "  |  Tel: " + telefonoEmpresa : "")
                + (emailEmpresa != null && !emailEmpresa.isEmpty() ? "  |  " + emailEmpresa : "");
        paint.setTextSize(9f);
        paint.setColor(Color.parseColor("#DCE1FF"));
        canvas.drawText(contactoEmpresa, MARGIN + 20, y + 68, paint);

        y += 108;

        // 2. Meta bar: Fecha de emisión & Estado del contrato
        String fechaEmision = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());
        String estado = contrato.estado != null && !contrato.estado.isEmpty() ? contrato.estado : "Activo";
        String codigoRef = contrato.id != null && contrato.id.length() >= 8
                ? contrato.id.substring(0, 8).toUpperCase(Locale.ROOT)
                : "CNT-001";

        paint.setColor(Color.parseColor("#F3F4F9"));
        RectF metaRect = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 36);
        canvas.drawRoundRect(metaRect, 8f, 8f, paint);

        paint.setColor(Color.parseColor("#44474F"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(10f);
        canvas.drawText("REF: #" + codigoRef + "   •   EMISIÓN: " + fechaEmision, MARGIN + 14, y + 22, paint);

        // Estado badge on right
        int badgeColor = Color.parseColor("#006C49");
        if ("Pendiente".equalsIgnoreCase(estado)) {
            badgeColor = Color.parseColor("#BA8400");
        } else if ("Cancelado".equalsIgnoreCase(estado)) {
            badgeColor = Color.parseColor("#BA1A1A");
        }
        paint.setColor(badgeColor);
        String estadoLabel = "ESTADO: " + estado.toUpperCase(Locale.ROOT);
        float badgeTextWidth = paint.measureText(estadoLabel);
        RectF badgeRect = new RectF(PAGE_WIDTH - MARGIN - badgeTextWidth - 28, y + 7, PAGE_WIDTH - MARGIN - 10, y + 29);
        canvas.drawRoundRect(badgeRect, 6f, 6f, paint);

        paint.setColor(Color.WHITE);
        canvas.drawText(estadoLabel, PAGE_WIDTH - MARGIN - badgeTextWidth - 19, y + 22, paint);

        y += 54;

        // 3. Section: DATOS DEL CLIENTE / COMPRADOR
        y = drawSectionHeader(canvas, paint, "1. DATOS DEL CLIENTE", y);

        paint.setColor(Color.parseColor("#FFFFFF"));
        RectF clientBox = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 84);
        canvas.drawRoundRect(clientBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.parseColor("#DCE1E9"));
        canvas.drawRoundRect(clientBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);

        String clienteNombreCompleto = contrato.getNombreCompletoCliente();
        String clienteDni = contrato.clienteDni != null && !contrato.clienteDni.isEmpty() ? contrato.clienteDni : "-";
        String clienteTel = contrato.clienteTelefono != null && !contrato.clienteTelefono.isEmpty() ? contrato.clienteTelefono : "-";
        String clienteEmail = contrato.clienteEmail != null && !contrato.clienteEmail.isEmpty() ? contrato.clienteEmail : "-";

        int colLeft = MARGIN + 16;
        int colRight = MARGIN + (contentWidth / 2) + 10;

        drawLabelValue(canvas, paint, "Nombre y Apellido:", clienteNombreCompleto, colLeft, y + 22);
        drawLabelValue(canvas, paint, "DNI / Documento:", clienteDni, colRight, y + 22);
        drawLabelValue(canvas, paint, "Teléfono / WhatsApp:", clienteTel, colLeft, y + 54);
        drawLabelValue(canvas, paint, "Correo Electrónico:", clienteEmail, colRight, y + 54);

        y += 104;

        // 4. Section: INMUEBLE OBJETO DEL ACUERDO
        y = drawSectionHeader(canvas, paint, "2. INMUEBLE SELECCIONADO", y);

        paint.setColor(Color.parseColor("#F8F9FF"));
        RectF propBox = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 52);
        canvas.drawRoundRect(propBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.parseColor("#C4C9D6"));
        canvas.drawRoundRect(propBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);

        String propTitulo = contrato.propiedadTitulo != null && !contrato.propiedadTitulo.isEmpty()
                ? contrato.propiedadTitulo
                : (contrato.propiedadId != null ? contrato.propiedadId : "Propiedad sin especificar");

        drawLabelValue(canvas, paint, "Propiedad / Unidad:", propTitulo, colLeft, y + 22);

        y += 72;

        // 5. Section: CONDICIONES ECONÓMICAS Y FORMA DE PAGO
        y = drawSectionHeader(canvas, paint, "3. DETALLE ECONÓMICO Y MODALIDAD DE PAGO", y);

        double montoTotal = contrato.montoTotal != null ? contrato.montoTotal : 0.0;
        double senia = contrato.seniaMonto != null ? contrato.seniaMonto : 0.0;
        double saldoRestante = Math.max(0.0, montoTotal - senia);
        String metodoPago = contrato.metodoPago != null && !contrato.metodoPago.isEmpty() ? contrato.metodoPago : "Efectivo";
        int cuotas = contrato.cuotas != null && contrato.cuotas > 0 ? contrato.cuotas : 1;
        boolean esCredito = "Crédito".equalsIgnoreCase(metodoPago) && cuotas > 1;

        paint.setColor(Color.parseColor("#FFFFFF"));
        RectF paymentBox = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 130);
        canvas.drawRoundRect(paymentBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.parseColor("#DCE1E9"));
        canvas.drawRoundRect(paymentBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);

        String montoStr = "$ " + currencyFormat.format(montoTotal);
        String seniaStr = senia > 0 ? "$ " + currencyFormat.format(senia) : "$ 0 (Sin seña)";
        String saldoStr = "$ " + currencyFormat.format(saldoRestante);
        String planStr;
        if (esCredito) {
            double valorCuota = saldoRestante / cuotas;
            planStr = cuotas + " cuotas de $ " + currencyFormat.format(valorCuota);
        } else {
            planStr = "Pago al Contado (" + metodoPago + ")";
        }

        String fechaInicio = contrato.fechaInicio != null && !contrato.fechaInicio.isEmpty() ? contrato.fechaInicio : "-";
        String fechaVenc = contrato.fechaVencimiento != null && !contrato.fechaVencimiento.isEmpty() ? contrato.fechaVencimiento : fechaInicio;

        drawLabelValueHighlight(canvas, paint, "Monto Total de Venta:", montoStr, colLeft, y + 22);
        drawLabelValue(canvas, paint, "Forma de Pago:", metodoPago, colRight, y + 22);

        drawLabelValue(canvas, paint, "Seña / Anticipo:", seniaStr, colLeft, y + 56);
        drawLabelValue(canvas, paint, "Plan de Cancelación:", planStr, colRight, y + 56);

        drawLabelValueHighlight(canvas, paint, "Saldo a Cancelar:", saldoStr, colLeft, y + 90);
        if (esCredito) {
            drawLabelValue(canvas, paint, "Inicio / Vencimiento:", fechaInicio + " al " + fechaVenc, colRight, y + 90);
        } else {
            drawLabelValue(canvas, paint, "Fecha del Contrato:", fechaInicio, colRight, y + 90);
        }

        y += 150;

        // 6. Section: OBSERVACIONES ADICIONALES
        y = drawSectionHeader(canvas, paint, "4. OBSERVACIONES Y CONDICIONES PARTICULARES", y);

        paint.setColor(Color.parseColor("#FFFFFF"));
        RectF obsBox = new RectF(MARGIN, y, PAGE_WIDTH - MARGIN, y + 80);
        canvas.drawRoundRect(obsBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.parseColor("#DCE1E9"));
        canvas.drawRoundRect(obsBox, 8f, 8f, paint);
        paint.setStyle(Paint.Style.FILL);

        String descripcion = contrato.descripcion != null && !contrato.descripcion.trim().isEmpty()
                ? contrato.descripcion.trim()
                : "Sin observaciones adicionales registradas para esta operación.";

        paint.setColor(Color.parseColor("#33363F"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(10f);
        drawMultilineText(canvas, paint, descripcion, colLeft, y + 22, contentWidth - 32, 15f, 4);

        y += 120;

        // 7. Signatures Section at bottom
        float sigY = Math.max(y + 40, PAGE_HEIGHT - 120);
        paint.setColor(Color.parseColor("#8E9199"));
        paint.setStrokeWidth(1f);

        // Left signature line (Cliente)
        float leftLineStart = MARGIN + 25;
        float leftLineEnd = MARGIN + (contentWidth / 2f) - 25;
        canvas.drawLine(leftLineStart, sigY, leftLineEnd, sigY, paint);

        // Right signature line (Inmobiliaria)
        float rightLineStart = MARGIN + (contentWidth / 2f) + 25;
        float rightLineEnd = PAGE_WIDTH - MARGIN - 25;
        canvas.drawLine(rightLineStart, sigY, rightLineEnd, sigY, paint);

        paint.setColor(Color.parseColor("#1A1B21"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(10f);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("FIRMA Y ACLARACIÓN DEL CLIENTE", (leftLineStart + leftLineEnd) / 2f, sigY + 16, paint);
        canvas.drawText("POR " + (nombreEmpresa != null ? nombreEmpresa.toUpperCase(Locale.ROOT) : "LA INMOBILIARIA"), (rightLineStart + rightLineEnd) / 2f, sigY + 16, paint);

        paint.setColor(Color.parseColor("#74777F"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(9f);
        canvas.drawText(clienteNombreCompleto + " (DNI: " + clienteDni + ")", (leftLineStart + leftLineEnd) / 2f, sigY + 30, paint);
        canvas.drawText("Representante Comercial / Administración", (rightLineStart + rightLineEnd) / 2f, sigY + 30, paint);

        // Footer note
        paint.setTextSize(8.5f);
        paint.setColor(Color.parseColor("#8E9199"));
        canvas.drawText("Documento generado desde el sistema de gestión " + (nombreEmpresa != null ? nombreEmpresa : "Inmobiliaria Venta") + ".", PAGE_WIDTH / 2f, PAGE_HEIGHT - 24, paint);
        paint.setTextAlign(Paint.Align.LEFT);

        pdfDocument.finishPage(page);

        try {
            File pdfDir = new File(context.getCacheDir(), "contratos_pdf");
            if (!pdfDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                pdfDir.mkdirs();
            }
            String safeClient = clienteNombreCompleto.replaceAll("[^a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-]", "_");
            String fileName = "Contrato_" + safeClient + ".pdf";
            File outFile = new File(pdfDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                pdfDocument.writeTo(fos);
            }
            pdfDocument.close();
            return outFile;
        } catch (Exception e) {
            pdfDocument.close();
            return null;
        }
    }

    private static float drawSectionHeader(Canvas canvas, Paint paint, String title, float y) {
        paint.setColor(Color.parseColor("#00164E"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(11f);
        canvas.drawText(title, MARGIN + 2, y + 12, paint);
        return y + 20;
    }

    private static void drawLabelValue(Canvas canvas, Paint paint, String label, String value, float x, float y) {
        paint.setColor(Color.parseColor("#74777F"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(9f);
        canvas.drawText(label, x, y, paint);

        paint.setColor(Color.parseColor("#1A1B21"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(10.5f);
        String truncated = truncateText(paint, value != null ? value : "-", 225f);
        canvas.drawText(truncated, x, y + 14, paint);
    }

    private static void drawLabelValueHighlight(Canvas canvas, Paint paint, String label, String value, float x, float y) {
        paint.setColor(Color.parseColor("#74777F"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        paint.setTextSize(9f);
        canvas.drawText(label, x, y, paint);

        paint.setColor(Color.parseColor("#00164E"));
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(12f);
        canvas.drawText(value != null ? value : "-", x, y + 15, paint);
    }

    private static String truncateText(Paint paint, String text, float maxWidth) {
        if (text == null) return "-";
        if (paint.measureText(text) <= maxWidth) return text;
        String ellipsis = "...";
        int len = text.length();
        while (len > 0 && paint.measureText(text.substring(0, len) + ellipsis) > maxWidth) {
            len--;
        }
        return text.substring(0, Math.max(0, len)) + ellipsis;
    }

    private static void drawMultilineText(Canvas canvas, Paint paint, String text, float x, float y, float maxWidth, float lineHeight, int maxLines) {
        if (text == null || text.isEmpty()) return;
        String[] words = text.replace("\n", " \n ").split("\\s+");
        StringBuilder line = new StringBuilder();
        int lineCount = 0;

        for (String word : words) {
            if ("\n".equals(word)) {
                canvas.drawText(line.toString(), x, y + (lineCount * lineHeight), paint);
                line = new StringBuilder();
                lineCount++;
                if (lineCount >= maxLines) return;
                continue;
            }
            String testLine = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(testLine) > maxWidth) {
                canvas.drawText(line.toString(), x, y + (lineCount * lineHeight), paint);
                line = new StringBuilder(word);
                lineCount++;
                if (lineCount >= maxLines) return;
            } else {
                line = new StringBuilder(testLine);
            }
        }
        if (line.length() > 0 && lineCount < maxLines) {
            canvas.drawText(line.toString(), x, y + (lineCount * lineHeight), paint);
        }
    }

    public static String guardarEnDescargas(Context context, File pdfFile) {
        if (context == null || pdfFile == null || !pdfFile.exists()) return null;
        String fileName = pdfFile.getName();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                ContentResolver resolver = context.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (OutputStream out = resolver.openOutputStream(uri);
                         FileInputStream in = new FileInputStream(pdfFile)) {
                        if (out != null) {
                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                            return fileName;
                        }
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    downloadsDir.mkdirs();
                }
                File destFile = new File(downloadsDir, fileName);
                try (FileOutputStream out = new FileOutputStream(destFile);
                     FileInputStream in = new FileInputStream(pdfFile)) {
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                    out.flush();
                    return fileName;
                }
            }
        } catch (Exception ignored) { }
        return null;
    }

    public static void compartirPdf(Context context, File pdfFile, Contrato contrato) {
        if (context == null || pdfFile == null || !pdfFile.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    pdfFile
            );

            String cliente = contrato != null ? contrato.getNombreCompletoCliente() : "";
            String prop = contrato != null && contrato.propiedadTitulo != null ? contrato.propiedadTitulo : "";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/pdf");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Detalle de Contrato - " + prop);
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Hola " + cliente + ", te compartimos el documento PDF con los detalles del contrato inmobiliario de \"" + prop + "\".");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, "Compartir PDF del contrato");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "No se pudo abrir el menú para compartir el PDF", Toast.LENGTH_SHORT).show();
        }
    }
}
