package controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.CategorieCompte;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.compta.CategorieCompteServicesImpl;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */

@Security.Authenticated(Secured.class)
public class CategorieCompteCtrl  extends Controller{
	
	FormFactory formFactory;
	CategorieCompteServicesImpl categorieServices;
	
	@Inject
	public CategorieCompteCtrl(FormFactory formFactory, CategorieCompteServicesImpl categorieServices) {
		super();
		this.formFactory = formFactory;
		this.categorieServices = categorieServices;
	}
	
	
	public Result show(String subAction,String mode, Long idCategorie, Request request) {
		String viewMode;
		CategorieCompte c;
		List<CategorieCompte> listes = new ArrayList<CategorieCompte>();
		if (0 == idCategorie) {
			c = new CategorieCompte();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = categorieServices.findById(idCategorie);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = categorieServices.findById(idCategorie);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = categorieServices.findById(idCategorie);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = categorieServices.findById(idCategorie);

		}
		
		if(mode.equals(ViewMode.VIEW_MODE_BUDGET)) {
			mode = ViewMode.VIEW_MODE_BUDGET;
			listes = categorieServices.listeCategorieCompteB();
		}
		else {
			mode = ViewMode.VIEW_MODE_COMPTA;
			listes = categorieServices.listeCategorieCompteC();
		}
		
		return ok(views.html.categorieCompte.render(viewMode, mode,listes, c, request));

	}
	
	public Result save(Request request) {
		
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		final String mode = formFactory.form().bindFromRequest(request).get("mode");
		
		Form<CategorieCompte> uForm = formFactory.form(CategorieCompte.class).bindFromRequest(request);
		
		CategorieCompte c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		
		if(mode.equals(ViewMode.VIEW_MODE_BUDGET))
			c.setIsForBudget(true); 
		else
			c.setIsForBudget(false);
		
		if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if(categorieServices.saveLogical(c, true).equals("ok")) {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("success", " Classe compte " + c.getLibelle() + " ajouter avec success");
			}else {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("error", " Classe compte " + c.getLibelle() + " non ajouter");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			if(categorieServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("success", " Classe compte " + c.getLibelle() + " modifier avec success");
			}else {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("error", " Classe compte " + c.getLibelle() + " non modifié");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setIsDeleted(true);
			if(categorieServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("success", " Classe compte " + c.getLibelle() + " supprimer avec success");
			}else {
				return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L))
						.flashing("error", " Classe compte " + c.getLibelle() + " non supprimer");
			}
		}
		
		return redirect(routes.CategorieCompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode, 0L));
	}
}
