package services;

import models.tables.pojos.EtudeConsommations;

import org.apache.poi.ss.usermodel.*;

import javax.inject.Singleton;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ExcelImportService {
    
    /**
     * Parser le fichier Excel et retourner la liste des objets
     */
    public List<EtudeConsommations> parseExcelFile(File file, String fileName, String username) throws IOException {
        List<EtudeConsommations> etudeList = new ArrayList<>();
        
        Workbook workbook = null;
        InputStream fis = null;
        
        try {
            fis = new FileInputStream(file);
            
            // WorkbookFactory détecte automatiquement le format (.xls ou .xlsx)
            // et retourne un objet Workbook (interface commune)
            workbook = WorkbookFactory.create(fis);
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Parcourir les lignes (sauter la première ligne d'en-tête)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    EtudeConsommations etude = new EtudeConsommations();
                    
                    // Colonnes selon votre fichier Excel
                    etude.setNumeroCarnet(getCellValueAsString(row.getCell(1)));
                    etude.setCodeAgent(getCellValueAsString(row.getCell(2)));
                    etude.setNomPrenoms(getCellValueAsString(row.getCell(3)));
                    etude.setAgent(getCellValueAsBigDecimal(row.getCell(4)));
                    etude.setConjoint(getCellValueAsBigDecimal(row.getCell(5)));
                    etude.setEnfants(getCellValueAsBigDecimal(row.getCell(6)));
                    etude.setPere(getCellValueAsBigDecimal(row.getCell(7)));
                    etude.setMere(getCellValueAsBigDecimal(row.getCell(8)));
                    etude.setSalaireBase(getCellValueAsBigDecimal(row.getCell(9)));
                    etude.setPourcentageRetenir(getCellValueAsBigDecimal(row.getCell(10)));
                    etude.setRetenueMensuelle(getCellValueAsBigDecimal(row.getCell(11)));
                    etude.setCreditAnnuel(getCellValueAsBigDecimal(row.getCell(12)));
                    etude.setCreditAnnuelX2(getCellValueAsBigDecimal(row.getCell(13)));
                    etude.setConsommationBons(getCellValueAsBigDecimal(row.getCell(14)));
                    etude.setConsommationPc(getCellValueAsBigDecimal(row.getCell(15)));
                    etude.setRemboursement(getCellValueAsBigDecimal(row.getCell(16)));
                    etude.setTotalConsommation(getCellValueAsBigDecimal(row.getCell(17)));
                    etude.setSolde_1(getCellValueAsBigDecimal(row.getCell(18)));
                    etude.setSolde_2(getCellValueAsBigDecimal(row.getCell(19)));
                    etude.setStatutAvertissement(getCellValueAsString(row.getCell(20)));
                    etude.setStatutSuspension(getCellValueAsString(row.getCell(21)));
                    
                    // Métadonnées
                    etude.setFichierSource(fileName);
                    etude.setUtilisateurImport(username);
                    
                    // Ajouter seulement si le code agent n'est pas vide
                    if (etude.getCodeAgent() != null && !etude.getCodeAgent().trim().isEmpty()) {
                        etudeList.add(etude);
                    }
                    
                } catch (Exception e) {
                    System.err.println("Erreur parsing ligne " + (i + 1) + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
        } catch (Exception e) {
            throw new IOException("Erreur lors de la lecture du fichier Excel: " + e.getMessage(), e);
        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return etudeList;
    }
    
    /**
     * Obtenir la valeur d'une cellule sous forme de String
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        CellType cellType = cell.getCellType();
        
        switch (cellType) {
            case STRING:
                String value = cell.getStringCellValue();
                return value != null ? value.trim() : null;
                
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    double numericValue = cell.getNumericCellValue();
                    // Si c'est un entier, retourner sans décimales
                    if (numericValue == (long) numericValue) {
                        return String.valueOf((long) numericValue);
                    } else {
                        return String.valueOf(numericValue);
                    }
                }
                
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
                
            case FORMULA:
                try {
                    // Essayer d'obtenir le résultat de la formule comme string
                    return cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        // Si c'est un nombre
                        double numValue = cell.getNumericCellValue();
                        if (numValue == (long) numValue) {
                            return String.valueOf((long) numValue);
                        } else {
                            return String.valueOf(numValue);
                        }
                    } catch (Exception e2) {
                        return null;
                    }
                }
                
            case BLANK:
                return null;
                
            case ERROR:
                return null;
                
            default:
                return null;
        }
    }
    
    /**
     * Obtenir la valeur d'une cellule sous forme de BigDecimal
     */
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return null;
        
        try {
            CellType cellType = cell.getCellType();
            
            switch (cellType) {
                case NUMERIC:
                    double numValue = cell.getNumericCellValue();
                    return BigDecimal.valueOf(numValue);
                    
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    
                    // Nettoyer la valeur
                    value = value.replace(",", "")
                                 .replace(" ", "")
                                 .replace("\"", "")
                                 .replace("'", "");
                    
                    // Gérer les cas spéciaux
                    if (value.isEmpty() || value.equals("-") || value.equalsIgnoreCase("null")) {
                        return null;
                    }
                    
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        System.err.println("Impossible de convertir en nombre: " + value);
                        return null;
                    }
                    
                case FORMULA:
                    try {
                        double formulaValue = cell.getNumericCellValue();
                        return BigDecimal.valueOf(formulaValue);
                    } catch (Exception e) {
                        return null;
                    }
                    
                case BLANK:
                    return null;
                    
                case ERROR:
                    return null;
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Erreur conversion BigDecimal: " + e.getMessage());
            return null;
        }
    }
}
