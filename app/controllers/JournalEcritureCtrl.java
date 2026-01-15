package controllers;

import java.sql.Timestamp;

import com.google.inject.Inject;

import models.tables.pojos.ClassCompte;
import models.tables.pojos.JournalEcriture;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.compta.JournalServicesImpl;
import utils.Secured;
import utils.ViewMode;
/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class JournalEcritureCtrl extends Controller{

	JournalServicesImpl journalServices;
	FormFactory formFactory;
	
	@Inject
	public JournalEcritureCtrl(JournalServicesImpl journalServices, FormFactory formFactory) {
		super();
		this.journalServices = journalServices;
		this.formFactory = formFactory;
	}
	
	
	public Result save(Request request) {
		
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		//final String de = formFactory.form().bindFromRequest(request).get("viewMode");
		Form<JournalEcriture> uForm = formFactory.form(JournalEcriture.class).bindFromRequest(request);
		JournalEcriture c =uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		
		
		if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if(journalServices.saveLogical(c, true).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L))
						.flashing("success", " Parametre ajouter avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L))
						.flashing("success", " Paramètre non ajouter");
			}
		}
		
		return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L));
	}
	
}
