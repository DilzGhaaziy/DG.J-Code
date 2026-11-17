package id.dg.jcode.practice;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Dialog Cetak: pilih format + isi identitas (opsional), lalu pilih lokasi file. */
final class PrintDialog {
    private PrintDialog() { }

    static void show(Component parent, String code, String output) {
        JRadioButton png = new JRadioButton("PNG", true);
        JRadioButton jpg = new JRadioButton("JPG");
        JRadioButton pdf = new JRadioButton("PDF");
        ButtonGroup g = new ButtonGroup();
        g.add(png); g.add(jpg); g.add(pdf);
        JTextField name = new JTextField(20);
        JTextField nim = new JTextField(20);
        JTextField date = new JTextField(LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")), 20);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        JPanel fmt = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        fmt.add(png); fmt.add(jpg); fmt.add(pdf);
        Object[][] rows = {{"Format", fmt}, {"Nama (opsional)", name}, {"NIM (opsional)", nim}, {"Tanggal (opsional)", date}};
        for (int i = 0; i < rows.length; i++) {
            c.gridx = 0; c.gridy = i; c.fill = GridBagConstraints.NONE;
            form.add(new JLabel((String) rows[i][0]), c);
            c.gridx = 1; c.fill = GridBagConstraints.HORIZONTAL;
            form.add((Component) rows[i][1], c);
        }
        if (JOptionPane.showConfirmDialog(parent, form, "Cetak", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;

        String ext = png.isSelected() ? "png" : jpg.isSelected() ? "jpg" : "pdf";
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter(ext.toUpperCase(), ext));
        fc.setSelectedFile(new File("praktek." + ext));
        if (fc.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith("." + ext)) f = new File(f.getPath() + "." + ext);

        PrintExporter.Info info = new PrintExporter.Info(name.getText().trim(), nim.getText().trim(), date.getText().trim());
        try {
            if (ext.equals("pdf")) PrintExporter.pdf(f, code, output, info);
            else PrintExporter.image(f, ext, code, output, info);
            JOptionPane.showMessageDialog(parent, "Berhasil disimpan:\n" + f.getAbsolutePath(), "Cetak",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(parent, "Gagal menyimpan: " + ex.getMessage(), "Cetak",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
