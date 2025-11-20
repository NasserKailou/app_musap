package controllers;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.google.inject.Inject;

import models.ins.tables.pojos.Categorie;
import models.tables.pojos.CategorieCompte;
import models.tables.pojos.ClassCompte;
import models.tables.pojos.JournalEcriture;
import play.mvc.Result;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Security;
import play.mvc.Http.Request;
import services.compta.CategorieCompteServicesImpl;
import services.compta.ClassComptaServicesImpl;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */

@Security.Authenticated(Secured.class)
public class CompteCtrl extends Controller{

	ClassComptaServicesImpl cptServices;
	CategorieCompteServicesImpl categorieServices;
	FormFactory formFactory;
	
	@Inject
	public CompteCtrl(ClassComptaServicesImpl cptServices, CategorieCompteServicesImpl categorieServices,
			FormFactory formFactory) {
		super();
		this.cptServices = cptServices;
		this.categorieServices = categorieServices;
		this.formFactory = formFactory;
	}
	
	public Result show(String subAction,String mode, Long numCpt,Long idCompte , Request request) {
		String viewMode;
		List<ClassCompte> listesCompte = new ArrayList<>();
		CategorieCompte categorie = new CategorieCompte();
		ClassCompte c ;
	
		if (0 == idCompte) {
			c = new ClassCompte();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = cptServices.findById(idCompte);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = cptServices.findById(idCompte);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = cptServices.findById(idCompte);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = cptServices.findById(idCompte);

		}
		if(mode.equals(ViewMode.VIEW_MODE_BUDGET)) {
			mode = ViewMode.VIEW_MODE_BUDGET;
			listesCompte = cptServices.listeCompteByCategorieB(numCpt);
			categorie = categorieServices.listeCategorieCompteByNumB(numCpt);
		}
		else {
			mode = ViewMode.VIEW_MODE_COMPTA;
			listesCompte = cptServices.listeCompteByCategorieC(numCpt);
			categorie = categorieServices.listeCategorieCompteByNumC(numCpt);
		}
		
		return ok(views.html.classCompte.render(viewMode,mode, listesCompte,categorie, c, request));

	}
	
		public Result saveRapide(Request request) {
				
				final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
				//final String mode = formFactory.form().bindFromRequest(request).get("mode");
				//final String de = formFactory.form().bindFromRequest(request).get("viewMode");
				Form<ClassCompte> uForm = formFactory.form(ClassCompte.class).bindFromRequest(request);
				ClassCompte c =uForm.get();
				c.setWhenDone(new Timestamp(System.currentTimeMillis()));
				c.setIsDeleted(false);
				c.setWhoDone(String.valueOf(request.session().get("login").get()));
				
				
				if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
					if(cptServices.saveLogical(c, true).equals("ok")) {
						return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L))
								.flashing("success", " Compte ajouter avec success");
					}else {
						return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L))
								.flashing("success", " Compte non ajouter");
					}
				}
				
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L));
			}
	
	public Result save(Request request) {
		
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		final String mode = formFactory.form().bindFromRequest(request).get("mode");
		
		Form<ClassCompte> uForm = formFactory.form(ClassCompte.class).bindFromRequest(request);
		
		ClassCompte c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		
		if(mode.equals(ViewMode.VIEW_MODE_BUDGET))
			c.setIsForBudget(true); 
		else
			c.setIsForBudget(false);
		
		if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if(cptServices.saveLogical(c, true).equals("ok")) {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("success", " Classe compte " + c.getLibelleCompte() + " ajouter avec success");
			}else {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("error", " Classe compte " + c.getLibelleCompte() + " non ajouter");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			if(cptServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("success", " Classe compte " + c.getLibelleCompte() + " modifier avec success");
			}else {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("error", " Classe compte " + c.getLibelleCompte() + " non modifié");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setIsDeleted(true);
			if(cptServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("success", " Classe compte " + c.getLibelleCompte() + " supprimer avec success");
			}else {
				return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L))
						.flashing("error", " Classe compte " + c.getLibelleCompte() + " non supprimer");
			}
		}
		
		return redirect(routes.CompteCtrl.show(ViewMode.VIEW_MODE_CREATE,mode,c.getCategorie(), 0L));
	}
}
