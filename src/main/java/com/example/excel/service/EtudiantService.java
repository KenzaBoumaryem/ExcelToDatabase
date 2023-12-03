package com.example.excel.service;
import com.example.excel.model.Etudiant;
import com.example.excel.repository.EtudiantRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Service
public class EtudiantService {

    private final EtudiantRepository etudiantRepository;

    @Autowired
    public EtudiantService(EtudiantRepository etudiantRepository) {
        this.etudiantRepository = etudiantRepository;
    }

    @Transactional
    public void insertDataFromExcel(MultipartFile file) throws IOException {
        List<Etudiant> etudiantsToSave = new ArrayList<>();
        List<Etudiant> etudiantsInError = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0) {
                    // Skip the header row
                    continue;
                }

                Etudiant etudiant = new Etudiant();
                etudiant.setCNE(row.getCell(0).getStringCellValue());
                etudiant.setNom(row.getCell(1).getStringCellValue());
                etudiant.setPrenom(row.getCell(2).getStringCellValue());
                etudiant.setDate_naissance(row.getCell(3).getDateCellValue());
                etudiant.setNote((float) row.getCell(4).getNumericCellValue());
                etudiant.setMention(row.getCell(5).getStringCellValue());

                if (etudiantRepository.existsById(etudiant.getCNE())) {
                    // If ID already exists, add to the error list
                    etudiantsInError.add(etudiant);
                } else {
                    // If ID does not exist, add to the list for saving
                    etudiantsToSave.add(etudiant);
                }
            }
        }

        // Save etudiants that do not have duplicate IDs
        etudiantRepository.saveAll(etudiantsToSave);

        // If there are errors, write them to an error Excel file
        if (!etudiantsInError.isEmpty()) {
            writeErrorsToExcel(etudiantsInError);
        }
    }

    private void writeErrorsToExcel(List<Etudiant> etudiantsInError) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Errors");

        // Create header row
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("CNE");
        headerRow.createCell(1).setCellValue("Nom");
        headerRow.createCell(2).setCellValue("Prenom");
        headerRow.createCell(3).setCellValue("Date de Naissance");
        headerRow.createCell(4).setCellValue("Note");
        headerRow.createCell(5).setCellValue("Mention");
        headerRow.createCell(6).setCellValue("Message");
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        // Write errors to the sheet
        for (int i = 0; i < etudiantsInError.size(); i++) {
            Etudiant etudiant = etudiantsInError.get(i);
            Row errorRow = sheet.createRow(i + 1);

            // Copy data from the etudiant object
            errorRow.createCell(0).setCellValue(etudiant.getCNE());
            errorRow.createCell(1).setCellValue(etudiant.getNom());
            errorRow.createCell(2).setCellValue(etudiant.getPrenom());
            Cell dateCell = errorRow.createCell(3);
            dateCell.setCellValue(dateFormat.format(etudiant.getDate_naissance()));
            errorRow.createCell(4).setCellValue(etudiant.getNote());
            errorRow.createCell(5).setCellValue(etudiant.getMention());

            // Add the error message column
            errorRow.createCell(6).setCellValue("Duplication de clé");
        }

        // Save the workbook to a file
        try (FileOutputStream fileOut = new FileOutputStream("error.xlsx")) {
            workbook.write(fileOut);
        }
    }
}


