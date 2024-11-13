package com.delta.dms.community.adapter;

import com.delta.datahive.searchobj.type.activitylog.ActivityLogResult;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

@Component
public class ExcelAdapter {
    private static final int ENGLISH_CHAR_WIDTH = 1;
    private static final int CHINESE_CHAR_WIDTH = 3;
    private static final int COLUMN_WIDTH_OFFSET = 4;

    public ByteArrayOutputStream generateExcel(Workbook workbook) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        workbook.write(byteArrayOutputStream);
        workbook.close();
        return byteArrayOutputStream;
    }

    public void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int maxWidth = 0;
            for (Row row : sheet) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    int cellWidth = getCellWidth(cell.toString());
                    maxWidth = Math.max(maxWidth, cellWidth);
                }
            }
            sheet.setColumnWidth(columnIndex, (maxWidth + COLUMN_WIDTH_OFFSET) * 256);
        }
    }

    private int getCellWidth(String cellValue) {
        int width = 0;
        for (char c : cellValue.toCharArray()) {
            if (isChinese(c)) {
                width += CHINESE_CHAR_WIDTH;
            } else {
                width += ENGLISH_CHAR_WIDTH;
            }
        }
        return width;
    }

    private boolean isChinese(char c) {
        return Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN;
    }


    public void createHeaderRow(Sheet sheet, String[] columns) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
        }
    }

    public void insertRow(Sheet sheet, int rowNum, Object... values) {
        Row row = sheet.createRow(rowNum);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            if (values[i] instanceof String) {
                cell.setCellValue((String) values[i]);
            } else if (values[i] instanceof Integer) {
                cell.setCellValue((Integer) values[i]);
            } else if (values[i] instanceof Long) {
                cell.setCellValue((Long) values[i]);
            } else if (values[i] instanceof Double) {
                cell.setCellValue((Double) values[i]);
            }
        }
    }

    public void setSheetOrder(Workbook workbook, String[] sheetNames) {
        for (int i = 0; i < sheetNames.length; i++) {
            workbook.setSheetOrder(sheetNames[i], i);
        }
    }

    public void createSheetWithActivityLog(
            Workbook workbook,
            String sheetName,
            String[] columns,
            List<ActivityLogResult> results,
            Function<ActivityLogResult, Object[]> rowMapper) {

        Sheet sheet = workbook.createSheet(sheetName);
        createHeaderRow(sheet, columns);

        int rowNum = 1;
        if (results != null) {
            for (ActivityLogResult result : results) {
                Object[] rowValues = rowMapper.apply(result);
                insertRow(sheet, rowNum++, rowValues);
            }
        }
        autoSizeColumns(sheet, columns.length);
    }

}