package id.dg.jcode.ui.util;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Desktop;
import java.net.URI;

/** Membuka URL di browser default tanpa membuat UI macet. */
public final class Browser {

    private Browser() {
    }

    public static void open(Component parent, String url) {
        Thread worker = new Thread(() -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url).start();
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(parent,
                        "Browser tidak bisa dibuka otomatis.\nSilakan buka alamat ini secara manual:\n" + url,
                        "Buka tautan", JOptionPane.WARNING_MESSAGE));
            }
        }, "open-browser");
        worker.setDaemon(true);
        worker.start();
    }
}
