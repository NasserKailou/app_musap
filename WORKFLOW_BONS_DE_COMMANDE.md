# Workflow de Gestion des Bons de Commande

## Vue d'ensemble

Ce document décrit le workflow en deux étapes pour la gestion des bons de commande dans l'application MUSAP.

## Processus en 2 Étapes

### ÉTAPE 1 : Émission du Bon de Commande (Bouton BLEU 🔵)

**Déclencheur** : L'adhérent se présente pour se faire soigner

**Action** : Cliquer sur le bouton bleu "Bon de Commande" dans la liste des adhérents

**Formulaire affiché** :
- ✅ Bénéficiaire (adhérent ou ayant droit)
- ✅ Type de prestation (hospitalisation, ordonnance, etc.)
- ✅ Structure partenaire
- ✅ Date de paiement (date système par défaut)
- ❌ **Ref Facture** - NON renseignée (cachée)
- ❌ **Structure émettrice** - NON renseignée (commentée/cachée)
- ❌ **Numéro** - NON renseigné (commenté/caché)

**Traitement à la sauvegarde** :
```java
c.setIsConfirmedBon(false);  // Bon NON confirmé
c.setRefFacture(null);       // Pas de référence facture
c.setStructureEmettriceRembourssement(null); // Pas de structure émettrice
c.setNumBon(null);           // Pas de numéro
// Calcul automatique de la date d'expiration
Timestamp dateExpiration = new Timestamp(c.getDatePayement().getTime() + (30L * 24 * 60 * 60 * 1000)); // +30 jours
```

**Résultat** :
- Bon de commande émis avec validité de **30 jours**
- Message : "Bon de commande émis avec succès. Validité: 30 jours"
- L'adhérent peut se faire soigner avec ce bon

---

### ÉTAPE 2 : Confirmation du Bon (Bouton VERT 🟢)

**Déclencheur** : L'adhérent ramène les justificatifs (facture)

**Action** : Cliquer sur le bouton vert "Règlement" dans la liste des adhérents

**Vue affichée** :
- Liste des bons NON confirmés (`is_confirmed_bon = false`) pour cet adhérent
- Table avec colonnes :
  - Bénéficiaire
  - Type de prestation
  - Structure
  - Date d'émission
  - Date d'expiration
  - Statut (badge "En attente")
  - Actions (bouton "Confirmer")

**Formulaire de confirmation** (modal ou page) :
- 📄 **Ref Facture** - OBLIGATOIRE
- 📅 **Date de règlement** - OBLIGATOIRE (date actuelle par défaut)
- Informations du bon (lecture seule)

**Traitement à la confirmation** :
```java
c.setIsConfirmedBon(true);  // Bon CONFIRMÉ
c.setWhenConfirmedBon(new Timestamp(System.currentTimeMillis())); // Date de confirmation
c.setRefFacture(refFactureFromForm); // Ajout de la référence facture
c.setDatePayement(dateReglementFromForm); // Mise à jour de la date de paiement
```

**Résultat** :
- Bon de commande confirmé
- Message : "Bon de commande confirmé avec succès"
- Le bon passe en statut "Confirmé" (consommé)

---

## Modifications Techniques Requises

### 1. Base de Données

**Ajouter la colonne `date_expiration`** (déjà créé : `db/patch_add_date_expiration.sql`) :
```sql
ALTER TABLE reglement 
ADD COLUMN IF NOT EXISTS date_expiration TIMESTAMP;
```

### 2. Modèle Java (Reglement.java)

Ajouter le champ (à regénérer avec jOOQ) :
```java
private Timestamp dateExpiration;
```

### 3. Contrôleur (ReglementCtrl.java)

#### Méthode `save()` - Ajouter gestion des modes :

