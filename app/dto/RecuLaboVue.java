package dto;

import models.tables.pojos.RecuLabo;

/**
 * Reçu de laboratoire enrichi pour l'affichage : POJO jOOQ généré + matricule
 * de l'adhérent (jointure). Hors de app/models pour survivre à la régénération jOOQ.
 *
 * @author nasser
 */
public class RecuLaboVue extends RecuLabo {

	private static final long serialVersionUID = 1L;

	private String matricule;
	/** nom de l'adhérent (utile lorsque le patient est un de ses ayants droit) */
	private String adherentPrincipal;
	/** lien de l'ayant droit avec l'adhérent (CONJOINT, ENFANT...) */
	private String lien;

	public String getMatricule() {
		return matricule;
	}

	public void setMatricule(String matricule) {
		this.matricule = matricule;
	}

	public String getAdherentPrincipal() {
		return adherentPrincipal;
	}

	public void setAdherentPrincipal(String adherentPrincipal) {
		this.adherentPrincipal = adherentPrincipal;
	}

	public String getLien() {
		return lien;
	}

	public void setLien(String lien) {
		this.lien = lien;
	}
}
