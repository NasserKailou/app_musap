package controllers;

import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import play.libs.Files.TemporaryFile;

import java.nio.file.Paths;

import javax.inject.Inject;


import models.tables.pojos.Users;
import models.tables.pojos.Adherent;
import models.tables.pojos.VAdherent;
import play.data.Form;
import play.data.FormFactory;
import play.filters.csrf.AddCSRFToken;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.AdherentMainServices;
import services.CategorieMainServices;
import services.ImageService;
import services.ProgrammeMainService;
import services.UserService;
import utils.CallJasperReport;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class AdherentCtrl extends Controller {

	FormFactory formFactory;
	AdherentMainServices adherentService;
	ImageService imageService;
	CategorieMainServices catService;
	ProgrammeMainService progService;
	CallJasperReport jasper;
	UserService userService;
	Long coutConsultation = 0L;

	
	@Inject
	public AdherentCtrl(FormFactory formFactory, AdherentMainServices adherentService, ImageService imageService,
			CategorieMainServices catService, ProgrammeMainService progService, CallJasperReport jasper,
			UserService userService) {
		//super();
		this.formFactory = formFactory;
		this.adherentService = adherentService;
		this.imageService = imageService;
		this.catService = catService;
		this.progService = progService;
		this.jasper = jasper;
		this.userService = userService;
		
	}

	public Result show(Request request, String subAction, Long idAd) {
		String viewMode;
		Adherent c;
		List<VAdherent> adherents = new ArrayList<VAdherent>();

		if (request.session().get("droit").get().equals("Adherent"))
			adherents = adherentService.getByTelNumber(request.session().get("login").get());
		else
			adherents = adherentService.listeAdherents();
		// adherents = adherentService.listeAdherents();

		// List<Personnels> medecins = persServices.listes("Medecin");
		if (0 == idAd) {
			c = new Adherent();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = adherentService.findById(idAd);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = adherentService.findById(idAd);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = adherentService.findById(idAd);

		}
		// System.out.println("la liste des consultations est :"+ consultations);
		return ok(views.html.adherents.render(viewMode, adherents, c, catService.findAll(),
				progService.findAllProgamme(), request));

	}

	public Result showSituationView(Request request) {
		return ok(views.html.rapports.render(request));
	}
//	public Result restaure(Request request, Long idConsul) {
//		Adherent c = adherentService.findById(idConsul);
//		c.setOnDeleted(false);
//		adherentService.update(c);
//
//		Adherent cc = new Adherent();
//
//		return ok(views.html.consultationsDeleted.render(ViewMode.VIEW_MODE_CREATE,
//				adherentService.listeConsultationsDeleted(), typeConServices.findAll(),
//				persServices.listes("Medecin"), cc, request));
//
//	}

	@AddCSRFToken
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<Adherent> uForm = formFactory.form(Adherent.class).bindFromRequest(request);
		Adherent ad = uForm.get();
		String dateDebut = formFactory.form().bindFromRequest(request).get("tmpDate2");
		String dateNaisse = formFactory.form().bindFromRequest(request).get("tmpDate");

		// ne renseigner ses variables que dans les cas ou il s'agit des deux operation
		// suivante
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE) || viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			ad.setDirection(1L);
			ad.setDivision(1L);
			ad.setService(1L);
			ad.setOnDeleted(false);
			ad.setWhenDone(new Timestamp(System.currentTimeMillis()));
			ad.setWhoDone(String.valueOf(request.session().get("login").get()));
			ad.setTelephone(adherentService.normaliserNumeroNiger(ad.getTelephone()));

			if(viewMode.equals(ViewMode.VIEW_MODE_CREATE))
				ad.setPourcentageTotalRetenue(2.5);
			if(viewMode.equals(ViewMode.VIEW_MODE_EDIT))
				ad.setPourcentageTotalRetenue(ad.getPourcentageTotalRetenue());

			ad.setDateNaiss(adherentService.getDateT(dateNaisse));
			ad.setDatePriseService(adherentService.getDateT(dateDebut));
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			ad.setPicture(new File("").getAbsolutePath() + "/public/images/adherents//1.jpg");
			if (adherentService.saveLogical(ad, true).equals("ok")) {
				Users a = new Users();
				a.setLogin(ad.getTelephone());
				a.setPasse("1234");
				a.setDroit("Adherent");
				a.setEtat(true);
				a.setNomPrenom(ad.getNomAd() + "-" + ad.getPrenomAd());
				a.setWhoDone("Admin_mutuel");
				a.setConnectFirst(true);
				a.setWhenDone(new Timestamp(System.currentTimeMillis()));
				userService.saveLogical(a, true);
				// a.setWhenDone(new Timestamp(System.currentTimeMillis()));
				// System.out.println("Consultations : " + c);
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Adhérent pour " + ad.getNomAd() + " a été ajouté avec success");
			} else {

				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Adhérent pour " + ad.getNomAd() + " non ajouter ");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			System.out.println("modif :" + (adherentService.saveLogical(ad, false)));
			if (adherentService.saveLogical(ad, false).equals("ok")) {
				// flash("success", "Consultations modifier avec success");
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Adhérent  modifier avec success");
			} else {
				// flash("error", "Consultations non modifier");
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Adhérent  non modifier");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			ad.setOnDeleted(true);
			ad.setWhenDone(new Timestamp(System.currentTimeMillis()));
			ad.setWhoDone(String.valueOf(request.session().get("login").get()));
			if (adherentService.saveLogical(ad, false).equals("ok")) {
				// flash("success", "Consultations Supprimer avec success");
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("success",
						"Adhérent  Supprimer avec success");
			} else {
				// flash("error", "Consultations non supprimer");
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
						"Adhérent  non supprimer");
			}
		}
		return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L));
	}

	/**
	 * ajouter des ayants droit à l'adherent
	 * 
	 * @param idConsul
	 * @return
	 */
	public Result addAyantDroit(Request request, Long idAdherent) {

		return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, idAdherent));

	}

	public Result formProfil(Request request, Long idAdherent) {
		adherentService.getById(idAdherent);

		return ok(views.html.photo.render(adherentService.getById(idAdherent), request));

	}

	public Result formProfilSave(Request request) {

		Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
		Http.MultipartFormData.FilePart<TemporaryFile> picture = body.getFile("photo");
		final String matricule = formFactory.form().bindFromRequest(request).get("matricule");
		if (picture != null) {
			String fileName = picture.getFilename();
			long fileSize = picture.getFileSize();
			String contentType = picture.getContentType();
			TemporaryFile file = picture.getRef();
			String extension = imageService.getExtension(fileName);
			// recuperer le chemin absolu du fichier
			String path = new File("").getAbsolutePath() + "/public/images/adherents/";
			
			try{
				Adherent c = adherentService.findById(adherentService.getByMatricule(String.valueOf(matricule)).getId());
				c.setIsOkForPrinting(true);
				adherentService.saveLogical(c, false);
				String r = imageService.isok(path, matricule, extension, file);

			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L));
			} catch(Exception e){
				
				return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error","Erreur lors du chargement , le matricule:"+ e.getMessage()+" ou est déja affecté a un autre agent");
			}
			//renseigne is_ok_for_printing pour l'impression des carte
			
		} else {
			return badRequest().flashing("error", "Missing file");
		}

	}

	public Result addRembourssement(Request request, Long idAdherent, String typeOP) {

		return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE,typeOP, 0L, idAdherent));

	}

	public Result print(Request request, String numMatricule, String fileName) {

		// String fileName = "recu";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName, numMatricule);

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + numMatricule + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Erreur d'impression");
		}
	}

	public Result rapportAdhBetwenne(Request request) {
		String dateD1 = formFactory.form().bindFromRequest(request).get("tmpDate");
		String dateD2 = formFactory.form().bindFromRequest(request).get("tmpDate2");
		String matricule = formFactory.form().bindFromRequest(request).get("adherent");

		String fileName = "situation_adherent_between";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName, adherentService.getDateT(dateD1), adherentService.getDateT(dateD2),
					matricule.replace(" ", ""));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + "" + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return ok(views.html.rapports.render(request)).flashing("error", " Erreur d'impression");
		}
	}

	public Result rapportBetwenne(Request request) {
		String dateD1 = formFactory.form().bindFromRequest(request).get("tmpDate");
		String dateD2 = formFactory.form().bindFromRequest(request).get("tmpDate2");

		String fileName = "situation_all_between";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName, adherentService.getDateT(dateD1), adherentService.getDateT(dateD2));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + "" + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return ok(views.html.rapports.render(request)).flashing("error", " Erreur d'impression");
		}
	}
//	public Result showSituationView(Request request) {
//		return ok(views.html.rapports.render(request));
//	}

public Result printCarte(Request request, Long idAdherent, String fileName) {

		// String fileName = "recu";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");
			System.out.println("le non du fichier :"+fileName);
			jasper.generateReport( idAdherent, fileName);

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + String.valueOf(idAdherent) + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Erreur d'impression");
		}
	}

	public Result impressionCarteMainsGlobal(Request request){
		String templateJrxmlName = formFactory.form().bindFromRequest(request).get("libelleCarte");
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");
			System.out.println("le non du fichier :"+templateJrxmlName);
			jasper.generateReport(templateJrxmlName);

			return ok(new java.io.File(templateDir + templateJrxmlName + "_" + now_string + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Erreur d'impression");
		}

	}	

	public Result path(Request request, String path) {
		return ok(new File(path));
	}

}
