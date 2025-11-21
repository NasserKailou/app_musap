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
