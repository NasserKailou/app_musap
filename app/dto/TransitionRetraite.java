package dto;

/**
 * Changement de situation à appliquer à un adhérent selon son âge
 * (aperçu avant application dans l'écran Retraites).
 *
 * @author nasser
 */
public class TransitionRetraite {

	public static final String FIN_COUVERTURE = "FIN_COUVERTURE";
	public static final String ARCHIVAGE_AYANTS_DROIT = "ARCHIVAGE_AYANTS_DROIT";
	public static final String COTISATION = "COTISATION";

	private final Long idAdherent;
	private final String matricule;
	private final String nom;
	private final Integer age;
	private final String statut;
	private final String action;
	private final String detail;
	private final int nbAyantsDroit;

	public TransitionRetraite(Long idAdherent, String matricule, String nom, Integer age, String statut, String action,
			String detail, int nbAyantsDroit) {
		this.idAdherent = idAdherent;
		this.matricule = matricule;
		this.nom = nom;
		this.age = age;
		this.statut = statut;
		this.action = action;
		this.detail = detail;
		this.nbAyantsDroit = nbAyantsDroit;
	}

	public Long getIdAdherent() { return idAdherent; }
	public String getMatricule() { return matricule; }
	public String getNom() { return nom; }
	public Integer getAge() { return age; }
	public String getStatut() { return statut; }
	public String getLibelleStatut() { return ParametreRetraite.libelleStatut(statut); }
	public String getAction() { return action; }
	public String getDetail() { return detail; }
	public int getNbAyantsDroit() { return nbAyantsDroit; }
}