```java
public Result save(Request request) {
    final String viewMode = formFactory.form().bindFromRequest(request).get("viewMode");
    Form<Reglement> uForm = formFactory.form(Reglement.class).bindFromRequest(request);
    String dateReglement = formFactory.form().bindFromRequest(request).get("tmpDate");
    Long benef = Long.parseLong(formFactory.form().bindFromRequest(request).get("benef"));
    
    Reglement c = uForm.get();
    c.setWhenDone(new Timestamp(System.currentTimeMillis()));
    c.setOnDeleted(false);
    c.setWhoDone(String.valueOf(request.session().get("login").get()));
    
    // Définir la date de paiement
    if (!String.valueOf(dateReglement.substring(0, 4))
            .equals(String.valueOf(request.session().get("gestion").get())))
        c.setDatePayement(regServices.getDateT(request.session().get("gestion").get() + "-12-31"));
    else
        c.setDatePayement(regServices.getDateT(dateReglement));
    
    // Gérer le bénéficiaire
    if (!benef.equals(c.getAdherent())) {
        c.setAyantDroit(benef);
    }
    
    // === ÉTAPE 1 : ÉMISSION DU BON ===
    if (viewMode.equals(ViewMode.VIEW_MODE_CREATE)) {
        c.setIsConfirmedBon(false);  // Bon NON confirmé
        c.setRefFacture(null);       // Pas de référence facture
        c.setStructureEmettriceRembourssement(null);
        c.setNumBon(null);
        
        // Calculer date d'expiration (+30 jours)
        long thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000;
        Timestamp dateExpiration = new Timestamp(c.getDatePayement().getTime() + thirtyDaysInMillis);
        // Note: Ajouter quand le modèle sera mis à jour
        // c.setDateExpiration(dateExpiration);
        
        if (regServices.saveLogical(c, true).equals("ok")) {
            return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", "Bon de commande émis avec succès. Validité: 30 jours");
        } else {
            return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", "Erreur lors de l'émission du bon");
        }
    }
    
    // === ÉTAPE 2 : CONFIRMATION DU BON ===
    else if (viewMode.equals("CONFIRM_BON")) {
        c.setIsConfirmedBon(true);  // Bon CONFIRMÉ
        c.setWhenConfirmedBon(new Timestamp(System.currentTimeMillis()));
        // ref_facture et date_payement sont déjà dans c via bindFromRequest
        
        if (regServices.saveLogical(c, false).equals("ok")) {
            return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("success", "Bon de commande confirmé avec succès");
        } else {
            return redirect(routes.ReglementCtrl.show(ViewMode.VIEW_MODE_CREATE, 0L, c.getAdherent()))
                    .flashing("error", "Erreur lors de la confirmation");
        }
    }
    
    // ... reste du code EDIT, DELETE, etc.
}
```

#### Nouvelle méthode `showConfirmBon()` :

```java
public Result showConfirmBon(String subAction, Long idAdherent, Request request) {
    // Récupérer les bons NON confirmés pour cet adhérent
    List<VReglement> bonsNonConfirmes = regServices.findReglementNonConfirmesByAdherent(idAdherent);
    
    return ok(views.html.confirmationBon.render(
        bonsNonConfirmes,
        adherentService.findById(idAdherent),
        request
    ));
}
```

### 4. Service (ReglementMainServices.java)

Ajouter méthode pour récupérer les bons non confirmés :

```java
public List<VReglement> findReglementNonConfirmesByAdherent(Long idAdherent) {
    List<VReglement> bons = con.connection()
        .selectFrom(V_REGLEMENT)
        .where(V_REGLEMENT.ON_DELETED.isFalse())
        .and(V_REGLEMENT.ID_ADHERENT.eq(idAdherent))
        .and(V_REGLEMENT.IS_CONFIRMED_BON.isFalse()
            .or(V_REGLEMENT.IS_CONFIRMED_BON.isNull()))
        .fetchInto(VReglement.class);
    con.connection().close();
    return bons;
}
```

### 5. Vue (rembourssement.scala.html)

Modifier le formulaire pour cacher les champs selon le mode :

