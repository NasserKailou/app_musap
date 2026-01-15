/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import java.util.List;
import models.tables.pojos.Users;

/**
 *
 * @author alpariss
 */
public interface IUser {
    public String saveLogical(Users u, boolean b);
    public String deleteUser(String login);
    public String ChangeEtat(String login, boolean etat);
    public List<Users> getAllUser();
    public Users getUserByLogin(String login);
    public Users authentification(String login, String password);
    public boolean isUserExist(String libelle);
}
