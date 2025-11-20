package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.inject.Inject;

import org.jooq.Configuration;

import static models.Tables.*;
import models.tables.daos.ReglementDetailDao;
import models.tables.pojos.ReglementDetail;
import models.tables.pojos.TypePrestation;
import models.tables.pojos.VAdherentAyantDroit;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ReglementDetailMainServices extends ReglementDetailDao {

	private final IConnectionHelper con;

	@Inject
	public ReglementDetailMainServices(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());
	}

	public String saveLogical(ReglementDetail reg, boolean b) {
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

	public List<ReglementDetail> findAll() {
		List<ReglementDetail> c = con.connection().selectFrom(models.Tables.REGLEMENT_DETAIL)
				.where(models.Tables.REGLEMENT_DETAIL.ON_DELETED.isFalse()).fetchInto(ReglementDetail.class);
		con.connection().close();
		return c;
	}
	
	public List<ReglementDetail> findAllDeleted() {
		List<ReglementDetail> c = con.connection().selectFrom(models.Tables.REGLEMENT_DETAIL)
				.where(models.Tables.REGLEMENT_DETAIL.ON_DELETED.isTrue()).fetchInto(ReglementDetail.class);
		con.connection().close();
		return c;
	}

	public List<ReglementDetail> getByReglement(Long reglement) {
		List<ReglementDetail> c = con.connection().selectFrom(models.Tables.REGLEMENT_DETAIL)
				.where(models.Tables.REGLEMENT_DETAIL.REGLEMENT.eq(reglement))
				.and(models.Tables.REGLEMENT_DETAIL.ON_DELETED.isFalse()).fetchInto(ReglementDetail.class);
		con.connection().close();
		return c;
	}

	public ReglementDetail findById(Long id) {
		return super.findById(id);
	}

}
