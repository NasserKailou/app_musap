CREATE OR REPLACE VIEW public.v_reglement_details
 AS
 select det.id,det.reglement, det.intitule, det.montant,det.who_done,det.when_done, det.on_deleted, det.last_update,
reg.id as id_reg, reg.nom_ad, reg.prenom_ad,reg.matricule, reg.structure, reg.couverture, reg.date_payement
 from public.reglement_detail as det inner join public.v_reglement as reg
 on det.reglement = reg.id