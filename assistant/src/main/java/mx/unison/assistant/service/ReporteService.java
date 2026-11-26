package mx.unison.assistant.service;

import org.openpdf.pdf.ITextRenderer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import mx.unison.assistant.data.PlanAcademicoDAO;
import mx.unison.assistant.data.EvidenciaDAO;

//plan academico
//evidencias

public class ReporteService {

  //turn SQL table into HTML
 



}


// Clase para convertir y exportar un HTML a PDF
class PdfExporter {

  //Creamos la base CSS del PDF que será exportado, en una hoja tamaño carta.
  private static final String CSS_de_pagina_PDF = """
      @page {
          size: letter;
          margin: 2cm 1.5cm 2cm 1.5cm;
          
          @bottom-right { 
            content: "Página " counter(page) " de " counter(pages); 
            font-size: 9px; 
            color: #666; 
          }
      }
      body { 
        font-family: Helvetica, Arial, sans-serif; 
        font-size: 10.5px; 
        color: #222; 
      }
      h1 { 
        font-size: 16px; 
        margin: 0 0 6px 0; 
      }
      h2 { 
        font-size: 13px; 
        margin: 14px 0 6px 0; 
      }
      table { 
        width: 100%; 
        border-collapse: collapse; 
        margin-top: 6px; 
      }
      th, td { 
        border: 1px solid #999; 
        padding: 4px 6px; 
        text-align: left; 
        vertical-align: top; 
      }
      th { 
        background-color: #e8e8e8; 
      }
      thead { 
        display: table-header-group; 
      }
      tr { 
        page-break-inside: avoid; 
      }
      """;
   

    /*


    
    Método para exportar un HTML a PDF, guardando el resultado en el destino especificado.
    
    
    "html" = contenido HTML a convertir. 
      !!!!!! FAVOR DE ALMACENAR LOS DATOS DE REPORTESERVICE DE SQL A HTML AQUÍ.
    
      "destino" = ruta del archivo PDF de salida.
      
    El archivo de destino se cierra automáticamente después de escribir el PDF.
    layout() y createPDF() son métodos de ITextRenderer que generan el PDF a partir del HTML.
    Donde layout() prepara el documento y createPDF() lo escribe en el OutputStream.
    Si no se puede generar el PDF, se lanza una IOException con el mensaje de error.
    Si el HTML está vacío, se generará un PDF en blanco.
    
    */
    void exportar(String html, Path destino) throws IOException {
      String documento = 
        "<html><head><meta charset=\"UTF-8\"/><style>" 
        + CSS_de_pagina_PDF
        + "</style></head>"
        + "<body>" 
        + html 
        + "</body></html>";

      try (OutputStream os = Files.newOutputStream(destino)) {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(documento);
        renderer.layout();
        renderer.createPDF(os);
      } 
      
      catch (Exception e) {
        throw new IOException("Error. No se pudo generar el PDF: " 
        + e.getMessage(), e);
      }
  }

  
}

//class xlsxexporter {}