package controllers;

import services.TypePrestationMainService;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VPartenaire;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.AddCSRFToken;
import play.mvc.*;
import play.mvc.Http.Request;
import utils.CallJasperReport;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class PrestationCtrl extends Controller {

    TypePrestationMainService prestaService;
	CallJasperReport jasper;
    private final FormFactory formFactory;

    @Inject
    public PrestationCtrl(TypePrestationMainService prestaService,CallJasperReport jasper, FormFactory formFactory) {
        this.prestaService = prestaService;
		this.jasper = jasper;
        this.formFactory = formFactory;
    }




    public Result show(Request request, String subAction, Long idPrestation) {
		String viewMode;
		TypePrestation c;
		List<TypePrestation> prestations = new ArrayList<>();

		prestations = prestaService.findAll();

		// List<Personnels> medecins = persServices.listes("Medecin");
		if (0 == idPrestation) {
			c = new TypePrestation();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = prestaService.findById(idPrestation);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = prestaService.findById(idPrestation);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = prestaService.findById(idPrestation);

		}
		return ok(views.html.prestations.render(viewMode, prestations,c, request));

	}

    
    @AddCSRFToken
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<TypePrestation> uForm = formFactory.form(TypePrestation.class).bindFromRequest(request);
		TypePrestation prestation = uForm.get();

		// ne renseigner ses variables que dans les cas ou il s'agit des deux operation
		// suivante
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE) || viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			prestation.setOnDeleted(false);

		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {

			if (prestaService.saveLogical(prestation, true).equals("ok"))
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Partenaire " + prestation.getPrestation() + " a été ajouté avec success");

			else
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"prestation " + prestation.getPrestation() + " non ajouter ");

		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			System.out.println("modif :" + (prestaService.saveLogical(prestation, false)));

			if (prestaService.saveLogical(prestation, false).equals("ok"))
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"prestation  modifier avec success");

			else
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"prestation  non modifier");

		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			prestation.setOnDeleted(true);
		
			if (prestaService.saveLogical(prestation, false).equals("ok"))
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"prestation  Supprimer avec success");

			else
				return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"prestation  non supprimer");

		}
		return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L));
	
	}
	public Result print(Request request, Long idPrestation, String fileName) {

		// String fileName = "recu";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName,String.valueOf(idPrestation));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + idPrestation + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.PrestationCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Erreur d'impression");
		}
	}

}
