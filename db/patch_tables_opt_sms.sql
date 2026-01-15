CREATE TABLE IF NOT EXISTS otp_message (
    id BIGSERIAL PRIMARY KEY,

    phone VARCHAR(20) NOT NULL,               -- Numéro du destinataire
    code VARCHAR(10) NOT NULL,                -- Code OTP généré
    message TEXT NOT NULL,                    -- Message SMS envoyé

    bon_commande BIGINT,                      -- Numéro du bon / prise en charge

    created_at TIMESTAMP DEFAULT NOW(),       -- Date de génération
    expires_at TIMESTAMP,                     -- Validité OTP

    is_used BOOLEAN DEFAULT FALSE,            -- Code déjà utilisé ?
    is_sent BOOLEAN DEFAULT FALSE,            -- SMS envoyé avec succès ?
    sent_response TEXT,                       -- Réponse API Sawki (JSON brut)

    attempt_count INT DEFAULT 0,              -- Tentatives de validation

    context VARCHAR(50)                       -- login, bon, prise_en_charge, etc.
);
