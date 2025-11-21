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
