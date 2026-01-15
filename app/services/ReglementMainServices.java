package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.inject.Inject;

import static models.Tables.*;

import models.tables.daos.ReglementDao;
import models.tables.pojos.Reglement;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VAdherentAyantDroit;
import models.tables.pojos.VReglement;
import models.tables.pojos.VReglementGlobalByAdherent;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ReglementMainServices extends ReglementDao {

	private final IConnectionHelper con;

	@Inject
	public ReglementMainServices(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());
	}

	public String saveLogical(Reglement reg, boolean b) {
		try {
			if (b)
				super.insert(reg);
			else
				super.update(reg);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<Reglement> findAll() {
		List<Reglement> c = con.connection().selectFrom(models.Tables.REGLEMENT)
				.where(models.Tables.REGLEMENT.ON_DELETED.isFalse()).fetchInto(Reglement.class);
		con.connection().close();
		return c;
	}

	public List<VReglement> findAllVReg() {
		List<VReglement> c = con.connection().selectFrom(models.Tables.V_REGLEMENT)
				.where(models.Tables.V_REGLEMENT.ON_DELETED.isFalse()).fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}

	/**
	 * Return la liste des reglement par adhérent decommenter /commenter pour
	 * cloturer ou ouvrir des nouvelle années apres la mise a jour 2021
	 * 
	 * @param idAdherent
	 * @return
	 */
	public List<VReglement> findReglementByAdherent(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		// String gestion = String.valueOf(d).substring(0, 4);
		System.out.println("la date est :" + d + " gestion :" + gestion);

		List<VReglement> c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ON_DELETED.isFalse())
				.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent)).and(V_REGLEMENT.ANNEE.eq(gestion)) //
				.fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}


		public List<VReglement> findBonToValidate() {
	
		List<VReglement> c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ON_DELETED.isFalse())
				.and(V_REGLEMENT.IS_CONFIRMED_BON.isFalse()).fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}

		public List<VReglement> findReglementBCByAdherent(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		// String gestion = String.valueOf(d).substring(0, 4);
		//System.out.println("la date est :" + d + " gestion :" + gestion);

		List<VReglement> c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ON_DELETED.isFalse())
				.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent)).and(V_REGLEMENT.ANNEE.eq(gestion)).and(V_REGLEMENT.TYPE_STRUCTURE.eq("PHARMACIE"))//
				.fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}

	public List<VReglement> findReglementPCByAdherent(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		// String gestion = String.valueOf(d).substring(0, 4);
		//System.out.println("la date est :" + d + " gestion :" + gestion);

		List<VReglement> c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ON_DELETED.isFalse())
				.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent)).and(V_REGLEMENT.ANNEE.eq(gestion))
				.and(V_REGLEMENT.TYPE_STRUCTURE.eq("HOPITAL").or(V_REGLEMENT.TYPE_STRUCTURE.eq("CLINIQUE")).or(V_REGLEMENT.TYPE_STRUCTURE.eq("LABORATOIRE")))//
				.fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}

/** public List<VRegBonCommande> findReglementBCByAdherent(Long idAdherent, String gestion) {
		List<VRegBonCommande> c = con.connection().selectFrom(V_REG_BON_COMMANDE).where(V_REG_BON_COMMANDE.ON_DELETED.isFalse())
				.and(V_REG_BON_COMMANDE.ID_ADHERENT.eq(idAdherent)).and(V_REG_BON_COMMANDE.ANNEE.eq(gestion))//
				.fetchInto(VRegBonCommande.class);
		con.connection().close();
		return c;
	}

	public List<VRegPriseEnCharge> findReglementPCByAdherent(Long idAdherent, String gestion) {
		List<VRegPriseEnCharge> c = con.connection().selectFrom(V_REG_PRISE_EN_CHARGE).where(V_REG_PRISE_EN_CHARGE.ON_DELETED.isFalse())
				.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent)).and(V_REGLEMENT.ANNEE.eq(gestion)).fetchInto(VRegPriseEnCharge.class);
					con.connection().close();
		return c;
	} */

	/**
	 * decommenté la gestion apres mise a jour des montant
	 * 
	 * @param idAdherent2021
	 * @return
	 */
	public List<VReglement> findReglementByAdherentByAnnee(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());
		// String gestion = String.valueOf(d).substring(0, 4);
		System.out.println("la date est :" + d);
		List<VReglement> c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ON_DELETED.isFalse())
				.and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent)).and(V_REGLEMENT.ANNEE.eq(gestion))
				.fetchInto(VReglement.class);
		con.connection().close();
		return c;
	}

		

