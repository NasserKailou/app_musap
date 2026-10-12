-- =====================================================================
-- Prise en charge des retraités - 2026-10-12
-- Script idempotent ; également appliqué au démarrage par
-- services.CotisationServices (tables) et services.RetraiteServices (vue).
--
-- Règles (paramétrables dans Paramètrages > Taux de cotisation) :
--  * 55 à 59 ans : part de base de la cotisation 5 % (taux PRE_RETRAITE) au lieu de 2,5 %,
--    pour financer la couverture après la retraite. Le plafond n'augmente PAS.
--  * 60 à 64 ans : retraité, seul l'adhérent est couvert (ayants droit archivés),
--    plafond annuel fixe 500 000 (seuil d'alerte 250 000), cotisation 0 % (taux RETRAITE).
--  * 65 ans et plus : fin de couverture, adhérent archivé, aucune prestation.
-- =====================================================================

CREATE TABLE IF NOT EXISTS parametre_retraite (
    id                    INTEGER PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    age_pre_retraite      INTEGER NOT NULL DEFAULT 55,
    age_retraite          INTEGER NOT NULL DEFAULT 60,
    age_fin_couverture    INTEGER NOT NULL DEFAULT 65,
    plafond_retraite      BIGINT  NOT NULL DEFAULT 500000,
    seuil_alerte_retraite BIGINT  NOT NULL DEFAULT 250000,
    who_done              VARCHAR(100),
    when_done             TIMESTAMP DEFAULT now()
);
INSERT INTO parametre_retraite (id, who_done) VALUES (1, 'initialisation') ON CONFLICT (id) DO NOTHING;

INSERT INTO taux_cotisation (code, libelle, taux, ordre, who_done) VALUES
    ('PRE_RETRAITE', 'Adhérent de 55 à 59 ans (pré-retraite)', 5.0, 0, 'initialisation'),
    ('RETRAITE',     'Adhérent retraité (60 ans et plus)',     0.0, 0, 'initialisation')
ON CONFLICT (code) DO NOTHING;

-- Journal des passages à la retraite / fins de couverture
CREATE TABLE IF NOT EXISTS retraite_journal (
    id        BIGSERIAL PRIMARY KEY,
    adherent  BIGINT NOT NULL REFERENCES adherent(id),
    action    VARCHAR(40) NOT NULL,      -- ARCHIVAGE_AYANTS_DROIT | FIN_COUVERTURE | COTISATION
    details   VARCHAR(500),
    who_done  VARCHAR(100),
    when_done TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_retraite_journal_adherent ON retraite_journal(adherent);

-- ---------------------------------------------------------------------
-- Vue v_adherent : mêmes colonnes, plafond selon l'âge.
--  * moins de 60 ans : salaire x (part de base ADHERENT + parts des ayants droit actifs) x 30 (alerte)
--    et x 60 (plafond) : identique à l'ancien calcul, sans la surcotisation de pré-retraite ;
--  * 60 à 64 ans : seuil et plafond fixes des retraités ;
--  * 65 ans et plus : 0 (aucune prestation).
-- Ancienne formule : salaire_net * pourcentage_total_retenue / 100 * 3 * 10 (x 2 pour le plafond).
-- ---------------------------------------------------------------------
CREATE OR REPLACE VIEW v_adherent AS
 SELECT ad.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.sexe,
    ad.matricule,
    ad.code_carnet,
    (cat.code::text || '-'::text) || cat.libelle::text AS categorie,
    ad.fonction,
    ad.structure_sigle,
    ad.date_naiss,
    date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_naiss::date::timestamp with time zone))::integer AS age,
    ad.date_prise_service,
    date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_prise_service::date::timestamp with time zone))::integer AS anciennete,
    ad.salaire_net,
    ad.pourcentage_total_retenue,
    ad.is_member,
    ad.email,
    ad.salaire_net::double precision * ad.pourcentage_total_retenue / 100::double precision AS total_cotisation_agent,
    CASE
        WHEN date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_naiss::date::timestamp with time zone)) >= COALESCE(pr.age_fin_couverture, 65) THEN 0::double precision
        WHEN date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_naiss::date::timestamp with time zone)) >= COALESCE(pr.age_retraite, 60) THEN COALESCE(pr.seuil_alerte_retraite, 250000)::double precision
        ELSE ad.salaire_net::double precision * (COALESCE(tad.taux, 2.5)::double precision + COALESCE(( SELECT sum(ay.pourcentage_retenue) FROM ayant_droit ay WHERE ay.adherent = ad.id AND COALESCE(ay.on_deleted, false) = false), 0::double precision)) / 100::double precision * 3::double precision * 10::double precision
    END AS total_credit_annuelle,
    CASE
        WHEN date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_naiss::date::timestamp with time zone)) >= COALESCE(pr.age_fin_couverture, 65) THEN 0::double precision
        WHEN date_part('year'::text, age(CURRENT_DATE::timestamp with time zone, ad.date_naiss::date::timestamp with time zone)) >= COALESCE(pr.age_retraite, 60) THEN COALESCE(pr.plafond_retraite, 500000)::double precision
        ELSE ad.salaire_net::double precision * (COALESCE(tad.taux, 2.5)::double precision + COALESCE(( SELECT sum(ay.pourcentage_retenue) FROM ayant_droit ay WHERE ay.adherent = ad.id AND COALESCE(ay.on_deleted, false) = false), 0::double precision)) / 100::double precision * 3::double precision * 10::double precision * 2::double precision
    END AS total_credit_a_consomer,
    COALESCE(sum(reg.montant_total), 0::numeric) AS montant_total_brute,
    COALESCE(sum(reg.montant_reglement), 0::numeric) AS montant_reglement_couvert,
    COALESCE(sum(reg.montant_paye), 0::numeric) AS montant_payer,
    ad.picture,
    ad.when_done,
    ad.who_done,
    ad.on_deleted,
    dir.libelle AS direction,
    dvi.libelle AS division,
    serv.libelle AS service,
    ad.telephone
   FROM adherent ad
     JOIN categorie cat ON cat.code::text = ad.categorie::text
     JOIN direction dir ON dir.id = ad.direction
     JOIN division dvi ON dvi.id = ad.division
     JOIN service serv ON serv.id = ad.service
     LEFT JOIN v_reglement reg ON reg.id_adherent = ad.id
     LEFT JOIN parametre_retraite pr ON pr.id = 1
     LEFT JOIN taux_cotisation tad ON tad.code::text = 'ADHERENT'::text
  GROUP BY ad.id, ad.nom_ad, ad.prenom_ad, ad.sexe, ad.matricule, ad.code_carnet, cat.code, cat.libelle, ad.fonction, ad.structure_sigle, ad.date_naiss, ad.date_prise_service, ad.salaire_net, ad.pourcentage_total_retenue, ad.picture, ad.when_done, ad.who_done, ad.on_deleted, dir.libelle, dvi.libelle, serv.libelle, ad.telephone,
    pr.age_fin_couverture, pr.age_retraite, pr.seuil_alerte_retraite, pr.plafond_retraite, tad.taux;
