package services;

import com.google.inject.Inject;

import models.tables.daos.EmailDao;
import models.tables.pojos.Email;
import utils.IConnectionHelper;

public class EmailManagerServices extends EmailDao{

private final IConnectionHelper con;
	

	@Inject
	public EmailManagerServices(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}


	public String saveLogical(Email cat, boolean b) {
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
}