```html
<!-- Ref Facture - CACHÉ en mode CREATE, VISIBLE en mode CONFIRM_BON -->
@if(!viewMode.equals("CREATE")) {
<div class="col-md-3">
  <div class="form-group">
    <label>
      <i class="fas fa-file-alt"></i> Ref Facture 
      <span class="text-danger">*</span>
    </label>
    <input type="text"
           id="refFacture"
           name="refFacture"
           class="form-control"
           placeholder="Référence de la facture"
           required>
  </div>
</div>
}

<!-- Structure Émettrice et Téléphone - TOUJOURS CACHÉS -->
@* Commenté - Non utilisé pour les bons de commande
<div class="col-md-6">
  <div class="form-group">
    <label>Structure émettrice</label>
    <input type="text" name="structureEmettriceRembourssement" class="form-control">
  </div>
</div>
*@
```

### 6. Vue (adherents.scala.html)

Différencier les deux boutons :

```html
<!-- BOUTON BLEU : Émission du bon -->
<td class="text-center">
  <a class="btn btn-primary btn-sm"
     title="Émettre un Bon de commande"
     href='@controllers.routes.ReglementCtrl.show("CREATE", 0L, c.getId())'>
    <i class="fas fa-file-invoice"></i>
  </a>
</td>

<!-- BOUTON VERT : Confirmation du bon -->
<td class="text-center">
  <a class="btn btn-success btn-sm"
     title="Confirmer un Bon (avec justificatifs)"
     href='@controllers.routes.ReglementCtrl.showConfirmBon("CONFIRM_BON", c.getId())'>
    <i class="fas fa-check-circle"></i>
  </a>
</td>
```

### 7. Nouvelle Vue (confirmationBon.scala.html)

Créer une nouvelle vue pour afficher la liste des bons non confirmés avec formulaire de confirmation.

---

## Routes à ajouter

```
# Confirmation des bons de commande
GET    /Reglement/confirm-bon/:subAction/    controllers.ReglementCtrl.showConfirmBon(subAction: String, idAdherent: Long, request:Request)
```

---

## Règles de Gestion

1. **Un bon ne peut être confirmé qu'une seule fois** (`is_confirmed_bon` passe de `false` à `true`)
2. **La date d'expiration est de 30 jours** à partir de la date d'émission
3. **La ref_facture est obligatoire** uniquement lors de la confirmation
4. **Les champs structure émettrice et numéro ne sont jamais renseignés** dans ce workflow
5. **Un bon expiré peut toujours être confirmé** (pas de validation d'expiration pour le moment)

---

## Tests à Effectuer

1. ✅ Créer un bon de commande sans ref_facture → OK
2. ✅ Vérifier que `is_confirmed_bon = false` après création
3. ✅ Vérifier le calcul de `date_expiration` (+30 jours)
4. ✅ Afficher la liste des bons non confirmés
5. ✅ Confirmer un bon avec ajout de ref_facture → `is_confirmed_bon = true`
6. ✅ Vérifier que `when_confirmed_bon` est renseigné
7. ✅ Le bon confirmé ne doit plus apparaître dans la liste des bons à confirmer

---

## Fichiers Modifiés

- ✅ `db/patch_add_date_expiration.sql` - Script SQL
- ⏳ `app/models/tables/pojos/Reglement.java` - À regénérer avec jOOQ
- ⏳ `app/controllers/ReglementCtrl.java` - Logique métier
- ⏳ `app/services/ReglementMainServices.java` - Requêtes BDD
- ⏳ `app/views/rembourssement.scala.html` - Formulaire adaptatif
- ⏳ `app/views/adherents.scala.html` - Boutons différenciés
- ⏳ `app/views/confirmationBon.scala.html` - Nouvelle vue (à créer)
- ⏳ `conf/routes` - Nouvelle route

---

## Prochaines Étapes

1. Exécuter le script SQL `patch_add_date_expiration.sql`
2. Regénérer les modèles jOOQ
3. Implémenter les modifications dans `ReglementCtrl.java`
4. Adapter les vues Scala
5. Tester le workflow complet
6. Documenter dans le manuel utilisateur

---

**Date de création** : 2025-11-21  
**Auteur** : Assistant IA - Implémentation MUSAP  
**Version** : 1.0
