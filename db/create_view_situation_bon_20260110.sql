CREATE OR REPLACE VIEW public.v_nbr_total_bon as  select count(id) from v_reglement where v_reglement.on_deleted is false;
CREATE OR REPLACE VIEW public.v_nbr_bon_en_instance as  select count(id) from v_reglement where v_reglement.is_confirmed_bon is false and v_reglement.on_deleted is false;
CREATE OR REPLACE VIEW public.v_nbr_bon_valider as  select count(id) from v_reglement where v_reglement.is_confirmed_bon is true and v_reglement.on_deleted is false;

CREATE OR REPLACE VIEW public.v_nbr_bon_expirer as SELECT COUNT(id)
FROM v_reglement
WHERE is_confirmed_bon = false and v_reglement.on_deleted is false
  AND date_expiration <= CURRENT_DATE;

ALTER TABLE reglement
ADD COLUMN is_remboursement BOOLEAN NOT NULL DEFAULT false;

