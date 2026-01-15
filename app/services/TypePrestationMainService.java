package services;

import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.TypePrestationDao;
import models.tables.pojos.Reglement;
import models.tables.pojos.StructurePartenaire;
import models.tables.pojos.TypePrestation;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class TypePrestationMainService  extends TypePrestationDao{
private final IConnectionHelper con;
	
	@Inject
	public TypePrestationMainService(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}
	
	public String saveLogical(TypePrestation strucuture, boolean b) {
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


	public TypePrestation findById(Long id) {
		return super.findById(id);
	}
	
	public List<TypePrestation> findAll() {
		List<TypePrestation> c = con.connection().selectFrom(models.Tables.TYPE_PRESTATION)
				.where(models.Tables.TYPE_PRESTATION.ON_DELETED.isFalse()).fetchInto(TypePrestation.class);
		con.connection().close();
		return c;
	}
	
	public List<TypePrestation> findOrdonnance() {
		List<TypePrestation> c = con.connection().selectFrom(models.Tables.TYPE_PRESTATION)
				.where(models.Tables.TYPE_PRESTATION.ON_DELETED.isFalse()).and(models.Tables.TYPE_PRESTATION.TYPE.eq("BC")).fetchInto(TypePrestation.class);
		con.connection().close();
		return c;
	}


	public List<TypePrestation> findOthers() {
		List<TypePrestation> c = con.connection().selectFrom(models.Tables.TYPE_PRESTATION)
				.where(models.Tables.TYPE_PRESTATION.ON_DELETED.isFalse()).and(models.Tables.TYPE_PRESTATION.TYPE.eq("PC")).fetchInto(TypePrestation.class);
		con.connection().close();
		return c;
	}

	public List<TypePrestation> getAllTypePrestation() {
		return findAll();
	}
}
