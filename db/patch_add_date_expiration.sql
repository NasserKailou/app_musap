-- Ajout de la colonne date_expiration à la table reglement
-- Cette colonne stocke la date d'expiration du bon de commande (30 jours après l'émission)

ALTER TABLE reglement 
ADD COLUMN IF NOT EXISTS date_expiration TIMESTAMP;

COMMENT ON COLUMN reglement.date_expiration IS 'Date d''expiration du bon de commande (30 jours après l''émission)';

-- Mise à jour des bons existants : date_expiration = date_payement + 30 jours
UPDATE reglement 
SET date_expiration = date_payement + INTERVAL '30 days'
WHERE date_expiration IS NULL AND date_payement IS NOT NULL;

-- Pour les bons sans date_payement, utiliser when_done + 30 jours
UPDATE reglement 
SET date_expiration = when_done + INTERVAL '30 days'
WHERE date_expiration IS NULL AND when_done IS NOT NULL;
