package controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.VPartenaire;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.AddCSRFToken;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.Http.Request;
import services.StructureMainServices;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class StructurePartenaireCtrl extends Controller {

	StructureMainServices structureService;
	private final FormFactory formFactory;

	@Inject
	public StructurePartenaireCtrl(StructureMainServices structureService, FormFactory formFactory) {
		super();
		this.structureService = structureService;
		this.formFactory = formFactory;
	}

	public Result show(Request request, String subAction, Long idStructure) {
		String viewMode;
		StructurePartenaire c;
		List<VPartenaire> structures = new ArrayList<>();

		structures = structureService.listPartenaire();

		// List<Personnels> medecins = persServices.listes("Medecin");
		if (0 == idStructure) {
			c = new StructurePartenaire();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = structureService.findById(idStructure);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = structureService.findById(idStructure);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = structureService.findById(idStructure);

		}
		return ok(views.html.structurePartenaires.render(viewMode, structures, c,structureService.listRegion(), request));

	}

	@AddCSRFToken
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<StructurePartenaire> uForm = formFactory.form(StructurePartenaire.class).bindFromRequest(request);
		StructurePartenaire partenaire = uForm.get();

		// ne renseigner ses variables que dans les cas ou il s'agit des deux operation
		// suivante
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE) || viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			partenaire.setIsDeleted(false);
			partenaire.setWhenDone(new Timestamp(System.currentTimeMillis()));
			partenaire.setWhoDone(String.valueOf(request.session().get("login").get()));

		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {

			if (structureService.saveLogical(partenaire, true).equals("ok"))
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Partenaire " + partenaire.getLibelle() + " a été ajouté avec success");

			else
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Partenaire " + partenaire.getLibelle() + " non ajouter ");

		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			System.out.println("modif :" + (structureService.saveLogical(partenaire, false)));

			if (structureService.saveLogical(partenaire, false).equals("ok"))
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Partenaire  modifier avec success");

			else
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Partenaire  non modifier");

		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			partenaire.setIsDeleted(true);
			partenaire.setWhenDone(new Timestamp(System.currentTimeMillis()));
			partenaire.setWhoDone(String.valueOf(request.session().get("login").get()));
			if (structureService.saveLogical(partenaire, false).equals("ok"))
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Partenaire  Supprimer avec success");

			else
				return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Partenaire  non supprimer");

		}
		return redirect(routes.StructurePartenaireCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L));
	}

}
