package controllers;

import models.tables.pojos.EtudeConsommations;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.ExcelImportService;
import services.ImportExcelClassServiceImpl;

import javax.inject.Inject;
import java.io.File;
import java.util.List;

public class EtudeConsommationCtrl extends Controller {

    private final ImportExcelClassServiceImpl etudeDao;
    private final ExcelImportService excelService;

    @Inject
    public EtudeConsommationCtrl(ImportExcelClassServiceImpl etudeDao, ExcelImportService excelService) {
        this.etudeDao = etudeDao;
        this.excelService = excelService;
    }

    /**
     * Afficher le formulaire d'import
     */
    public Result showImportForm(Http.Request request) {
        List<EtudeConsommations> etudes = etudeDao.findAll();
        return ok(views.html.etudeConsommationImport.render(etudes, request));
    }

    /**
     * Traiter l'import du fichier Excel
     */
    public Result importExcel(Http.Request request) {

        // Play 2.8 → utiliser TemporaryFile
        Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
        Http.MultipartFormData.FilePart<TemporaryFile> filePart = body.getFile("fichierExcel");

        if (filePart == null) {
            return redirect(routes.EtudeConsommationCtrl.showImportForm())
                    .flashing("error", "Aucun fichier sélectionné");
        }

        // Conversion TemporaryFile → java.io.File
        TemporaryFile tempFile = filePart.getRef();
       File file = tempFile.path().toFile();


        String fileName = filePart.getFilename();
        String username = request.session().get("username").orElse("Système");

        try {
            // Parser le fichier Excel
            List<EtudeConsommations> etudes =
                    excelService.parseExcelFile(file, fileName, username);

            if (etudes.isEmpty()) {
                return redirect(routes.EtudeConsommationCtrl.showImportForm())
                        .flashing("warning", "Le fichier ne contient aucune donnée valide.");
            }

            // Option : supprimer les anciennes données
            boolean supprimerAnciennes = body.asFormUrlEncoded().get("supprimerAnciennes") != null;
            if (supprimerAnciennes) {
                List<EtudeConsommations> all = etudeDao.findAll();
                etudeDao.delete(all);
            }

            // Insérer les nouvelles données
            etudeDao.insert(etudes);

            return redirect(routes.EtudeConsommationCtrl.showImportForm())
                    .flashing("success",
                            "Import réussi : " + etudes.size() + " enregistrements importés");

        } catch (Exception e) {
            e.printStackTrace();
            return redirect(routes.EtudeConsommationCtrl.showImportForm())
                    .flashing("error", "Erreur lors de l'import : " + e.getMessage());
        }
    }
}
