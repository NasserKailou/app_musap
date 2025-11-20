package controllers;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.inject.Inject;
import com.fasterxml.jackson.databind.node.ObjectNode;

import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.Email;
import models.tables.pojos.Reglement;
import models.tables.pojos.ReglementDetail;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VAdherentAyantDroit;
import models.tables.pojos.VReglementGlobalByAdherent;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Http.Request;
import play.mvc.Result;
import play.mvc.Security;
import services.AdherentMainServices;
import services.AyantDroitMainServices;
import services.EmailManagerServices;
import services.Mail;
import services.ReglementDetailMainServices;
import services.ReglementMainServices;
import services.StructureMainServices;
import services.TypePrestationMainService;
import utils.CallJasperReport;
import utils.Secured;
import utils.ViewMode;

/**
 * 
 * @author nasser
 *
 */
@Security.Authenticated(Secured.class)
public class ReglementCtrl extends Controller {

	//private static final Long PLAFOND_REGLEMENT_ANNUELLE = 350000L;
	// private static final Long sommeReg = 0L;
	FormFactory formFactory;
	ReglementMainServices regServices;
	ReglementDetailMainServices reDetailMainServices;
	AdherentMainServices adherentService;
	StructureMainServices structureService;
	TypePrestationMainService typePresta;
	CallJasperReport jasper;
	Mail email;
	EmailManagerServices emailServices;

	@Inject
	public ReglementCtrl(FormFactory formFactory, ReglementMainServices regServices,
			ReglementDetailMainServices reDetailMainServices, AdherentMainServices adherentService,
			StructureMainServices structureService, TypePrestationMainService typePresta, CallJasperReport jasper, Mail email,EmailManagerServices emailServices) {
		super();
		this.formFactory = formFactory;
		this.regServices = regServices;
		this.reDetailMainServices = reDetailMainServices;
		this.adherentService = adherentService;
		this.structureService = structureService;
		this.typePresta = typePresta;
		this.jasper = jasper;
		this.email = email;
		this.emailServices = emailServices;
	}

