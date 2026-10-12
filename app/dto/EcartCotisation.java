package dto;

/**
 * Adhérent dont le taux de cotisation enregistré diffère du taux calculé.
 *
 * @author nasser
 */
public class EcartCotisation {

	private Long idAdherent;
	private String matricule;
	private String nom;
	private Double enregistre;
	private double calcule;
	private int nbAyantsDroit;
	private String statut = ParametreRetraite.ACTIF;

	public String getStatut() { return statut; }
	public void setStatut(String statut) { this.statut = statut; }
	public String getLibelleStatut() { return ParametreRetraite.libelleStatut(statut); }

	public Long getIdAdherent() { return idAdherent; }
	public void setIdAdherent(Long idAdherent) { this.idAdherent = idAdherent; }

	public String getMatricule() { return matricule; }
	public void setMatricule(String matricule) { this.matricule = matricule; }

	public String getNom() { return nom; }
	public void setNom(String nom) { this.nom = nom; }

	public Double getEnregistre() { return enregistre; }
	public void setEnregistre(Double enregistre) { this.enregistre = enregistre; }

	public double getCalcule() { return calcule; }
	public void setCalcule(double calcule) { this.calcule = calcule; }

	public int getNbAyantsDroit() { return nbAyantsDroit; }
	public void setNbAyantsDroit(int nbAyantsDroit) { this.nbAyantsDroit = nbAyantsDroit; }
}
