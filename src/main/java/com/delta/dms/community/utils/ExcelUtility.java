package com.delta.dms.community.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.delta.dms.community.model.ExcelHeaderDetail;

public class ExcelUtility {

  private static final int CHARACTER_WIDTH = 256;
  private static final String DEFAULT_SHEET_NAME = "Sheet0";
  private static final String DEFAULT_ENGLISH_FONT_NAME = "Ariel";

  private ExcelUtility() {}

  public static void setHyperlinkStyle(Workbook workbook, Cell cell)
  {
    CellStyle hLinkStyle = workbook.createCellStyle();
    final Font hLinkFont = workbook.createFont();
    hLinkFont.setFontName(DEFAULT_ENGLISH_FONT_NAME);
    hLinkFont.setUnderline(Font.U_SINGLE);
    hLinkFont.setColor(IndexedColors.BLUE.getIndex() );
    hLinkStyle.setFont(hLinkFont);
    cell.setCellStyle(hLinkStyle);
  }

  public static byte[] convertToExcel(
      List<ExcelHeaderDetail> headerList, List<Map<String, String>> dataList) throws IOException {
    Workbook workbook = new XSSFWorkbook();
    writeToExcel(workbook, DEFAULT_SHEET_NAME, headerList, dataList, 0);
    return convertToByteAndClose(workbook);
  }

  public static void writeToExcel(
      Workbook workbook,
      String sheetName,
      List<ExcelHeaderDetail> headerList,
      List<Map<String, String>> dataList,
      Integer startRowIndex) {
    Sheet sheet = workbook.createSheet(sheetName);
    createRowHeader(workbook, sheet, headerList, startRowIndex);

    CellStyle wrapTextStyle = getContentStyle(workbook);
    int rowIndex = startRowIndex + 1;
    for (Map<String, String> data : dataList) {
      Row row = sheet.createRow(rowIndex++);
      for (int i = 0; i < headerList.size(); i++) {
        Cell cell = row.createCell(i);
        cell.setCellStyle(wrapTextStyle);
        cell.setCellValue(StringUtils.defaultString(data.get(headerList.get(i).getKey())));
      }
    }
  }

  public static void writeToExcelForHorizontal(
          Workbook workbook,
          String sheetName,
          String headerName,
          String data,
          Integer rowIndex,
          Hyperlink link
  ) {
    Sheet sheet = workbook.getSheet(sheetName);
    CellStyle wrapTextStyle = getContentStyle(workbook);
    Row row = sheet.createRow(rowIndex);
    Cell headerCell = row.createCell(0);
    headerCell.setCellStyle(wrapTextStyle);
    headerCell.setCellValue(headerName);
    Cell contentCell = row.createCell(1);
    contentCell.setCellStyle(wrapTextStyle);
    contentCell.setCellValue(data);
    if (link != null) {
      contentCell.setHyperlink(link);
      setHyperlinkStyle(workbook, contentCell);
    }
  }

  public static void writeToExcelForHorizontal(
          Workbook workbook,
          String sheetName,
          String headerName,
          String data,
          Integer rowIndex
  ) {
    writeToExcelForHorizontal(workbook, sheetName, headerName, data, rowIndex, null);
  }

  public static byte[] convertToByteAndClose(Workbook workbook) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    workbook.write(outputStream);
    outputStream.close();
    workbook.close();
    return outputStream.toByteArray();
  }

  private static void createRowHeader(
      Workbook workbook, Sheet sheet, List<ExcelHeaderDetail> headerList, Integer rowIndex) {
    CellStyle headerCellStyle = getHeaderStyle(workbook);
    Row rowHeader = sheet.createRow(rowIndex);
    for (int i = 0; i < headerList.size(); i++) {
      Cell cell = rowHeader.createCell(i);
      cell.setCellValue(headerList.get(i).getValue());
      cell.setCellStyle(headerCellStyle);
      sheet.setColumnWidth(i, headerList.get(i).getWidth() * CHARACTER_WIDTH);
    }
  }

  private static CellStyle getHeaderStyle(Workbook workbook) {
    Font headerFont = workbook.createFont();
    headerFont.setBold(true);
    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setFont(headerFont);
    cellStyle.setWrapText(true);
    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    return cellStyle;
  }

  private static CellStyle getContentStyle(Workbook workbook) {
    CellStyle cellStyle = workbook.createCellStyle();
    cellStyle.setWrapText(true);
    cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
    return cellStyle;
  }
}
