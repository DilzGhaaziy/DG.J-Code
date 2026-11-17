package id.dg.jcode.pro;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/** Wizard sederhana untuk membuat proyek Java Application baru. */
public final class NewProjectWizard extends JDialog {

    private static final String PKG_REGEX = "([a-z_][a-z0-9_]*)(\\.[a-z_][a-z0-9_]*)*";
    private static final String CLASS_REGEX = "[A-Za-z_$][A-Za-z0-9_$]*";

    private final JTextField nameField = new JTextField(24);
    private final JTextField locationField = new JTextField(24);
    private final JTextField packageField = new JTextField(24);
    private final JTextField mainField = new JTextField("Main", 24);
    private final JLabel preview = new JLabel(" ");
    private final JLabel error = new JLabel(" ");
    private Project result;
    private boolean packageEdited;

    private NewProjectWizard(Window owner) {
        super(owner, "Proyek Baru \u2014 Java Application", ModalityType.APPLICATION_MODAL);
        Path base = Path.of(System.getProperty("user.home"), "DGJCodeProjects");
        locationField.setText(base.toString());
        nameField.setText(uniqueName(base));
        packageField.setText(suggestPackage(nameField.getText()));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 8);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        JButton browse = new JButton("Pilih...");
        browse.addActionListener(e -> chooseLocation());

        addRow(form, c, 0, "Nama proyek", nameField, null);
        addRow(form, c, 1, "Lokasi", locationField, browse);
        addRow(form, c, 2, "Paket (boleh kosong)", packageField, null);
        addRow(form, c, 3, "Main class", mainField, null);

        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 3;
        form.add(preview, c);
        c.gridy = 5;
        error.setForeground(new java.awt.Color(0xCF222E));
        form.add(error, c);

        JButton cancel = new JButton("Batal");
        JButton create = new JButton("Buat Proyek");
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> create());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
        buttons.add(cancel);
        buttons.add(create);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(create);

        DocumentListener update = simpleListener(this::updatePreview);
        nameField.getDocument().addDocumentListener(simpleListener(() -> {
            if (!packageEdited) {
                packageField.setText(suggestPackage(nameField.getText()));
            }
            updatePreview();
        }));
        locationField.getDocument().addDocumentListener(update);
        packageField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyTyped(java.awt.event.KeyEvent e) {
                packageEdited = true;
            }
        });

        updatePreview();
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    public static Optional<Project> show(Window owner) {
        NewProjectWizard w = new NewProjectWizard(owner);
        w.setVisible(true);
        return Optional.ofNullable(w.result);
    }

    private void addRow(JPanel form, GridBagConstraints c, int row, String label,
                        JTextField field, JButton extra) {
        c.gridy = row;
        c.gridwidth = 1;
        c.gridx = 0;
        c.weightx = 0;
        form.add(new JLabel(label), c);
        c.gridx = 1;
        c.weightx = 1;
        form.add(field, c);
        if (extra != null) {
            c.gridx = 2;
            c.weightx = 0;
            form.add(extra, c);
        }
    }

    private void chooseLocation() {
        JFileChooser chooser = new JFileChooser(locationField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Pilih lokasi proyek");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            locationField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void updatePreview() {
        preview.setText("Folder proyek: " + Path.of(locationField.getText().trim(), nameField.getText().trim()));
        error.setText(" ");
    }

    private void create() {
        String name = nameField.getText().trim();
        String pkg = packageField.getText().trim();
        String main = mainField.getText().trim();
        if (!name.matches("[A-Za-z0-9_\\- ]+")) {
            error.setText("Nama proyek hanya boleh huruf, angka, spasi, - dan _.");
            return;
        }
        if (!pkg.isEmpty() && !pkg.matches(PKG_REGEX)) {
            error.setText("Nama paket memakai huruf kecil dan titik, contoh: belajar.java");
            return;
        }
        if (!main.matches(CLASS_REGEX)) {
            error.setText("Nama class harus diawali huruf dan tanpa spasi, contoh: Main");
            return;
        }
        try {
            Path parent = Path.of(locationField.getText().trim());
            Files.createDirectories(parent);
            result = Project.create(parent, name, pkg, main);
            dispose();
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Proyek gagal dibuat:\n" + ex.getMessage(),
                    "Proyek Baru", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String uniqueName(Path base) {
        for (int i = 1; i < 1000; i++) {
            String candidate = "JavaApplication" + i;
            if (!Files.exists(base.resolve(candidate))) {
                return candidate;
            }
        }
        return "JavaApplication";
    }

    private static String suggestPackage(String name) {
        String p = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "");
        if (p.isEmpty() || Character.isDigit(p.charAt(0))) {
            p = "app" + p;
        }
        return p;
    }

    private static DocumentListener simpleListener(Runnable r) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                r.run();
            }
        };
    }
}
