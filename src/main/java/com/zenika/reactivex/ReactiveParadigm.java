package com.zenika.reactivex;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import rx.Observable;
import rx.exceptions.Exceptions;
import rx.functions.Action1;
import rx.functions.Func0;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class ReactiveParadigm {
    public static void main(String[] args) {
        new ReactiveParadigm().process("Annuaire.xlsx", "Listing");
    }

    public void process(String excelFile, String sheetName) {

//        These simple lines iterate over all cells from all rows from all sheets from the given excel file
//
//        workbookObservable(excelFile)
//                .flatMap(workbook -> Observable.from(workbook::sheetIterator))
//                .flatMap(sheet -> Observable.from(sheet::rowIterator))
//                .flatMap(row -> Observable.from(row::cellIterator))
//                .subscribe(
//                        cell -> System.out.println(cell.getSheet().getSheetName() + "[" + cell.getRowIndex() + "," + cell.getColumnIndex() + "]=" + cell.toString()),
//                        System.err::println
//                );

        workbookObservable(excelFile)
                //emits some Workbook
                .doOnError(error -> System.err.println("Cannot open Excel file " + excelFile))
                .flatMap(workbook -> Observable.from(workbook::sheetIterator))
                //emits some Sheet
                .filter(sheet -> sheet.getSheetName().equalsIgnoreCase(sheetName))
                .switchIfEmpty(Observable.error(new UnknownSheetException("No sheet found with name " + sheetName)))
                .flatMap(sheet -> Observable.from(sheet::rowIterator))
                //emits some Row
                .skip(1 /*Header row*/)
                .flatMap(this::buildContactObservable)
                //emits some Zenika
                .compose(new FilterEmail())
                .compose(new FilterEmploymentDateForTheLast(600, ChronoUnit.DAYS))
                .compose(new CheckEmailAgainstFullname())
                .subscribe(
                        zenika -> System.out.println(zenika),
                        error -> System.err.println(error.getMessage())
                );
    }

    private Observable<Zenika> buildContactObservable(Row row) {
        return Observable.zip(email(row), location(row), lastname(row), firstname(row), employmentDate(row), Zenika::new);
    }

    Observable<String> email(Row row) {
        return Observable.from(row::cellIterator)
                .filter(cell -> Cell.CELL_TYPE_STRING == cell.getCellType())
                .map(Cell::getStringCellValue)
                .filter(cell -> cell.contains("@"))
                .map(String::toLowerCase)
                .map(String::trim)
                .defaultIfEmpty("");
    }

    Observable<String> location(Row row) {
        return Observable.from(row::cellIterator)
                .filter(cell -> 0 == cell.getColumnIndex())
                .map(Cell::getStringCellValue)
                .map(String::trim);
    }

    Observable<String> lastname(Row row) {
        return Observable.from(row::cellIterator)
                .filter(cell -> 1 == cell.getColumnIndex())
                .map(Cell::getStringCellValue)
                .map(String::trim);
    }

    Observable<String> firstname(Row row) {
        return Observable.from(row::cellIterator)
                .filter(cell -> 2 == cell.getColumnIndex())
                .map(Cell::getStringCellValue)
                .map(String::trim);
    }

    Observable<Date> employmentDate(Row row) {
        return Observable.from(row::cellIterator)
                .filter(cell -> 3 == cell.getColumnIndex())
                .map(Cell::getDateCellValue);
    }

    class Zenika {
        public String email;
        public String location;
        public String lastname;
        public String firstname;
        public Date employmentDate;

        public Zenika(String email, String location, String lastname, String firstname, Date employmentDate) {
            this.email = email;
            this.location = location;
            this.lastname = lastname;
            this.firstname = firstname;
            this.employmentDate = employmentDate;
        }

        @Override
        public String toString() {
            return firstname + " " + lastname + " " + "<" + email + ">" + " from " + location + " was hired on " + employmentDate;
        }
    }

    private Observable<Workbook> workbookObservable(String excelFile) {
        Func0<Workbook> resourceFactory = () -> {
            try {
                return WorkbookFactory.create(new File(excelFile));
            } catch (Exception e) {
                throw Exceptions.propagate(e);
            }
        };

        Action1<Workbook> disposeAction = workbook -> {
            try {
                workbook.close();
            } catch (IOException e) {
                throw Exceptions.propagate(e);
            }
        };

        return Observable.using(resourceFactory, Observable::just, disposeAction);
    }

    class FilterEmail implements Observable.Transformer<Zenika, Zenika> {
        @Override
        public Observable<Zenika> call(Observable<Zenika> zenikaObservable) {
            return zenikaObservable
                    .doOnNext(zenika -> {
                        if (zenika.email.isEmpty()) {
                            System.err.println(zenika.lastname + " has no email!");
                        }
                    })
                    .filter(zenika -> !zenika.email.isEmpty());
        }
    }

    class FilterEmploymentDateForTheLast implements Observable.Transformer<Zenika, Zenika> {
        final int given;
        final ChronoUnit unit;

        public FilterEmploymentDateForTheLast(int given, ChronoUnit unit) {
            this.given = given;
            this.unit = unit;
        }

        @Override
        public Observable<Zenika> call(Observable<Zenika> zenikaObservable) {
            return zenikaObservable
                    .doOnNext(zenika -> {
                        if (zenika.employmentDate == null) {
                            System.err.println(zenika.lastname + " has no employment date!");
                        }
                    })
                    .filter(zenika -> zenika.employmentDate != null)
                    .filter(zenika -> zenika.employmentDate.toInstant().isAfter(Instant.now().minus(given, unit)));
        }
    }

    class CheckEmailAgainstFullname implements Observable.Transformer<Zenika, Zenika> {
        @Override
        public Observable<Zenika> call(Observable<Zenika> zenikaObservable) {
            return zenikaObservable.doOnNext(zenika -> {
                if (!zenika.email.isEmpty() && !StringUtils.equalsIgnoreCase(StringUtils.stripAccents(zenika.firstname + "." + zenika.lastname + "@zenika.com"), zenika.email)) {
                    System.err.println("Email " + zenika.email + " doesn't match lastname " + zenika.firstname + " " + zenika.lastname);
                }
            });
        }
    }

    class UnknownSheetException extends RuntimeException {
        public UnknownSheetException(String s) {
            super(s);
        }
    }
}
