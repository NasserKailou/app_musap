# Instructions pour Mettre à Jour les Vues

## 1. Modification de rembourssement.scala.html

### Ligne ~234-248 : Commenter le champ "Ref Facture"

**Remplacer:**
```html
<!-- Ref Facture -->
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
```

**Par:**
```html
@* Ref Facture - CACHÉ en mode CREATE (Étape 1: Émission du bon) *@
@* Ce champ sera renseigné lors de la confirmation (Étape 2) *@
@*
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
*@
```

### Ligne ~329-364 : Commenter toute la section "Informations Complémentaires"

**Commenter cette section entière:**
```html
<!-- Informations Complémentaires -->
<div class="card card-secondary">
  <div class="card-header">
    <h3 class="card-title">Informations Complémentaires (Si remboursement en espèces)</h3>
  </div>
  <div class="card-body">
    ...
  </div>
</div>
```

**Avec:**
```html
@* === SECTION COMMENTÉE POUR LES BONS DE COMMANDE ===
<!-- Informations Complémentaires -->
<div class="card card-secondary">
  ...
</div>
*@
```

---

## 2. Modification de adherents.scala.html

### Ligne ~570-583 : Différencier les boutons Bleu et Vert

**Trouver cette section:**
```html
<td class="text-center">
  <a class="btn btn-primary btn-sm"
     title="Ajouter un Bon de commande"
     href='@controllers.routes.AdherentCtrl.addRembourssement(c.getId())'>
    <i class="fas fa-file-invoice"></i>
  </a>
</td>
<td class="text-center">
  <a class="btn btn-success btn-sm"
     title="Remboursement"
     href='@controllers.routes.AdherentCtrl.addRembourssement(c.getId())'>
    <i class="fas fa-money-bill-wave"></i>
  </a>
</td>
```

**Remplacer par:**
```html
@* BOUTON BLEU: Émission du bon de commande *@
<td class="text-center">
  <a class="btn btn-primary btn-sm"
     title="Émettre un Bon de commande"
     href='@controllers.routes.ReglementCtrl.show("CREATE", 0L, c.getId())'>
    <i class="fas fa-file-invoice"></i>
  </a>
</td>

@* BOUTON VERT: Confirmation du bon (avec justificatifs) *@
<td class="text-center">
  <a class="btn btn-success btn-sm"
     title="Confirmer un Bon (avec justificatifs)"
     href='@controllers.routes.ReglementCtrl.showConfirmBon("CONFIRM_BON", c.getId())'>
    <i class="fas fa-check-circle"></i>
  </a>
</td>
```

---

## 3. Modification de ReglementCtrl.java

### Remplacer la méthode save() (ligne ~390-478)

**Utiliser le code dans:** `app/controllers/ReglementCtrl_save_method_new.java`

### Ajouter la nouvelle méthode showConfirmBon()

**Ajouter après la méthode save():**

Utiliser le code dans: `app/controllers/ReglementCtrl_confirm_method.java`

---

## 4. Modification de ReglementMainServices.java

### Ajouter la méthode findReglementNonConfirmesByAdherent()

**Ajouter à la fin de la classe (avant le dernier }):**

Utiliser le code dans: `app/services/ReglementMainServices_confirm_method.java`

---

## 5. Ajouter la route dans conf/routes

**Ajouter cette ligne dans la section "Reglement Ressources":**

```
GET    /Reglement/confirm-bon/:subAction/    controllers.ReglementCtrl.showConfirmBon(subAction: String, idAdherent: Long, request:Request)
```

---

## 6. La vue confirmationBon.scala.html

**Fichier déjà créé:** `app/views/confirmationBon.scala.html`

Ce fichier est prêt à être utilisé tel quel.

---

## Ordre d'Application des Modifications

1. Modifier `conf/routes` (ajouter la route)
2. Modifier `app/services/ReglementMainServices.java` (ajouter la méthode)
3. Modifier `app/controllers/ReglementCtrl.java` (remplacer save() + ajouter showConfirmBon())
4. Vérifier que `app/views/confirmationBon.scala.html` est bien créé
5. Modifier `app/views/rembourssement.scala.html` (commenter ref_facture et section complémentaire)
6. Modifier `app/views/adherents.scala.html` (changer les routes des boutons)

---

## Test du Workflow

### Test Étape 1: Émission du bon

1. Se connecter à l'application
2. Aller dans "Adhérents"
3. Cliquer sur le bouton BLEU d'un adhérent
4. Remplir le formulaire (sans ref_facture)
5. Valider
6. Vérifier le message: "Bon de commande émis avec succès. Validité: 30 jours"

### Test Étape 2: Confirmation du bon

1. Sur le même adhérent, cliquer sur le bouton VERT
2. Voir la liste des bons non confirmés
3. Cliquer sur "Confirmer" pour un bon
4. Ajouter la référence facture
5. Valider
6. Vérifier le message: "Bon de commande confirmé avec succès"
7. Le bon ne doit plus apparaître dans la liste des bons non confirmés

---

**Date:** 2025-11-21  
**Auteur:** Assistant IA
