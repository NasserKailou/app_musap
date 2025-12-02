package controllers;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Files.TemporaryFile;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.AdherentMainServices;
import services.AyantDroitMainServices;
import services.ImageService;
import utils.CallJasperReport;
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

	@Inject
	public AyantDroitCtrl(FormFactory formFactory, AyantDroitMainServices ayantDroitService, ImageService imageService,
			AdherentMainServices adherentService, CallJasperReport jasper) {
		super();
		this.formFactory = formFactory;
		this.ayantDroitService = ayantDroitService;
		this.imageService = imageService;
		this.adherentService = adherentService;
		this.jasper = jasper;
	}

	private double getTauxPourLien(String lien) {
				if (lien == null) return 0.0;

				String v = lien.trim().toUpperCase();
				switch (v) {
					case "ENFANT":
						return 1.0;
					case "CONJOINT":
						return 1.5;
					case "PARENT":
						return 3.0;
					default:
						return 0.0;
				}
			}


	public Result show(String subAction, Long idAyantDroit, Long idAdherent, Request request) {

//		if (!isAdmin()) {
//			return redirect(routes.AuthenticationCtrl.logout());
//		}
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
		//System.out.println( ayantDroitService.findAyantDroitByAdherent(idAdherent));
		return ok(views.html.ayantDroit.render(viewMode, ayantDroitService.findAyantDroitByAdherent(idAdherent), c,
				adherentService.findById(idAdherent), request));

	}

	

	public Result restaure(Long idPart, Request request) {
		AyantDroit c = ayantDroitService.findById(idPart);
		c.setOnDeleted(false);
		ayantDroitService.update(c);
		return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
	}

	public Result save(Request request) {

    final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

    Form<AyantDroit> uForm = formFactory.form(AyantDroit.class).bindFromRequest(request);
    String dateNaiss = formFactory.form().bindFromRequest(request).get("tmpDate");

    AyantDroit c = uForm.get();
    c.setWhenDone(new Timestamp(System.currentTimeMillis()));
    c.setOnDeleted(false);
    c.setWhoDone(String.valueOf(request.session().get("login").get()));
    
    // Adhérent concerné
    Adherent ad = adherentService.findById(c.getAdherent());

    // ================== GESTION DES POURCENTAGES ==================
    double deltaPourcentage = 0.0;

    if (ViewMode.VIEW_MODE_CREATE.equals(viewMode)) {
        // Nouveau AYANT DROIT : on ajoute son taux
        double taux = getTauxPourLien(c.getLien());
        c.setPourcentageRetenue(taux);
        deltaPourcentage = taux;
		c.setDateNaiss(ayantDroitService.getDateT(dateNaiss));


    } else if (ViewMode.VIEW_MODE_EDIT.equals(viewMode)) {
        // On récupère l'ancien pour recalculer le delta
        AyantDroit ancien = ayantDroitService.findById(c.getId());
        if (ancien != null && !Boolean.TRUE.equals(ancien.getOnDeleted())) {
            double ancienTaux = (ancien.getPourcentageRetenue() != null)
                    ? ancien.getPourcentageRetenue()
                    : getTauxPourLien(ancien.getLien());

            double nouveauTaux = getTauxPourLien(c.getLien());
            c.setPourcentageRetenue(nouveauTaux);

            deltaPourcentage = nouveauTaux - ancienTaux;
        }
		c.setDateNaiss(ayantDroitService.getDateT(dateNaiss));

    } else if (ViewMode.VIEW_MODE_DELETE.equals(viewMode)) {
        // Suppression → on retire son taux
        AyantDroit ancien = ayantDroitService.findById(c.getId());
        if (ancien != null && !Boolean.TRUE.equals(ancien.getOnDeleted())) {
            double ancienTaux = (ancien.getPourcentageRetenue() != null)
                    ? ancien.getPourcentageRetenue()
                    : getTauxPourLien(ancien.getLien());

            deltaPourcentage = -ancienTaux;
        }
        c.setOnDeleted(true);
		c.setDateNaiss(c.getDateNaiss());

    }
    // En mode TRAITE on ne touche pas aux pourcentages

    // Application du delta sur l’adhérent
    if (deltaPourcentage != 0.0 && ad != null) {
        Double total = ad.getPourcentageTotalRetenue();
        if (total == null) total = 0.0;
        total = total + deltaPourcentage;
        if (total < 0.0) total = 0.0; // sécurité pour éviter les valeurs négatives
        ad.setPourcentageTotalRetenue(total);
        adherentService.saveLogical(ad, false);
    }
    // ================== FIN GESTION POURCENTAGES ==================

    // ====== CRUD classique sur AyantDroit ======
    if (ViewMode.VIEW_MODE_CREATE.equals(viewMode)) {
        c.setPicture(new File("").getAbsolutePath() + "/public/images/ayantDroits/1.jpg");

        if ("ok".equals(ayantDroitService.saveLogical(c, true))) {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", " AyantDroit " + c.getNomAy() + " ajouté avec succès");
        } else {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", " AyantDroit " + c.getNomAy() + " non ajouté");
        }

    } else if (ViewMode.VIEW_MODE_EDIT.equals(viewMode)) {

        if ("ok".equals(ayantDroitService.saveLogical(c, false))) {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", " AyantDroit modifié avec succès");
        } else {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", " AyantDroit non modifié");
        }

    } else if (ViewMode.VIEW_MODE_TRAITE.equals(viewMode)) {

        if ("ok".equals(ayantDroitService.saveLogical(c, false))) {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", " AyantDroit traité avec succès");
        } else {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", "Échec lors du traitement de l'AyantDroit");
        }

    } else if (ViewMode.VIEW_MODE_DELETE.equals(viewMode)) {

        if ("ok".equals(ayantDroitService.saveLogical(c, false))) {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", " AyantDroit supprimé avec succès");
        } else {
            return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", " AyantDroit non supprimé");
        }
    }

    return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
}


