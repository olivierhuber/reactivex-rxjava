package com.zenika.reactivex;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ImperativeParadigm {
    public static void main(String[] args) {
        new ImperativeParadigm().process("Annuaire.xlsx", "Listing");
    }

    public void process(String excelFile, String sheetName) {
        try {
            Workbook workbook = WorkbookFactory.create(new File(excelFile));
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                if (workbook.getSheetName(i).equals(sheetName)) {
                    Sheet sheet = workbook.getSheetAt(i);
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row != null) {
                            String email = email(row);
                            String location = location(row);
                            String lastname = lastname(row);
                            String firstname = firstname(row);
                            Date employmentDate = employmentDate(row);

                            if (employmentDate == null || employmentDate.toInstant().isAfter(Instant.now().minus(365, ChronoUnit.DAYS))) {
                                System.out.println(firstname + " " + lastname + " " + "<" + email + ">" + " from " + location + " was hired on " + employmentDate);
                                if (!StringUtils.equalsIgnoreCase(StringUtils.stripAccents(firstname + "." + lastname + "@zenika.com"), email)) {
                                    System.err.println("The email " + email + " doesn't match the name " + firstname + " " + lastname);
                                }
                            }
                        }
                    }
                }
            }
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static Date employmentDate(Row row) {
        return date(row, 3);
    }

    static Date date(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell != null) {
            try {
                return cell.getDateCellValue();
            } catch (Exception e) {
//                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    static String firstname(Row row) {
        return string(row, 2);
    }

    static String lastname(Row row) {
        return string(row, 1);
    }

    static String location(Row row) {
        return string(row, 0);
    }

    static String string(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell != null) {
            try {
                return cell.getStringCellValue().trim();
            } catch (Exception e) {
//                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    static String email(Row row) {
        for (int i = 0; i <= row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null) {
                try {
                    String maybeEmail = cell.getStringCellValue();
                    if (maybeEmail != null && maybeEmail.contains("@")) {
                        return maybeEmail.toLowerCase().trim();
                    }
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

}
