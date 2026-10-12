package dto;

import java.sql.Timestamp;

/**
 * Ligne des rapports d'opérations : un bon de commande (BC) ou une prise en
 * charge (PC), avec son émetteur, la région de l'émetteur et le bénéficiaire.
 *
 * @author nasser
 */
public class OperationVue {

	private Long id;
	private String typeReglement;
	private Timestamp dateOperation;
	private String emetteur;
	private String loginEmetteur;
	private String region;
	private String beneficiaire;
	private String qualite;
	private Long idAdherent;
	private String adherent;
	private String matricule;
	private String structure;
	private String prestation;
	private Long montant;
	private Boolean isConfirmedBon;
	private String annulePar;
	private Timestamp annuleLe;
	private String motif;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getTypeReglement() { return typeReglement; }
	public void setTypeReglement(String typeReglement) { this.typeReglement = typeReglement; }

	public Timestamp getDateOperation() { return dateOperation; }
	public void setDateOperation(Timestamp dateOperation) { this.dateOperation = dateOperation; }

	public String getEmetteur() { return emetteur; }
	public void setEmetteur(String emetteur) { this.emetteur = emetteur; }

	public String getLoginEmetteur() { return loginEmetteur; }
	public void setLoginEmetteur(String loginEmetteur) { this.loginEmetteur = loginEmetteur; }

	public String getRegion() { return region; }
	public void setRegion(String region) { this.region = region; }

	public String getBeneficiaire() { return beneficiaire; }
	public void setBeneficiaire(String beneficiaire) { this.beneficiaire = beneficiaire; }

	public String getQualite() { return qualite; }
	public void setQualite(String qualite) { this.qualite = qualite; }

	public Long getIdAdherent() { return idAdherent; }
	public void setIdAdherent(Long idAdherent) { this.idAdherent = idAdherent; }

	public String getAdherent() { return adherent; }
	public void setAdherent(String adherent) { this.adherent = adherent; }

	public String getMatricule() { return matricule; }
	public void setMatricule(String matricule) { this.matricule = matricule; }

	public String getStructure() { return structure; }
	public void setStructure(String structure) { this.structure = structure; }

	public String getPrestation() { return prestation; }
	public void setPrestation(String prestation) { this.prestation = prestation; }

	public Long getMontant() { return montant; }
	public void setMontant(Long montant) { this.montant = montant; }

	public Boolean getIsConfirmedBon() { return isConfirmedBon; }
	public void setIsConfirmedBon(Boolean isConfirmedBon) { this.isConfirmedBon = isConfirmedBon; }

	public String getAnnulePar() { return annulePar; }
	public void setAnnulePar(String annulePar) { this.annulePar = annulePar; }

	public Timestamp getAnnuleLe() { return annuleLe; }
	public void setAnnuleLe(Timestamp annuleLe) { this.annuleLe = annuleLe; }

	public String getMotif() { return motif; }
	public void setMotif(String motif) { this.motif = motif; }

	/** BON ou PRISE EN CHARGE */
	public String getTypeLibelle() {
		return "PC".equalsIgnoreCase(typeReglement) ? "PRISE EN CHARGE" : "BON";
	}

	/** Qualité du bénéficiaire : "Adhérent" ou "Enfant de NOM ADHERENT" */
	public String getQualiteLibelle() {
		if (qualite == null || "ADHERENT".equals(qualite))
			return "Adhérent";
		String l = qualite.substring(0, 1) + qualite.substring(1).toLowerCase();
		return l + (adherent != null && !adherent.isEmpty() ? " de " + adherent : "");
	}
}
