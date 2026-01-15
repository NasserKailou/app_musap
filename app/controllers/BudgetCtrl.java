package controllers;

import java.sql.Timestamp;

import com.google.inject.Inject;

import models.tables.pojos.Budget;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.BudgetMainServices;
import services.ParamsServices;
import services.compta.ClassComptaServicesImpl;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class BudgetCtrl extends Controller {

	BudgetMainServices budgetServices;
	ClassComptaServicesImpl compteServices;
	ParamsServices paramServices;
	FormFactory formFactory;

	@Inject
	public BudgetCtrl(BudgetMainServices budgetServices, ClassComptaServicesImpl compteServices,
			ParamsServices paramServices, FormFactory formFactory) {
		super();
		this.budgetServices = budgetServices;
		this.compteServices = compteServices;
		this.paramServices = paramServices;
		this.formFactory = formFactory;
	}

	public Result show(String subAction, Long idBudget, String gestion, Request request) {
		String viewMode;
		Budget c;

		if (0 == idBudget) {
			c = new Budget();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = budgetServices.findById(idBudget);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = budgetServices.findById(idBudget);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = budgetServices.findById(idBudget);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = budgetServices.findById(idBudget);

		}

		return ok(views.html.budget.render(viewMode, budgetServices.findBudgetByGestion(gestion),
				compteServices.listeCompteBudget(), c, request));

	}

	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<Budget> uForm = formFactory.form(Budget.class).bindFromRequest(request);

		Budget c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		c.setGestion(paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getId());

		System.out.println("params gestion:" + c.getGestion() +" compte "+ c.getCompte() + " budget" + c.getTypeBuget());
		
		if(budgetServices.findUnik(c.getGestion(), c.getCompte(), c.getTypeBuget()).getId() >=1 && viewMode.equals(ViewMode.VIEW_MODE_CREATE)){
			return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
					paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("error", " Ligne Buget deja renseigner");
		}
		//System.out.println("ajout budget : "+ budgetServices.saveLogical(c, true));
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if (budgetServices.saveLogical(c, true).equals("ok")) {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("success"," Buget  ajouter avec success");
			} else {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("error", " Buget  non ajouter");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			if (budgetServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("success"," Buget  modifier avec success");
			} else {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("error", " Buget  non modifié");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setIsDeleted(true);
			if (budgetServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("success"," Buget  supprimer avec success");
			} else {
				return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
						paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion())).flashing("error"," Buget  non supprimer");
			}
		}

		return redirect(routes.BudgetCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
				paramServices.paramsByGestion(String.valueOf(request.session().get("gestion").get())).getGestion()));
	}

}
