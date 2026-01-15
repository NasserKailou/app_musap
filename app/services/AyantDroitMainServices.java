package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.AyantDroitDao;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.VAdherentAyantDroit;
import models.tables.pojos.VAyantDroit;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class AyantDroitMainServices extends AyantDroitDao {

	private final IConnectionHelper con;

	@Inject
	public AyantDroitMainServices(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public List<AyantDroit> findLikeName(String query) {
		String lnom = null == query ? "" : query;
		lnom = lnom.endsWith("%") ? lnom : lnom + "%";
		return con.connection().selectFrom(models.Tables.AYANT_DROIT).where(models.Tables.AYANT_DROIT.NOM_AY.like(lnom))
				.fetchInto(AyantDroit.class);
	}

	public String saveLogical(AyantDroit ayantDroit, boolean b) {
		try {
			if (b)
				super.insert(ayantDroit);
			else
				super.update(ayantDroit);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<AyantDroit> findAll() {
		List<AyantDroit> c = con.connection().selectFrom(models.Tables.AYANT_DROIT)
				.where(models.Tables.AYANT_DROIT.ON_DELETED.isFalse()).fetchInto(AyantDroit.class);
		con.connection().close();
		return c;
	}

	/**
	 * Return la liste des ayantDroits par adhérent
	 * 
	 * @param idAdherent
	 * @return
	 */
	public List<VAyantDroit> findAyantDroitByAdherent(Long idAdherent) {
		List<VAyantDroit> c = con.connection().selectFrom(models.Tables.V_AYANT_DROIT)
				.where(models.Tables.V_AYANT_DROIT.ON_DELETED.isFalse())
				.and(models.Tables.V_AYANT_DROIT.ADHERENT.eq(idAdherent)).fetchInto(VAyantDroit.class);
		con.connection().close();
		return c;
	}

	public List<VAdherentAyantDroit> findLikeAyantDroit(String query) {
		String detail = null == query ? "" : query;
		detail = detail.endsWith("%") ? detail : detail + "%";
		List<VAdherentAyantDroit> detailsList = con.connection().selectFrom(models.Tables.V_ADHERENT_AYANT_DROIT)
				.where(models.Tables.V_ADHERENT_AYANT_DROIT.NOM_AY.like(detail)).fetchInto(VAdherentAyantDroit.class);
		con.connection().close();
		return detailsList;
	}

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

	public AyantDroit findById(Long id) {
		return super.findById(id);
	}
}
