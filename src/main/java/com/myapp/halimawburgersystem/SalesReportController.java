package com.myapp.halimawburgersystem;

import com.myapp.model.Staff;
import com.myapp.model.User;
import com.myapp.util.GeminiService;
import com.myapp.util.SalesReportService;
import com.myapp.util.SalesReportService.SalesSummary;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class SalesReportController {

    @FXML private Button btnDashboard;
    @FXML private Button btnOrders;
    @FXML private Button btnKitchen;
    @FXML private Button btnMenuItems;
    @FXML private Button btnCombos;
    @FXML private Button btnInventory;
    @FXML private Button btnSales;
    @FXML private Button btnStaff;

    @FXML private Label pageTitle;
    @FXML private Label topbarDate;

    @FXML private Label sidebarAvatarText;
    @FXML private Label sidebarUserName;
    @FXML private Label sidebarUserRole;

    @FXML private Label lblRevenue;
    @FXML private Label lblRevenueDelta;
    @FXML private Label lblOrders;
    @FXML private Label lblOrdersDelta;

    @FXML private Label lblAnalysisStatus;
    @FXML private Label lblRecommendationStatus;
    

    // WebViews for chart and AI responses
    @FXML private WebView chartWebView;
    @FXML private WebView dailyChartWebView;
    @FXML private WebView analysisWebView;
    @FXML private WebView recommendationWebView;

    @FXML private ComboBox<String> cmbDailyFilter;
    private int dailyFilterDays = 7;

    // Shared CSS for AI response WebViews to match app theme
    private static final String WEB_CSS =
            "<style>" +
                    "* { box-sizing: border-box; margin: 0; padding: 0; }" +
                    "body { background: #2a1f0e; font-family: 'Segoe UI', sans-serif;" +
                    "       color: #c8a97a; font-size: 13px; padding: 4px 2px; }" +
                    "h4 { color: #e8c99a; font-size: 13px; font-weight: 600;" +
                    "     text-transform: uppercase; letter-spacing: 0.05em;" +
                    "     margin: 12px 0 6px 0; border-bottom: 1px solid rgba(200,169,122,0.2);" +
                    "     padding-bottom: 4px; }" +
                    "h4:first-child { margin-top: 0; }" +
                    "ul { padding-left: 16px; margin: 4px 0; }" +
                    "li { margin: 4px 0; line-height: 1.5; }" +
                    "b { color: #e8c99a; font-weight: 600; }" +
                    "</style>";


    @FXML
    public void initialize() {
        updateTopbarDate();
        updateSidebarUser();
        if (cmbDailyFilter != null) {
            cmbDailyFilter.setItems(FXCollections.observableArrayList("Last 7 Days", "Last 30 Days", "This Month"));
            cmbDailyFilter.setValue("Last 7 Days");
        }
        loadReport();
    }

    public void setActiveNav(String page) {
        if (btnDashboard != null) btnDashboard.getStyleClass().remove("nav-item-active");
        if (btnOrders != null)    btnOrders.getStyleClass().remove("nav-item-active");
        if (btnKitchen != null)   btnKitchen.getStyleClass().remove("nav-item-active");
        if (btnMenuItems != null) btnMenuItems.getStyleClass().remove("nav-item-active");
        if (btnCombos != null)    btnCombos.getStyleClass().remove("nav-item-active");
        if (btnInventory != null) btnInventory.getStyleClass().remove("nav-item-active");
        if (btnSales != null)     btnSales.getStyleClass().remove("nav-item-active");
        if (btnStaff != null)     btnStaff.getStyleClass().remove("nav-item-active");

        if (page.equals("Sales Reports") && btnSales != null)
            btnSales.getStyleClass().add("nav-item-active");
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

    private void loadReport() {
        setStatus(lblAnalysisStatus, "Loading...");
        setStatus(lblRecommendationStatus, "Loading...");
        loadHtml(analysisWebView, "<i style='color:#c8a97a'>Loading analysis...</i>");
        loadHtml(recommendationWebView, "<i style='color:#c8a97a'>Loading recommendations...</i>");

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
                loadDailyChart();
                runAiAnalysis(summary);
                runAiRecommendations(summary);
            });
        });

        dbTask.setOnFailed(e -> Platform.runLater(() -> {
            setStatus(lblAnalysisStatus, "Database error");
            setStatus(lblRecommendationStatus, "Database error");
        }));

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

        StringBuilder labels = new StringBuilder();
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < s.hourlyRevenue.size(); i++) {
            String[] hr = s.hourlyRevenue.get(i);
            if (i > 0) { labels.append(","); data.append(","); }
            labels.append("\"").append(hr[0]).append("\"");
            data.append(hr[1]);
        }

        if (s.hourlyRevenue.isEmpty()) {
            labels.append("\"No data yet\"");
            data.append("0");
        }

        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js'></script>"
                + "<style>body{margin:0;padding:4px;background:#1a1208;font-family:sans-serif;}"
                + "canvas{max-height:140px;}</style></head><body>"
                + "<canvas id='chart'></canvas>"
                + "<script>"
                + "new Chart(document.getElementById('chart'),{"
                + "  type:'bar',"
                + "  data:{"
                + "    labels:[" + labels + "],"
                + "    datasets:[{"
                + "      label:'Revenue (PHP)',"
                + "      data:[" + data + "],"
                + "      backgroundColor:'rgba(220,80,50,0.75)',"
                + "      borderRadius:4,"
                + "      barThickness:40,"
                + "      maxBarThickness:60"
                + "    }]"
                + "  },"
                + "  options:{"
                + "    responsive:true,"
                + "    layout:{padding:{top:0,bottom:0}},"
                + "    plugins:{legend:{display:false}},"
                + "    scales:{"
                + "      y:{beginAtZero:true,ticks:{color:'#c8a97a',callback:v=>'₱'+v,maxTicksLimit:5},grid:{color:'rgba(200,169,122,0.1)'}},"
                + "      x:{ticks:{color:'#c8a97a'},grid:{display:false},offset:true}"
                + "    }"
                + "  }"
                + "});"
                + "</script></body></html>";

        chartWebView.getEngine().loadContent(html);
    }

    private void renderDailyChart(List<String[]> dailyData) {
        if (dailyChartWebView == null) return;

        StringBuilder labels = new StringBuilder();
        StringBuilder data = new StringBuilder();
        for (int i = 0; i < dailyData.size(); i++) {
            String[] day = dailyData.get(i);
            if (i > 0) { labels.append(","); data.append(","); }
            labels.append("\"").append(day[0]).append("\"");
            data.append(day[1]);
        }

        if (dailyData.isEmpty()) {
            labels.append("\"No data\"");
            data.append("0");
        }

        String html = "<!DOCTYPE html><html><head>"
                + "<script src='https://cdnjs.cloudflare.com/ajax/libs/Chart.js/4.4.1/chart.umd.js'></script>"
                + "<style>body{margin:0;padding:4px;background:#1a1208;font-family:sans-serif;}"
                + "canvas{max-height:140px;}</style></head><body>"
                + "<canvas id='dailyChart'></canvas>"
                + "<script>"
                + "new Chart(document.getElementById('dailyChart'),{"
                + "  type:'bar',"
                + "  data:{"
                + "    labels:[" + labels + "],"
                + "    datasets:[{"
                + "      label:'Revenue (PHP)',"
                + "      data:[" + data + "],"
                + "      backgroundColor:'rgba(220,80,50,0.75)',"
                + "      borderRadius:4,"
                + "      barThickness:40,"
                + "      maxBarThickness:60"
                + "    }]"
                + "  },"
                + "  options:{"
                + "    responsive:true,"
                + "    layout:{padding:{top:0,bottom:0}},"
                + "    plugins:{legend:{display:false}},"
                + "    scales:{"
                + "      y:{beginAtZero:true,ticks:{color:'#c8a97a',callback:v=>'₱'+v,maxTicksLimit:5},grid:{color:'rgba(200,169,122,0.1)'}},"
                + "      x:{ticks:{color:'#c8a97a'},grid:{display:false},offset:true}"
                + "    }"
                + "  }"
                + "});"
                + "</script></body></html>";

        dailyChartWebView.getEngine().loadContent(html);
    }

    @FXML
    private void onDailyFilterChanged() {
        if (cmbDailyFilter == null || cmbDailyFilter.getValue() == null) return;

        String selected = cmbDailyFilter.getValue();
        switch (selected) {
            case "Last 7 Days" -> dailyFilterDays = 7;
            case "Last 30 Days" -> dailyFilterDays = 30;
            case "This Month" -> dailyFilterDays = LocalDate.now().getDayOfMonth();
        }

        loadDailyChart();
    }

    private void loadDailyChart() {
        Task<List<String[]>> task = new Task<>() {
            @Override
            protected List<String[]> call() throws Exception {
                return SalesReportService.fetchDailyRevenue(dailyFilterDays);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            renderDailyChart(task.getValue());
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            if (dailyChartWebView != null) {
                dailyChartWebView.getEngine().loadContent("<html><body style='background:#1a1208;margin:0;padding:20px;'><span style='color:#c0392b'>Failed to load data</span></body></html>");
            }
        }));

        new Thread(task).start();
    }

    private void runAiAnalysis(SalesSummary summary) {
        setStatus(lblAnalysisStatus, "Analyzing...");
        String prompt = SalesReportService.buildAnalyticsPrompt(summary);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return GeminiService.analyze(prompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setStatus(lblAnalysisStatus, "Done");
            loadHtml(analysisWebView, task.getValue());
            checkAllDone();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setStatus(lblAnalysisStatus, "AI error");
            loadHtml(analysisWebView, "<span style='color:#c0392b'>Failed to load analysis.</span>");
            checkAllDone();
        }));

        new Thread(task).start();
    }

    private void runAiRecommendations(SalesSummary summary) {
        setStatus(lblRecommendationStatus, "Analyzing...");
        String prompt = SalesReportService.buildRecommendationPrompt(summary);

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return GeminiService.analyze(prompt);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            setStatus(lblRecommendationStatus, "Done");
            loadHtml(recommendationWebView, task.getValue());
            checkAllDone();
        }));

        task.setOnFailed(e -> Platform.runLater(() -> {
            setStatus(lblRecommendationStatus, "AI error");
            loadHtml(recommendationWebView, "<span style='color:#c0392b'>Failed to load recommendations.</span>");
            checkAllDone();
        }));

        new Thread(task).start();
    }

    // Wraps AI HTML response with shared CSS and loads into WebView
    private void loadHtml(WebView webView, String content) {
        if (webView == null) return;
        String fullHtml = "<!DOCTYPE html><html><head>" + WEB_CSS + "</head><body>" + content + "</body></html>";
        webView.getEngine().loadContent(fullHtml);
    }

    private void checkAllDone() {
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