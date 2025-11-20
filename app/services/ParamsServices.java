package services;

import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.ParamsDao;
import models.tables.pojos.Params;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ParamsServices extends ParamsDao {

	private final IConnectionHelper con;

	@Inject
	public ParamsServices(IConnectionHelper con) {

		this.con = con;
		setConfiguration(con.connection().configuration());

	}

	public String saveLogical(Params adherent, boolean b) {
		try {
			if (b)
				super.insert(adherent);
			else
				super.update(adherent);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<Params> listesParams() {
		return con.connection().selectFrom(models.Tables.PARAMS).where(models.Tables.PARAMS.IS_DELETED.isFalse())
				.fetchInto(Params.class);
	}

	public List<Params> listesParamsDeleted() {
		return con.connection().selectFrom(models.Tables.PARAMS).where(models.Tables.PARAMS.IS_DELETED.isTrue())
				.fetchInto(Params.class);
	}

	public Params paramsByGestion(String gestion) {
		return con.connection().selectFrom(models.Tables.PARAMS).where(models.Tables.PARAMS.IS_DELETED.isFalse())
				.and(models.Tables.PARAMS.GESTION.eq(gestion)).fetchOneInto(Params.class);
	}
	
	
}
