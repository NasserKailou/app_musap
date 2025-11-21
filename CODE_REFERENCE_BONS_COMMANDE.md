# Code de Référence - Workflow Bons de Commande

**⚠️ IMPORTANT**: Les fichiers `.java` séparés ont été supprimés car ils causaient des erreurs de compilation.  
Ce fichier contient tout le code à intégrer manuellement dans les fichiers existants.

---

## 1. ReglementCtrl.java - Méthode showConfirmBon()

**À AJOUTER après la méthode `show()` (ligne ~109)**

```java
/**
 * Affiche la liste des bons de commande non confirmés pour confirmation (Bouton VERT)
 * Cette méthode affiche les bons avec is_confirmed_bon = false ou null
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
		request.session().get("gestion").get()
	);
	
	System.out.println(">>> Nombre de bons non confirmés trouvés: " + bonsNonConfirmes.size());
	
	// Afficher la vue de confirmation
	return ok(views.html.confirmationBon.render(
		bonsNonConfirmes,
		adherent,
		structureService.findAll(),
		request
	));
}
```

---

## 2. ReglementCtrl.java - Méthode save() COMPLÈTE

**REMPLACER la méthode save() existante (ligne ~390-478) par celle-ci:**

```java
/**
 * Méthode de sauvegarde améliorée pour gérer le workflow des bons de commande en 2 étapes
 * ÉTAPE 1 (CREATE): Émission du bon sans confirmation (is_confirmed_bon = false)
 * ÉTAPE 2 (CONFIRM_BON): Confirmation du bon avec justificatifs (is_confirmed_bon = true)
 */
public Result save(Request request) {

	final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");

	Form<Reglement> uForm = formFactory.form(Reglement.class).bindFromRequest(request);
	String dateReglement = formFactory.form().bindFromRequest(request).get("tmpDate");
	System.out.println("Date reglement :" + dateReglement + " - ViewMode: " + viewMode);
	Long benef = Long.parseLong(formFactory.form().bindFromRequest(request).get("benef"));

	Long sommeReg = 0L;
	Reglement c = uForm.get();
	c.setWhenDone(new Timestamp(System.currentTimeMillis()));
	c.setOnDeleted(false);
	c.setWhoDone(String.valueOf(request.session().get("login").get()));

	// renseigner la date de paiement en fonction du reglement
	if (!String.valueOf(dateReglement.substring(0, 4))
			.equals(String.valueOf(request.session().get("gestion").get())))
		c.setDatePayement(regServices.getDateT(request.session().get("gestion").get() + "-12-31"));
	else
		c.setDatePayement(regServices.getDateT(dateReglement));

	// Gérer le bénéficiaire
	if (!benef.equals(c.getAdherent())) {
		c.setAyantDroit(benef);
	}

	// ============================================================
	// ÉTAPE 1: ÉMISSION DU BON DE COMMANDE (Bouton BLEU)
	// ============================================================
	if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
		System.out.println(">>> ÉMISSION DU BON DE COMMANDE (Étape 1)");
		
		// Configuration du bon NON confirmé
		c.setIsConfirmedBon(false);  // Bon en attente de confirmation
		c.setRefFacture(null);       // Ref facture sera ajoutée à la confirmation
		c.setStructureEmettriceRembourssement(null); // Non utilisé pour les bons
		c.setNumBon(null);           // Non utilisé pour les bons
		
		// Calculer la date d'expiration (+30 jours)
		long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
		Timestamp dateExpiration = new Timestamp(c.getDatePayement().getTime() + thirtyDaysInMillis);
		c.setDateExpiration(dateExpiration);
		
		System.out.println(">>> Date émission: " + c.getDatePayement());
		System.out.println(">>> Date expiration: " + dateExpiration);
		System.out.println(">>> is_confirmed_bon: false");

		if (regServices.saveLogical(c, true).equals("ok")) {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("success", "Bon de commande émis avec succès. Validité: 30 jours");
		} else {
			System.out.println("Erreur sauvegarde: " + regServices.saveLogical(c, true));
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("error", "Erreur lors de l'émission du bon de commande");
		}
	}
	
	// ============================================================
	// ÉTAPE 2: CONFIRMATION DU BON DE COMMANDE (Bouton VERT)
	// ============================================================
	else if (viewMode.equals("CONFIRM_BON")) {
		System.out.println(">>> CONFIRMATION DU BON DE COMMANDE (Étape 2)");
		
		// Marquer le bon comme confirmé
		c.setIsConfirmedBon(true);  // Bon confirmé/consommé
		c.setWhenConfirmedBon(new Timestamp(System.currentTimeMillis())); // Date de confirmation
		
		// La ref_facture et date_payement sont déjà dans c via bindFromRequest
		System.out.println(">>> Ref Facture: " + c.getRefFacture());
		System.out.println(">>> Date règlement: " + c.getDatePayement());
		System.out.println(">>> is_confirmed_bon: true");
		System.out.println(">>> when_confirmed_bon: " + c.getWhenConfirmedBon());
		
		if (regServices.saveLogical(c, false).equals("ok")) {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("success", "Bon de commande confirmé avec succès");
		} else {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("error", "Erreur lors de la confirmation du bon");
		}
	}
	
	// ============================================================
	// MODES EXISTANTS (EDIT, DELETE, TRAITE)
	// ============================================================
	else if (viewMode.equals(ViewMode.VIEW_MODE_EDIT)) {
		if (regServices.saveLogical(c, false).equals("ok")) {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("success", "Reglement modifier avec success");
		} else {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("error", "Reglement non modifier");
		}
	} else if (viewMode.equals(ViewMode.VIEW_MODE_TRAITE)) {
		if (regServices.saveLogical(c, false).equals("ok")) {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("success", "Reglement traiter avec success");
		} else {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("error", "Echec lors du traitement du Reglement");
		}
	} else if (viewMode.equals(ViewMode.VIEW_MODE_DELETE)) {
		c.setOnDeleted(true);
		if (regServices.saveLogical(c, false).equals("ok")) {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("success", "Reglement Supprimer avec success");
		} else {
			return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
					.flashing("error", "Reglement non supprimer");
		}
	}
	return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()));
}
```

