package com.bhspl.ui;

import com.bhspl.db.DatabaseManager;
import com.bhspl.util.UIHelper;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.text.SimpleDateFormat;


/**
 * Premium-styled Employee Add / Edit Form.
 * Matches the application's violet / indigo design language.
 */
public class EmployeeForm extends JDialog {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color C_HEADER      = new Color(0x5B21B6);
    private static final Color C_HEADER2     = new Color(0x7C3AED);
    private static final Color C_LABEL       = new Color(0x374151);
    private static final Color C_FIELD_BORDER= new Color(0xD1D5DB);
    private static final Color C_FIELD_FOCUS = new Color(0x7C3AED);
    private static final Color C_BTN_SAVE    = new Color(0x059669);
    private static final Color C_BTN_CANCEL  = new Color(0xDC2626);
    private static final Color C_BG          = new Color(0xF9FAFB);
    private static final Color C_CARD        = Color.WHITE;
    private static final Font  FNT_TITLE     = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font  FNT_SUB       = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font  FNT_LABEL     = new Font("Segoe UI", Font.BOLD, 12);
    private static final Font  FNT_FIELD     = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font  FNT_BTN       = new Font("Segoe UI", Font.BOLD, 13);

    // ── State ─────────────────────────────────────────────────────────────────
    private final String   empId;
    private final Runnable callback;
    private final DatabaseManager db = DatabaseManager.INSTANCE;
    private final Map<String, JComponent> fields = new LinkedHashMap<>();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    // ── Constructor ───────────────────────────────────────────────────────────
    public EmployeeForm(JFrame parent, String empId, Runnable callback) {
        super(parent, empId == null ? "Add Employee" : "Edit Employee — " + empId, true);
        this.empId    = empId;
        this.callback = callback;
        setSize(740, 680);
        UIHelper.centerWindow(this, 740, 680);
        buildUI();
        if (empId != null) loadData();
        setVisible(true);
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(C_BG);

        root.add(buildHeader(),  BorderLayout.NORTH);
        root.add(buildContent(), BorderLayout.CENTER);
        root.add(buildFooter(),  BorderLayout.SOUTH);

        setContentPane(root);
    }

    /** Gradient header */
    private JPanel buildHeader() {
        JPanel hdr = new JPanel(new MigLayout("ins 0 28 0 28, fill, gap 14", "[] [grow] []", "[]")) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, C_HEADER, getWidth(), 0, C_HEADER2);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        hdr.setPreferredSize(new Dimension(0, 78));

