package controllers;

import models.tables.pojos.Adherent;
import models.tables.pojos.EtudeConsommations;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import services.AdherentMainServices;
import services.ExcelImportService;
import services.ImportExcelClassServiceImpl;

import javax.inject.Inject;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;

public class EtudeConsommationCtrl extends Controller {

    private final ImportExcelClassServiceImpl etudeDao;
    private final AdherentMainServices adherentService;
    private final ExcelImportService excelService;
    private final FormFactory formFactory;

    @Inject
    public EtudeConsommationCtrl(ImportExcelClassServiceImpl etudeDao,AdherentMainServices adherentService, ExcelImportService excelService, FormFactory formFactory) {
        this.etudeDao = etudeDao;
        this.adherentService = adherentService;
        this.excelService = excelService;
        this.formFactory = formFactory;
    }

    /**
     * Afficher le formulaire d'import
     */
    public Result showImportForm(Http.Request request) {
        List<EtudeConsommations> etudes = etudeDao.findAll();
        return ok(views.html.etudeConsommationImport.render(etudes,adherentService.listeAdherents(), request));
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

    /**
     * Mettre à jour uniquement le salaire de base d'un adhérent depuis l'import
     * Note: Le crédit annuel est calculé automatiquement à partir du salaire
     */
    public Result updateAdherent(Http.Request request) {
        try {
            //Form<Object> form = formFactory.form().bindFromRequest(request);
            Form<Adherent> uForm = formFactory.form(Adherent.class).bindFromRequest(request);
            Adherent ad = uForm.get();
            Long adherentId = ad.getId();
            //BigDecimal salaireBase = new BigDecimal(ad.getsalaireNet());
            
            // Récupérer l'adhérent
            Adherent adherent = adherentService.findById(adherentId);
            
            if (adherent == null) {
                return redirect(routes.EtudeConsommationCtrl.showImportForm())
                        .flashing("error", "Adhérent non trouvé");
            }
            
            // Mettre à jour UNIQUEMENT le salaire de base
            // Le crédit annuel sera recalculé automatiquement
            adherent.setSalaireNet(ad.getSalaireNet());
            
            // Sauvegarder
            String result = adherentService.saveLogical(adherent, false);
            
            if ("ok".equals(result)) {
                return redirect(routes.EtudeConsommationCtrl.showImportForm())
                        .flashing("success", "Salaire de base mis à jour avec succès. Le crédit annuel sera recalculé automatiquement.");
            } else {
                return redirect(routes.EtudeConsommationCtrl.showImportForm())
                        .flashing("error", "Erreur lors de la mise à jour: " + result);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return redirect(routes.EtudeConsommationCtrl.showImportForm())
                    .flashing("error", "Erreur lors de la mise à jour: " + e.getMessage());
        }
    }
}