//	public Boolean isPlafond(Long idAdherent) {
//		List<Reglement> listes = con.connection()
//	}

	/**
	 * return le total regler pour un adherent par an
	 * 
	 * @param idAdherent
	 * @return
	 */
	public Long sommeRegler(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		String gestionsss = String.valueOf(d).substring(0, 4);

		// List<VReglement> c = this.findReglementByAdherent(idAdherent);
		List<VReglement> c = this.findReglementByAdherentByAnnee(idAdherent, gestion);
		Long totalRegler = 0L;
		for (VReglement element : c) {
			// totalRegler += element.getMontantTotal();
			totalRegler += element.getMontantReglement();
		}
		System.out.println("Le total Solder est :" + totalRegler + " F CFA");

		return totalRegler;
	}

	public Long sommeReglerBC(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		String gestionsss = String.valueOf(d).substring(0, 4);

		// List<VReglement> c = this.findReglementByAdherent(idAdherent);
		List<VReglement> c = this.findReglementBCByAdherent(idAdherent, gestion);
		Long totalRegler = 0L;
		for (VReglement element : c) {
			// totalRegler += element.getMontantTotal();
			totalRegler += element.getMontantReglement();
		}
		System.out.println("Le total Solder est :" + totalRegler + " F CFA");

		return totalRegler;
	}


	public Long sommeReglerPC(Long idAdherent, String gestion) {
		Timestamp d = new Timestamp(System.currentTimeMillis());

		String gestionsss = String.valueOf(d).substring(0, 4);

		// List<VReglement> c = this.findReglementByAdherent(idAdherent);
		List<VReglement> c = this.findReglementPCByAdherent(idAdherent, gestion);
		Long totalRegler = 0L;
		for (VReglement element : c) {
			// totalRegler += element.getMontantTotal();
			totalRegler += element.getMontantReglement();
		}
		System.out.println("Le total Solder est :" + totalRegler + " F CFA");

		return totalRegler;
	}

	public Long sommeReglerByAnnee(Long idAdherent, String gestion) {
		List<VReglement> c = this.findReglementByAdherentByAnnee(idAdherent, gestion);

		Long totalRegler = 0L;
		for (VReglement element : c) {
			totalRegler += element.getMontantReglement();
		}
		System.out.println("Le total Solder est :" + totalRegler + " F CFA");

		return totalRegler;
	}

//	public Boolean isFull(Long idAdherent) {
//		
//		Boolean isOk = false;
//		Long
//		
//	}
	public Timestamp getDateT(String d) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		LocalDate localDate = LocalDate.parse(d, formatter);
		LocalDateTime dateV = null;

		if (null != d && !d.trim().isEmpty()) {
			try {
				// System.out.println("la date est :"+dateV+tmpDate);

				dateV = LocalDateTime.of(localDate, LocalTime.of(0, 0));

			} catch (Exception e) {
				e.getMessage();
			}
		}

		return Timestamp.valueOf(dateV);

	}

	public Reglement findById(Long id) {
		return super.findById(id);
	}

	public VReglement findVRegById(Long id) {
		 VReglement c = con.connection().selectFrom(V_REGLEMENT).where(V_REGLEMENT.ID.eq(id)).fetchOneInto(VReglement.class);
		con.connection().close();
		return c;
	}

	public List<VAdherentAyantDroit> getAdherentAndAyantByAdherent(Long idAdherent) {
		List<VAdherentAyantDroit> vad = con.connection().selectFrom(V_ADHERENT_AYANT_DROIT)
				.where(V_ADHERENT_AYANT_DROIT.ID_ADHERENT.eq(idAdherent)).fetchInto(VAdherentAyantDroit.class);
		con.connection().close();
		return vad;
	}

	public List<TypePrestation> getAllTypePrestation() {
		List<TypePrestation> tp = con.connection().selectFrom(TYPE_PRESTATION)
				.where(TYPE_PRESTATION.ON_DELETED.isFalse()).fetchInto(TypePrestation.class);
		con.connection().close();
		return tp;
	}

	public List<VReglementGlobalByAdherent> getAllRegementByAdherent() {

		List<VReglementGlobalByAdherent> lites = con.connection().selectFrom(V_REGLEMENT_GLOBAL_BY_ADHERENT)
				.fetchInto(VReglementGlobalByAdherent.class);
		con.connection().close();
		return lites;
	}

	public List<VReglementGlobalByAdherent> getAllRegementByAdherent(String anne) {

		List<VReglementGlobalByAdherent> lites = con.connection().selectFrom(V_REGLEMENT_GLOBAL_BY_ADHERENT)
				.where(V_REGLEMENT_GLOBAL_BY_ADHERENT.ANNEE.eq(anne)).fetchInto(VReglementGlobalByAdherent.class);
		con.connection().close();
		return lites;
	}
	
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

	public  String normaliserNumeroNiger(String telephone) {

    if (telephone == null || telephone.trim().isEmpty()) {
				throw new IllegalArgumentException("Le numéro de téléphone est vide ou null");
			}

			// Nettoyage : suppression des espaces, tirets, etc.
			telephone = telephone.replaceAll("[^0-9]", "");

			// Si le numéro commence déjà par 227 → on laisse
			if (telephone.startsWith("227")) {
				return telephone;
			}

			// Sinon on ajoute 227
			return "227" + telephone;
		}


}
