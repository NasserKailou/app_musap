package controllers;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Files.TemporaryFile;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.AdherentMainServices;
import services.AyantDroitMainServices;
import services.CotisationServices;
import services.ImageService;
import utils.Acces;
import utils.CallJasperReport;
import utils.Permission;
import utils.Permissions;
import utils.Secured;
import utils.ViewMode;

/**
 *
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class AyantDroitCtrl extends Controller {

	FormFactory formFactory;
	AyantDroitMainServices ayantDroitService;
	ImageService imageService;
	AdherentMainServices adherentService;
	CallJasperReport jasper;
	CotisationServices cotisationServices;

	@Inject
	public AyantDroitCtrl(FormFactory formFactory, AyantDroitMainServices ayantDroitService, ImageService imageService,
			AdherentMainServices adherentService, CallJasperReport jasper, CotisationServices cotisationServices) {
		super();
		this.formFactory = formFactory;
		this.ayantDroitService = ayantDroitService;
		this.imageService = imageService;
		this.adherentService = adherentService;
		this.jasper = jasper;
		this.cotisationServices = cotisationServices;
	}

	@Permission({ Permissions.AYANT_DROIT_CONSULTER, Permissions.AYANT_DROIT_GERER })
	public Result show(String subAction, Long idAyantDroit, Long idAdherent, Request request) {

		Adherent adherent = adherentService.findById(idAdherent);
		if (adherent == null || !Acces.peutVoirAdherent(request, adherent.getTelephone(), Permissions.ADHERENT_LISTE)
				&& !Acces.peut(request, Permissions.AYANT_DROIT_GERER)) {
			return redirect(routes.HomeController.acceuil()).flashing("error", "Adhérent introuvable ou non autorisé");
		}
		String viewMode;
		AyantDroit c;

		if (0 == idAyantDroit) {
			c = new AyantDroit();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = ayantDroitService.findById(idAyantDroit);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = ayantDroitService.findById(idAyantDroit);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = ayantDroitService.findById(idAyantDroit);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = ayantDroitService.findById(idAyantDroit);

		}
		if (c == null) {
			c = new AyantDroit();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		}
		return ok(views.html.ayantDroit.render(viewMode, ayantDroitService.findAyantDroitByAdherent(idAdherent), c,
				adherent, cotisationServices.detail(idAdherent), cotisationServices.listLiens(), request));

	}

	@Permission(Permissions.AYANT_DROIT_GERER)
	public Result restaure(Long idPart, Request request) {
		AyantDroit c = ayantDroitService.findById(idPart);
		if (c == null) {
			return redirect(routes.HomeController.acceuil()).flashing("error", "Ayant droit introuvable");
		}
		c.setOnDeleted(false);
		ayantDroitService.update(c);
		// la réactivation n'était pas prise en compte dans la cotisation
		Double total = cotisationServices.recalculer(c.getAdherent());
		return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
				.flashing("success", "Ayant droit réactivé" + messageCotisation(total));
	}

	@Permission(Permissions.AYANT_DROIT_GERER)
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<AyantDroit> uForm = formFactory.form(AyantDroit.class).bindFromRequest(request);
		String dateNaiss = formFactory.form().bindFromRequest(request).get("tmpDate");
		AyantDroit c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setOnDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		if (c.getLien() != null)
			c.setLien(c.getLien().trim().toUpperCase());

		// Adhérent concerné
		Adherent ad = adherentService.findById(c.getAdherent());
		if (ad == null) {
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Adhérent introuvable");
		}

		// La part de cotisation de chaque ayant droit et le total de l'adhérent sont
		// recalculés intégralement après l'enregistrement (CotisationServices).
		if (ViewMode.VIEW_MODE_CREATE.equals(viewMode)) {
			c.setDateNaiss(ayantDroitService.getDateT(dateNaiss));
			c.setPourcentageRetenue(0.0);

		} else if (ViewMode.VIEW_MODE_EDIT.equals(viewMode)) {
			AyantDroit ancien = ayantDroitService.findById(c.getId());
			c.setDateNaiss(ayantDroitService.getDateT(dateNaiss));
			if (ancien != null) {
				c.setPourcentageRetenue(ancien.getPourcentageRetenue());
				c.setPicture(ancien.getPicture());
				c.setIsOkForPrinting(ancien.getIsOkForPrinting());
			}

		} else if (ViewMode.VIEW_MODE_DELETE.equals(viewMode)) {
			AyantDroit anc = ayantDroitService.findById(c.getId());
			if (anc == null) {
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", "Ayant droit introuvable");
			}
			c.setOnDeleted(true);
			c.setDateNaiss(anc.getDateNaiss());
			c.setNomAy(anc.getNomAy());
			c.setLien(anc.getLien());
			c.setGenre(anc.getGenre()); // le genre était écrasé par le lien
			c.setPicture(anc.getPicture());
			c.setIsOkForPrinting(anc.getIsOkForPrinting());
			c.setPourcentageRetenue(anc.getPourcentageRetenue());
		}

		// ====== CRUD classique sur AyantDroit ======
		String message;
		boolean ok;
		if (ViewMode.VIEW_MODE_CREATE.equals(viewMode)) {
			c.setPicture(new File("").getAbsolutePath() + "/public/images/ayantDroits/1.jpg");
			ok = "ok".equals(ayantDroitService.saveLogical(c, true));
			message = ok ? " AyantDroit " + c.getNomAy() + " ajouté avec succès"
					: " AyantDroit " + c.getNomAy() + " non ajouté";

		} else if (ViewMode.VIEW_MODE_EDIT.equals(viewMode)) {
			ok = "ok".equals(ayantDroitService.saveLogical(c, false));
			message = ok ? " AyantDroit modifié avec succès" : " AyantDroit non modifié";

		} else if (ViewMode.VIEW_MODE_TRAITE.equals(viewMode)) {
			ok = "ok".equals(ayantDroitService.saveLogical(c, false));
			message = ok ? " AyantDroit traité avec succès" : "Échec lors du traitement de l'AyantDroit";

		} else if (ViewMode.VIEW_MODE_DELETE.equals(viewMode)) {
			ok = "ok".equals(ayantDroitService.saveLogical(c, false));
			message = ok ? " AyantDroit supprimé avec succès" : " AyantDroit non supprimé";
		} else {
			return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
		}

		if (ok) {
			Double total = cotisationServices.recalculer(ad.getId());
			message += messageCotisation(total);
		}
		return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
				.flashing(ok ? "success" : "error", message);
	}

	private static String messageCotisation(Double total) {
		return total == null ? ""
				: " — cotisation de l'adhérent : " + String.format("%.2f", total).replace('.', ',') + " % du salaire";
	}

	@Permission(Permissions.PHOTO_GERER)
	public Result formProfil(Request request, Long idAdherent) {

		return ok(views.html.photoAyantDroit.render(idAdherent, request));

	}

	public Result path(Request request, String path) {
		return ok(new File(path));
	}

	@Permission(Permissions.PHOTO_GERER)
	public Result formProfilSave(Request request) {

		Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
		Http.MultipartFormData.FilePart<TemporaryFile> picture = body.getFile("photo");
		final String id = formFactory.form().bindFromRequest(request).get("id-ay");
		if (picture != null) {
			String fileName = picture.getFilename();
			TemporaryFile file = picture.getRef();
			String extension = imageService.getExtension(fileName);
			// recuperer le chemin absolu du fichier
			String path = new File("").getAbsolutePath() + "/public/images/ayantDroits/";
			// renseigne is_ok_for_printing pour l'impression des carte
			AyantDroit c = ayantDroitService.findById(Long.valueOf(id));
			c.setIsOkForPrinting(true);
			ayantDroitService.saveLogical(c, false);

			imageService.isok2(path, id, extension, file);

			ObjectNode json = Json.newObject();
			json.put("success", true);
			json.put("picture", path + id + "." + extension);
			return ok(json);
		} else {
			ObjectNode json = Json.newObject();
			json.put("success", false);
			json.put("message", "Fichier manquant");
			return badRequest(json);
		}

	}

	@Permission(Permissions.ADHERENT_IMPRIMER)
	public Result printCarte(Request request, Long idAyantDroit, String fileName) {

		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			System.out.println("le non du fichier :" + fileName);
			jasper.generateReport(idAyantDroit, fileName);

			return ok(new java.io.File(
					templateDir + fileName + "_" + now_string + "_" + String.valueOf(idAyantDroit) + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L,
					ayantDroitService.findById(idAyantDroit).getAdherent())).flashing("error",
							"Erreur d'impression");
		}
	}
}