---

## 3. ReglementMainServices.java - Méthode findReglementNonConfirmesByAdherent()

**À AJOUTER à la fin de la classe (avant le dernier `}`):**

```java
/**
 * Récupère les bons de commande non confirmés pour un adhérent
 * Filtre les bons où is_confirmed_bon = false ou null
 * 
 * @param idAdherent L'ID de l'adhérent
 * @param gestion L'année de gestion
 * @return Liste des bons non confirmés
 */
public List<VReglement> findReglementNonConfirmesByAdherent(Long idAdherent, String gestion) {
	System.out.println(">>> Recherche bons non confirmés - Adhérent: " + idAdherent + ", Gestion: " + gestion);
	
	List<VReglement> bons = con.connection()
		.selectFrom(V_REGLEMENT)
		.where(V_REGLEMENT.ON_DELETED.isFalse())
		.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent))
		.and(V_REGLEMENT.ANNEE.eq(gestion))
		.and(V_REGLEMENT.IS_CONFIRMED_BON.isFalse()
			.or(V_REGLEMENT.IS_CONFIRMED_BON.isNull()))
		.fetchInto(VReglement.class);
	
	con.connection().close();
	
	System.out.println(">>> Nombre de bons non confirmés trouvés: " + bons.size());
	return bons;
}
```

---

## 4. Modifications des Vues

Voir le fichier **INSTRUCTIONS_VIEWS_UPDATE.md** pour les détails complets.

---

**Date:** 2025-11-21  
**Note:** Ces fichiers de référence Java séparés ont été supprimés pour éviter les erreurs de compilation.  
Le code doit être copié-collé dans les fichiers existants.
