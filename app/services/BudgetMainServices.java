package services;

import java.util.List;

import com.google.inject.Inject;

import models.tables.daos.BudgetDao;
import models.tables.pojos.Budget;
import models.tables.pojos.Categorie;
import models.tables.pojos.VBudget;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class BudgetMainServices extends BudgetDao {
	private final IConnectionHelper con;

	@Inject
	public BudgetMainServices(IConnectionHelper con) {
		super();
		this.con = con;

		setConfiguration(con.connection().configuration());
	}

	public String saveLogical(Budget cat, boolean b) {
		try {
			if (b)
				super.insert(cat);
			else
				super.update(cat);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	public List<VBudget> findBudgetByGestion(String gestion) {
		List<VBudget> c = con.connection().selectFrom(models.Tables.V_BUDGET)
				.where(models.Tables.V_BUDGET.IS_DELETED.isFalse()).and(models.Tables.V_BUDGET.GESTION.eq(gestion))
				.fetchInto(VBudget.class);
		con.connection().close();
		return c;

	}

	public List<VBudget> findBudgetByGestionType(String gestion, String type) {
		List<VBudget> c = con.connection().selectFrom(models.Tables.V_BUDGET)
				.where(models.Tables.V_BUDGET.IS_DELETED.isFalse()).and(models.Tables.V_BUDGET.GESTION.eq(gestion))
				.fetchInto(VBudget.class);
		con.connection().close();
		return c;

	}
	
	public List<VBudget> findBudget() {
		List<VBudget> c = con.connection().selectFrom(models.Tables.V_BUDGET)
				.where(models.Tables.V_BUDGET.IS_DELETED.isFalse())
				.orderBy(models.Tables.V_BUDGET.COMPTE.asc(), models.Tables.V_BUDGET.GESTION.asc())
				.fetchInto(VBudget.class);
		con.connection().close();
		return c;

	}

	public Budget findUnik(Long gestion, Long compte, String typeBudget) {
		Budget c = con.connection().selectFrom(models.Tables.BUDGET)
				.where(models.Tables.BUDGET.TYPE_BUGET.eq(typeBudget)).and(models.Tables.BUDGET.GESTION.eq(gestion))
				.and(models.Tables.BUDGET.COMPTE.eq(compte))
				.orderBy(models.Tables.BUDGET.COMPTE.asc(), models.Tables.BUDGET.GESTION.asc())
				.fetchOneInto(Budget.class);
		con.connection().close();
		return c;

	}

	public Long montantBudgetTypeByGestion(String gestion, String type) {
		long total = 0L;
		for (VBudget b : findBudgetByGestion(gestion)) {
			total += b.getMontant();
		}

		return total;
	}

}