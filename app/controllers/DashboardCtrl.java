package controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.Request;
import play.mvc.Result;
import services.DashboardService;

/**
 * Contrôleur pour le Dashboard Analytics
 * 
 * @author AI Assistant
 */
public class DashboardCtrl extends Controller {

    private final DashboardService dashboardService;

    @Inject
    public DashboardCtrl(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Affiche la page principale du dashboard
     */
    public Result index(Request request) {
        // Vérifier l'authentification
        if (request.session().get("login").isEmpty()) {
            return redirect(routes.HomeController.index())
                .flashing("error", "Vous devez être connecté pour accéder au dashboard");
        }

        try {
            // Récupérer les statistiques de base
            long totalAdherents = dashboardService.getTotalAdherentsActifs();
            long totalAyantsDroit = dashboardService.getTotalAyantsDroit();
            BigDecimal totalRemboursements = dashboardService.getTotalRemboursements();
            long nombreRemboursements = dashboardService.getTotalNombreRemboursements();

            // Récupérer les indicateurs de croissance
            Map<String, Object> croissance = dashboardService.getIndicateursCroissance();

            // Récupérer les derniers remboursements
            List<Map<String, Object>> derniersRemboursements = 
                dashboardService.getDerniersRemboursements();

            return ok(views.html.dashboard.render(
                totalAdherents,
                totalAyantsDroit,
                totalRemboursements,
                nombreRemboursements,
                croissance,
                derniersRemboursements,
                request
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError("Erreur lors du chargement du dashboard: " + e.getMessage());
        }
    }

    /**
     * API JSON - Remboursements mensuels
     */
    public Result getRemboursementsMensuelsJson(Request request) {
        if (request.session().get("login").isEmpty()) {
            return unauthorized(Json.toJson(Map.of("error", "Non authentifié")));
        }

        try {
            Map<String, Object> data = dashboardService.getRemboursementsMensuels();
            return ok(Json.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(Json.toJson(
                Map.of("error", "Erreur lors de la récupération des données: " + e.getMessage())
            ));
        }
    }

    /**
     * API JSON - Remboursements par type de prestation
     */
    public Result getRemboursementsParTypePrestationJson(Request request) {
        if (request.session().get("login").isEmpty()) {
            return unauthorized(Json.toJson(Map.of("error", "Non authentifié")));
        }

        try {
            Map<String, Object> data = dashboardService.getRemboursementsParTypePrestation();
            return ok(Json.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(Json.toJson(
                Map.of("error", "Erreur lors de la récupération des données: " + e.getMessage())
            ));
        }
    }

    /**
     * API JSON - Adhérents par structure
     */
    public Result getAdherentsParStructureJson(Request request) {
        if (request.session().get("login").isEmpty()) {
            return unauthorized(Json.toJson(Map.of("error", "Non authentifié")));
        }

        try {
            Map<String, Object> data = dashboardService.getAdherentsParStructure();
            return ok(Json.toJson(data));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(Json.toJson(
                Map.of("error", "Erreur lors de la récupération des données: " + e.getMessage())
            ));
        }
    }

    /**
     * API JSON - Tous les indicateurs pour le dashboard
     */
    public Result getAllIndicateursJson(Request request) {
        if (request.session().get("login").isEmpty()) {
            return unauthorized(Json.toJson(Map.of("error", "Non authentifié")));
        }

        try {
            Map<String, Object> allData = new HashMap<>();
            
            // Statistiques de base
            allData.put("totalAdherents", dashboardService.getTotalAdherentsActifs());
            allData.put("totalAyantsDroit", dashboardService.getTotalAyantsDroit());
            allData.put("totalRemboursements", dashboardService.getTotalRemboursements());
            allData.put("nombreRemboursements", dashboardService.getTotalNombreRemboursements());
            
            // Données graphiques
            allData.put("remboursementsMensuels", dashboardService.getRemboursementsMensuels());
            allData.put("remboursementsParType", dashboardService.getRemboursementsParTypePrestation());
            allData.put("adherentsParStructure", dashboardService.getAdherentsParStructure());
            
            // Indicateurs de croissance
            allData.put("croissance", dashboardService.getIndicateursCroissance());
            
            // Derniers remboursements
            allData.put("derniersRemboursements", dashboardService.getDerniersRemboursements());

            return ok(Json.toJson(allData));
        } catch (Exception e) {
            e.printStackTrace();
            return internalServerError(Json.toJson(
                Map.of("error", "Erreur lors de la récupération des données: " + e.getMessage())
            ));
        }
    }
}