        // SVG employee icon
        JLabel iconLbl = new JLabel();
        try {
            FlatSVGIcon svgIcon = new FlatSVGIcon("icons/employees.svg", 36, 36);
            svgIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> new Color(0xC4B5FD)));
            iconLbl.setIcon(svgIcon);
        } catch (Exception ignored) {}

        JPanel textCol = new JPanel(new MigLayout("ins 0, wrap, gap 0"));
        textCol.setOpaque(false);

        JLabel titleLbl = new JLabel(empId == null ? "New Employee Registration" : "Edit Employee Profile");
        titleLbl.setFont(FNT_TITLE);
        titleLbl.setForeground(Color.WHITE);

        JLabel sub = new JLabel(empId == null
            ? "Fill in the details below and click Save"
            : "Updating Employee ID: " + empId);
        sub.setFont(FNT_SUB);
        sub.setForeground(new Color(0xC4B5FD));

        textCol.add(titleLbl);
        textCol.add(sub);

        // Right: edit/add indicator badge
        JLabel badgeLbl = new JLabel(empId == null ? "NEW" : "EDIT");
        try {
            FlatSVGIcon bIcon = new FlatSVGIcon(empId == null ? "icons/plus.svg" : "icons/edit.svg", 14, 14);
            bIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> new Color(0xFDE68A)));
            badgeLbl.setIcon(bIcon);
            badgeLbl.setIconTextGap(6);
        } catch (Exception ignored) {}

        badgeLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badgeLbl.setForeground(new Color(0xFDE68A));
        badgeLbl.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xFDE68A), 1, true),
            BorderFactory.createEmptyBorder(3, 10, 3, 10)));

        hdr.add(iconLbl, "aligny center");
        hdr.add(textCol, "aligny center, pushx");
        hdr.add(badgeLbl, "aligny center");

        return hdr;
    }

    /** Tabbed content */
    private JTabbedPane buildContent() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tabs.setBackground(C_BG);
        tabs.setForeground(C_LABEL);
        tabs.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));

        tabs.addTab("  Basic Info  ",  wrapTab(buildBasicTab()));
        tabs.addTab("  Contact  ",     wrapTab(buildContactTab()));
        tabs.addTab("  Finance & IDs ",wrapTab(buildFinanceTab()));
        tabs.addTab("  Device  ",      wrapTab(buildDeviceTab()));

        // Colour selected tab
        tabs.addChangeListener(e -> tabs.repaint());
        return tabs;
    }

    private JScrollPane wrapTab(JPanel p) {
        JScrollPane sp = new JScrollPane(p);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(C_BG);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        return sp;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(C_CARD);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE5E7EB)));

        JButton cancelBtn = footerBtn("Cancel", C_BTN_CANCEL);
        try {
            FlatSVGIcon xIcon = new FlatSVGIcon("icons/x.svg", 16, 16);
            xIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
            cancelBtn.setIcon(xIcon);
            cancelBtn.setIconTextGap(8);
        } catch (Exception ignored) {}
        cancelBtn.addActionListener(e -> dispose());

        JButton saveBtn = footerBtn("Save Employee", C_BTN_SAVE);
        try {
            FlatSVGIcon cIcon = new FlatSVGIcon("icons/check.svg", 16, 16);
            cIcon.setColorFilter(new FlatSVGIcon.ColorFilter(c -> Color.WHITE));
            saveBtn.setIcon(cIcon);
            saveBtn.setIconTextGap(8);
        } catch (Exception ignored) {}
        saveBtn.addActionListener(e -> save());

        footer.add(cancelBtn);
        footer.add(saveBtn);
        return footer;
    }

    private JButton footerBtn(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFont(FNT_BTN);
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(160, 38));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.darker()); }
            @Override public void mouseExited(MouseEvent e)  { b.setBackground(bg); }
        });
        return b;
    }

    // ── Tab builders ──────────────────────────────────────────────────────────
    private JPanel buildBasicTab() {
        FormBuilder fb = new FormBuilder();
        fb.add("Emp ID *",        "emp_id",     field("e.g. EMP001"));
        fb.add("Full Name *",     "emp_name",   field("Full name of employee"));
        fb.add("Date of Birth",   "dob",        field("DD-MM-YYYY"));
        fb.add("Date of Join",    "doj",        field(LocalDate.now().format(df)));
        fb.add("Gender",          "gender",     combo("Male", "Female", "Other"));
        fb.add("Blood Group",     "blood_group",combo("A+","A-","B+","B-","O+","O-","AB+","AB-"));
        fb.add("Department *",    "department", comboFromDb("SELECT dept_name FROM departments ORDER BY dept_name"));
        fb.add("Designation *",   "designation",comboFromDb("SELECT desig_name FROM designations ORDER BY level_order"));
        fb.add("Shift",           "shift",      comboFromDb("SELECT shift_name FROM shifts ORDER BY shift_name"));
        fb.add("Status",          "status",     combo("Active", "Inactive"));
        return fb.build();
    }

    private JPanel buildContactTab() {
        FormBuilder fb = new FormBuilder();
        fb.add("Phone *",            "phone",             field("Mobile number"));
        fb.add("Email",              "email",             field("email@example.com"));
        fb.add("Emergency Contact",  "emergency_contact", field("Name / Number"));
        fb.addTextArea("Address",    "address");
        return fb.build();
    }

    private JPanel buildFinanceTab() {
        FormBuilder fb = new FormBuilder();
        fb.add("Basic Salary",  "basic_salary", field("0.00"));
        fb.add("Bank Account",  "bank_account", field("Account No & IFSC"));
        fb.add("PAN Number",    "pan_number",   field("ABCDE1234F"));
        fb.add("Aadhaar",       "aadhaar",      field("12-digit Aadhaar"));
        return fb.build();
    }

    private JPanel buildDeviceTab() {
        FormBuilder fb = new FormBuilder();

        JPanel hint = new JPanel(new BorderLayout());
        hint.setBackground(new Color(0xEDE9FE));
        hint.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xC4B5FD), 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel hl = new JLabel("<html><b>ℹ Device Enroll ID</b><br>" +
            "This is the numeric ID registered on the biometric device.<br>" +
            "Check the device LCD or use Diagnose in Device Manager.</html>");
        hl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hl.setForeground(new Color(0x4C1D95));
        hint.add(hl);
        fb.addFreeComponent(hint);

        fb.add("Device ID",       "device_id",       field("0"));
        fb.add("Finger ID",       "finger_id",       field("0"));
        fb.add("Device Enroll ID","device_enroll_id", field("Enrollment ID from biometric"));
        return fb.build();
    }

    // ── Field factory helpers ─────────────────────────────────────────────────
    private JTextField field(String placeholder) {
        JTextField tf = new JTextField();
        tf.setFont(FNT_FIELD);
        tf.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(C_FIELD_BORDER, 1, true),
            BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        tf.setPreferredSize(new Dimension(280, 34));
        tf.putClientProperty("JTextField.placeholderText", placeholder);

        // Violet focus ring
        tf.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_FIELD_FOCUS, 2, true),
                    BorderFactory.createEmptyBorder(5, 9, 5, 9)));
            }
            @Override public void focusLost(FocusEvent e) {
                tf.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(C_FIELD_BORDER, 1, true),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            }
        });
        return tf;
    }

    private JComboBox<String> combo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items);
        cb.setFont(FNT_FIELD);
        cb.setBackground(Color.WHITE);
        cb.setPreferredSize(new Dimension(280, 34));
        cb.setBorder(BorderFactory.createLineBorder(C_FIELD_BORDER, 1, true));
        return cb;
    }

    private JComboBox<String> comboFromDb(String sql) {
        List<String> list = new ArrayList<>();
        list.add("");
        try {
            List<Map<String, Object>> rows = db.query(sql);
            for (Map<String, Object> r : rows)
                list.add(r.values().iterator().next().toString());
        } catch (Exception ignored) {}
        return combo(list.toArray(new String[0]));
    }

    // ── Load / Save ───────────────────────────────────────────────────────────
    private void loadData() {
        try {
            Map<String, Object> r = db.queryOne("SELECT * FROM employees WHERE emp_id=?", empId);
            if (r == null) return;

            for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
                String k = entry.getKey();
                JComponent c = entry.getValue();
                Object val = r.get(k);
                String s = (val == null) ? "" : val.toString();

                if (val instanceof java.sql.Date || val instanceof java.util.Date)
                    s = new SimpleDateFormat("dd-MM-yyyy").format(val);

                if (c instanceof JTextField) ((JTextField) c).setText(s);
                else if (c instanceof JComboBox) ((JComboBox<?>) c).setSelectedItem(s);
                else if (c instanceof JTextArea) ((JTextArea) c).setText(s);
            }

            // Lock emp_id when editing
            JComponent empIdField = fields.get("emp_id");
            if (empIdField instanceof JTextField tf) {
                tf.setEditable(false);
                tf.setBackground(new Color(0xF3F4F6));
                tf.setForeground(new Color(0x9CA3AF));
            }
        } catch (Exception e) {
            showError("Error loading employee: " + e.getMessage());
        }
    }

    private void save() {
        String eid  = getVal("emp_id");
        String name = getVal("emp_name");

        if (eid.isEmpty() || name.isEmpty()) {
            showWarning("Employee ID and Full Name are required.");
            return;
        }

        try {
            Object[] data = {
                getVal("emp_name"), parseDate("dob"), parseDate("doj"), getVal("gender"),
                getVal("email"), getVal("phone"), getVal("department"), getVal("designation"),
                getVal("shift"), getVal("blood_group"), getVal("address"), getVal("emergency_contact"),
                getVal("bank_account"), getVal("pan_number"), getVal("aadhaar"),
                doubleVal("basic_salary"), getVal("status"),
                intVal("device_id"), intVal("finger_id"),
                getVal("device_enroll_id").isEmpty() ? null : getVal("device_enroll_id")
            };

            if (empId == null) {
                Object[] full = new Object[data.length + 1];
                full[0] = eid;
                System.arraycopy(data, 0, full, 1, data.length);
                db.execute(
                    "INSERT INTO employees (emp_id, emp_name, dob, doj, gender, email, phone, " +
                    "department, designation, shift, blood_group, address, emergency_contact, " +
                    "bank_account, pan_number, aadhaar, basic_salary, status, device_id, finger_id, device_enroll_id) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", full);
            } else {
                Object[] upd = new Object[data.length + 1];
                System.arraycopy(data, 0, upd, 0, data.length);
                upd[data.length] = empId;
                db.execute(
                    "UPDATE employees SET emp_name=?, dob=?, doj=?, gender=?, email=?, phone=?, " +
                    "department=?, designation=?, shift=?, blood_group=?, address=?, emergency_contact=?, " +
                    "bank_account=?, pan_number=?, aadhaar=?, basic_salary=?, status=?, device_id=?, finger_id=?, device_enroll_id=? " +
                    "WHERE emp_id=?", upd);
            }

            showSuccess("Employee saved successfully!");
            if (callback != null) callback.run();
            dispose();
        } catch (Exception e) {
            showError("Error saving employee:\n" + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getVal(String key) {
        JComponent c = fields.get(key);
        if (c instanceof JTextField tf)  return tf.getText().trim();
        if (c instanceof JComboBox<?> cb) { Object o = cb.getSelectedItem(); return o == null ? "" : o.toString().trim(); }
        if (c instanceof JTextArea ta)   return ta.getText().trim();
        return "";
    }

    private java.sql.Date parseDate(String key) {
        String s = getVal(key);
        if (s.isEmpty() || s.contains("D")) return null;
        try {
            if (s.matches("\\d{2}-\\d{2}-\\d{4}")) {
                String[] p = s.split("-");
                return java.sql.Date.valueOf(p[2] + "-" + p[1] + "-" + p[0]);
            }
            return java.sql.Date.valueOf(s);
        } catch (Exception e) { return null; }
    }

    private double doubleVal(String key) {
        try { return Double.parseDouble(getVal(key)); } catch (Exception e) { return 0; }
    }

    private int intVal(String key) {
        try { return Integer.parseInt(getVal(key)); } catch (Exception e) { return 0; }
    }

    private void showSuccess(String msg) {
        styledDialog(msg, "Success", new Color(0x059669), new Color(0xD1FAE5));
    }
    private void showError(String msg) {
        styledDialog(msg, "Error", new Color(0xDC2626), new Color(0xFEE2E2));
    }
    private void showWarning(String msg) {
        styledDialog(msg, "Required", new Color(0xD97706), new Color(0xFEF3C7));
    }

    private void styledDialog(String msg, String title, Color fg, Color bg) {
        JDialog d = new JDialog(this, title, true);
        d.setSize(380, 160);
        UIHelper.centerWindow(d, 380, 160);

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(bg);
        p.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lbl = new JLabel("<html><body style='width:280px'>" + msg + "</body></html>");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(fg);
        p.add(lbl, BorderLayout.CENTER);

        JButton ok = new JButton("OK");
        ok.setFont(FNT_BTN);
        ok.setBackground(fg);
        ok.setForeground(Color.WHITE);
        ok.setFocusPainted(false);
        ok.setBorderPainted(false);
        ok.setOpaque(true);
        ok.setPreferredSize(new Dimension(80, 32));
        ok.addActionListener(e2 -> d.dispose());

        JPanel btnP = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnP.setOpaque(false);
        btnP.add(ok);
        p.add(btnP, BorderLayout.SOUTH);

        d.setContentPane(p);
        d.setVisible(true);
    }

    // ── Inner form builder ────────────────────────────────────────────────────
    private class FormBuilder {
        private final JPanel panel;
        private int row = 0;

        FormBuilder() {
            panel = new JPanel(new GridBagLayout());
            panel.setBackground(C_BG);
            panel.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
        }

        void add(String label, String key, JComponent comp) {
            GridBagConstraints gbcL = new GridBagConstraints();
            gbcL.gridx = 0; gbcL.gridy = row;
            gbcL.anchor = GridBagConstraints.EAST;
            gbcL.insets = new Insets(6, 8, 6, 14);

            JLabel lbl = new JLabel(label + ":");
            lbl.setFont(FNT_LABEL);
            lbl.setForeground(C_LABEL);
            lbl.setPreferredSize(new Dimension(160, 24));
            panel.add(lbl, gbcL);

            GridBagConstraints gbcF = new GridBagConstraints();
            gbcF.gridx = 1; gbcF.gridy = row;
            gbcF.fill = GridBagConstraints.HORIZONTAL;
            gbcF.weightx = 1.0;
            gbcF.insets = new Insets(6, 0, 6, 8);
            panel.add(comp, gbcF);

            if (!fields.containsKey(key)) fields.put(key, comp);
            row++;
        }

        void addTextArea(String label, String key) {
            JTextArea ta = new JTextArea(4, 20);
            ta.setFont(FNT_FIELD);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(C_FIELD_BORDER, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            ta.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) {
                    ta.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(C_FIELD_FOCUS, 2),
                        BorderFactory.createEmptyBorder(5, 9, 5, 9)));
                }
                @Override public void focusLost(FocusEvent e) {
                    ta.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(C_FIELD_BORDER, 1),
                        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
                }
            });
            fields.put(key, ta);
            add(label, key, new JScrollPane(ta));
        }

        void addFreeComponent(JComponent comp) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0; gbc.gridy = row++;
            gbc.gridwidth = 2;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(4, 8, 12, 8);
            panel.add(comp, gbc);
        }

        JPanel build() {
            // Push everything to top
            GridBagConstraints push = new GridBagConstraints();
            push.gridy = row; push.weighty = 1.0;
            panel.add(Box.createVerticalGlue(), push);
            return panel;
        }
    }
}
