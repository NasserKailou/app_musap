package dto;

/**
 * Règles de prise en charge des retraités (table parametre_retraite, une seule ligne).
 *
 * - à partir de agePreRetraite (55 ans) : cotisation de base portée au taux PRE_RETRAITE (5 %)
 *   pour financer la couverture après la retraite ;
 * - à partir de ageRetraite (60 ans) : statut retraité, seul l'adhérent est couvert
 *   (ayants droit archivés), plafond annuel fixe plafondRetraite (500 000) ;
 * - à partir de ageFinCouverture (65 ans) : adhérent archivé, aucune prestation.
 *
 * @author nasser
 */
public class ParametreRetraite {

	public static final String ACTIF = "ACTIF";
	public static final String PRE_RETRAITE = "PRE_RETRAITE";
	public static final String RETRAITE = "RETRAITE";
	public static final String FIN_COUVERTURE = "FIN_COUVERTURE";

	private Integer agePreRetraite = 55;
	private Integer ageRetraite = 60;
	private Integer ageFinCouverture = 65;
	private Long plafondRetraite = 500000L;
	private Long seuilAlerteRetraite = 250000L;

	public Integer getAgePreRetraite() { return agePreRetraite; }
	public void setAgePreRetraite(Integer agePreRetraite) { this.agePreRetraite = agePreRetraite; }

	public Integer getAgeRetraite() { return ageRetraite; }
	public void setAgeRetraite(Integer ageRetraite) { this.ageRetraite = ageRetraite; }

	public Integer getAgeFinCouverture() { return ageFinCouverture; }
	public void setAgeFinCouverture(Integer ageFinCouverture) { this.ageFinCouverture = ageFinCouverture; }

	public Long getPlafondRetraite() { return plafondRetraite; }
	public void setPlafondRetraite(Long plafondRetraite) { this.plafondRetraite = plafondRetraite; }

	public Long getSeuilAlerteRetraite() { return seuilAlerteRetraite; }
	public void setSeuilAlerteRetraite(Long seuilAlerteRetraite) { this.seuilAlerteRetraite = seuilAlerteRetraite; }

	/** Statut selon l'âge : ACTIF, PRE_RETRAITE, RETRAITE ou FIN_COUVERTURE */
	public String statut(Integer age) {
		if (age == null)
			return ACTIF;
		if (age >= ageFinCouverture)
			return FIN_COUVERTURE;
		if (age >= ageRetraite)
			return RETRAITE;
		if (age >= agePreRetraite)
			return PRE_RETRAITE;
		return ACTIF;
	}

	public static String libelleStatut(String statut) {
		switch (statut) {
		case PRE_RETRAITE:
			return "Pré-retraite";
		case RETRAITE:
			return "Retraité";
		case FIN_COUVERTURE:
			return "Fin de couverture";
		default:
			return "Actif";
		}
	}
}