	public Result show(String subAction, Long idReglement, Long idAdherent, Request request) {

//		if (!isAdmin()) {
//			return redirect(routes.AuthenticationCtrl.logout());
//		}

		String viewMode;
		Reglement c;
		List<TypePrestation> tps = regServices.getAllTypePrestation();
		List<VAdherentAyantDroit> vad = regServices.getAdherentAndAyantByAdherent(idAdherent);

		if (0 == idReglement) {
			c = new Reglement();
			viewMode = ViewMode.VIEW_MODE_CREATE;
		} else if (ViewMode.VIEW_MODE_EDIT.equals(subAction)) {
			c = regServices.findById(idReglement);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (ViewMode.VIEW_MODE_TRAITE.equals(subAction)) {
			c = regServices.findById(idReglement);
			viewMode = ViewMode.VIEW_MODE_TRAITE;
		} else if (ViewMode.VIEW_MODE_DELETE.equals(subAction)) {
			c = regServices.findById(idReglement);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			viewMode = ViewMode.VIEW_MODE_VIEW;
			c = regServices.findById(idReglement);

		}
		return ok(views.html.rembourssement.render(viewMode,
				regServices.findReglementByAdherent(idAdherent, request.session().get("gestion").get()), c,
				adherentService.findById(idAdherent), structureService.findAll(),
				regServices.sommeRegler(idAdherent, request.session().get("gestion").get()),Long.valueOf(request.session().get("plafond").get()), tps, vad, request));

	}

	public Result showRegementGlobal(Request request) {
		return ok(views.html.rembourssementGlobal.render(regServices.getAllRegementByAdherent(), request));
	}

	public Result restaure(Long idPart, Request request) {
		Reglement c = regServices.findById(idPart);
		c.setOnDeleted(false);
		regServices.update(c);
		return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
	}

	public Result reglementDetailForm(String action, Long idReglement, Long idAdherent, Long idDetails,
			Request request) {
		String viewMode = ViewMode.VIEW_MODE_CREATE;
		ReglementDetail detail = null;
		if (action.equals(ViewMode.VIEW_MODE_CREATE)) {
			viewMode = ViewMode.VIEW_MODE_CREATE;
			detail = new ReglementDetail();
		} else if (action.equals(ViewMode.VIEW_MODE_EDIT)) {
			detail = reDetailMainServices.findById(idDetails);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (action.equals(ViewMode.VIEW_MODE_DELETE)) {
			detail = reDetailMainServices.findById(idDetails);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		}

		Adherent ad = adherentService.getById(idAdherent);
		List<ReglementDetail> rd = reDetailMainServices.getByReglement(idReglement);

		return ok(views.html.remboursementDetail.render(viewMode, idReglement, detail, ad, rd,
				regServices.sommeRegler(idAdherent, request.session().get("gestion").get()),Long.valueOf(request.session().get("plafond").get()), request));
	}

	public Result reglementDetailsave(Request request) {

		Long sommeReg = 0L;
		Long total = 0L;
		Long montanTotal = 0L;
		Long diff = 0L;
		//Long alerte = 350000L;
		Form<ReglementDetail> uForm = formFactory.form(ReglementDetail.class).bindFromRequest(request);
		Long ad = Long.parseLong(formFactory.form().bindFromRequest(request).get("adh"));
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		ReglementDetail rd = uForm.get();
		rd.setWhenDone(new Timestamp(System.currentTimeMillis()));
		rd.setWhoDone(String.valueOf(request.session().get("login").get()));
		rd.setOnDeleted(false);
		montanTotal = rd.getPrixUnitaire()*rd.getQuantite();
		rd.setMontant(montanTotal);

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE) || viewMode.equals(ViewMode.VIEW_MODE_EDIT))
//			sommeReg = regServices.sommeRegler(regServices.findById(rd.getReglement()).getAdherent()) + (new Double(rd.getMontant()
//					* (Double.valueOf(typePresta.findById(regServices.findById(rd.getReglement()).getTypePrestation())
//							.getCouverture()) / 100)).longValue());

			sommeReg = regServices.sommeRegler(regServices.findById(rd.getReglement()).getAdherent(),
					request.session().get("gestion").get());

		System.out.println("la somme total des rembourssement est de : " + sommeReg + " F CFLA");
		// :"+ rd.getIntitule());

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			if (sommeReg > Long.valueOf(request.session().get("plafond").get())) {
				
				if (reDetailMainServices.saveLogical(rd, true).equals("ok")) {
					total = regServices.sommeRegler(regServices.findById(rd.getReglement()).getAdherent(),
							request.session().get("gestion").get());
				//la partie envoi des mail est commenté car les email des agents ne sont pas renseigner
					//if(!adherentService.getById(ad).getEmail().isEmpty()) {
						//envoi d'E-mail d'Alerte de consomation
					//	envoiEmail(adherentService.getVAdherentReglGlobal(adherentService.getById(ad).getEmail(), request.session().get("gestion").get() ), request);
						
					//}
					
					return redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(),
							ad, 0L)).flashing("error",
									" Reglement pris en charge!!!, Dépassement de plafond !!!!, le montant que vous voudriez ajouter à la comsomation actuel vous donne un total est de :"
											+ total + " > " + Long.valueOf(request.session().get("plafond").get()));
				} else {
					return redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE,
							rd.getReglement(), ad, rd.getId())).flashing("error", " Reglement détail non ajouté");
				}
				
			} else {
				if (reDetailMainServices.saveLogical(rd, true).equals("ok")) {
					//si la sommes des consomation a atteint 350000F on envoi une alerte
					//on calcul le total des dépenses (existant + celui qu'on vient de saisir)r
					System.out.println("Le seuil est de :"+ Long.valueOf(request.session().get("seuilAlerte").get()));
					total = sommeReg + rd.getMontant();
					if(total >= Long.valueOf(request.session().get("seuilAlerte").get()))
						envoiEmail(adherentService.getVAdherentReglGlobal(adherentService.getById(ad).getEmail(), request.session().get("gestion").get() ), request);
					
					return redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE,
							rd.getReglement(), ad, rd.getId())).flashing("success", " Reglement détail ajouté");
				} else {
					return redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE,
							rd.getReglement(), ad, rd.getId())).flashing("error", " Reglement détail non ajouté");
				}
			}
		}
		if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			rd.setLastUpdate(new Timestamp(System.currentTimeMillis()));
			if (sommeReg > Long.valueOf(request.session().get("plafond").get())) {
				System.out.println("ad :" + ad + " ,rd :" + rd.getId());
				return redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(),
						ad, 0L)).flashing("error",
								" Reglement Non pris en charge!!!, Dépassement de plafond !!!!, le montant que vous voudriez ajouter +  la comsomation actuel est de :"
										+ sommeReg + " > " + Long.valueOf(request.session().get("plafond").get()));
			} else {
				if (reDetailMainServices.saveLogical(rd, false).equals("ok")) {
					redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(), ad,
							rd.getId())).flashing("success", " Detail Reglement modifié avec succès !!! ");
				} else {
					redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(), ad,
							rd.getId())).flashing("error", " Detail Reglement non modifié !!! ");
				}
			}
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			rd.setOnDeleted(true);
			rd.setMontant(0L);
			rd.setLastUpdate(new Timestamp(System.currentTimeMillis()));
			if (reDetailMainServices.saveLogical(rd, false).equals("ok")) {
				redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(), ad,
						rd.getId())).flashing("success", " Détail Reglement supprimer!!! ");
			} else {
				redirect(routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(), ad,
						rd.getId())).flashing("error", " Detail Reglement non supprimé !!! ");
			}
		}

		return redirect(
				routes.ReglementCtrl.reglementDetailForm(ViewMode.VIEW_MODE_CREATE, rd.getReglement(), ad, rd.getId()));
	}

	public Result showAlerteView(Request request) {
		return ok(views.html.alerteMail.render(0, request));
	}
	
	public Result infoViaMail(Request request) {
		Email mail = new Email();
		//Long alerte = 350000L;
		Long diff = 0L;
		int nombre = 0;
		String dest, subject, message;
		//subject = "MAINS: Alerte consomation";
		List<VReglementGlobalByAdherent> liste = regServices.getAllRegementByAdherent(request.session().get("gestion").get());
		
		
		for(VReglementGlobalByAdherent element : liste) {
			
			if ( element.getTotalAnnuel() >= Long.valueOf(request.session().get("seuilAlerte").get())  && !element.getEmail().isEmpty()) {
				
				if(element.getTotalAnnuel() <= Long.valueOf(request.session().get("plafond").get())) {
				subject = "MAINS: Alerte consomation";
				diff = Long.valueOf(request.session().get("plafond").get()) -   element.getTotalAnnuel();
				
				message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que votre consomation au titre de l'année "+request.session().get("gestion").get()
				+" est de "+element.getTotalAnnuel()+" F CFA à ce jour .\r\r"
						+ "Il vous reste un total de "+ diff + "F CFA \r\r Cordialement \r\r Signé \r Le Président";
				
				//envoi du messqge
				email.sendMail(element.getEmail(), subject, message);
				
				//capture des infos et insertion dans la DB
				mail.setDestinateur(element.getEmail());
				mail.setSubject(subject);
				mail.setMessage(message);
				mail.setWhoDone(request.session().get("login").get());
				mail.setWhenDone(new Timestamp(System.currentTimeMillis()));
				
				emailServices.saveLogical(mail, true);
				
				mail = new Email();
				
				nombre = nombre + 1 ;
				}
				
				
				if(element.getTotalAnnuel() > Long.valueOf(request.session().get("plafond").get())) {
					diff =element.getTotalAnnuel() - Long.valueOf(request.session().get("plafond").get()) ;
					subject = "MAINS: Dépassement de plafond";
					
					message =  "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que vous avez atteint le plafond ( de "+Long.valueOf(request.session().get("plafond").get()) +" F CFA ) des coûts des prestations auquel vous avez droit.\r"
							+ " votre consomation actuelle pour l'année "+ request.session().get("gestion").get() +" est de :"+ element.getTotalAnnuel() +" F CFA dont un dépassement de :" + diff +" F CFA \r\r"
							+ "Cordialement \r\r Signé \r Le Président";
					
					//envoi du messqge
					email.sendMail(element.getEmail(), subject, message);
					
					//capture des infos et insertion dans la DB
					mail.setDestinateur(element.getEmail());
					mail.setSubject(subject);
					mail.setMessage(message);
					mail.setWhoDone(request.session().get("login").get());
					mail.setWhenDone(new Timestamp(System.currentTimeMillis()));
					
					emailServices.saveLogical(mail, true);
					
					mail = new Email();
					
					nombre = nombre + 1 ;
					}
			
			}
		}
		
		//return ok(views.html.rembourssement.render(request, nombre));
		return ok(views.html.alerteMail.render(nombre, request))
				.flashing("success", " E-mail envoyé "+ nombre +" avec success");
		
		}

		public void envoiEmail(VReglementGlobalByAdherent element, Request request) {
			Email mail = new Email();
			//Long alerte = 350000L;
			Long diff = 0L;
			int nombre = 0;
			String dest, subject, message;
			
			System.out.println("Evoi void mail");
			
			if(element.getTotalAnnuel() <= Long.valueOf(request.session().get("plafond").get())) {
				System.out.println("Evoi void mail 350000");
				subject = "MAINS: Alerte consomation";
				diff = Long.valueOf(request.session().get("plafond").get()) -   element.getTotalAnnuel();
				
				message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que votre consomation au titre de l'année "+request.session().get("gestion").get()
				+" est de "+element.getTotalAnnuel()+" F CFA à ce jour .\r\r"
						+ "Il vous reste un total de "+ diff + "F CFA \r\r Cordialement \r\r Signé \r Le Président";
				
				//envoi du messqge
				email.sendMail(element.getEmail(), subject, message);
				
				//capture des infos et insertion dans la DB
				mail.setDestinateur(element.getEmail());
				mail.setSubject(subject);
				mail.setMessage(message);
				mail.setWhoDone(request.session().get("login").get());
				mail.setWhenDone(new Timestamp(System.currentTimeMillis()));
				
				emailServices.saveLogical(mail, true);
				
				mail = new Email();
				
				nombre = nombre + 1 ;
				}
				
				
				if(element.getTotalAnnuel() > Long.valueOf(request.session().get("plafond").get())) {
					System.out.println("Evoi void mail 500000");
					diff =element.getTotalAnnuel() - Long.valueOf(request.session().get("plafond").get()) ;
					subject = "MAINS: Dépassement de plafond";
					
					message =  "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que vous avez atteint le plafond ( de "+Long.valueOf(request.session().get("plafond").get()) +" F CFA ) des coûts des prestations auquel vous avez droit.\r"
							+ " votre consomation actuelle pour l'année "+ request.session().get("gestion").get() +" est de :"+ element.getTotalAnnuel() +" F CFA dont un dépassement de :" + diff +" F CFA \r\r"
							+ "Cordialement \r\r Signé \r Le Président";
					
					//envoi du messqge
					email.sendMail(element.getEmail(), subject, message);
					
					//capture des infos et insertion dans la DB
					mail.setDestinateur(element.getEmail());
					mail.setSubject(subject);
					mail.setMessage(message);
					mail.setWhoDone(request.session().get("login").get());
					mail.setWhenDone(new Timestamp(System.currentTimeMillis()));
					
					emailServices.saveLogical(mail, true);
					
					mail = new Email();
					
					nombre = nombre + 1 ;
					}
			
			}
		
	
	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<Reglement> uForm = formFactory.form(Reglement.class).bindFromRequest(request);
		String dateReglement = formFactory.form().bindFromRequest(request).get("tmpDate");
		System.out.println("Date reglement :" + dateReglement + " #################");
		Long benef = Long.parseLong(formFactory.form().bindFromRequest(request).get("benef"));

		Long sommeReg = 0L;
		Reglement c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setOnDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));

		// renseigner la date de paiement en fonction du reglement
