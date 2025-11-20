package services;

import com.google.inject.Inject;

import models.tables.daos.ConnexionDao;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class ConnexionMainServices extends ConnexionDao{

private final IConnectionHelper con;
	

	@Inject
	public ConnexionMainServices(IConnectionHelper con) {
		super();
		this.con = con;
		
		setConfiguration(con.connection().configuration());
	}

}
