package pe.andina.rrhh.adapter.out.excel;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pe.andina.rrhh.application.port.out.ExcelPort;
import pe.andina.rrhh.domain.exception.DomainException;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class PoiExcelAdapter implements ExcelPort {

    private static final Logger log = LoggerFactory.getLogger(PoiExcelAdapter.class);

    @Override
    public byte[] escribir(String nombreHoja, List<String[]> filas) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(WorkbookUtil.createSafeSheetName(
                    nombreHoja == null || nombreHoja.isBlank() ? "Hoja1" : nombreHoja));
            CellStyle header = headerStyle(wb);
            CellStyle body = bodyStyle(wb);
            int columnas = filas.isEmpty() ? 0 : filas.getFirst().length;
            for (int i = 0; i < filas.size(); i++) {
                Row row = sheet.createRow(i);
                String[] cols = filas.get(i);
                for (int c = 0; c < cols.length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(cols[c] == null ? "" : cols[c]);
                    cell.setCellStyle(i == 0 ? header : body);
                }
            }
            if (columnas > 0) {
                sheet.createFreezePane(0, 1);
                sheet.setAutoFilter(new CellRangeAddress(0, Math.max(filas.size() - 1, 0), 0, columnas - 1));
                for (int c = 0; c < columnas; c++) {
                    sheet.autoSizeColumn(c);
                    int width = sheet.getColumnWidth(c);
                    sheet.setColumnWidth(c, Math.min(Math.max(width + 512, 3500), 14000));
                }
            }
            wb.write(out);
            log.info("Excel generado hoja={} filas={}", sheet.getSheetName(), filas.size());
            return out.toByteArray();
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("No se pudo generar el Excel", ex);
            throw DomainException.badRequest("No se pudo generar el Excel");
        }
    }

    @Override
    public List<String[]> leer(InputStream contenido) {
        DataFormatter fmt = new DataFormatter();
        try (Workbook wb = new XSSFWorkbook(contenido)) {
            if (wb.getNumberOfSheets() == 0) {
                throw DomainException.badRequest("El archivo Excel no contiene hojas");
            }
            Sheet sheet = wb.getSheetAt(0);
            List<String[]> filas = new ArrayList<>();
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                short last = row.getLastCellNum();
                if (last < 1) {
                    continue;
                }
                String[] cols = new String[last];
                for (int c = 0; c < last; c++) {
                    cols[c] = row.getCell(c) == null ? "" : fmt.formatCellValue(row.getCell(c)).trim();
                }
                filas.add(cols);
            }
            log.info("Excel leído hoja={} filas={}", sheet.getSheetName(), filas.size());
            return filas;
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("No se pudo leer el Excel: {}", ex.getMessage());
            throw DomainException.badRequest("No se pudo leer el Excel: " + ex.getMessage());
        }
    }

    private static CellStyle headerStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private static CellStyle bodyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }
}
