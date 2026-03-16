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

	/**
	 * Retourne tous les bons de commande (règlements non supprimés),
	 * triés par date de création décroissante.
	 */
	public List<Reglement> findAllBons() {
		List<Reglement> c = con.connection().selectFrom(models.Tables.REGLEMENT)
				.where(models.Tables.REGLEMENT.ON_DELETED.isFalse())
				.orderBy(models.Tables.REGLEMENT.WHEN_DONE.desc())
				.fetchInto(Reglement.class);
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
	
	

}
