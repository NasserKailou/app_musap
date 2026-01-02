package services;

import java.util.List;

import com.google.inject.Inject;

import static models.Tables.*;

import models.tables.pojos.VBonDeCommande;
import utils.IConnectionHelper;

/**
 * Service pour accéder à la vue VBonDeCommande
 * 
 * @author nasser
 */
public class VBonDeCommandeServices {

    private final IConnectionHelper con;

    @Inject
    public VBonDeCommandeServices(IConnectionHelper con) {
        super();
        this.con = con;
    }

    /**
     * Récupère tous les bons de commande (non supprimés)
     * 
     * @return Liste de tous les bons de commande
     */
    public List<VBonDeCommande> findAll() {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }

    /**
     * Récupère tous les bons de commande confirmés
     * 
     * @return Liste des bons confirmés
     */
    public List<VBonDeCommande> findAllConfirmed() {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .and(V_BON_DE_COMMANDE.IS_CONFIRMED_BON.isTrue())
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }

    /**
     * Récupère tous les bons de commande non confirmés
     * 
     * @return Liste des bons non confirmés
     */
    public List<VBonDeCommande> findAllUnconfirmed() {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .and(V_BON_DE_COMMANDE.IS_CONFIRMED_BON.isFalse()
                        .or(V_BON_DE_COMMANDE.IS_CONFIRMED_BON.isNull()))
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }

    /**
     * Récupère les bons de commande par structure
     * 
     * @param idStructure ID de la structure
     * @return Liste des bons pour cette structure
     */
    public List<VBonDeCommande> findByStructure(Long idStructure) {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .and(V_BON_DE_COMMANDE.ID_STRUCTURE.eq(idStructure))
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }

    /**
     * Récupère les bons de commande par adhérent
     * 
     * @param idAdherent ID de l'adhérent
     * @return Liste des bons pour cet adhérent
     */
    public List<VBonDeCommande> findByAdherent(Long idAdherent) {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .and(V_BON_DE_COMMANDE.ID_ADHERENT.eq(idAdherent))
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }

    /**
     * Récupère les bons de commande par année
     * 
     * @param annee Année de gestion
     * @return Liste des bons pour cette année
     */
    public List<VBonDeCommande> findByAnnee(String annee) {
        List<VBonDeCommande> bons = con.connection()
                .selectFrom(V_BON_DE_COMMANDE)
                .where(V_BON_DE_COMMANDE.ON_DELETED.isFalse())
                .and(V_BON_DE_COMMANDE.ANNEE.eq(annee))
                .fetchInto(VBonDeCommande.class);
        con.connection().close();
        return bons;
    }
}
