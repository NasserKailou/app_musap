package controllers;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.google.inject.Inject;

import models.tables.pojos.Depense;
import models.tables.pojos.Ecriture;
import play.data.Form;
import play.data.FormFactory;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.compta.CategorieCompteServicesImpl;
import services.compta.ClassComptaServicesImpl;
import services.compta.DepenseServicesImpl;
import services.compta.EcritureServicesImpl;
import services.compta.JournalServicesImpl;
import utils.CallJasperReport;
import utils.Secured;
import utils.ViewMode;
/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class OperationComptableCtrl extends Controller {

	FormFactory formFactory;
	ClassComptaServicesImpl cptServices;
	DepenseServicesImpl depService;
	JournalServicesImpl journalService;
	EcritureServicesImpl ecritureService;
	CategorieCompteServicesImpl categorieServices;
	CallJasperReport jasper;
	
	@Inject
	public OperationComptableCtrl(FormFactory formFactory, ClassComptaServicesImpl cptServices,
			DepenseServicesImpl depService, JournalServicesImpl journalService, EcritureServicesImpl ecritureService,
			CategorieCompteServicesImpl categorieServices, CallJasperReport jasper) {
		super();
		this.formFactory = formFactory;
		this.cptServices = cptServices;
		this.depService = depService;
		this.journalService = journalService;
		this.ecritureService = ecritureService;
		this.categorieServices = categorieServices;
		this.jasper = jasper;
	}

	
	
	
	public Result showDepense(String subAction, Long idDepense, Request request) {
		String viewMode;
		Depense c;
		
		if (0 == idDepense) {
			c = new Depense();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = depService.findById(idDepense);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = depService.findById(idDepense);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = depService.findById(idDepense);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = depService.findById(idDepense);

		}
		
		return ok(views.html.depense.render(viewMode, depService.listDepense(), c, request));

	}
	

	public Result saveDepense(Request request) {
		
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		final String date1 = formFactory.form().bindFromRequest(request).get("dateTmp");
		Form<Depense> uForm = formFactory.form(Depense.class).bindFromRequest(request);
		
		Depense c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		c.setDateDepense(ecritureService.getDateT(date1));
		
		
		if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if(depService.saveLogical(c, true).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("success", " Operation " + c.getRefDepense() + " ajouter avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("error", " Operation " + c.getRefDepense() + " non ajouter");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			if(depService.saveLogical(c, false).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("success", " Operation " + c.getRefDepense() + " modifier avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("error", " Operation " + c.getRefDepense() + " non modifié");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setIsDeleted(true);
			if(depService.saveLogical(c, false).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("success", " Operation " + c.getRefDepense() + " supprimer avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L))
						.flashing("error", " Operation " + c.getRefDepense() + " non supprimer");
			}
		}
		
		return redirect(routes.OperationComptableCtrl.showDepense(ViewMode.VIEW_MODE_CREATE, 0L));
	}

	public Result showEcriture(String subAction, Long idDepense, Long idEcriture, Request request) {
		String viewMode;
		Ecriture c;
		
		if (0 == idEcriture) {
			c = new Ecriture();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = ecritureService.findById(idEcriture);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = ecritureService.findById(idEcriture);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = ecritureService.findById(idEcriture);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = ecritureService.findById(idEcriture);

		}
		
		return ok(views.html.ecritureComptable.render(viewMode, ecritureService.listeEcriture(idDepense), 
				c,depService.findById(idDepense),cptServices.listeCompte(),journalService.listeJournal(),categorieServices.listeCategorieCompteC(), request));

	}
	
	public Result saveEcriture(Request request) {
		
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		final String date1 = formFactory.form().bindFromRequest(request).get("dateTmp");
		Form<Ecriture> uForm = formFactory.form(Ecriture.class).bindFromRequest(request);
		
		Ecriture c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setIsDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		c.setDateOperation(ecritureService.getDateT(date1));
		
		//System.out.println("Ecriture :"+c +" ajout "+ ecritureService.saveLogical(c, true));
		
		if(viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if(ecritureService.saveLogical(c, true).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("success", " Ecriture "+c.getRefFacture()+" - "  + " ajouter avec success");
			}else {
				System.out.println("Log SQL :"+ ecritureService.saveLogical(c, true));
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("error", " Ecriture "+c.getRefFacture()+" - "  + " non ajouter");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			if(ecritureService.saveLogical(c, false).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("success", " Ecriture "+c.getRefFacture()+" - "  + " modifier avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("error", " Ecriture "+c.getRefFacture()+" - "  + " non modifié");
			}
		}else if(viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setIsDeleted(true);
			if(ecritureService.saveLogical(c, false).equals("ok")) {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("success", " Ecriture "+c.getRefFacture()+" - "  + " supprimer avec success");
			}else {
				return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L))
						.flashing("error", " Ecriture "+c.getRefFacture()+" - "  + " non supprimer");
			}
		}
		
		return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,c.getDepense(), 0L));
	}
	
	public Result printJournal(Request request) {
		
		String dateD1 = formFactory.form().bindFromRequest(request).get("date1");
		String dateD2 = formFactory.form().bindFromRequest(request).get("date2");

		String fileName = "Journal_comptableb_between";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName, ecritureService.getDateT(dateD1), ecritureService.getDateT(dateD2));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + "" + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.OperationComptableCtrl.showEcriture(ViewMode.VIEW_MODE_CREATE,1L, 0L));
		}
		
	}
}
