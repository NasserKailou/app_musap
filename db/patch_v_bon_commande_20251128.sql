DROP VIEW public.v_reglement_details;
DROP VIEW public.v_bon_de_commande;

CREATE OR REPLACE VIEW public.v_bon_de_commande AS
SELECT 
    reg.id,
    ad.nom_ad,
    ad.prenom_ad,
    ad.id AS id_adherent,
    ad.matricule,
	ad.code_carnet,
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

    -- =====================================
    -- 1) TAUX EFFECTIF UNIQUE (PUBLIC/PRIVE)
    -- =====================================
    COALESCE(
        CASE
            WHEN upper(trim(both FROM struc.statut_structure)) = 'PUBLIC' THEN ty.taux_public
            WHEN upper(trim(both FROM struc.statut_structure)) = 'PRIVE'  THEN ty.taux_prive
            ELSE ty.couverture
        END,
        ty.couverture
    )::bigint AS taux_effectif,

    struc.type_structure,
    struc.statut_structure,

    ay.nom_ay,
    ay.lien,
    ay.genre,
    reg.ayant_droit,

    -- =====================
    -- 2) MONTANT TOTAL
    -- =====================
    COALESCE((
        SELECT SUM(COALESCE(rd.montant, 0))
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id 
          AND rd.on_deleted IS FALSE
    ), 0)::bigint AS montant_total,

    -- ==============================
    -- 3) MONTANT RÉGLEMENT (Mutuelle)
    -- ==============================
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0)::numeric *
            (
                COALESCE(
                    CASE
                        WHEN upper(trim(both FROM struc.statut_structure)) = 'PUBLIC' 
                            THEN ty.taux_public::numeric
                        WHEN upper(trim(both FROM struc.statut_structure)) = 'PRIVE' 
                            THEN ty.taux_prive::numeric
                        ELSE ty.couverture::numeric
                    END,
                    ty.couverture::numeric
                ) / 100
            )
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id 
          AND rd.on_deleted IS FALSE
    ), 0)::bigint AS montant_reglement,

    -- ==============================
    -- 4) MONTANT PAYÉ PAR ADHÉRENT
    -- ==============================
    COALESCE((
        SELECT SUM(
            COALESCE(rd.montant, 0)::numeric *
            (
                (100::numeric -
                    COALESCE(
                        CASE
                            WHEN upper(trim(both FROM struc.statut_structure)) = 'PUBLIC' 
                                THEN ty.taux_public::numeric
                            WHEN upper(trim(both FROM struc.statut_structure)) = 'PRIVE' 
                                THEN ty.taux_prive::numeric
                            ELSE ty.couverture::numeric
                        END,
                        ty.couverture::numeric
                    )
                ) / 100
            )
        )
        FROM reglement_detail rd
        WHERE rd.reglement = reg.id 
          AND rd.on_deleted IS FALSE
    ), 0)::bigint AS montant_paye,

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

ORDER BY reg.id;


CREATE OR REPLACE VIEW public.v_reglement_details
 AS
 SELECT det.id,
    det.reglement,
    det.intitule,
    det.montant,
    det.quantite,
    det.prix_unitaire,
    det.who_done,
    det.when_done,
    det.on_deleted,
    det.last_update,
    reg.ref_facture,
    reg.id AS id_bon,
    reg.nom_ad,
    reg.prenom_ad,
    reg.matricule,
    reg.structure,
    reg.taux_effectif,
    reg.date_payement,
    reg.prestation,
    reg.lien,
    reg.is_confirmed_bon,
    reg.when_confirmed_bon,
    reg.date_expiration,
    COALESCE(det.montant, 0::bigint) * det.quantite * reg.taux_effectif::bigint / 100 AS total_couvert,
    COALESCE(det.montant, 0::bigint) * det.quantite * (100 - reg.taux_effectif::integer)::bigint / 100 AS total_restant
   FROM reglement_detail det
     JOIN v_bon_de_commande reg ON det.reglement = reg.id;