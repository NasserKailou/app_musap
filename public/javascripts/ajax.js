window.addEventListener('load', function () {

    // ✅ VÉRIFICATION : N'initialiser QUE si pas déjà fait
    if (!$.fn.DataTable.isDataTable('#reglementDetailsTable')) {
        console.log('Initialisation DataTable depuis ajax.js');

        $("#reglementDetailsTable").DataTable({
            responsive: true,
            lengthChange: false,
            autoWidth: false,
            pagingType: "full_numbers",
            language: {
              "processing": "Traitement en cours...",
              "search": "Rechercher&nbsp;:",
              "lengthMenu": "Afficher _MENU_ éléments",
              "info": "Affichage de _START_ à _END_ sur _TOTAL_ éléments",
              "infoEmpty": "Aucun élément",
              "infoFiltered": "(filtré de _MAX_ éléments)",
              "loadingRecords": "Chargement...",
              "zeroRecords": "Aucun élément trouvé",
              "emptyTable": "Aucune donnée disponible",
              "paginate": {
                "first": "Premier",
                "previous": "Précédent",
                "next": "Suivant",
                "last": "Dernier"
              }
            },
            columnDefs: [
                { targets: -1, className: "text-center" }
            ]
        });
    } else {
        console.log('⚠️ DataTable déjà initialisé (probablement depuis la vue)');
    }

    // Gestion du bouton AJOUTER
    $("[data-target='#modal-detail-reglement']").on("click", function () {
        console.log("Clique sur AJOUTER");
        $("#modal-title-detail").text("Ajouter un détail");
        $("#viewMode").val("CREATE");
        $("#detailId").val("");
        $("#intitule").val("");
        $("#montant").val("");
    });

    // Gestion du bouton MODIFIER
    $(document).on("click", ".btn-edit-detail", function (e) {
        e.preventDefault();
        var id = $(this).data("id");
        console.log("Clique sur Modifier, id = " + id);

        $("#modal-title-detail").text("Modifier un détail");
        $("#viewMode").val("EDIT");
        $("#detailId").val(id);
        $("#modal-detail-reglement").modal("show");

        $("#intitule").val("");
        $("#montant").val("");

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
    $form[0].reset();
    $form.find('input[name="viewMode"]').val('CREATE');
    $form.find('input[name="id"]').val('');
    $('#reglementModalLabel').text('Nouveau Bon de Commande');
    $('#reglementModal').modal('show');
  });

  // ====== MODIFIER BON DE COMMANDE ======
  $('.btn-edit-reglement').on('click', function (e) {
    e.preventDefault();
    var idReglement = $(this).data('id');
    var $form = $('form[name="form"]');

    $.ajax({
      url: '@controllers.routes.ReglementCtrl.getReglementJson(0L)'.toString().replace('0', idReglement),
      method: 'GET',
      dataType: 'json',
      success: function (data) {
        $form.find('input[name="viewMode"]').val('EDIT');
        $form.find('input[name="id"]').val(data.id);

        if (data.adherent) {
          $form.find('input[name="adherent"]').val(data.adherent);
        }

        if (data.ayantDroit) {
          $('#benef').val(data.ayantDroit);
        } else if (data.adherent) {
          $('#benef').val(data.adherent);
        }

        if (data.typePrestation) {
          $('#tprestation').val(data.typePrestation);
        }

        if (data.structure) {
          $('#structure').val(data.structure);
        }

        if (data.tmpDate) {
          $form.find('input[name="tmpDate"]').val(data.tmpDate);
        }

        if (data.structureEmettriceRembourssement) {
          $('#structureEmettriceRembourssement').val(data.structureEmettriceRembourssement);
        }

        if (data.telStructureEmettrice) {
          $('#telStructureEmettrice').val(data.telStructureEmettrice);
        }

        $('#reglementModalLabel').text('Modifier un Bon de Commande');
        $('#reglementModal').modal('show');
      },
      error: function () {
        alert("Impossible de charger le règlement pour modification.");
      }
    });
  });

});

// Configuration globale DataTables en français
$.extend(true, $.fn.dataTable.defaults, {
  "language": {
    "processing": "Traitement en cours...",
    "search": "Rechercher&nbsp;:",
    "lengthMenu": "Afficher _MENU_ éléments",
    "info": "Affichage de _START_ à _END_ sur _TOTAL_ éléments",
    "infoEmpty": "Aucun élément",
    "infoFiltered": "(filtré de _MAX_ éléments)",
    "loadingRecords": "Chargement...",
    "zeroRecords": "Aucun élément trouvé",
    "emptyTable": "Aucune donnée disponible",
    "paginate": {
      "first": "Premier",
      "previous": "Précédent",
      "next": "Suivant",
      "last": "Dernier"
    },
    "aria": {
      "sortAscending": ": tri croissant",
      "sortDescending": ": tri décroissant"
    }
  }
});
