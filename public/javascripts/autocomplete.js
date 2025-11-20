/**
* @author anasser.
* @since 29-03-2021
* recherche avec auto completion pour les adherents
*/
$(function () {
            /*--recherche dynamic utilisateur, typeahead--*/
            $("#nomay").autocomplete({
                serviceUrl: '/json-ayantDroit',
                dataType: 'json',

                //params: {paramnam: paramvalue},
                minChars: 3,
                width: 300,
                transformResult: function (response) {
                    return {
                        suggestions: $.map(response.options, function (item) {
                            return {value: "" + item.nomAy, data: item};
                        })
                    };
                },
                formatResult: function (suggestion, currentValue) {
                    var result = suggestion;
                    if (suggestion && suggestion.data && suggestion.data.matricule) {
                        result = suggestion.data.nomAy + ' - ' + suggestion.data.nomAd + ' '+ suggestion.data.prenomAd;
                    }
                    return result;
                },
                onSelect: function (suggestion) {
                        $("#mattadh").val(suggestion.data.matricule);
                        $("#nomadh").val(suggestion.data.nomAd);
                    }

            });

        });



/**
* @author anasser.
* @since 03-02-2020
* recherche avec auto completion pour les type de partenaire
*/
$(function () {
            /*--recherche dynamic utilisateur, typeahead--*/
            $("#partenaire").autocomplete({
                serviceUrl: '/json-partenaire',
                dataType: 'json',

                //params: {paramnam: paramvalue},
                minChars: 3,
                width: 300,
                transformResult: function (response) {
                    return {
                        suggestions: $.map(response.option, function (item) {
                            return {value: "" + item.libelle, data: item};
                        })
                    };
                },
                formatResult: function (suggestion, currentValue) {
                    var result = suggestion;
                    if (suggestion && suggestion.data && suggestion.data.libelle) {
                        result = suggestion.data.libelle ;
                    }
                    return result;
                },
                onSelect: function (suggestion) {
                        $("#partenaireid").val(suggestion.data.id);
                        $("#taux").val(suggestion.data.tauxCouverture);
                    }

            });

        });

$(function () {
    /*--recherche dynamic utilisateur, typeahead--*/
    $("#compte").autocomplete({
        serviceUrl: '/json-CompteComptable',
        dataType: 'json',

        //params: {paramnam: paramvalue},
        minChars: 2,
        width: 300,
        transformResult: function (response) {
            return {
                suggestions: $.map(response.options, function (item) {
                    return {value: "" + item.numCompte, data: item};
                })
            };
        },
        formatResult: function (suggestion, currentValue) {
            var result = suggestion;
            if (suggestion && suggestion.data && suggestion.data.numCompte) {
                result = suggestion.data.numCompte + ' - ' + suggestion.data.libelleCompte ;
            }
            return result;
        },
        onSelect: function (suggestion) {
                $("#compteLibelle").val(suggestion.data.libelleCompte);
            }

    });

});


$(function () {
    /*--recherche dynamic utilisateur, typeahead--*/
    $("#compteB").autocomplete({
        serviceUrl: '/json-CompteBudget',
        dataType: 'json',

        //params: {paramnam: paramvalue},
        minChars: 1,
        width: 300,
        transformResult: function (response) {
            return {
                suggestions: $.map(response.options, function (item) {
                    return {value: "" + item.numCompte, data: item};
                })
            };
        },
        formatResult: function (suggestion, currentValue) {
            var result = suggestion;
            if (suggestion && suggestion.data && suggestion.data.numCompte) {
                result = suggestion.data.numCompte + ' - ' + suggestion.data.libelleCompte ;
            }
            return result;
        },
        onSelect: function (suggestion) {
                $("#compteLibelle").val(suggestion.data.libelleCompte);
            }

    });

});

$(document).ready(function(){
	//$( ".pl" ).hide();
	    $(document).on('change', '.isP', function () {
	    var ra = document.getElementsByName('rad');
	      if(ra[0].checked)
	      $( ".pl" ).hide();
	      else
	      $( ".pl" ).show();
	    });


	    $(document).on('change', '.isT', function () {
	        var tr = document.getElementsByName('trans');
	          if(tr[0].checked)
	          $( ".transfert" ).hide();
	          else
	          $( ".transfert" ).show();
	        });
	});