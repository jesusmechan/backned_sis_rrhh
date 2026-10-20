package pe.andina.rrhh.application.port.out;

import java.io.InputStream;
import java.util.List;

/** Generación y lectura de libros Excel. Lo implementa el adaptador Apache POI. */
public interface ExcelPort {

    byte[] escribir(String nombreHoja, List<String[]> filas);

    List<String[]> leer(InputStream contenido);
}
