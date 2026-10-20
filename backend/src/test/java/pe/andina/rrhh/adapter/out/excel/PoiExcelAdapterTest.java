package pe.andina.rrhh.adapter.out.excel;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PoiExcelAdapterTest {

    private final PoiExcelAdapter adapter = new PoiExcelAdapter();

    @Test
    void escribeYLeeElMismoContenido() {
        List<String[]> original = List.of(
                new String[]{"Código", "Nombre"},
                new String[]{"E001", "Ana Pérez"}
        );

        byte[] xlsx = adapter.escribir("trabajadores", original);
        assertTrue(xlsx.length > 0);

        List<String[]> leido = adapter.leer(new ByteArrayInputStream(xlsx));
        assertEquals(2, leido.size());
        assertEquals("Código", leido.get(0)[0]);
        assertEquals("E001", leido.get(1)[0]);
        assertEquals("Ana Pérez", leido.get(1)[1]);
    }
}
