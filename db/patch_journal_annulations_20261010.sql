-- =====================================================================
-- Journal des annulations / restaurations des bons et prises en charge
-- 2026-10-10 - script idempotent (table aussi créée au démarrage par
-- services.RapportOperationsServices)
-- =====================================================================
CREATE TABLE IF NOT EXISTS reglement_annulation (
    id        BIGSERIAL PRIMARY KEY,
    reglement BIGINT NOT NULL REFERENCES reglement(id),
    action    VARCHAR(20) NOT NULL,          -- ANNULATION | RESTAURATION
    motif     VARCHAR(500),
    who_done  VARCHAR(100),                  -- login de l'utilisateur
    when_done TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_reglement_annulation_reglement ON reglement_annulation(reglement);
