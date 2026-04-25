package com.myapp.halimawburgersystem;

import com.myapp.model.Staff;
import com.myapp.model.User;
import com.myapp.util.GeminiService;
import com.myapp.util.SalesReportService;
import com.myapp.util.SalesReportService.SalesSummary;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SalesReportController {

    // --- Sidebar nav buttons ---
    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    // --- Topbar ---
    @FXML private Label pageTitle;
    @FXML private Label topbarDate;

    // --- Sidebar user info ---
    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    // --- Stat cards ---
    @FXML private Label lblRevenue;
    @FXML private Label lblRevenueDelta;
    @FXML private Label lblOrders;
    @FXML private Label lblOrdersDelta;

    // --- AI output ---
    @FXML private Label lblAnalysisStatus;
    @FXML private Label lblAnalysisText;
    @FXML private Label lblRecommendationStatus;
    @FXML private Label lblRecommendationText;
    @FXML private Button btnRefresh;

    // --- Chart ---
    @FXML private WebView chartWebView;

    @FXML
    public void initialize() {
        updateTopbarDate();
        updateSidebarUser();
        loadReport();
    }

    public void setActiveNav(String page) {
        // Style matching your existing pattern
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("nav-item-active");
        if (btnOrders != null)    btnOrders.getStyleClass().remove("nav-item-active");
        if (btnKitchen != null)   btnKitchen.getStyleClass().remove("nav-item-active");
        if (btnMenuItems != null) btnMenuItems.getStyleClass().remove("nav-item-active");
        if (btnCombos != null)    btnCombos.getStyleClass().remove("nav-item-active");
        if (btnInventory != null) btnInventory.getStyleClass().remove("nav-item-active");
        if (btnSales != null)     btnSales.getStyleClass().remove("nav-item-active");
        if (btnStaff != null)     btnStaff.getStyleClass().remove("nav-item-active");

        switch (page) {
            case "Sales Reports" -> { if (btnSales != null) btnSales.getStyleClass().add("nav-item-active"); }
            case "Dashboard"     -> { if (btnDashboard != null) btnDashboard.getStyleClass().add("nav-item-active"); }
        }
    }

    @FXML
    private void onNavigate(javafx.event.ActionEvent event) {
        Button source = (Button) event.getSource();
        try {
            switch (source.getText()) {
                case "Dashboard"       -> Main.showDashboard();
                case "Orders"          -> Main.showOrders();
                case "Kitchen Queue"   -> Main.showKitchen();
                case "Menu Items"      -> Main.showMenuItems();
                case "Combos & Promos" -> Main.showCombos();
                case "Inventory"       -> Main.showInventory();
                case "Sales Reports"   -> Main.showSalesReport();
                case "Staff"           -> Main.showStaff();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onLogout() {
        try {
            Main.clearSession();
            Main.showLogin();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onRefresh() {
        loadReport();
    }

    private void loadReport() {
        // Disable refresh while loading
        if (btnRefresh != null) btnRefresh.setDisable(true);
        setStatus(lblAnalysisStatus, "Loading...");
        setStatus(lblRecommendationStatus, "Loading...");
        if (lblAnalysisText != null) lblAnalysisText.setText("");
        if (lblRecommendationText != null) lblRecommendationText.setText("");

        Task<SalesSummary> dbTask = new Task<>() {
            @Override
            protected SalesSummary call() throws Exception {
                return SalesReportService.fetchSummary();
            }
        };

        dbTask.setOnSucceeded(e -> {
            SalesSummary summary = dbTask.getValue();
            Platform.runLater(() -> {
                updateStatCards(summary);
                renderChart(summary);
                runAiAnalysis(summary);
                runAiRecommendations(summary);
            });
        });

        dbTask.setOnFailed(e -> {
            Platform.runLater(() -> {
                setStatus(lblAnalysisStatus, "Database error");
                setStatus(lblRecommendationStatus, "Database error");
                if (btnRefresh != null) btnRefresh.setDisable(false);
            });
        });

        new Thread(dbTask).start();
    }

    private void updateStatCards(SalesSummary s) {
        if (lblRevenue != null) lblRevenue.setText(String.format("₱%.2f", s.revenueToday));
        if (lblOrders != null)  lblOrders.setText(String.valueOf(s.ordersToday));

        if (lblRevenueDelta != null) {
            double diff = s.revenueToday - s.revenueYesterday;
            lblRevenueDelta.setText((diff >= 0 ? "▲" : "▼") + String.format(" ₱%.2f vs yesterday", Math.abs(diff)));
            lblRevenueDelta.getStyleClass().removeAll("delta-up", "delta-down");
            lblRevenueDelta.getStyleClass().add(diff >= 0 ? "delta-up" : "delta-down");
        }

        if (lblOrdersDelta != null) {
            int diff = s.ordersToday - s.ordersYesterday;
            lblOrdersDelta.setText((diff >= 0 ? "▲" : "▼") + " " + Math.abs(diff) + " vs yesterday");
            lblOrdersDelta.getStyleClass().removeAll("delta-up", "delta-down");
            lblOrdersDelta.getStyleClass().add(diff >= 0 ? "delta-up" : "delta-down");
        }
    }

    private void renderChart(SalesSummary s) {
        if (chartWebView == null) return;

        // Build labels and data for hourly revenue chart
        StringBuilder labels = new StringBuilder();
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < s.hourlyRevenue.size(); i++) {
            String[] hr = s.hourlyRevenue.get(i);
            if (i > 0) { labels.append(","); data.append(","); }
            labels.append("\"").append(hr[0]).append("\"");
            data.append(hr[1]);
        }

        // If no data yet, show placeholder
        if (s.hourlyRevenue.isEmpty()) {
            labels.append("\"No data yet\"");
            data.append("0");
        }

        String html = "<!DOCTYPE html><html><head>"
            + "<script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js'></script>"
            + "<style>"
            + "body{margin:0;padding:0;background:#1a1208;font-family:'Segoe UI',sans-serif;}"
            + "canvas{max-height:250px;}"
            + "</style></head><body>"
            + "<canvas id='chart'></canvas>"
            + "<script>"
            + "new Chart(document.getElementById('chart'),{"
            + "  type:'bar',"
            + "  data:{"
            + "    labels:[" + labels + "],"
            + "    datasets:[{"
            + "      label:'Revenue (PHP)',"
            + "      data:[" + data + "],"
            + "      backgroundColor:'rgba(200,80,10,0.85)',"
            + "      borderColor:'#c8500a',"
            + "      borderWidth:1,"
            + "      borderRadius:4,"
            + "      barThickness:40,"
            + "      maxBarThickness:60"
            + "    }]"
            + "  },"
            + "  options:{"
            + "    responsive:true,"
            + "    maintainAspectRatio:false,"
            + "    layout:{padding:{top:0,bottom:0}},"
            + "    plugins:{"
            + "      legend:{display:false},"
            + "      tooltip:{"
            + "        backgroundColor:'#2e2410',"
            + "        titleColor:'#f5ede0',"
            + "        bodyColor:'#c4a882',"
            + "        borderColor:'#5c4828',"
            + "        borderWidth:1,"
            + "        padding:8,"
            + "        displayColors:false,"
            + "        callbacks:{"
            + "          label:function(ctx){return '₱' + ctx.parsed.y.toLocaleString();}"
            + "        }"
            + "      }"
            + "    },"
            + "    scales:{"
            + "      x:{"
            + "        grid:{display:false},"
            + "        ticks:{color:'#c8a97a',font:{size:10}}"
            + "      },"
            + "      y:{"
            + "        beginAtZero:true,"
            + "        grid:{color:'rgba(200,169,122,0.1)'},"
            + "        border:{display:false},"
            + "        ticks:{"
            + "          color:'#c8a97a',"
            + "          font:{size:10},"
            + "          callback:function(v){return '₱' + v.toLocaleString();}"
            + "        }"
            + "      }"
            + "    }"
            + "  }"
            + "});"
            + "</script></body></html>";

        WebEngine engine = chartWebView.getEngine();
        engine.loadContent(html);
    }

    private void runAiAnalysis(SalesSummary summary) {
        setStatus(lblAnalysisStatus, "Analyzing with AI...");
        String prompt = SalesReportService.buildAnalyticsPrompt(summary);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return GeminiService.analyze(prompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setStatus(lblAnalysisStatus, "Done");
            if (lblAnalysisText != null) lblAnalysisText.setText(task.getValue());
            checkAllDone();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setStatus(lblAnalysisStatus, "AI error - check API key");
            checkAllDone();
        }));

        new Thread(task).start();
    }

    private void runAiRecommendations(SalesSummary summary) {
        setStatus(lblRecommendationStatus, "Generating recommendations...");
        String prompt = SalesReportService.buildRecommendationPrompt(summary);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return GeminiService.analyze(prompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setStatus(lblRecommendationStatus, "Done");
            if (lblRecommendationText != null) lblRecommendationText.setText(task.getValue());
            checkAllDone();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setStatus(lblRecommendationStatus, "AI error - check API key");
            checkAllDone();
        }));

        new Thread(task).start();
    }

    private void checkAllDone() {
        // Re-enable refresh when both AI calls finish
        boolean analysisDone = lblAnalysisStatus != null &&
            !lblAnalysisStatus.getText().equals("Analyzing with AI...");
        boolean recoDone = lblRecommendationStatus != null &&
            !lblRecommendationStatus.getText().equals("Generating recommendations...");
        if (analysisDone && recoDone && btnRefresh != null) {
            btnRefresh.setDisable(false);
        }
    }

    private void setStatus(Label lbl, String text) {
        if (lbl != null) lbl.setText(text);
    }

    private void updateTopbarDate() {
        if (topbarDate != null) {
            String date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE, MMM d yyyy"));
            topbarDate.setText(date);
        }
    }

    private void updateSidebarUser() {
        User user = Main.getCurrentUser();
        Staff staff = Main.getCurrentStaff();
        if (staff != null) {
            if (sidebarUserName != null) sidebarUserName.setText(staff.getName());
            if (sidebarUserRole != null) sidebarUserRole.setText(staff.getRole().toString());
            if (sidebarAvatarText != null) {
                String[] parts = staff.getName().split(" ");
                String initials = parts.length >= 2
                    ? "" + parts[0].charAt(0) + parts[1].charAt(0)
                    : staff.getName().substring(0, Math.min(2, staff.getName().length()));
                sidebarAvatarText.setText(initials.toUpperCase());
            }
        }
    }
}
