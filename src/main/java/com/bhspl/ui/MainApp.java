package com.bhspl.ui;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.bhspl.util.UIHelper;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import com.bhspl.service.SyncService;

public class MainApp extends JFrame {

    private final String username;
    private final String role;

    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JLabel pageTitle;
    private ReportsPanel reportsPanel;
    private DashboardPanel dashboard;
    private final java.util.Map<String, JPanel> panelCache = new java.util.HashMap<>();

    private static final Object[][] NAV_ITEMS = {
            { "Dashboard", "dashboard.svg", null },
            { "Employee Master", "employees.svg", null },
            { "Attendance", "attendance.svg", null },
            { "FOLDER:Reports", "reports.svg", new String[] { "Daily", "Monthly", "Leave Report" } },
            { "Raw Punch Log", "punch_log.svg", null },
            { "Device Manager", "devices.svg", null },
            { "FOLDER:Leave", "leaves.svg",
                    new String[] { "Leave Manager", "OD Requests", "Leave Policy", "Leave Balance", "Holidays" } },
            { "FOLDER:Masters", "designation.svg",
                    new String[] { "Departments", "Designations", "Shifts", "Weekly Off" } },
            { "FOLDER:System", "settings.svg", new String[] { "User Management", "Settings", "DB Backup", "About" } }
    };

    public MainApp(String username, String role) {
        this.username = username;
        this.role = role;

        setTitle("BHSPL Attendance Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        buildUI();
    }

    private void buildUI() {
        setLayout(new MigLayout("ins 0, gap 0, fill", "[280!]0[grow]", "[grow]"));

        // ----- SIDEBAR -----
        JPanel sidebar = new JPanel(new MigLayout("ins 0, wrap, gap 0, fill", "[grow]", "[100!]0[grow]"));
        sidebar.setBackground(UIHelper.SIDEBAR_BG);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIHelper.BORDER));

        // Logo Area
        JPanel logoPanel = new JPanel(new MigLayout("ins 0, fill", "[center]", "[center]"));
        logoPanel.setBackground(UIHelper.SIDEBAR_BG);
        logoPanel.setPreferredSize(new Dimension(280, 100));

