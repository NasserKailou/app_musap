CREATE OR REPLACE VIEW public.v_reg_bon_commande AS
SELECT
    reg.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.id AS id_adherent,
    ad.matricule,
    reg.ref_facture,
    struc.libelle AS structure,
    reg.date_payement,
    reg.who_done,
    reg.when_done,
    reg.is_confirmed_bon,
    reg.when_confirmed_bon,
    reg.date_expiration,
    reg.type_reglement,
    substring(reg.date_payement::varchar, 1, 4)::varchar AS annee,
    reg.on_deleted,
    ty.prestation,
    ty.couverture,
    ty.taux_public,
    ty.taux_prive,
    struc.type_structure,
    struc.statut_structure,
    ay.nom_ay,
    ay.lien,
    ay.genre,
    reg.ayant_droit,

    -- MONTANT TOTAL
    COALESCE((
        SELECT SUM(COALESCE(rd.montant, 0::bigint))
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_total,

    -- TAUX EFFECTIF
    COALESCE(
        CASE
            WHEN upper(trim(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public
            WHEN upper(trim(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive
            ELSE ty.couverture
        END,
        ty.couverture
    ) AS taux_couverture_effectif,

    -- MONTANT REGLEMENT
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0::bigint)::numeric
            * COALESCE(
                CASE
                    WHEN upper(trim(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public::numeric
                    WHEN upper(trim(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive::numeric
                    ELSE ty.couverture::numeric
                END,
                ty.couverture::numeric
            ) / 100::numeric
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_reglement,

    -- MONTANT PAYE (part adhérent)
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0::bigint)::numeric
            * (100::numeric - COALESCE(
                CASE
                    WHEN upper(trim(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public::numeric
                    WHEN upper(trim(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive::numeric
                    ELSE ty.couverture::numeric
                END,
                ty.couverture::numeric
            )) / 100::numeric
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_paye,

    struc.id AS id_structure

FROM
    structure_partenaire struc,
    adherent ad,
    type_prestation ty,
    reglement reg
    LEFT JOIN ayant_droit ay ON reg.ayant_droit = ay.id
WHERE
    reg.structure = struc.id
    AND reg.adherent = ad.id
    AND reg.type_prestation = ty.id
    AND reg.on_deleted IS FALSE
    AND upper(trim(struc.type_structure)) = 'PHARMACIE'
ORDER BY
    reg.id;


CREATE OR REPLACE VIEW public.v_reg_prise_en_charge AS
SELECT
    reg.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.id AS id_adherent,
    ad.matricule,
    reg.ref_facture,
    struc.libelle AS structure,
    reg.date_payement,
    reg.who_done,
    reg.when_done,
    reg.is_confirmed_bon,
    reg.when_confirmed_bon,
    reg.date_expiration,
    reg.type_reglement,
    substring(reg.date_payement::varchar, 1, 4)::varchar AS annee,
    reg.on_deleted,
    ty.prestation,
    ty.couverture,
    ty.taux_public,
    ty.taux_prive,
    struc.type_structure,
    struc.statut_structure,
    ay.nom_ay,
    ay.lien,
    ay.genre,
    reg.ayant_droit,

    -- MONTANT TOTAL
    COALESCE((
        SELECT SUM(COALESCE(rd.montant, 0::bigint))
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_total,

    -- TAUX EFFECTIF
    COALESCE(
        CASE
            WHEN UPPER(TRIM(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public
            WHEN UPPER(TRIM(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive
            ELSE ty.couverture
        END,
        ty.couverture
    ) AS taux_couverture_effectif,

    -- MONTANT REGLEMENT
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0::bigint)::numeric
            * COALESCE(
                CASE
                    WHEN UPPER(TRIM(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public::numeric
                    WHEN UPPER(TRIM(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive::numeric
                    ELSE ty.couverture::numeric
                END,
                ty.couverture::numeric
            ) / 100::numeric
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_reglement,

    -- MONTANT PAYE (part adhérent)
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0::bigint)::numeric
            * (100::numeric - COALESCE(
                CASE
                    WHEN UPPER(TRIM(struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public::numeric
                    WHEN UPPER(TRIM(struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive::numeric
                    ELSE ty.couverture::numeric
                END,
                ty.couverture::numeric
            )) / 100::numeric
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id
          AND rd.on_deleted IS FALSE
    ), 0::numeric)::bigint AS montant_paye,

    struc.id AS id_structure

FROM
    structure_partenaire struc,
    adherent ad,
    type_prestation ty,
    reglement reg
    LEFT JOIN ayant_droit ay ON reg.ayant_droit = ay.id
WHERE
    reg.structure = struc.id
    AND reg.adherent = ad.id
    AND reg.type_prestation = ty.id
    AND reg.on_deleted IS FALSE
    AND UPPER(TRIM(struc.type_structure)) IN ('HOPITAL', 'CLINIQUE', 'LABORATOIRE')
ORDER BY
    reg.id;


