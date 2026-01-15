package controllers;

import java.io.File;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import models.tables.pojos.VReglement;
import models.tables.pojos.Adherent;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.Email;
import models.tables.pojos.OtpMessage;
import models.tables.pojos.Reglement;
import models.tables.pojos.ReglementDetail;
import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VAdherentAyantDroit;
import models.tables.pojos.VReglementGlobalByAdherent;
import play.Logger;
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
import services.MessageServiceImpl;
import services.OtpService;
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

	// private static final Long PLAFOND_REGLEMENT_ANNUELLE = 350000L;
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
	OtpService otpService;
	MessageServiceImpl messagesServices;


	@Inject
	public ReglementCtrl(
			FormFactory formFactory,
			ReglementMainServices regServices,
			ReglementDetailMainServices reDetailMainServices,
			AdherentMainServices adherentService,
			StructureMainServices structureService,
			TypePrestationMainService typePresta,
			CallJasperReport jasper,
			Mail email,
			EmailManagerServices emailServices,
		OtpService otpService, MessageServiceImpl messagesServices) {

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
		this.otpService = otpService;
		this.messagesServices = messagesServices;
	}

	 // 1. Demander un OTP
    public Result requestOtp(Http.Request request) {
        String phone = request.getQueryString("phone"); // ex: 2279XXXXXXX
        otpService.sendOtp(phone);
        return ok("OTP envoyé");
    }

    // 2. Vérifier un OTP
    public Result verifyOtp(Http.Request request) {
        String phone = request.getQueryString("phone");
        String code  = request.getQueryString("code");

        boolean ok = otpService.verifyOtp(phone, code);
        if (ok) {
            // ici tu connectes l’utilisateur / valides l’action
            return ok("OTP valide");
        } else {
            return badRequest("OTP invalide ou expiré");
        }
    }

	public Result show(String subAction, String typeOP, Long idReglement, Long idAdherent, Request request) {

		String viewMode;
		Reglement c;
		List<StructurePartenaire> listesPartenaires = new ArrayList<>();
		List<TypePrestation> listeTypePrestation = new ArrayList<>();
		List<VReglement> listeReg = new ArrayList<>();

		if (typeOP.equals("BC")) {
			listesPartenaires = structureService.findAllPharmacieByRegion(Long.valueOf(request.session().get("region").get()));
			listeTypePrestation = typePresta.findOrdonnance();
			listeReg = regServices.findReglementBCByAdherent(idAdherent, request.session().get("gestion").get());
		} else {
			listesPartenaires = structureService.findAllHopitauxByRegion(Long.valueOf(request.session().get("region").get()));
			listeTypePrestation = typePresta.findOthers();
				listeReg = regServices.findReglementPCByAdherent(idAdherent, request.session().get("gestion").get());
		}

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

		return ok(views.html.rembourssement.render(
				viewMode,
				typeOP,
				listeReg,
				c,
				adherentService.getVAdherentById(idAdherent),
				listesPartenaires,
				regServices.sommeRegler(idAdherent, request.session().get("gestion").get()),
				regServices.sommeReglerBC(idAdherent, request.session().get("gestion").get()),
				regServices.sommeReglerPC(idAdherent, request.session().get("gestion").get()),
				Long.valueOf(request.session().get("plafond").get()),
				listeTypePrestation,
				vad,
				request));
	}

	//@AddCSRFToken  // ✅ Ajouter cette annotation
	public Result showBonToValidate(Request request){
		//return ok(views.html.validationBon.render(regServices.findBonToValidate(),request));
		return ok(views.html.validationBon.render(regServices.findAllVReg(),request));
	}

	public Result showRapSynthese(Request request){
		//return ok(views.html.validationBon.render(regServices.findBonToValidate(),request));
		return ok(views.html.rapSynthese.render(regServices.findAllVReg(),request));
	}

	public Result showRapSuperviseur(Request request){
		//return ok(views.html.validationBon.render(regServices.findBonToValidate(),request));
		return ok(views.html.rapSuperviseur.render(regServices.findAllVReg(),request));
	}

	/**
	 * Affiche la liste des bons de commande non confirmés pour confirmation
	 * (Bouton VERT) Cette méthode affiche les bons avec is_confirmed_bon = false ou
	 * null
	 * 
	 * @param subAction Le mode d'action
	 * @param idAdherent L'ID de l'adhérent
	 * @param request La requête HTTP
	 * @return La vue avec la liste des bons non confirmés
	 */
	public Result showConfirmBon(String subAction, Long idAdherent, Request request) {
		System.out.println(">>> showConfirmBon - Adhérent ID: " + idAdherent);

		// Récupérer l'adhérent
		Adherent adherent = adherentService.findById(idAdherent);

		// Récupérer les bons NON confirmés pour cet adhérent
		List<models.tables.pojos.VReglement> bonsNonConfirmes = regServices.findReglementNonConfirmesByAdherent(
				idAdherent,
				request.session().get("gestion").get());

		System.out.println(">>> Nombre de bons non confirmés trouvés: " + bonsNonConfirmes.size());

		// Afficher la vue de confirmation
		return ok(views.html.confirmationBon.render(
				bonsNonConfirmes,
				adherent,
				structureService.findAll(),
				request));
	}

	public Result showRegementGlobal(Request request) {
		return ok(views.html.rembourssementGlobal.render(
				regServices.getAllRegementByAdherent(),
				request));
	}

	public Result restaure(Long idPart, Request request) {
		Reglement c = regServices.findById(idPart);
		c.setOnDeleted(false);
		regServices.update(c);
		return redirect(routes.ReglementCtrl.show(
				ViewMode.VIEW_MODE_CREATE,
				"",
				0L,
				c.getAdherent()));
	}

	public Result reglementDetailForm(
			String action,
			String typeOP,
			Long idReglement,
			Long idAdherent,
			Long idDetails,
			Request request) {

		String viewMode = ViewMode.VIEW_MODE_CREATE;
		ReglementDetail detail;

		if (action.equals(ViewMode.VIEW_MODE_CREATE)) {
			viewMode = ViewMode.VIEW_MODE_CREATE;
			detail = new ReglementDetail();
		} else if (action.equals(ViewMode.VIEW_MODE_EDIT)) {
			detail = reDetailMainServices.findById(idDetails);
			viewMode = ViewMode.VIEW_MODE_EDIT;
		} else if (action.equals(ViewMode.VIEW_MODE_DELETE)) {
			detail = reDetailMainServices.findById(idDetails);
			viewMode = ViewMode.VIEW_MODE_DELETE;
		} else {
			// fallback
			detail = new ReglementDetail();
		}

		Adherent ad = adherentService.getById(idAdherent);
		List<ReglementDetail> rd = reDetailMainServices.getByReglement(idReglement);

		return ok(views.html.remboursementDetail.render(
				viewMode,
				adherentService.getVAdherentById(idAdherent),
				typeOP,
				idReglement,
				detail,
				rd,
				regServices.sommeRegler(idAdherent, request.session().get("gestion").get()),
				Long.valueOf(request.session().get("plafond").get()),
				request));
	}

	public Result reglementDetailsave(Request request) {

		Long sommeReg = 0L;
		Long total = 0L;
		Long montanTotal = 0L;
		Long diff = 0L;

		Long ad = Long.parseLong(formFactory.form().bindFromRequest(request).get("adh"));
		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
		String typeOperation = formFactory.form().bindFromRequest(request).get("typeOP");
		Long idReglement = Long.parseLong(formFactory.form().bindFromRequest(request).get("reglement"));

		// Vérifier si c'est une saisie multiple (mode CREATE avec plusieurs lignes)
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			// Récupérer les données du formulaire multiple
			java.util.Map<String, String[]> formData = request.body().asFormUrlEncoded();
			
			// Vérifier s'il y a des données de saisie multiple
			boolean isSaisieMultiple = formData.containsKey("intitules[1]");
			
			if (isSaisieMultiple) {
				// Mode saisie multiple
				int nombreLignesSaisies = 0;
				int nombreLignesEnregistrees = 0;
				int nombreLignesRejetees = 0;
				List<String> messagesErreurs = new ArrayList<>();
				
				for (int i = 1; i <= 10; i++) {
					String intituleKey = "intitules[" + i + "]";
					String quantiteKey = "quantites[" + i + "]";
					String prixKey = "prixUnitaires[" + i + "]";
					
					String[] intituleArray = formData.get(intituleKey);
					String[] quantiteArray = formData.get(quantiteKey);
					String[] prixArray = formData.get(prixKey);
					
					// Vérifier si la ligne a un intitulé (critère de validation)
					if (intituleArray != null && intituleArray.length > 0 && !intituleArray[0].trim().isEmpty()) {
						nombreLignesSaisies++;
						
						try {
							String intitule = intituleArray[0].trim();
							Long quantite = (quantiteArray != null && quantiteArray.length > 0) ? 
								Long.parseLong(quantiteArray[0]) : 1L;
							Long prixUnitaire = (prixArray != null && prixArray.length > 0) ? 
								Long.parseLong(prixArray[0]) : 0L;
							
							// Créer une nouvelle ligne de détail
							ReglementDetail rd = new ReglementDetail();
							rd.setReglement(idReglement);
							rd.setIntitule(intitule);
							rd.setQuantite(quantite);
							rd.setPrixUnitaire(prixUnitaire);
							rd.setMontant(quantite * prixUnitaire);
							rd.setWhenDone(new Timestamp(System.currentTimeMillis()));
							rd.setWhoDone(String.valueOf(request.session().get("login").get()));
							rd.setOnDeleted(false);
							
							// Vérifier les plafonds
							Double totalCreditAnnuelle = adherentService.getVAdherentById(
								regServices.findById(idReglement).getAdherent()).getTotalCreditAnnuelle();
							Double totalCreditAConsomer = adherentService.getVAdherentById(
								regServices.findById(idReglement).getAdherent()).getTotalCreditAConsomer();
							Long totalConsommation = regServices.sommeRegler(
								regServices.findById(idReglement).getAdherent(),
								request.session().get("gestion").get());
							Long totalAVailider = totalConsommation + rd.getMontant();
							
							// Vérifier si le plafond est dépassé
							if (totalAVailider >= totalCreditAConsomer) {
								messagesErreurs.add("Ligne " + i + " (" + intitule + "): Plafond dépassé");
								nombreLignesRejetees++;
								continue;
							}
							
							// Enregistrer la ligne
							if (reDetailMainServices.saveLogical(rd, true).equals("ok")) {
								nombreLignesEnregistrees++;
								
								// Vérifier si le seuil d'alerte est atteint
								if (totalAVailider >= totalCreditAnnuelle) {
									Long diff2 = totalAVailider - totalCreditAnnuelle.longValue();
									otpService.sendAlerteSeuil(
										regServices.findVRegById(idReglement).getTelephone(), 
										diff2);
								}
							} else {
								messagesErreurs.add("Ligne " + i + " (" + intitule + "): Erreur d'enregistrement");
								nombreLignesRejetees++;
							}
							
						} catch (Exception e) {
							messagesErreurs.add("Ligne " + i + ": Erreur - " + e.getMessage());
							nombreLignesRejetees++;
							e.printStackTrace();
						}
					}
				}
				
				// Message de retour
				String message;
				String flashType;
				
				if (nombreLignesSaisies == 0) {
					message = "Aucune ligne n'a été saisie";
					flashType = "warning";
				} else if (nombreLignesEnregistrees == nombreLignesSaisies) {
					message = nombreLignesEnregistrees + " ligne(s) enregistrée(s) avec succès";
					flashType = "success";
				} else if (nombreLignesEnregistrees > 0) {
					message = nombreLignesEnregistrees + " ligne(s) enregistrée(s), " + 
						nombreLignesRejetees + " ligne(s) rejetée(s)";
					if (!messagesErreurs.isEmpty()) {
						message += ". Détails: " + String.join(", ", messagesErreurs);
					}
					flashType = "warning";
				} else {
					message = "Aucune ligne enregistrée. " + String.join(", ", messagesErreurs);
					flashType = "error";
				}
				
				return redirect(routes.ReglementCtrl.reglementDetailForm(
					ViewMode.VIEW_MODE_CREATE,
					typeOperation,
					idReglement,
					ad,
					0L)).flashing(flashType, message);
			}
		}

		// Mode EDIT/DELETE ou saisie simple : traitement classique d'une seule ligne
		Form<ReglementDetail> uForm = formFactory.form(ReglementDetail.class).bindFromRequest(request);
		ReglementDetail rd = uForm.get();
		rd.setWhenDone(new Timestamp(System.currentTimeMillis()));
		rd.setWhoDone(String.valueOf(request.session().get("login").get()));
		rd.setOnDeleted(false);

		montanTotal = rd.getPrixUnitaire() * rd.getQuantite();
		rd.setMontant(montanTotal);
		System.out.println("envoi SMS............." ); 

		System.out.println("la somme total des rembourssement est de : " + sommeReg + " F CFLA");
		Double totalCreditAnnuelle = adherentService.getVAdherentById(regServices.findById(rd.getReglement()).getAdherent()).getTotalCreditAnnuelle();
		Double totalCreditAConsomer =  adherentService.getVAdherentById(regServices.findById(rd.getReglement()).getAdherent()).getTotalCreditAConsomer();
		Long totalConsommation = regServices.sommeRegler(regServices.findById(rd.getReglement()).getAdherent(),request.session().get("gestion").get());
		Long totalAVailider = totalConsommation + montanTotal;
		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {

			if (totalAVailider >= totalCreditAConsomer) {
				otpService.sendSuspension(regServices.findVRegById(rd.getReglement()).getTelephone());
					System.out.println("Seuil Plafond des dépenses");
						return redirect(routes.ReglementCtrl.reglementDetailForm(
								ViewMode.VIEW_MODE_CREATE,
								typeOperation,
								rd.getReglement(),
								ad,
								0L)).flashing(
										"error",
										" Bon non pris en charge !!!,Cette nouvelle Ligne ajouter a vos commosmation précedente donne :"+totalAVailider+
										" > "+totalCreditAConsomer+" Vous avez atteint le plafond des consomations annuel qui vous sont autorisé!!!!");
					

			}  else {

				// seuil des alerte atteint( avertissement)
			if (totalAVailider >= totalCreditAnnuelle) {
			Long diff2 = totalAVailider - totalCreditAnnuelle.longValue();
			otpService.sendAlerteSeuil(regServices.findVRegById(rd.getReglement()).getTelephone(), diff2);

				System.out.println("Seuil Allerte");
				total = regServices.sommeRegler(
						regServices.findById(rd.getReglement()).getAdherent(),
						request.session().get("gestion").get());

	        		if (reDetailMainServices.saveLogical(rd, true).equals("ok")) {
					System.out.println("Seuil Allerte : Enregistrement");
						return redirect(routes.ReglementCtrl.reglementDetailForm(
								ViewMode.VIEW_MODE_CREATE,
								typeOperation,
								rd.getReglement(),
								ad,
								0L)).flashing(
										"error",
										" Reglement pris en charge!!!,Mais Seuil d'avertissement!!!!, le montant que vous voudriez ajouter à la comsomation actuel vous donne un total est de :"
												+ totalConsommation + " > "
												+ adherentService.getVAdherentById(regServices.findById(rd.getReglement()).getAdherent()).getTotalCreditAnnuelle()
												+ " superieur au SEUIL d'alerte");
					} else {
						return redirect(routes.ReglementCtrl.reglementDetailForm(
								ViewMode.VIEW_MODE_CREATE,
								typeOperation,
								rd.getReglement(),
								ad,
								rd.getId())).flashing(
										"error",
										" Reglement détail non ajouté");
					}
				

			}

					if (reDetailMainServices.saveLogical(rd, true).equals("ok")) {
						System.out.println("Cycle Normale");
								return redirect(routes.ReglementCtrl.reglementDetailForm(
								ViewMode.VIEW_MODE_CREATE,
								typeOperation,
								rd.getReglement(),
								ad,
								rd.getId())).flashing("success"," Ligne ajouté avec succès");
					} else {
						return redirect(routes.ReglementCtrl.reglementDetailForm(
								ViewMode.VIEW_MODE_CREATE,
								typeOperation,
								rd.getReglement(),
								ad,
								rd.getId())).flashing("error"," Reglement détail non ajouté");
					}
				}
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
			rd.setLastUpdate(new Timestamp(System.currentTimeMillis()));
			if (totalAVailider >= totalCreditAConsomer) {
				System.out.println("ad :" + ad + " ,rd :" + rd.getId());
				return redirect(routes.ReglementCtrl.reglementDetailForm(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						rd.getReglement(),
						ad,
						0L)).flashing("error"," Reglement Non pris en charge!!!, Dépassement de plafond !!!!, le montant que vous voudriez ajouter +  la comsomation actuel est de :"
										+ totalAVailider + " > "+ totalCreditAConsomer);
			} else {
				if (reDetailMainServices.saveLogical(rd, false).equals("ok")) {
					return redirect(routes.ReglementCtrl.reglementDetailForm(
							ViewMode.VIEW_MODE_CREATE,
							typeOperation,
							rd.getReglement(),
							ad,
							rd.getId())).flashing("success"," Detail Reglement modifié avec succès !!! ");
				} else {
					return redirect(routes.ReglementCtrl.reglementDetailForm(
							ViewMode.VIEW_MODE_CREATE,
							typeOperation,
							rd.getReglement(),
							ad,
							rd.getId())).flashing("error"," Detail Reglement non modifié !!! ");
				}
			}
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
			rd.setOnDeleted(true);
			rd.setMontant(0L);
			rd.setLastUpdate(new Timestamp(System.currentTimeMillis()));
			if (reDetailMainServices.saveLogical(rd, false).equals("ok")) {
				return redirect(routes.ReglementCtrl.reglementDetailForm(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						rd.getReglement(),
						ad,
						rd.getId())).flashing(
								"success",
								" Détail Reglement supprimer!!! ");
			} else {
				return redirect(routes.ReglementCtrl.reglementDetailForm(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						rd.getReglement(),
						ad,
						rd.getId())).flashing(
								"error",
								" Detail Reglement non supprimé !!! ");
			}
		}

		return redirect(routes.ReglementCtrl.reglementDetailForm(
				ViewMode.VIEW_MODE_CREATE,
				typeOperation,
				rd.getReglement(),
				ad,
				rd.getId()));
	}

	public Result showAlerteView(Request request) {
		return ok(views.html.alerteMail.render(0, request));
	}

	public Result infoViaMail(Request request) {
		Email mail = new Email();
		Long diff = 0L;
		int nombre = 0;
		String dest, subject, message;

		List<VReglementGlobalByAdherent> liste = regServices
				.getAllRegementByAdherent(request.session().get("gestion").get());

		for (VReglementGlobalByAdherent element : liste) {

			if (element.getTotalAnnuel() >= Long.valueOf(request.session().get("seuilAlerte").get())
					&& !element.getEmail().isEmpty()) {

				if (element.getTotalAnnuel() <= Long.valueOf(request.session().get("plafond").get())) {
					subject = "MAINS: Alerte consomation";
					diff = Long.valueOf(request.session().get("plafond").get()) - element.getTotalAnnuel();

					message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que votre consomation au titre de l'année "
							+ request.session().get("gestion").get()
							+ " est de " + element.getTotalAnnuel() + " F CFA à ce jour .\r\r"
							+ "Il vous reste un total de " + diff
							+ "F CFA \r\r Cordialement \r\r Signé \r Le Président";

					email.sendMail(element.getEmail(), subject, message);

					mail.setDestinateur(element.getEmail());
					mail.setSubject(subject);
					mail.setMessage(message);
					mail.setWhoDone(request.session().get("login").get());
					mail.setWhenDone(new Timestamp(System.currentTimeMillis()));

					emailServices.saveLogical(mail, true);

					mail = new Email();

					nombre = nombre + 1;
				}

				if (element.getTotalAnnuel() > Long.valueOf(request.session().get("plafond").get())) {
					diff = element.getTotalAnnuel() - Long.valueOf(request.session().get("plafond").get());
					subject = "MAINS: Dépassement de plafond";

					message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que vous avez atteint le plafond ( de "
							+ Long.valueOf(request.session().get("plafond").get())
							+ " F CFA ) des coûts des prestations auquel vous avez droit.\r"
							+ " votre consomation actuelle pour l'année "
							+ request.session().get("gestion").get()
							+ " est de :" + element.getTotalAnnuel()
							+ " F CFA dont un dépassement de :" + diff + " F CFA \r\r"
							+ "Cordialement \r\r Signé \r Le Président";

					email.sendMail(element.getEmail(), subject, message);

					mail.setDestinateur(element.getEmail());
					mail.setSubject(subject);
					mail.setMessage(message);
					mail.setWhoDone(request.session().get("login").get());
					mail.setWhenDone(new Timestamp(System.currentTimeMillis()));

					emailServices.saveLogical(mail, true);

					mail = new Email();

					nombre = nombre + 1;
				}
			}
		}

		return ok(views.html.alerteMail.render(nombre, request))
				.flashing("success", " E-mail envoyé " + nombre + " avec success");
	}

	public void envoiEmail(VReglementGlobalByAdherent element, Request request) {
		Email mail = new Email();
		Long diff = 0L;
		int nombre = 0;
		String dest, subject, message;

		System.out.println("Evoi void mail PLAFOND ");
		System.out.println("plafond :" + request.session().get("plafond").get());

		if (element.getTotalAnnuel() <= Long.valueOf(request.session().get("plafond").get())) {
			System.out.println("Evoi void mail 350000");
			subject = "MUSAP: Alerte consomation";
			diff = Long.valueOf(request.session().get("plafond").get()) - element.getTotalAnnuel();

			message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que votre consomation au titre de l'année "
					+ request.session().get("gestion").get()
					+ " est de " + element.getTotalAnnuel() + " F CFA à ce jour .\r\r"
					+ "Il vous reste un total de " + diff
					+ "F CFA \r\r Cordialement \r\r Signé \r Le Président";

			email.sendMail(element.getEmail(), subject, message);

			mail.setDestinateur(element.getEmail());
			mail.setSubject(subject);
			mail.setMessage(message);
			mail.setWhoDone(request.session().get("login").get());
			mail.setWhenDone(new Timestamp(System.currentTimeMillis()));

			emailServices.saveLogical(mail, true);

			mail = new Email();

			nombre = nombre + 1;
		}

		if (element.getTotalAnnuel() > Long.valueOf(request.session().get("plafond").get())) {
			System.out.println("Evoi void mail 500000");
			diff = element.getTotalAnnuel() - Long.valueOf(request.session().get("plafond").get());
			subject = "MAINS: Dépassement de plafond";

			message = "Chèr(e) adhérent(e),\r\r La MAINS vous notifie que vous avez atteint le plafond ( de "
					+ Long.valueOf(request.session().get("plafond").get())
					+ " F CFA ) des coûts des prestations auquel vous avez droit.\r"
					+ " votre consomation actuelle pour l'année "
					+ request.session().get("gestion").get()
					+ " est de :" + element.getTotalAnnuel()
					+ " F CFA dont un dépassement de :" + diff + " F CFA \r\r"
					+ "Cordialement \r\r Signé \r Le Président";

			email.sendMail(element.getEmail(), subject, message);

			mail.setDestinateur(element.getEmail());
			mail.setSubject(subject);
			mail.setMessage(message);
			mail.setWhoDone(request.session().get("login").get());
			mail.setWhenDone(new Timestamp(System.currentTimeMillis()));

			emailServices.saveLogical(mail, true);

			mail = new Email();

			nombre = nombre + 1;
		}
	}

	public Result save(Request request) {

		final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

		Form<Reglement> uForm = formFactory.form(Reglement.class).bindFromRequest(request);
		String dateReglement = formFactory.form().bindFromRequest(request).get("tmpDate");
		String typeOperation = formFactory.form().bindFromRequest(request).get("typeOP");
		System.out.println("Date reglement :" + dateReglement + " #################");
		Long benef = Long.parseLong(formFactory.form().bindFromRequest(request).get("benef"));

		Long sommeReg = 0L;
		Reglement c = uForm.get();
		c.setWhenDone(new Timestamp(System.currentTimeMillis()));
		c.setOnDeleted(false);
		c.setWhoDone(String.valueOf(request.session().get("login").get()));
		c.setDatePayement(new Timestamp(System.currentTimeMillis()));
		c.setTypeReglement(typeOperation);
		long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
		Timestamp dateExpiration = new Timestamp((new Timestamp(System.currentTimeMillis())).getTime() + thirtyDaysInMillis);
		c.setDateExpiration(dateExpiration);
		c.setIsConfirmedBon(false);
	
	 /** 	if (!String.valueOf(dateReglement.substring(0, 4))
				.equals(String.valueOf(request.session().get("gestion").get()))) {
			c.setDatePayement(regServices.getDateT(request.session().get("gestion").get() + "-12-31"));
		} else {
		//	c.setDatePayement(regServices.getDateT(dateReglement));
		} */

		System.out.println("total regler :" + sommeReg + " F CFA");

		Double totalCreditAnnuelle = adherentService.getVAdherentById(c.getAdherent()).getTotalCreditAnnuelle();
		Double totalCreditAConsomer =  adherentService.getVAdherentById(c.getAdherent()).getTotalCreditAConsomer();
		Long totalConsommation = regServices.sommeRegler(c.getAdherent(),request.session().get("gestion").get());
		

		if (!benef.equals(c.getAdherent())) {
			c.setAyantDroit(benef);
		}

		if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
			//en cas de seuil atteint

			if(totalConsommation >= totalCreditAnnuelle ){
			Long diff = totalConsommation - totalCreditAnnuelle.longValue();
			otpService.sendAlerteSeuil(adherentService.findById(c.getAdherent()).getTelephone(), diff);
				if (regServices.saveLogical(c, true).equals("ok")) {
				
					
					return redirect(routes.ReglementCtrl.show(
							ViewMode.VIEW_MODE_CREATE,
							typeOperation,
							0L,
							c.getAdherent())).flashing("warning"," Bon ajouter avec succèss, seuil d'alerte Atteint, penser a moderer votre consommation");
				

			} else {
				System.out.println("msg :" + regServices.saveLogical(c, true));
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"error",
								" Reglement non ajouter ");
			}


			}

			//plafond des dépense atteint, impossible d'ajouter un nouveau Bon
			if(totalConsommation >= totalCreditAConsomer ){
				otpService.sendSuspension(adherentService.findById(c.getAdherent()).getTelephone());
				return redirect(routes.ReglementCtrl.show(
							ViewMode.VIEW_MODE_CREATE,
							typeOperation,
							0L,
							c.getAdherent())).flashing(
									"error",
									" Impossible d'jouter un nouveau bon, vous avez déja atteint le plafond de vos crédits annuelle !!!!");
			}
			if (regServices.saveLogical(c, true).equals("ok")) {
				
					
					return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE,typeOperation,0L,c.getAdherent())).flashing("success"," Opération effectuée avec succès  ");
				

			} else {
				System.out.println("msg :" + regServices.saveLogical(c, true));
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"error",
								" Reglement non ajouter ");
			}

		} else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {

			if (regServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"success",
								" Reglement  modifier avec success");
			} else {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"error",
								"Reglement  non modifier");
			}

		} else if (viewMode.equals(ViewMode.VIEW_MODE_TRAITE)) {

			if (regServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"success",
								" Reglement  traiter avec success");
			} else {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"error",
								"Echec lors du traitement du Reglement");
			}

		} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {

			c.setOnDeleted(true);
			if (regServices.saveLogical(c, false).equals("ok")) {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"success",
								" Reglement  Supprimer avec success");
			} else {
				return redirect(routes.ReglementCtrl.show(
						ViewMode.VIEW_MODE_CREATE,
						typeOperation,
						0L,
						c.getAdherent())).flashing(
								"error",
								" Reglement  non supprimer");
			}
		}

		return redirect(routes.ReglementCtrl.show(
				ViewMode.VIEW_MODE_CREATE,
				typeOperation,
				0L,
				c.getAdherent()));
	}

	public Result rapportAnnuel(Request request, String fileName) {

		String annee = formFactory.form().bindFromRequest(request).get("annee");

		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";

		try {
			jasper.generateReport(
					fileName,
					annee.replace(" ", ""),
					Long.valueOf(request.session().get("plafond").get()));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + annee + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return ok(views.html.rembourssementGlobal.render(
					regServices.getAllRegementByAdherent(),
					request)).flashing(
							"error",
							" Erreur d'impression");
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
			jasper.generateReport(
					fileName,
					adherentService.getDateT(dateD1),
					adherentService.getDateT(dateD2));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + "" + ".pdf"))
					.flashing("success", "Impression OK");

		} catch (Exception e) {
			System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return ok(views.html.rapports.render(request))
					.flashing("error", " Erreur d'impression");
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

	// Route pour la confirmation
	//@RequiresCSRFCheck(false)
	public Result confirmerBon(Long bonId, Long adherentId) {
		try {
			Reglement reglement = regServices.findById(bonId);

			if (reglement == null) {
				return notFound("Bon de commande non trouvé");
			}

			reglement.setIsConfirmedBon(true);
			regServices.update(reglement);

			return ok("Bon confirmé avec succès");
		} catch (Exception e) {
			e.printStackTrace();
			return internalServerError("Erreur lors de la confirmation");
		}
	}

	/**
	 * Réactualiser un bon de commande (remettre isConfirmedBon à false) Réservé aux
	 * SuperAdmin uniquement
	 */
	public Result reactualiserBon(Long bonId, Long adherentId) {
		try {
			// String droit = session("droit");

			Reglement reglement = regServices.findById(bonId);

			if (reglement == null) {
				return notFound("Bon de commande non trouvé");
			}

			if (!reglement.getIsConfirmedBon()) {
				return badRequest("Le bon n'est pas confirmé");
			}

			reglement.setIsConfirmedBon(false);
			regServices.update(reglement);

			Logger.info("Bon de commande réactualisé - ID: " + bonId + " par SuperAdmin: ");

			return ok("Bon réactualisé avec succès");

		} catch (Exception e) {
			Logger.error("Erreur lors de la réactualisation du bon: " + e.getMessage(), e);
			return internalServerError("Erreur lors de la réactualisation");
		}
	}


	public Result print(Request request, Long numBon, String fileName) {

		// String fileName = "recu";
		LocalDateTime now = LocalDateTime.now();
		String now_string = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
		String templateDir = new File("").getAbsolutePath() + "/reports/spool/";

		String messageCourt = "Cher(e) adherent, votre bon " + regServices.findVRegById(numBon).getId() + " d'un montant de " +  regServices.findVRegById(numBon).getMontantReglement()+ " F CFA a ete emis avec succes. Si vous netes pas l'auteur, contactez la MUSAPOSTE. Merci.";
    	String messageCourtPC = "Cher(e) adherent, votre prise en charge numero " + regServices.findVRegById(numBon).getId() + " a ete emis avec succes. Si vous netes pas l'auteur, contactez la MUSAPOSTE. Merci.";
    String smsText = "";
	if(regServices.findVRegById(numBon).getTypeReglement().equals("BC"))
		smsText = messageCourt;
	else
		smsText = messageCourtPC;

		try {
			 OtpMessage sms = new OtpMessage();
			// flash("success", "impression ok");
			System.out.println("num bon a imprimer :"+ numBon);
			try{
				
				sms.setIsSent(true);
				sms.setIsUsed(true);
				if(!messagesServices.getMessageByNumBonMontant(regServices.findVRegById(numBon).getId(),regServices.findVRegById(numBon).getMontantReglement()))
					sms.setSentResponse(otpService.sendSMSBC(regServices.findVRegById(numBon).getTelephone(), smsText));
				else
					sms.setSentResponse("message deja envoyé");
				
			} catch(Exception e){
				    sms.setIsSent(false);
					sms.setIsUsed(false);
					sms.setSentResponse("ERROR: " + e.getMessage());

				e.printStackTrace();
           		 System.out.println("Erreur envoi SMS : " + e.getMessage());
			}
			sms.setBonCommande(regServices.findVRegById(numBon).getId() );
			sms.setMontantBon(regServices.findVRegById(numBon).getMontantReglement());
			sms.setPhone(regServices.findVRegById(numBon).getTelephone());
			sms.setMessageTexte(messageCourt);
			sms.setCreatedAt(new Timestamp(System.currentTimeMillis()));	

			messagesServices.saveLogical(sms, true);
		
			jasper.generateReport(fileName, String.valueOf(numBon));

			return ok(new java.io.File(templateDir + fileName + "_" + now_string + "_" + numBon + ".pdf"))
					.flashing("success", "impression ok");

		} catch (Exception e) {
			// flash("error", "erreur impression");
			// System.out.println(e.getMessage() + "+++++++--**///////++++++++");
			return redirect(routes.AdherentCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L)).flashing("error",
					"Erreur d'impression");
		}
	}
}
