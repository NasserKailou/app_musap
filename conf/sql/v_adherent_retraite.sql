-- Vue v_adherent avec plafonds selon l'âge (voir db/patch_retraite_20261012.sql)
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
