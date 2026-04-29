package com.bhspl.ui;
 
 import com.bhspl.db.DatabaseManager;
 import com.bhspl.util.UIHelper;
 import net.miginfocom.swing.MigLayout;
 
 import javax.swing.*;
 import java.awt.*;
 import java.sql.SQLException;
 import java.util.Map;
 
 /**
  * Premium Designation Form popup with modern styling.
  */
 public class DesigForm extends JDialog {
     private final Integer id;
     private final Runnable callback;
     private final DatabaseManager db = DatabaseManager.INSTANCE;
     private JTextField nameField, levelField, descField;
     private JComboBox<String> statusCombo;
 
     public DesigForm(JFrame parent, Integer id, Runnable callback) {
         super(parent, id == null ? "Add Designation" : "Edit Designation", true);
         this.id = id;
         this.callback = callback;
         
         setSize(450, 480);
         UIHelper.centerWindow(this, 450, 480);
         buildUI();
         if (id != null) loadData();
         setVisible(true);
     }
 
     private void buildUI() {
         JPanel root = new JPanel(new MigLayout("fill, ins 0, gap 0, wrap", "[grow]", "[] [grow] []"));
         root.setBackground(Color.WHITE);
 
         // Header
         UIHelper.GradientPanel hdr = new UIHelper.GradientPanel(new Color(0x4F46E5), new Color(0x7C3AED));
         hdr.setLayout(new MigLayout("ins 20", "[] 15 [grow]"));
         
         try {
             com.formdev.flatlaf.extras.FlatSVGIcon icon = new com.formdev.flatlaf.extras.FlatSVGIcon("icons/reports.svg", 32, 32);
             icon.setColorFilter(new com.formdev.flatlaf.extras.FlatSVGIcon.ColorFilter(c -> Color.WHITE));
             hdr.add(new JLabel(icon));
         } catch (Exception ignored) {}
         
         JLabel title = new JLabel(id == null ? "New Designation" : "Update Designation");
         title.setFont(new Font("Segoe UI", Font.BOLD, 18));
         title.setForeground(Color.WHITE);
         hdr.add(title);
         root.add(hdr, "growx, h 70!");
 
         // Form Area
         JPanel form = new JPanel(new MigLayout("ins 25, wrap 1, gapy 12, fillx", "[grow, fill]"));
         form.setBackground(Color.WHITE);
 
         form.add(createLabel("Designation Name *"));
         nameField = createTextField("e.g. Senior Software Engineer");
         form.add(nameField);
 
         form.add(createLabel("Level Order (Hierarchy)"));
         levelField = createTextField("e.g. 1 (Higher number = Lower level)");
         form.add(levelField);
 
         form.add(createLabel("Description"));
         descField = createTextField("Short description of role...");
         form.add(descField);
 
         form.add(createLabel("Status"));
         statusCombo = new JComboBox<>(new String[]{"Active", "Inactive"});
         statusCombo.setFont(new Font("Segoe UI", Font.PLAIN, 14));
         form.add(statusCombo);
 
         root.add(form, "grow, push");
 
         // Footer
         JPanel footer = new JPanel(new MigLayout("ins 20, gap 12", "push [] []"));
         footer.setBackground(new Color(0xF8FAFC));
         
         JButton cancelBtn = UIHelper.makeButton("Cancel", new Color(0x64748B), "x.svg");
         cancelBtn.addActionListener(e -> dispose());
         
         JButton saveBtn = UIHelper.makeButton("Save Designation", UIHelper.SUCCESS, "check.svg");
         saveBtn.addActionListener(e -> save());
         
         footer.add(cancelBtn);
         footer.add(saveBtn);
         root.add(footer, "growx");
 
         setContentPane(root);
     }
 
     private JLabel createLabel(String text) {
         JLabel l = new JLabel(text);
         l.setFont(new Font("Segoe UI", Font.BOLD, 13));
         l.setForeground(new Color(0x475569));
         return l;
     }
 
     private JTextField createTextField(String placeholder) {
         JTextField f = new JTextField();
         f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
         f.putClientProperty("JTextField.placeholderText", placeholder);
         return f;
     }
 
     private void loadData() {
         try {
             Map<String, Object> r = db.queryOne("SELECT * FROM designations WHERE id=?", id);
             if (r != null) {
                 nameField.setText(DatabaseManager.str(r, "desig_name"));
                 levelField.setText(DatabaseManager.str(r, "level_order"));
                 descField.setText(DatabaseManager.str(r, "description"));
                 statusCombo.setSelectedItem(DatabaseManager.str(r, "status"));
             }
         } catch (Exception e) {
             e.printStackTrace();
         }
     }
 
     private void save() {
         String name = nameField.getText().trim();
         if (name.isEmpty()) {
             UIHelper.showError(this, "Designation name is required.");
             return;
         }
 
         try {
             int level = 0;
             try {
                 if (!levelField.getText().trim().isEmpty()) {
                     level = Integer.parseInt(levelField.getText().trim());
                 }
             } catch (NumberFormatException nfe) {
                 UIHelper.showError(this, "Level must be a number.");
                 return;
             }
 
             if (id == null) {
                 db.execute("INSERT INTO designations (desig_name, level_order, description, status) VALUES (?,?,?,?)",
                     name, level, descField.getText().trim(), statusCombo.getSelectedItem());
             } else {
                 db.execute("UPDATE designations SET desig_name=?, level_order=?, description=?, status=? WHERE id=?",
                     name, level, descField.getText().trim(), statusCombo.getSelectedItem(), id);
             }
             
             if (callback != null) callback.run();
             dispose();
         } catch (SQLException e) {
             UIHelper.showError(this, "Error saving designation: " + e.getMessage());
         }
     }
 }