/** 
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<AyantDroit> uForm = formFactory.form(AyantDroit.class).bindFromRequest(request);
		String dateNaiss = formFactory.form().bindFromRequest(request).get("tmpDate");
		
		AyantDroit c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setOnDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		c.setDateNaiss(ayantDroitService.getDateT(dateNaiss));

		Adherent ad = adherentService.findById(c.getAdherent());

		if(c.getLien().equals("ENFANT") ){
			c.setPourcentageRetenue(1.0);
			ad.setPourcentageTotalRetenue(ad.getPourcentageTotalRetenue()+1.0);
			adherentService.saveLogical(ad, false);
		}
			
		if(c.getLien().equals("CONJOINT")){
			ad.setPourcentageTotalRetenue(ad.getPourcentageTotalRetenue()+1.5);
			adherentService.saveLogical(ad, false);
		}
			
		if(c.getLien().equals("PARENT")){
			ad.setPourcentageTotalRetenue(ad.getPourcentageTotalRetenue()+3.0);
			adherentService.saveLogical(ad, false);
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			c.setPicture(new File("").getAbsolutePath() + "/public/images/ayantDroits//1.jpg");
			if (ayantDroitService.saveLogical(c, true).equals("ok")) {
				// flash("success", " AyantDroit " + c.getLibelle() + " ajouter avec success");
				// System.out.println("AyantDroits : " + c);
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " AyantDroit " + c.getNomAy() + " ajouter avec success");
			} else {
				System.out.println("msg :" + ayantDroitService.saveLogical(c, true));
				// flash("error", " AyantDroit " + c.getLibelle() + " non ajouter ");
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", " AyantDroit " + c.getNomAy() + " non ajouter ");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			// a.setLogin(login);
			if (ayantDroitService.saveLogical(c, false).equals("ok")) {
				// flash("success", " AyantDroit modifier avec success");
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " AyantDroit  modifier avec success");
			} else {
				// flash("error", "AyantDroits non modifier");
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", "AyantDroits  non modifier");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_TRAITE)) {
			
			if (ayantDroitService.saveLogical(c, false).equals("ok")) {

				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " AyantDroit  traiter avec success");
			} else {
				// flash("error", "Echec lors du traitement de l'AyantDroit");
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", "Echec lors du traitement de l'AyantDroit");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setOnDeleted(true);
			if (ayantDroitService.saveLogical(c, false).equals("ok")) {
				// flash("success", " AyantDroit Supprimer avec success");
				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " AyantDroit  Supprimer avec success");
			} else {

				return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", " AyantDroit  non supprimer");
			}
		}
		return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
	}
*/
	public Result formProfil(Request request, Long idAdherent) {
		
		return ok(views.html.photoAyantDroit.render(idAdherent,request));

	}
	
	public Result path(Request request, String path) {
        return ok(new File(path));
    }

	public Result formProfilSave(Request request) {

		Http.MultipartFormData<TemporaryFile> body = request.body().asMultipartFormData();
	    Http.MultipartFormData.FilePart<TemporaryFile> picture = body.getFile("photo");
	    final String id = formFactory.form().bindFromRequest(request).get("id-ay");
	    AyantDroit ay = ayantDroitService.findById(Long.parseLong(id));
	    if (picture != null) {
	      String fileName = picture.getFilename();
	      long fileSize = picture.getFileSize();
	      String contentType = picture.getContentType();
	      TemporaryFile file = picture.getRef();
	      String extension=imageService.getExtension(fileName);
	      //recuperer le chemin absolu du fichier
	      String path = new File("").getAbsolutePath() +"/public/images/ayantDroits/";
	     // String r=imageService.isok("/home/alpariss/Documents/Projets/mutuel-ins/public/images/", id, extension, file);
		//renseigne is_ok_for_printing pour l'impression des carte
		 AyantDroit c = ayantDroitService.findById(Long.valueOf(id));
		 	c.setIsOkForPrinting(true);
			ayantDroitService.saveLogical(c, false);

	      String r=imageService.isok2(path, id, extension, file);
	      return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, ay.getAdherent()));
	    } else {
	      return badRequest().flashing("error", "Missing file");
	    }

	}
	
	public Result printCarte(Request request, Long idAyantDroit, String fileName) {

		// String fileName = "recu";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");
			System.out.println("le non du fichier :"+fileName);
			jasper.generateReport( idAyantDroit, fileName);

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + String.valueOf(idAyantDroit) + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.AyantDroitCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, ayantDroitService.findById(idAyantDroit).getAdherent())).flashing("error",
					"Erreur d'impression");
		}
	}
}
