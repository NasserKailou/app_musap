package services;


import com.google.inject.Inject;

import models.tables.daos.OtpMessageDao;
import models.tables.pojos.OtpMessage;
import utils.IConnectionHelper;

public class MessageServiceImpl extends OtpMessageDao {

	private final IConnectionHelper con;

	@Inject
	public MessageServiceImpl(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());

	}



	public String saveLogical(OtpMessage message, boolean b) {
		try {
			if (b)
				super.insert(message);
			else
				super.update(message);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}
    
}