        try {
            JLabel logoLabel = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    try {
                        java.net.URL logoUrl = getClass().getResource("/logo.png");
                        if (logoUrl != null) {
                            Image img = javax.imageio.ImageIO.read(logoUrl);
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                            int w = getWidth();
                            int h = (w * img.getHeight(null)) / img.getWidth(null);
                            if (h > getHeight()) {
                                h = getHeight();
                                w = (h * img.getWidth(null)) / img.getHeight(null);
                            }
                            g2.drawImage(img, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
                            g2.dispose();
                        }
                    } catch (Exception e) {
                    }
                }
            };
            logoPanel.add(logoLabel, "w 180!, h 60!, center");
        } catch (Exception e) {
            JLabel logoText = new JLabel("BAVYA");
            logoText.setFont(new Font("Segoe UI", Font.BOLD, 22));
            logoText.setForeground(UIHelper.PRIMARY);
            logoPanel.add(logoText, "center");
        }
        sidebar.add(logoPanel, "growx, h 100!");

        // Navigation Items
        JPanel tabsPanel = new JPanel(new MigLayout("ins 0, wrap, gap 0", "[grow]", "[]"));
        tabsPanel.setOpaque(false);
        ButtonGroup navGroup = new ButtonGroup();
        JToggleButton firstBtn = null;

        JPanel reportsSubMenu = new JPanel(new MigLayout("ins 0, wrap, gap 0, hidemode 3", "[grow]", "[]"));
        reportsSubMenu.setOpaque(false);
        reportsSubMenu.setVisible(false);

        for (Object[] item : NAV_ITEMS) {
            String label = (String) item[0];
            String iconPath = (String) item[1];
            String[] subItems = (String[]) item[2];

            if (label.startsWith("FOLDER:")) {
                String folderName = label.substring(7);
                JPanel subMenu = new JPanel(new MigLayout("ins 0, wrap, gap 0, hidemode 3", "[grow]", "[]"));
                subMenu.setOpaque(false);
                subMenu.setVisible(false);

                FlatSVGIcon rightIcon = new FlatSVGIcon("icons/chevron_right.svg", 16, 16);
                FlatSVGIcon downIcon = new FlatSVGIcon("icons/chevron_down.svg", 16, 16);
                rightIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> new Color(0x94A3B8)));
                downIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIHelper.PRIMARY));

                JToggleButton folderBtn = createNavButton(folderName, iconPath);
                folderBtn.putClientProperty("JButton.trailingIcon", rightIcon);
                navGroup.add(folderBtn);
                tabsPanel.add(folderBtn, "growx");

                for (String sub : subItems) {
                    if ("Settings".equals(sub) && "Operator".equals(role))
                        continue;

                    JToggleButton subBtn = createSubNavButton(sub);
                    navGroup.add(subBtn);
                    subMenu.add(subBtn, "growx");

                    subBtn.addActionListener(e -> {
                        String pageName = sub;
                        if ("Daily".equals(sub) || "Monthly".equals(sub) || "Leave Report".equals(sub)) {
                            pageName = "Reports";
                        }

                        ensurePanelLoaded(pageName);
                        cardLayout.show(contentPanel, pageName);

                        if ("Reports".equals(pageName)) {
                            pageTitle.setText("Reports - " + sub);
                            if (reportsPanel != null)
                                reportsPanel.setActiveTab(sub);
                        } else {
                            pageTitle.setText(sub);
                        }
                    });
                }
                tabsPanel.add(subMenu, "growx, hidemode 3");

                folderBtn.addActionListener(e -> {
                    boolean visible = !subMenu.isVisible();
                    subMenu.setVisible(visible);
                    folderBtn.putClientProperty("JButton.trailingIcon", visible ? downIcon : rightIcon);
                });
            } else {
                JToggleButton btn = createNavButton(label, iconPath);
                navGroup.add(btn);
                tabsPanel.add(btn, "growx");
                if (firstBtn == null)
                    firstBtn = btn;

                btn.addActionListener(e -> {
                    ensurePanelLoaded(label);
                    cardLayout.show(contentPanel, label);
                    pageTitle.setText(label);
                });
            }
        }
        JScrollPane navScroll = new JScrollPane(tabsPanel);
        navScroll.setBorder(null);
        navScroll.setOpaque(false);
        navScroll.getViewport().setOpaque(false);
        navScroll.getVerticalScrollBar().setUnitIncrement(16);
        navScroll.getVerticalScrollBar().putClientProperty("JScrollBar.showButtons", false);
        navScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        sidebar.add(navScroll, "grow, pushy");
        add(sidebar, "grow");

        // Make logo clickable
        logoPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final JToggleButton dashBtn = firstBtn;
        logoPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (dashBtn != null)
                    dashBtn.setSelected(true);
            }
        });

        // ----- MAIN AREA -----
        JPanel mainArea = new JPanel(new MigLayout("ins 0, gap 0, wrap, fill", "[grow]", "[min!]0[grow]0[28!]"));
        mainArea.setBackground(UIHelper.BG_MAIN);

        // Header — Uses MigLayout for better responsiveness than BorderLayout
        JPanel header = new JPanel(new MigLayout("ins 0 24 0 24, fill, gap 10", "[grow] [pref!]", "[:52:52]"));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIHelper.BORDER));

        // LEFT — page title
        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("Segoe UI", Font.BOLD, 20));
        pageTitle.setForeground(UIHelper.PRIMARY);
        header.add(pageTitle, "left, growx, pushx");

        // RIGHT — clock + sync button + user box
        JPanel rightPanel = new JPanel(new MigLayout("ins 0, gap 16, aligny center", "[] [] [] [] []", "[]"));
        rightPanel.setOpaque(false);

        // Clock
        JLabel clockLbl = new JLabel();
        clockLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        clockLbl.setForeground(UIHelper.TEXT_LIGHT);
        rightPanel.add(clockLbl, "hidemode 3"); // hidemode 3 ensures it takes no space when hidden

        // Sync button — self-painting rounded button
        JButton syncNowBtn = new JButton("Sync Devices") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isEnabled()
                        ? (getModel().isRollover() ? UIHelper.ACCENT.darker() : UIHelper.ACCENT)
                        : UIHelper.ACCENT.darker());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        syncNowBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        syncNowBtn.setForeground(Color.WHITE);
        syncNowBtn.setContentAreaFilled(false);
        syncNowBtn.setFocusPainted(false);
        syncNowBtn.setBorderPainted(false);
        syncNowBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        syncNowBtn.setBorder(BorderFactory.createEmptyBorder(7, 14, 7, 14));
        try {
            FlatSVGIcon sIcon = new FlatSVGIcon("icons/sync.svg", 14, 14);
            sIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
            syncNowBtn.setIcon(sIcon);
            syncNowBtn.setIconTextGap(7);
            syncNowBtn.setHorizontalTextPosition(SwingConstants.RIGHT);
        } catch (Exception ignored) {
        }
        syncNowBtn.setToolTipText("Sync all biometric devices now");

        dashboard = new DashboardPanel();
        panelCache.put("Dashboard", dashboard);

        syncNowBtn.addActionListener(e -> {
            syncNowBtn.setEnabled(false);
            syncNowBtn.setText("Syncing...");
            SyncService.forceUpdateToday(() -> {
                syncNowBtn.setEnabled(true);
                syncNowBtn.setText("Sync Devices");
                if (dashboard != null)
                    dashboard.publicLoadTable();
                UIHelper.showSuccess(this, "Synchronization completed successfully.");
            });
        });
        rightPanel.add(syncNowBtn);

        // Separator
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(1, 24));
        sep.setForeground(UIHelper.BORDER);
        rightPanel.add(sep, "h 24!");

        // User icon + name
        FlatSVGIcon uIcon = new FlatSVGIcon("icons/user.svg", 18, 18);
        uIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIHelper.PRIMARY));
        JLabel userIconLbl = new JLabel(uIcon);

        JLabel userLbl = new JLabel(username);
        userLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userLbl.setForeground(UIHelper.PRIMARY);

        JPanel userBox = new JPanel(new MigLayout("ins 0, gap 6", "[] []", "[]"));
        userBox.setOpaque(false);
        userBox.add(userIconLbl);
        userBox.add(userLbl);
        rightPanel.add(userBox);

        // Logout button
        FlatSVGIcon lIcon = new FlatSVGIcon("icons/logout.svg", 18, 18);
        lIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> UIHelper.DANGER));
        JButton logoutBtn = new JButton(lIcon);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.setToolTipText("Logout Securely");
        logoutBtn.addActionListener(e -> {
            if (UIHelper.confirm(this, "Logout", "Are you sure you want to log out?")) {
                dispose();
                new LoginWindow().setVisible(true);
            }
        });
        rightPanel.add(logoutBtn);

        header.add(rightPanel, "right");

        // Add a component listener to hide clock/user info on small screens
        addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                boolean wide = getWidth() > 1150;
                clockLbl.setVisible(wide);
                userBox.setVisible(getWidth() > 950);
            }
        });

        // Clock ticker
        Timer t = new Timer(1000, e -> {
            clockLbl.setText(java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd MMM  HH:mm:ss")));
        });
        t.setInitialDelay(0);
        t.start();

        mainArea.add(header, "growx");

        // Content
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setOpaque(false);

        // Footer for Sync Status
        JPanel footer = new JPanel(new MigLayout("ins 4 24 4 24", "[grow]"));
        footer.setBackground(new Color(0xf8fafc));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIHelper.BORDER));
        JLabel statusLbl = new JLabel("System Ready");
        statusLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        statusLbl.setForeground(UIHelper.TEXT_LIGHT);
        footer.add(statusLbl, "growx");

        com.bhspl.service.SyncService
                .setStatusListener(msg -> SwingUtilities.invokeLater(() -> statusLbl.setText("Sync: " + msg)));

        contentPanel.add(dashboard, "Dashboard");
        // Panels are loaded lazily when their navigation button is clicked

        mainArea.add(contentPanel, "grow, push, wmin 0");
        mainArea.add(footer, "growx");
        add(mainArea, "grow, push");

        if (firstBtn != null)
            firstBtn.setSelected(true);
    }

    private void ensurePanelLoaded(String name) {
        if (panelCache.containsKey(name))
            return;

        JPanel panel = null;
        switch (name) {
            case "Employee Master":
                panel = new EmployeePanel();
                break;
            case "Attendance":
                panel = new AttendancePanel();
                break;
            case "Raw Punch Log":
                panel = new RawPunchLogPanel();
                break;
            case "Leave Manager":
                panel = new LeavePanel();
                break;
            case "OD Requests":
                panel = new ODRequestPanel();
                break;
            case "Leave Policy":
                panel = new LeavePolicyPanel();
                break;
            case "Leave Balance":
                panel = new LeaveBalancePanel();
                break;
            case "Holidays":
                panel = new HolidayPanel();
                break;
            case "Device Manager":
                panel = new DevicePanel();
                break;
            case "Departments":
                panel = new DepartmentPanel();
                break;
            case "Designations":
                panel = new DesignationPanel();
                break;
            case "Shifts":
                panel = new ShiftPanel();
                break;
            case "Weekly Off":
                panel = new WeeklyOffPanel();
                break;
            case "User Management":
                panel = new UserManagementPanel();
                break;
            case "DB Backup":
                panel = new BackupPanel();
                break;
            case "About":
                panel = new AboutPanel();
                break;
            case "Settings":
                panel = new SettingsPanel();
                break;
            case "Reports":
                reportsPanel = new ReportsPanel(false);
                panel = reportsPanel;
                break;
        }

        if (panel != null) {
            panelCache.put(name, panel);
            contentPanel.add(panel, name);
            contentPanel.revalidate();
            contentPanel.repaint();
        }
    }

    private JToggleButton createNavButton(String name, String iconPath) {
        FlatSVGIcon icon = new FlatSVGIcon("icons/" + iconPath, 18, 18);
        JToggleButton btn = new JToggleButton(name, icon);
        btn.setIconTextGap(15);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        btn.setForeground(UIHelper.TEXT_LIGHT);
        btn.setBackground(UIHelper.SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(14, 24, 14, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            btn.setBackground(selected ? UIHelper.SIDEBAR_SEL : UIHelper.SIDEBAR_BG);
            btn.setForeground(selected ? UIHelper.SIDEBAR_TEXT_SEL : UIHelper.TEXT_LIGHT);
            btn.setFont(selected ? new Font("Segoe UI", Font.BOLD, 16) : new Font("Segoe UI", Font.PLAIN, 16));

            if (selected) {
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 5, 0, 0, UIHelper.PRIMARY),
                        new EmptyBorder(14, 19, 14, 10)));
            } else {
                btn.setBorder(new EmptyBorder(14, 24, 14, 10));
            }

            if (selected) {
                if (!name.contains("\u25B6") && !name.contains("\u25BC")) {
                    cardLayout.show(contentPanel, name);
                    pageTitle.setText(name);
                }
            }
        });

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!btn.isSelected())
                    btn.setBackground(UIHelper.SIDEBAR_HOVER);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!btn.isSelected())
                    btn.setBackground(UIHelper.SIDEBAR_BG);
            }
        });

        return btn;
    }

    private JToggleButton createSubNavButton(String name) {
        JToggleButton btn = new JToggleButton(name);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setForeground(UIHelper.TEXT_LIGHT);
        btn.setBackground(UIHelper.SIDEBAR_BG);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(10, 45, 10, 10));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addItemListener(e -> {
            boolean selected = e.getStateChange() == ItemEvent.SELECTED;
            btn.setBackground(selected ? UIHelper.SIDEBAR_SEL : UIHelper.SIDEBAR_BG);
            btn.setForeground(selected ? UIHelper.SIDEBAR_TEXT_SEL : UIHelper.TEXT_LIGHT);
            btn.setFont(selected ? new Font("Segoe UI", Font.BOLD, 14) : new Font("Segoe UI", Font.PLAIN, 14));

            if (selected) {
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 4, 0, 0, UIHelper.PRIMARY),
                        new EmptyBorder(10, 41, 10, 10)));
            } else {
                btn.setBorder(new EmptyBorder(10, 45, 10, 10));
            }
        });

        return btn;
    }

}
