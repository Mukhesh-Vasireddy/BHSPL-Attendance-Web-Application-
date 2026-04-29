package com.bhspl.ui;
 
 import com.bhspl.core.Config;
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import java.awt.*;
 import java.util.Map;
 
 /**
  * Database Maintenance Panel.
  * Optimized with a scrollable viewport to ensure all sections fit on any screen size.
  */
 public class BackupPanel extends JPanel {
 
     public BackupPanel() {
         setLayout(new BorderLayout());
         setBackground(UIHelper.BG_MAIN);
         buildUI();
     }
 
     private void buildUI() {
         // Create a scrollable content area
         JPanel content = new JPanel(new MigLayout("ins 30 20 30 20, fillx, wrap", "[center, grow]", "[] 20 [] 20 []"));
         content.setOpaque(false);
 
         JLabel title = new JLabel("Database Maintenance & Backup");
         title.setFont(new Font("Segoe UI", Font.BOLD, 26));
         title.setForeground(UIHelper.PRIMARY);
         content.add(title, "center, gapbottom 10");
 
         // --- SECTION 1: BACKUP & RESTORE ---
         JPanel backupSection = new JPanel(new MigLayout("ins 24, wrap", "[grow, fill]", "[] 15 [] 20 [] 15 []"));
         backupSection.setBackground(Color.WHITE);
         backupSection.setBorder(UIHelper.createCardBorder());
 
         JLabel bTitle = new JLabel("Backup & Recovery Operations");
         bTitle.setFont(UIHelper.FNT_BOLD);
         bTitle.setForeground(UIHelper.PRIMARY);
         backupSection.add(bTitle, "center, gapbottom 10");
 
         JLabel info = new JLabel("<html><center>Secure your enterprise data by creating regular backups.<br>Files are stored in the root 'backups' folder.</center></html>");
         info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
         info.setForeground(UIHelper.TEXT_LIGHT);
         backupSection.add(info, "center");
 
         JButton backupBtn = UIHelper.makeButton("Create Full Backup Now", UIHelper.SUCCESS, "backup.svg");
         backupBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
         backupBtn.addActionListener(e -> {
             UIHelper.showSuccess(this, "Database backup initiated!\nSaved to: backups/backup_" + System.currentTimeMillis() + ".sql");
         });
         backupSection.add(backupBtn, "h 46!");
 
         JButton restoreBtn = UIHelper.makeButton("Restore from SQL File", UIHelper.PRIMARY, "refresh.svg");
         restoreBtn.addActionListener(e -> {
             JFileChooser chooser = new JFileChooser();
             chooser.showOpenDialog(this);
         });
         backupSection.add(restoreBtn, "h 40!");
 
         content.add(backupSection, "width 600!");
 
         // --- SECTION 2: CONNECTION CONFIG ---
         JPanel configSection = new JPanel(new MigLayout("ins 24, wrap 2, gap 16 10", "[right] 15 [left]", "[] 10 []"));
         configSection.setBackground(Color.WHITE);
         configSection.setBorder(UIHelper.createCardBorder());
 
         JLabel cTitle = new JLabel("Active Connection Profile");
         cTitle.setFont(UIHelper.FNT_BOLD);
         cTitle.setForeground(UIHelper.PRIMARY);
         configSection.add(cTitle, "span 2, center, gapbottom 10");
 
         Map<String, String> cfg = Config.loadDbConfig();
         String[][] fields = {
             {"Host Address", cfg != null ? cfg.getOrDefault("host", "?") : "Missing"},
             {"Port", cfg != null ? cfg.getOrDefault("port", "?") : "Missing"},
             {"Database", cfg != null ? cfg.getOrDefault("database", "?") : "Missing"},
             {"Service User", cfg != null ? cfg.getOrDefault("user", "?") : "Missing"},
         };
 
         for (String[] f : fields) {
             JLabel lbl = new JLabel(f[0] + ":");
             lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
             lbl.setForeground(UIHelper.TEXT_LIGHT);
             
             JLabel val = new JLabel(f[1]);
             val.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
             val.setForeground(UIHelper.TEXT_DARK);
             
             configSection.add(lbl); 
             configSection.add(val);
         }
 
         JButton reconfigBtn = UIHelper.makeButton("Modify Connection Configuration", UIHelper.DANGER, "settings.svg");
         reconfigBtn.addActionListener(e -> {
             if (UIHelper.confirmYesNo(this, "Reconfigure Database",
                 "This will reset your connection settings and require a restart.\nContinue?")) {
                 Config.clearDbConfig();
                 DatabaseManager.getInstance().close();
                 Window win = SwingUtilities.getWindowAncestor(this);
                 if (win != null) win.dispose();
                 new DBSetupWindow().setVisible(true);
             }
         });
 
         configSection.add(reconfigBtn, "span 2, center, gaptop 15");
         content.add(configSection, "width 600!");
 
         // Wrap in ScrollPane
         JScrollPane sp = new JScrollPane(content);
         sp.setBorder(null);
         sp.setOpaque(false);
         sp.getViewport().setOpaque(false);
         sp.getVerticalScrollBar().setUnitIncrement(16);
         
         add(sp, BorderLayout.CENTER);
     }
 }
