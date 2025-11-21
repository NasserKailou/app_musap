package services;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.stream.Collectors;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.jooq.DatePart;

import com.google.inject.Inject;

import models.Tables;
import models.tables.pojos.*;
import utils.IConnectionHelper;

/**
 * Service pour les statistiques et analytics du Dashboard
 * 
 * @author AI Assistant
 */
public class DashboardService {

    private final IConnectionHelper con;

    @Inject
    public DashboardService(IConnectionHelper con) {
        this.con = con;
    }

    /**
     * Récupère le nombre total d'adhérents actifs
     */
    public long getTotalAdherentsActifs() {
        try {
            Long count = con.connection()
                .selectCount()
                .from(Tables.ADHERENT)
                .where(Tables.ADHERENT.ON_DELETED.isFalse())
                .fetchOne(0, Long.class);
            return count != null ? count : 0L;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère le nombre total d'ayants droit
     */
    public long getTotalAyantsDroit() {
        try {
            Long count = con.connection()
                .selectCount()
                .from(Tables.AYANT_DROIT)
                .where(Tables.AYANT_DROIT.ON_DELETED.isFalse())
                .fetchOne(0, Long.class);
            return count != null ? count : 0L;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère le montant total des remboursements
     */
    public BigDecimal getTotalRemboursements() {
        try {
            org.jooq.Result<org.jooq.Record1<java.math.BigDecimal>> result = con.connection()
                .select(DSL.sum(Tables.REGLEMENT_DETAIL.MONTANT))
                .from(Tables.REGLEMENT_DETAIL)
                .join(Tables.REGLEMENT).on(Tables.REGLEMENT.ID.eq(Tables.REGLEMENT_DETAIL.REGLEMENT))
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .fetch();
            
            if (result != null && !result.isEmpty()) {
                Object value = result.get(0).get(0);
                if (value instanceof BigDecimal) {
                    return (BigDecimal) value;
                } else if (value != null) {
                    return new BigDecimal(value.toString());
                }
            }
            return BigDecimal.ZERO;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère le nombre total de remboursements
     */
    public long getTotalNombreRemboursements() {
        try {
            Long count = con.connection()
                .selectCount()
                .from(Tables.REGLEMENT)
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .fetchOne(0, Long.class);
            return count != null ? count : 0L;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère les statistiques mensuelles des remboursements pour l'année en cours
     */
    public Map<String, Object> getRemboursementsMensuels() {
        try {
            int currentYear = Year.now().getValue();
            
            var result = con.connection()
                .select(
                    DSL.extract(Tables.REGLEMENT.DATE_PAYEMENT, DatePart.MONTH).as("mois"),
                    DSL.count().as("nombre"),
                    DSL.sum(Tables.REGLEMENT_DETAIL.MONTANT).as("montant")
                )
                .from(Tables.REGLEMENT)
                .leftJoin(Tables.REGLEMENT_DETAIL).on(Tables.REGLEMENT_DETAIL.REGLEMENT.eq(Tables.REGLEMENT.ID))
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .and(DSL.extract(Tables.REGLEMENT.DATE_PAYEMENT, DatePart.YEAR).eq(currentYear))
                .groupBy(DSL.extract(Tables.REGLEMENT.DATE_PAYEMENT, DatePart.MONTH))
                .orderBy(DSL.extract(Tables.REGLEMENT.DATE_PAYEMENT, DatePart.MONTH))
                .fetch();

            List<String> mois = new ArrayList<>();
            List<Integer> nombres = new ArrayList<>();
            List<BigDecimal> montants = new ArrayList<>();

            String[] nomsMois = {"Jan", "Fév", "Mar", "Avr", "Mai", "Jun", 
                                 "Jul", "Aoû", "Sep", "Oct", "Nov", "Déc"};

            // Initialiser tous les mois à 0
            for (int i = 0; i < 12; i++) {
                mois.add(nomsMois[i]);
                nombres.add(0);
                montants.add(BigDecimal.ZERO);
            }

            // Remplir avec les données réelles
            for (Record record : result) {
                Integer moisNum = record.get("mois", Integer.class);
                if (moisNum != null && moisNum >= 1 && moisNum <= 12) {
                    int index = moisNum - 1;
                    nombres.set(index, record.get("nombre", Integer.class));
                    Object montantObj = record.get("montant");
                    BigDecimal montant = montantObj != null ? 
                        new BigDecimal(montantObj.toString()) : BigDecimal.ZERO;
                    montants.set(index, montant);
                }
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", mois);
            data.put("nombres", nombres);
            data.put("montants", montants);

            return data;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère la répartition des remboursements par type de prestation
     */
    public Map<String, Object> getRemboursementsParTypePrestation() {
        try {
            var result = con.connection()
                .select(
                    Tables.TYPE_PRESTATION.PRESTATION,
                    DSL.count().as("nombre"),
                    DSL.sum(Tables.REGLEMENT_DETAIL.MONTANT).as("montant")
                )
                .from(Tables.REGLEMENT)
                .join(Tables.TYPE_PRESTATION).on(Tables.TYPE_PRESTATION.ID.eq(Tables.REGLEMENT.TYPE_PRESTATION))
                .leftJoin(Tables.REGLEMENT_DETAIL).on(Tables.REGLEMENT_DETAIL.REGLEMENT.eq(Tables.REGLEMENT.ID))
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .groupBy(Tables.TYPE_PRESTATION.PRESTATION)
                .orderBy(DSL.sum(Tables.REGLEMENT_DETAIL.MONTANT).desc())
                .fetch();

            List<String> labels = new ArrayList<>();
            List<Integer> nombres = new ArrayList<>();
            List<BigDecimal> montants = new ArrayList<>();

            for (Record record : result) {
                labels.add(record.get(Tables.TYPE_PRESTATION.PRESTATION, String.class));
                nombres.add(record.get("nombre", Integer.class));
                Object montantObj = record.get("montant");
                BigDecimal montant = montantObj != null ? 
                    new BigDecimal(montantObj.toString()) : BigDecimal.ZERO;
                montants.add(montant);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("nombres", nombres);
            data.put("montants", montants);

            return data;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère la répartition des adhérents par structure
     */
    public Map<String, Object> getAdherentsParStructure() {
        try {
            org.jooq.Result<org.jooq.Record2<String, Integer>> result = con.connection()
                .select(
                    Tables.ADHERENT.STRUCTURE,
                    DSL.count().as("nombre")
                )
                .from(Tables.ADHERENT)
                .where(Tables.ADHERENT.ON_DELETED.isFalse())
                .and(Tables.ADHERENT.STRUCTURE.isNotNull())
                .groupBy(Tables.ADHERENT.STRUCTURE)
                .orderBy(DSL.count().desc())
                .limit(10)
                .fetch();

            List<String> labels = new ArrayList<>();
            List<Integer> nombres = new ArrayList<>();

            for (Record record : result) {
                String structure = record.get(Tables.ADHERENT.STRUCTURE, String.class);
                labels.add(structure != null ? structure : "Non défini");
                nombres.add(record.get("nombre", Integer.class));
            }

            Map<String, Object> data = new HashMap<>();
            data.put("labels", labels);
            data.put("nombres", nombres);

            return data;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère les derniers remboursements (5 plus récents)
     */
    public List<Map<String, Object>> getDerniersRemboursements() {
        try {
            var result = con.connection()
                .select(
                    Tables.REGLEMENT.ID,
                    Tables.REGLEMENT.REF_FACTURE,
                    Tables.REGLEMENT.DATE_PAYEMENT,
                    Tables.ADHERENT.NOM_AD,
                    Tables.ADHERENT.PRENOM_AD,
                    Tables.TYPE_PRESTATION.PRESTATION.as("prestation"),
                    DSL.sum(Tables.REGLEMENT_DETAIL.MONTANT).as("montant")
                )
                .from(Tables.REGLEMENT)
                .join(Tables.ADHERENT).on(Tables.ADHERENT.ID.eq(Tables.REGLEMENT.ADHERENT))
                .leftJoin(Tables.TYPE_PRESTATION).on(Tables.TYPE_PRESTATION.ID.eq(Tables.REGLEMENT.TYPE_PRESTATION))
                .leftJoin(Tables.REGLEMENT_DETAIL).on(Tables.REGLEMENT_DETAIL.REGLEMENT.eq(Tables.REGLEMENT.ID))
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .groupBy(
                    Tables.REGLEMENT.ID,
                    Tables.REGLEMENT.REF_FACTURE,
                    Tables.REGLEMENT.DATE_PAYEMENT,
                    Tables.ADHERENT.NOM_AD,
                    Tables.ADHERENT.PRENOM_AD,
                    Tables.TYPE_PRESTATION.PRESTATION
                )
                .orderBy(Tables.REGLEMENT.DATE_PAYEMENT.desc())
                .limit(5)
                .fetch();

            List<Map<String, Object>> remboursements = new ArrayList<>();

            for (Record record : result) {
                Map<String, Object> item = new HashMap<>();
                item.put("id", record.get(Tables.REGLEMENT.ID));
                item.put("reference", record.get(Tables.REGLEMENT.REF_FACTURE));
                item.put("date", record.get(Tables.REGLEMENT.DATE_PAYEMENT));
                item.put("adherent", record.get(Tables.ADHERENT.NOM_AD) + " " + 
                                     record.get(Tables.ADHERENT.PRENOM_AD));
                item.put("prestation", record.get("prestation"));
                Object montantObj = record.get("montant");
                BigDecimal montant = montantObj != null ? 
                    new BigDecimal(montantObj.toString()) : BigDecimal.ZERO;
                item.put("montant", montant);
                remboursements.add(item);
            }

            return remboursements;
        } finally {
            con.connection().close();
        }
    }

    /**
     * Récupère les indicateurs de croissance (comparaison avec le mois précédent)
     */
    public Map<String, Object> getIndicateursCroissance() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime startOfLastMonth = startOfMonth.minusMonths(1);

            // Adhérents ce mois
            Long adherentsCeMois = con.connection()
                .selectCount()
                .from(Tables.ADHERENT)
                .where(Tables.ADHERENT.ON_DELETED.isFalse())
                .and(Tables.ADHERENT.WHEN_DONE.ge(Timestamp.valueOf(startOfMonth)))
                .fetchOne(0, Long.class);

            // Adhérents mois dernier
            Long adherentsMoisDernier = con.connection()
                .selectCount()
                .from(Tables.ADHERENT)
                .where(Tables.ADHERENT.ON_DELETED.isFalse())
                .and(Tables.ADHERENT.WHEN_DONE.ge(Timestamp.valueOf(startOfLastMonth)))
                .and(Tables.ADHERENT.WHEN_DONE.lt(Timestamp.valueOf(startOfMonth)))
                .fetchOne(0, Long.class);

            // Remboursements ce mois
            Long remboursementsCeMois = con.connection()
                .selectCount()
                .from(Tables.REGLEMENT)
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .and(Tables.REGLEMENT.DATE_PAYEMENT.ge(Timestamp.valueOf(startOfMonth)))
                .fetchOne(0, Long.class);

            // Remboursements mois dernier
            Long remboursementsMoisDernier = con.connection()
                .selectCount()
                .from(Tables.REGLEMENT)
                .where(Tables.REGLEMENT.ON_DELETED.isFalse())
                .and(Tables.REGLEMENT.DATE_PAYEMENT.ge(Timestamp.valueOf(startOfLastMonth)))
                .and(Tables.REGLEMENT.DATE_PAYEMENT.lt(Timestamp.valueOf(startOfMonth)))
                .fetchOne(0, Long.class);

            Map<String, Object> data = new HashMap<>();
            
            // Calcul pourcentage adhérents
            double pctAdherents = 0;
            if (adherentsMoisDernier != null && adherentsMoisDernier > 0) {
                pctAdherents = ((adherentsCeMois != null ? adherentsCeMois : 0) - adherentsMoisDernier) * 100.0 / adherentsMoisDernier;
            }
            
            // Calcul pourcentage remboursements
            double pctRemboursements = 0;
            if (remboursementsMoisDernier != null && remboursementsMoisDernier > 0) {
                pctRemboursements = ((remboursementsCeMois != null ? remboursementsCeMois : 0) - remboursementsMoisDernier) * 100.0 / remboursementsMoisDernier;
            }

            data.put("adherentsCeMois", adherentsCeMois != null ? adherentsCeMois : 0L);
            data.put("adherentsMoisDernier", adherentsMoisDernier != null ? adherentsMoisDernier : 0L);
            data.put("pctAdherents", Math.round(pctAdherents * 10.0) / 10.0);
            
            data.put("remboursementsCeMois", remboursementsCeMois != null ? remboursementsCeMois : 0L);
            data.put("remboursementsMoisDernier", remboursementsMoisDernier != null ? remboursementsMoisDernier : 0L);
            data.put("pctRemboursements", Math.round(pctRemboursements * 10.0) / 10.0);

            return data;
        } finally {
            con.connection().close();
        }
    }
}
