-- =====================================================
-- Table: etude_consommations
-- Description: Stocke les données de consommation des bons de commande
-- =====================================================

CREATE TABLE etude_consommations (
    id BIGSERIAL PRIMARY KEY,
    
    -- Informations de base
    numero_carnet VARCHAR(50),
    code_agent VARCHAR(50),
    nom_prenoms VARCHAR(255),
    
    -- Charges familiales
    agent DECIMAL(10,2),
    conjoint DECIMAL(10,2),
    enfants DECIMAL(10,2),
    pere DECIMAL(10,2),
    mere DECIMAL(10,2),
    
    -- Informations salariales
    salaire_base DECIMAL(15,2),
    pourcentage_retenir DECIMAL(5,2),
    retenue_mensuelle DECIMAL(15,2),
    credit_annuel DECIMAL(15,2),
    credit_annuel_x2 DECIMAL(15,2),
    
    -- Consommations
    consommation_bons DECIMAL(15,2),
    consommation_pc DECIMAL(15,2),
    remboursement DECIMAL(15,2),
    total_consommation DECIMAL(15,2),
    
    -- Soldes
    solde_1 DECIMAL(15,2),
    solde_2 DECIMAL(15,2),
    
    -- Statut
    statut_avertissement VARCHAR(50),
    statut_suspension VARCHAR(50),
    
    -- Métadonnées
    fichier_source VARCHAR(255),
    date_import TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    utilisateur_import VARCHAR(100),
    
    -- Index pour améliorer les performances
    CONSTRAINT uk_code_agent UNIQUE(code_agent)
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_nom_prenoms ON etude_consommations(nom_prenoms);
CREATE INDEX idx_numero_carnet ON etude_consommations(numero_carnet);
CREATE INDEX idx_statut_avertissement ON etude_consommations(statut_avertissement);
CREATE INDEX idx_statut_suspension ON etude_consommations(statut_suspension);
CREATE INDEX idx_date_import ON etude_consommations(date_import);

-- Commentaires sur les colonnes
COMMENT ON TABLE etude_consommations IS 'Table stockant les données d''étude de consommations des bons de commande';
COMMENT ON COLUMN etude_consommations.numero_carnet IS 'Numéro du carnet de l''adhérent';
COMMENT ON COLUMN etude_consommations.code_agent IS 'Code unique de l''agent';
COMMENT ON COLUMN etude_consommations.salaire_base IS 'Salaire de base de l''agent';
COMMENT ON COLUMN etude_consommations.total_consommation IS 'Total des consommations (Bons + PC + Remboursement)';
COMMENT ON COLUMN etude_consommations.statut_avertissement IS 'Statut d''avertissement (AVERTIS, etc.)';
COMMENT ON COLUMN etude_consommations.statut_suspension IS 'Statut de suspension (SUSPENDUS, etc.)';
