/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import com.google.inject.Inject;
import java.util.List;

import models.Tables;
import models.tables.daos.UsersDao;
import models.tables.pojos.Region;
import models.tables.pojos.Users;
import utils.BCryptHash;
import utils.IConnectionHelper;

/**
 * 
 * @author nasser
 *
 */
public class UserService extends UsersDao implements IUser {

	private final IConnectionHelper con;

	@Inject
	public UserService(IConnectionHelper con) {
		super();
		this.con = con;
		setConfiguration(con.connection().configuration());
	}

	@Override
	public String saveLogical(Users user, boolean b) {
	/* 	
		Users user = new Users();
		user.setNomPrenom(u.getNomPrenom());
		user.setLogin(u.getLogin());
		user.setDroit(u.getDroit());
		user.setEtat(u.getEtat());
		user.setConnectFirst(u.getConnectFirst());
		user.setPasse(BCryptHash.hashPassword(u.getPasse()));
		user.setWhenDone(u.getWhenDone());
		user.setWhoDone(u.getWhoDone());
		*/
		user.setPasse(BCryptHash.hashPassword(user.getPasse()));
		try {
			if (b)
				super.insert(user);
			else
				super.update(user);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@Override
	public String deleteUser(String login) {
		try {
			super.delete(getUserByLogin(login));
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}

	}

	@Override
	public String ChangeEtat(String login, boolean etat) {
		Users u = getUserByLogin(login);
		u.setEtat(etat);
		try {
			super.update(u);
			return "ok";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@Override
	public List<Users> getAllUser() {
		return super.findAll();
	}

	@Override
	public Users getUserByLogin(String login) {
		return con.connection().selectFrom(Tables.USERS).where(Tables.USERS.LOGIN.eq(login)).fetchOneInto(Users.class);
	}

	@Override
	public Users authentification(String login, String password) {

		return con.connection().selectFrom(Tables.USERS).where(Tables.USERS.LOGIN.eq(login))
				.and(Tables.USERS.PASSE.eq(password)).and(Tables.USERS.ETAT.eq(true)).fetchOneInto(Users.class);

	}

	@Override
	public boolean isUserExist(String libelle) {
		List<Users> users = super.fetchByLogin(libelle);
		return users.size() > 0;
	}

	public List<Region> listRegion() {

		//return con.connection().selectFrom(models.Tables.REGION).where(Tables.REGION.IS_DELETED.isFalse())
		return con.connection().selectFrom(models.Tables.REGION)
				.fetchInto(Region.class);
	}

}
