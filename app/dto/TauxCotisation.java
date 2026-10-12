package dto;

/**
 * Taux de cotisation (en % du salaire) : part de base de l'adhérent ou part
 * ajoutée par ayant droit selon son lien. nombreMax limite le nombre d'ayants
 * droit de ce lien pris en compte (null = illimité).
 *
 * @author nasser
 */
public class TauxCotisation {

	public static final String CODE_ADHERENT = "ADHERENT";

	private String code;
	private String libelle;
	private Double taux;
	private Integer nombreMax;
	private Integer ordre;

	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }

	public String getLibelle() { return libelle; }
	public void setLibelle(String libelle) { this.libelle = libelle; }

	public Double getTaux() { return taux; }
	public void setTaux(Double taux) { this.taux = taux; }

	public Integer getNombreMax() { return nombreMax; }
	public void setNombreMax(Integer nombreMax) { this.nombreMax = nombreMax; }

	public Integer getOrdre() { return ordre; }
	public void setOrdre(Integer ordre) { this.ordre = ordre; }

	public boolean estBase() { return CODE_ADHERENT.equals(code); }
}
