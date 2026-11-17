package id.dg.jcode.pro;

import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.theme.Fonts;
import id.dg.jcode.ui.theme.ThemeManager;
import id.dg.jcode.ui.theme.ThemeMode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;

/** Pengaturan: tema dan ukuran font editor. */
public final class SettingsDialog extends JDialog {

    public SettingsDialog(Window owner, Settings settings, Runnable onApplied) {
        super(owner, "Pengaturan", ModalityType.APPLICATION_MODAL);

        JComboBox<ThemeMode> theme = new JComboBox<>(ThemeMode.values());
        theme.setSelectedItem(ThemeManager.current());
        JSpinner size = new JSpinner(new SpinnerNumberModel(settings.getEditorFontSize(),
                Settings.MIN_FONT_SIZE, Settings.MAX_FONT_SIZE, 1));
        JLabel preview = new JLabel("int nilai = 42;  // pratinjau");
        preview.setFont(Fonts.code(settings.getEditorFontSize()));
        size.addChangeListener(e -> preview.setFont(Fonts.code((Integer) size.getValue())));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(24, 24, 8, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 0, 4, 16);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("Tema"), c);
        c.gridx = 1;
        form.add(theme, c);
        c.gridx = 0;
        c.gridy = 1;
        form.add(new JLabel("Ukuran font kode"), c);
        c.gridx = 1;
        form.add(size, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        c.insets = new Insets(16, 0, 4, 0);
        form.add(preview, c);

        JButton cancel = new JButton("Batal");
        JButton save = new JButton("Simpan");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            ThemeMode mode = (ThemeMode) theme.getSelectedItem();
            if (mode == null) {
                mode = ThemeMode.NORWAY_TODAY;
            }
            settings.setTheme(mode);
            settings.setEditorFontSize((Integer) size.getValue());
            settings.save();
            dispose();
            ThemeManager.switchTo(mode);
            onApplied.run();
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setBorder(BorderFactory.createEmptyBorder(8, 24, 16, 24));
        buttons.add(cancel);
        buttons.add(save);

        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(save);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }
}