//		System.out.println(
//				"LA gestion en fonction de la date paiement saisie : " + String.valueOf(dateReglement.substring(0, 4)));
		// verifier si la date de paiement est differente de la gestion saisie par
		// l'utilisateur
		if (!String.valueOf(dateReglement.substring(0, 4))
				.equals(String.valueOf(request.session().get("gestion").get())))
			c.setDatePayement(regServices.getDateT(request.session().get("gestion").get() + "-12-31"));
		else
			c.setDatePayement(regServices.getDateT(dateReglement));

		// sommeReg = regServices.sommeRegler(c.getAdherent()) + c.getMontant();
		System.out.println("total regler :" + sommeReg + " F CFA");
		if (!benef.equals(c.getAdherent())) {
			c.setAyantDroit(benef);
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {

			if (regServices.saveLogical(c, true).equals("ok")) {
				String msg = "";
				if (sommeReg >= Long.valueOf(request.session().get("plafond").get())) {
					msg = "error";
					return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent())).flashing(
							msg,
							" Reglement de la facture " + c.getRefFacture() + " avec depassement du plafond prévu!!!");
				} else {
					msg = "success";
					return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
							.flashing(msg, " Reglement de la facture " + c.getRefFacture() + " ajouter avec success");
				}

			} else {
				System.out.println("msg :" + regServices.saveLogical(c, true));
				// flash("error", " AyantDroit " + c.getLibelle() + " non ajouter ");
				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", " Reglement non ajouter ");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			// a.setLogin(login);
			if (regServices.saveLogical(c, false).equals("ok")) {
				// flash("success", " AyantDroit modifier avec success");
				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " Reglement  modifier avec success");
			} else {
				// flash("error", "AyantDroits non modifier");
				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", "Reglement  non modifier");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_TRAITE)) {

			if (regServices.saveLogical(c, false).equals("ok")) {

				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " Reglement  traiter avec success");
			} else {
				// flash("error", "Echec lors du traitement de l'AyantDroit");
				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", "Echec lors du traitement du Reglement");
			}
		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			c.setOnDeleted(true);
			if (regServices.saveLogical(c, false).equals("ok")) {
				// flash("success", " AyantDroit Supprimer avec success");
				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("success", " Reglement  Supprimer avec success");
			} else {

				return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
						.flashing("error", " Reglement  non supprimer");
			}
		}
		return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
	}

	public Result rapportAnnuel(Request request, String fileName) {

		String annee = formFactory.form().bindFromRequest(request).get("annee");

		// String fileName = "situation_global_annuel";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";
		try {
			// flash("success", "impression ok");

			jasper.generateReport(fileName, annee.replace(" ", ""), Long.valueOf(request.session().get("plafond").get()));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + annee + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return ok(views.html.rembourssementGlobal.render(regServices.getAllRegementByAdherent(), request))
					.flashing("error", " Erreur d'impression");
		}
	}
	
	

	public Result rapportBetwenne(Request request) {
		String dateD1 = formFactory.form().bindFromRequest(request).get("tmpDate");
		String dateD2 = formFactory.form().bindFromRequest(request).get("tmpDate2");

		String fileName = "situation_global_between";
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

public Result detailJson(Long id) {
    ReglementDetail d = reDetailMainServices.findById(id);
    if (d == null) {
        return notFound("Détail introuvable");
    }
    ObjectNode json = Json.newObject();
    json.put("id", d.getId());
    json.put("intitule", d.getIntitule());
    json.put("montant", d.getMontant());
    return ok(json);
}

public Result getReglementJson(Long id) {
    Reglement r = regServices.findById(id);
    if (r == null) {
        return notFound("Règlement introuvable");
    }

    ObjectNode json = Json.newObject();
    json.put("id", r.getId());
    json.put("adherent", r.getAdherent()); // id de l’adhérent
    if (r.getAyantDroit() != null) {
        json.put("ayantDroit", r.getAyantDroit());
    }
    if (r.getTypePrestation() != null) {
        json.put("typePrestation", r.getTypePrestation());
    }
    if (r.getStructure() != null) {
        json.put("structure", r.getStructure());
    }
    if (r.getDatePayement() != null) {
        json.put("tmpDate", String.valueOf(r.getDatePayement()));
    }
    if (r.getStructureEmettriceRembourssement() != null) {
        json.put("structureEmettriceRembourssement", r.getStructureEmettriceRembourssement());
    }
    if (r.getTelStructureEmettrice() != null) {
        json.put("telStructureEmettrice", r.getTelStructureEmettrice());
    }

    return ok(json);
}


}
