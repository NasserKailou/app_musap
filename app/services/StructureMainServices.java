package services;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.inject.Inject;

import org.jooq.Configuration;
import org.python.antlr.PythonParser.return_stmt_return;

import models.Tables;
import models.tables.daos.AyantDroitDao;
import models.tables.daos.StructurePartenaireDao;
import models.tables.pojos.AyantDroit;
import models.tables.pojos.Region;
import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.VPartenaire;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class StructureMainServices extends StructurePartenaireDao {

	private final IConnectionHelper con;

	@Inject
	public StructureMainServices(IConnectionHelper con) {
		super();
		this.con = con;

		setConfiguration(con.connection().configuration());
	}

	public String saveLogical(StructurePartenaire strucuture, boolean b) {
		try {
			if (b)
				super.insert(strucuture);
			else
				super.update(strucuture);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<VPartenaire> listPartenaire() {

		List<VPartenaire> c = con.connection().selectFrom(models.Tables.V_PARTENAIRE)
				.where(models.Tables.V_PARTENAIRE.IS_DELETED.isFalse()).fetchInto(VPartenaire.class);
		con.connection().close();
		return c;
	}

	public List<Region> listRegion() {

		return con.connection().selectFrom(models.Tables.REGION).where(Tables.REGION.IS_DELETED.isFalse())
				.fetchInto(Region.class);
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

	public StructurePartenaire findById(Long id) {
		return super.findById(id);
	}
}
