# Instructions de Redémarrage de l'Application

## 🔧 Problème Identifié

L'erreur 404 sur `/api/stats/reglements-structure` indique que l'application Play Framework n'a pas rechargé la nouvelle route.

**Tout le code est correct :**
- ✅ Route existe dans `conf/routes` (ligne 12)
- ✅ Méthode `getStatsReglementsByStructure()` existe dans `HomeController.java`
- ✅ Service `VBonDeCommandeServices` existe et est injecté
- ✅ Tous les modèles jOOQ sont générés

## 🚀 Solution : Redémarrer l'Application

### Option 1 : Redémarrage complet (RECOMMANDÉ)

Si vous utilisez **sbt** en mode développement :

```bash
# 1. Arrêtez l'application actuelle (dans le terminal où elle tourne)
Ctrl+C

# 2. Redémarrez l'application
cd /home/user/webapp
sbt clean compile run
```

### Option 2 : Rechargement dans sbt

Si l'application tourne déjà dans une console sbt :

```bash
# Dans la console sbt, tapez :
reload
```

### Option 3 : Si vous utilisez un service systemd

```bash
sudo systemctl restart play-app
# ou le nom de votre service
```

### Option 4 : Si vous utilisez pm2

```bash
pm2 restart app-musap
# ou le nom de votre processus
```

## 🧪 Tester Après Redémarrage

### Test 1 : Test manuel avec curl

```bash
cd /home/user/webapp
./test_dashboard_api.sh
```

**Réponse attendue :** JSON avec les données (status 200)

```json
{
  "structures": [...],
  "bonsConfirmes": [...],
  "bonsNonConfirmes": [...],
  "totalAdherents": 123,
  "totalAyantsDroit": 456,
  ...
}
```

### Test 2 : Test dans le navigateur

1. Ouvrez DevTools (F12)
2. Allez dans l'onglet Network
3. Accédez à : `http://localhost:9000/accueil`
4. Cherchez la requête à `/api/stats/reglements-structure`
5. Vérifiez que le statut est **200 OK** (et non 404)

### Test 3 : Vérifier la console

Dans la console du navigateur, vous devriez voir :
```
Données reçues: {structures: Array(5), bonsConfirmes: Array(5), ...}
```

Et **PAS** :
```
Erreur lors du chargement des statistiques: Not Found
```

## 📊 Vérification de la Base de Données

Si après redémarrage le problème persiste, vérifiez que la vue existe :

```sql
-- Connectez-vous à votre base de données MySQL/MariaDB
SELECT COUNT(*) FROM v_bon_de_commande;

-- Si erreur "Table doesn't exist", exécutez les patches :
SOURCE /home/user/webapp/db/patch_global_view_20251121.sql;
```

## 🔍 Logs à Consulter

Après le démarrage, vérifiez les logs de l'application :

```bash
# Les logs devraient montrer :
[info] play.api.Play - Application started (Dev)
[info] play.core.server.AkkaHttpServer - Listening for HTTP on /0.0.0.0:9000

# Et PAS d'erreurs comme :
[error] c.HomeController - Cannot inject VBonDeCommandeServices
[error] Routes compilation error
```

## ⚠️ Problèmes Courants

### Problème : "Class not found: VBonDeCommandeServices"

**Solution :** Recompilez complètement
```bash
cd /home/user/webapp
sbt clean
sbt compile
sbt run
```

### Problème : "Route not found"

**Solution :** Vérifiez le fichier routes
```bash
# Vérifiez que la route existe :
grep "api/stats" conf/routes

# Devrait afficher :
# GET     /api/stats/reglements-structure    controllers.HomeController.getStatsReglementsByStructure(request:Request)
```

### Problème : "Error in SQL query"

**Solution :** Vérifiez la vue de base de données
```bash
# Connectez-vous à MySQL et exécutez :
SHOW CREATE VIEW v_bon_de_commande;

# Si la vue n'existe pas, exécutez les patches SQL dans db/
```

## ✅ Checklist de Vérification

Avant de dire que ça marche, vérifiez :

- [ ] L'application a redémarré sans erreur
- [ ] Le test curl retourne 200 (pas 404)
- [ ] Le dashboard s'affiche dans le navigateur
- [ ] Les cartes statistiques montrent des chiffres
- [ ] Le graphique en barres s'affiche
- [ ] Le graphique circulaire s'affiche
- [ ] Le tableau de données se charge
- [ ] Pas de messages d'erreur dans la console

## 📞 Si Ça Ne Marche Toujours Pas

Si après toutes ces étapes le problème persiste :

1. **Capturez les logs complets** au démarrage de l'application
2. **Testez manuellement** l'endpoint avec curl
3. **Vérifiez la base de données** que la vue `v_bon_de_commande` existe
4. **Vérifiez les permissions** que l'utilisateur de la BD peut accéder à la vue

Partagez les logs d'erreur pour un diagnostic plus approfondi.

---

**Date :** 2025-11-21  
**Branche :** genspark_ai_developer  
**Fichiers modifiés :** Aucun (tout est déjà en place)  
**Action requise :** Redémarrage de l'application uniquement
