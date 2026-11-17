package id.dg.jcode.ui;

import id.dg.jcode.AppInfo;
import id.dg.jcode.pro.ProfessionalPanel;
import id.dg.jcode.settings.Settings;
import id.dg.jcode.ui.home.HomePanel;
import id.dg.jcode.ui.icons.AppLogo;
import id.dg.jcode.ui.theme.ThemeDialog;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Jendela utama dengan CardLayout: Beranda, Professional, Praktek.
 * Menu bar hanya dipasang saat mode Professional aktif.
 */
public final class MainFrame extends JFrame {

    public static final String CARD_HOME = "home";
    public static final String CARD_PROFESSIONAL = "professional";
    public static final String CARD_PRACTICE = "practice";

    private final Settings settings;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final ProfessionalPanel professional;
    private final id.dg.jcode.practice.PracticePanel practicePanel;

    public MainFrame(Settings settings) {
        super(AppInfo.NAME);
        this.settings = settings;
        setIconImages(AppLogo.windowIcons());

        professional = new ProfessionalPanel(settings, this::showHome);
        practicePanel = new id.dg.jcode.practice.PracticePanel(() -> cards.show(root, CARD_HOME));

        root.add(new HomePanel(this::showProfessional, this::showPractice, this::openThemeDialog), CARD_HOME);
        root.add(professional, CARD_PROFESSIONAL);
        root.add(practicePanel, CARD_PRACTICE);
        setContentPane(root);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (professional.confirmCloseAll()) {
                    professional.shutdown();
                    dispose();
                    System.exit(0);
                }
            }
        });

        setMinimumSize(new Dimension(960, 640));
        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    public void showHome() {
        setJMenuBar(null);
        cards.show(root, CARD_HOME);
        setTitle(AppInfo.NAME);
        revalidate();
    }

    public void showProfessional() {
        setJMenuBar(professional.menuBar());
        cards.show(root, CARD_PROFESSIONAL);
        setTitle(AppInfo.NAME + " \u2014 Professional");
        revalidate();
    }

    public void showPractice() {
        setJMenuBar(null);
        cards.show(root, CARD_PRACTICE);
        setTitle(AppInfo.NAME + " \u2014 Praktek");
        revalidate();
    }

    private void openThemeDialog() {
        new ThemeDialog(this, settings).setVisible(true);
    }
}