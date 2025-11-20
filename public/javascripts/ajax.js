window.addEventListener('load', function () {


    // Initialisation DataTable sur ton tableau
    $("#reglementDetailsTable").DataTable({
        responsive: true,
        lengthChange: false,
        autoWidth: false,
        pagingType: "full_numbers",
        language: {
            url: "//cdn.datatables.net/plug-ins/1.13.7/i18n/fr-FR.json"
        },
        columnDefs: [
            { targets: -1, className: "text-center" } // colonne Actions centrée
        ]
    });

    // Gestion du bouton AJOUTER (si nécessaire)
    $("[data-target='#modal-detail-reglement']").on("click", function () {
        console.log("Clique sur AJOUTER");

        $("#modal-title-detail").text("Ajouter un détail");
        $("#viewMode").val("CREATE");

        $("#detailId").val("");
        $("#intitule").val("");
        $("#montant").val("");
    });

    // Gestion du bouton MODIFIER (ouvrir le modal + charger les données)
    $(document).on("click", ".btn-edit-detail", function (e) {
        e.preventDefault();

        var id = $(this).data("id");
        console.log("Clique sur Modifier, id = " + id);

        $("#modal-title-detail").text("Modifier un détail");
        $("#viewMode").val("EDIT");
        $("#detailId").val(id);

        // ouvrir d'abord le modal
        $("#modal-detail-reglement").modal("show");

        // vider les champs
        $("#intitule").val("");
        $("#montant").val("");

        // URL JSON : adapte si besoin selon ta route
        var url = "/reglement/detail/json/" + id;

        $.ajax({
            url: url,
            type: "GET",
            dataType: "json",
            success: function (data) {
                console.log("Détail reçu", data);

                $("#detailId").val(data.id);
                $("#intitule").val(data.intitule);
                $("#montant").val(data.montant);
            },
            error: function (xhr, status, error) {
                console.error("Erreur AJAX : ", status, error);
                alert("Erreur lors du chargement du détail (regarde la console F12).");
            }
        });
    });

});

  function openPopup(target, x, y, w, h) {
    var win = window.open(
      target,
      'popup',
      'height=' + h + ', width=' + w + ', top=' + y + ', left=' + x +
      ', toolbar=no, menubar=yes, location=no, resizable=yes, scrollbars=yes, status=no'
    );
    win.window.focus();
    return false;
  }

  $(function () {

    // ====== NOUVEAU BON DE COMMANDE ======
    $('#btn-nouveau-bc').on('click', function () {
      var $form = $('form[name="form"]');

      // On reset tous les champs du formulaire
      $form[0].reset();

      // On remet les champs cachés
      $form.find('input[name="viewMode"]').val('CREATE');
      $form.find('input[name="id"]').val(''); // pas d'id en CREATE

      // Optionnel : mettre le bénéficiaire par défaut sur l’adhérent
      $form.find('#benef').val('@adherent.getId()');

      // Titre du modal
      $('#reglementModalLabel').text('Nouveau Bon de Commande');

      // Boutons : on s’assure que seul le bouton Enregistrer (CREATE) est visible
      // (si tu veux vraiment gérer visibilité par JS, tu peux donner des id aux boutons)
      $('#reglementModal').modal('show');
    });

    // ====== MODIFIER BON DE COMMANDE (AJAX) ======
    $('.btn-edit-reglement').on('click', function (e) {
      e.preventDefault();

      var idReglement = $(this).data('id');
      var $form = $('form[name="form"]');

      $.ajax({
        url: '@controllers.routes.ReglementCtrl.getReglementJson(0L)'.toString().replace('0', idReglement),
        method: 'GET',
        dataType: 'json',
        success: function (data) {

          // On met le formulaire en mode EDIT
          $form.find('input[name="viewMode"]').val('EDIT');
          $form.find('input[name="id"]').val(data.id);
          if (data.adherent) {
            $form.find('input[name="adherent"]').val(data.adherent);
          }

          // Bénéficiaire : si ayantDroit présent, on le sélectionne, sinon l’adhérent
          if (data.ayantDroit) {
            $('#benef').val(data.ayantDroit);
          } else if (data.adherent) {
            $('#benef').val(data.adherent);
          }

          // Type de prestation
          if (data.typePrestation) {
            $('#tprestation').val(data.typePrestation);
          }

          // Structure
          if (data.structure) {
            $('#structure').val(data.structure);
          }

          // Date de paiement
          if (data.tmpDate) {
            $form.find('input[name="tmpDate"]').val(data.tmpDate);
          }

          // Structure émettrice
          if (data.structureEmettriceRembourssement) {
            $('#structureEmettriceRembourssement').val(data.structureEmettriceRembourssement);
          } else {
            $('#structureEmettriceRembourssement').val('');
          }

          // Téléphone structure
          if (data.telStructureEmettrice) {
            $('#telStructureEmettrice').val(data.telStructureEmettrice);
          } else {
            $('#telStructureEmettrice').val('');
          }

          // Titre du modal
          $('#reglementModalLabel').text('Modifier un Bon de Commande');

          // Si tu veux gérer l’affichage des boutons (Enregistrer / Modifier) :
          // - tu peux leur donner des id et faire .show() / .hide() ici

          // On ouvre le modal
          $('#reglementModal').modal('show');
        },
        error: function () {
          alert("Impossible de charger le règlement pour modification.");
        }
      });
    });

    // (OPTIONNEL) Si tu utilises DataTables, tu peux le réinitialiser ici
    /*
    if ($.fn.DataTable) {
      $('#example2').DataTable({
        paging: true,
        lengthChange: true,
        searching: true,
        ordering: true,
        info: true,
        autoWidth: false,
        responsive: true
      });
    }
    */
  });
