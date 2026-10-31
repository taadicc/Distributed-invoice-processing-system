package net.unibl.etf.mdp.approvalclient.ui;

import net.unibl.etf.mdp.approvalclient.mq.InvoicePublisher;
import net.unibl.etf.mdp.common.config.Status;
import net.unibl.etf.mdp.common.config.Topics;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.InvoiceItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class ApprovalFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ApprovalFrame.class);

    private static final Color POZADINA = new Color(246, 247, 249);
    private static final Color KARTICA = Color.WHITE;
    private static final Color IVICA = new Color(223, 226, 231);
    private static final Color TEKST = new Color(32, 36, 44);
    private static final Color TEKST_SLAB = new Color(120, 128, 140);
    private static final Color AKCENAT = new Color(37, 99, 235);
    private static final Color ZELENA = new Color(22, 143, 90);
    private static final Color CRVENA = new Color(200, 52, 52);
    private static final Color IZBOR = new Color(232, 240, 254);

    private static final Font F_NASLOV = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font F_LABELA = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_VRIJEDNOST = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font F_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font F_IZNOS = new Font("Segoe UI", Font.BOLD, 24);

    private final DefaultListModel<Invoice> model = new DefaultListModel<>();
    private final JList<Invoice> lista = new JList<>(model);

    private final JLabel brojFakture = new JLabel("—");
    private final JLabel datum = new JLabel("—");
    private final JLabel nazivKlijenta = new JLabel("—");
    private final JLabel jib = new JLabel("—");
    private final JLabel email = new JLabel("—");
    private final JLabel izvorniFajl = new JLabel("—");

    private final JLabel iznosOriginal = new JLabel("—");
    private final JLabel iznosBAM = new JLabel("—");
    private final JLabel izvorKursa = new JLabel("—");

    private final DefaultTableModel stavkeModel = new DefaultTableModel(
            new Object[] { "Opis", "Količina", "Cijena", "Ukupno" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable stavke = new JTable(stavkeModel);

    private final JTextField razlogPolje = new JTextField();
    private final JButton odobriDugme = new JButton("Odobri");
    private final JButton odbijDugme = new JButton("Odbij");

    private final JLabel brojac = new JLabel("0");
    private final JLabel statusTraka = new JLabel("Čeka zahtjeve…");
    private final JPanel prazanPanel = new JPanel(new BorderLayout());
    private final JPanel detaljiPanel = new JPanel(new BorderLayout());
    private final JPanel sadrzajDesno = new JPanel(new BorderLayout());

    public ApprovalFrame() {
        super("Approval klijent — odobravanje faktura");

        napraviKomponente();
        postaviPonasanje();
        prikaziDetalje(null);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1060, 680);
        setMinimumSize(new Dimension(900, 560));
        setLocationRelativeTo(null);
    }

    private void napraviKomponente() {

        getContentPane().setBackground(POZADINA);
        setLayout(new BorderLayout());

        add(napraviZaglavlje(), BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                napraviListu(), napraviDesnuStranu());
        split.setDividerLocation(300);
        split.setBorder(new EmptyBorder(12, 14, 8, 14));
        split.setBackground(POZADINA);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);

        statusTraka.setFont(F_LABELA);
        statusTraka.setForeground(TEKST_SLAB);
        statusTraka.setBorder(new EmptyBorder(8, 18, 10, 18));
        JPanel dno = new JPanel(new BorderLayout());
        dno.setBackground(POZADINA);
        dno.add(statusTraka, BorderLayout.WEST);
        add(dno, BorderLayout.SOUTH);
    }

    private JPanel napraviZaglavlje() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(KARTICA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, IVICA),
                new EmptyBorder(16, 22, 16, 22)));

        JLabel naslov = new JLabel("Odobravanje faktura");
        naslov.setFont(F_NASLOV);
        naslov.setForeground(TEKST);

        JLabel podnaslov = new JLabel("Zahtjevi iznad praga čekaju ručnu odluku");
        podnaslov.setFont(F_LABELA);
        podnaslov.setForeground(TEKST_SLAB);

        JPanel lijevo = new JPanel();
        lijevo.setLayout(new BoxLayout(lijevo, BoxLayout.Y_AXIS));
        lijevo.setOpaque(false);
        naslov.setAlignmentX(Component.LEFT_ALIGNMENT);
        podnaslov.setAlignmentX(Component.LEFT_ALIGNMENT);
        lijevo.add(naslov);
        lijevo.add(Box.createVerticalStrut(3));
        lijevo.add(podnaslov);

        brojac.setFont(new Font("Segoe UI", Font.BOLD, 13));
        brojac.setForeground(Color.WHITE);
        brojac.setHorizontalAlignment(SwingConstants.CENTER);
        brojac.setOpaque(true);
        brojac.setBackground(AKCENAT);
        brojac.setBorder(new EmptyBorder(6, 14, 6, 14));

        JPanel desno = new JPanel(new BorderLayout());
        desno.setOpaque(false);
        JLabel oznaka = new JLabel("Na čekanju  ");
        oznaka.setFont(F_LABELA);
        oznaka.setForeground(TEKST_SLAB);
        desno.add(oznaka, BorderLayout.WEST);
        desno.add(brojac, BorderLayout.CENTER);

        panel.add(lijevo, BorderLayout.WEST);
        panel.add(desno, BorderLayout.EAST);
        return panel;
    }

    private JPanel napraviListu() {

        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer(new InvoiceCellRenderer());
        lista.setFixedCellHeight(62);
        lista.setBackground(KARTICA);
        lista.setBorder(null);

        JScrollPane scroll = new JScrollPane(lista);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(KARTICA);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(KARTICA);
        panel.setBorder(BorderFactory.createLineBorder(IVICA));
        panel.add(zaglavljeSekcije("ZAHTJEVI NA ČEKANJU"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel napraviDesnuStranu() {

        sadrzajDesno.setBackground(KARTICA);
        sadrzajDesno.setBorder(BorderFactory.createLineBorder(IVICA));

        prazanPanel.setBackground(KARTICA);
        JLabel prazno = new JLabel("Izaberite fakturu sa liste", SwingConstants.CENTER);
        prazno.setFont(F_VRIJEDNOST);
        prazno.setForeground(TEKST_SLAB);
        prazanPanel.add(prazno, BorderLayout.CENTER);

        detaljiPanel.setBackground(KARTICA);
        detaljiPanel.add(napraviGornjiDio(), BorderLayout.NORTH);
        detaljiPanel.add(napraviTabeluStavki(), BorderLayout.CENTER);
        detaljiPanel.add(napraviDonjiDio(), BorderLayout.SOUTH);

        sadrzajDesno.add(prazanPanel, BorderLayout.CENTER);
        return sadrzajDesno;
    }

    private JPanel napraviGornjiDio() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(KARTICA);

        JPanel podaci = new JPanel(new GridBagLayout());
        podaci.setBackground(KARTICA);
        podaci.setBorder(new EmptyBorder(18, 22, 14, 22));

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 10, 26);

        dodajPolje(podaci, c, 0, 0, "BROJ FAKTURE", brojFakture, true);
        dodajPolje(podaci, c, 1, 0, "DATUM", datum, false);
        dodajPolje(podaci, c, 2, 0, "IZVORNI FAJL", izvorniFajl, false);

        dodajPolje(podaci, c, 0, 1, "KLIJENT", nazivKlijenta, true);
        dodajPolje(podaci, c, 1, 1, "JIB", jib, false);
        dodajPolje(podaci, c, 2, 1, "EMAIL", email, false);
        
        c.gridx = 3;
        c.gridy = 0;
        c.weightx = 1.0;
        c.fill = GridBagConstraints.HORIZONTAL;
        podaci.add(Box.createHorizontalGlue(), c);
        panel.add(podaci);
        panel.add(linija());
        return panel;
    }

    private void dodajPolje(JPanel panel, GridBagConstraints c,
                            int kolona, int red, String naslov, JLabel vrijednost, boolean bold) {

        JLabel labela = new JLabel(naslov);
        labela.setFont(F_LABELA);
        labela.setForeground(TEKST_SLAB);

        vrijednost.setFont(bold ? F_BOLD : F_VRIJEDNOST);
        vrijednost.setForeground(TEKST);

        JPanel grupa = new JPanel();
        grupa.setLayout(new BoxLayout(grupa, BoxLayout.Y_AXIS));
        grupa.setBackground(KARTICA);
        labela.setAlignmentX(Component.LEFT_ALIGNMENT);
        vrijednost.setAlignmentX(Component.LEFT_ALIGNMENT);
        grupa.add(labela);
        grupa.add(Box.createVerticalStrut(3));
        grupa.add(vrijednost);

        c.gridx = kolona;
        c.gridy = red;
        panel.add(grupa, c);
    }

    private JPanel napraviTabeluStavki() {

        stavke.setFont(F_VRIJEDNOST);
        stavke.setRowHeight(30);
        stavke.setShowVerticalLines(false);
        stavke.setGridColor(new Color(238, 240, 243));
        stavke.setBackground(KARTICA);
        stavke.setSelectionBackground(KARTICA);
        stavke.setSelectionForeground(TEKST);
        stavke.setFocusable(false);
        stavke.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = stavke.getTableHeader();
        header.setFont(F_LABELA);
        header.setForeground(TEKST_SLAB);
        header.setBackground(KARTICA);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, IVICA));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer desno = new DefaultTableCellRenderer();
        desno.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 1; i <= 3; i++) {
            stavke.getColumnModel().getColumn(i).setCellRenderer(desno);
        }
        stavke.getColumnModel().getColumn(0).setPreferredWidth(300);

        JScrollPane scroll = new JScrollPane(stavke);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(KARTICA);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(KARTICA);
        panel.setBorder(new EmptyBorder(4, 22, 4, 22));
        panel.add(zaglavljeSekcije("STAVKE"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel napraviDonjiDio() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(KARTICA);

        panel.add(linija());

        JPanel iznosi = new JPanel(new BorderLayout());
        iznosi.setBackground(KARTICA);
        iznosi.setBorder(new EmptyBorder(14, 22, 14, 22));

        JPanel lijevo = new JPanel();
        lijevo.setLayout(new BoxLayout(lijevo, BoxLayout.Y_AXIS));
        lijevo.setBackground(KARTICA);

        JLabel oznakaOriginal = new JLabel("IZNOS NA FAKTURI");
        oznakaOriginal.setFont(F_LABELA);
        oznakaOriginal.setForeground(TEKST_SLAB);
        iznosOriginal.setFont(F_VRIJEDNOST);
        iznosOriginal.setForeground(TEKST);
        izvorKursa.setFont(F_LABELA);
        izvorKursa.setForeground(TEKST_SLAB);

        oznakaOriginal.setAlignmentX(Component.LEFT_ALIGNMENT);
        iznosOriginal.setAlignmentX(Component.LEFT_ALIGNMENT);
        izvorKursa.setAlignmentX(Component.LEFT_ALIGNMENT);
        lijevo.add(oznakaOriginal);
        lijevo.add(Box.createVerticalStrut(3));
        lijevo.add(iznosOriginal);
        lijevo.add(Box.createVerticalStrut(6));
        lijevo.add(izvorKursa);

        JPanel desno = new JPanel();
        desno.setLayout(new BoxLayout(desno, BoxLayout.Y_AXIS));
        desno.setBackground(KARTICA);

        JLabel oznakaBAM = new JLabel("UKUPNO U BAM");
        oznakaBAM.setFont(F_LABELA);
        oznakaBAM.setForeground(TEKST_SLAB);
        iznosBAM.setFont(F_IZNOS);
        iznosBAM.setForeground(AKCENAT);

        oznakaBAM.setAlignmentX(Component.RIGHT_ALIGNMENT);
        iznosBAM.setAlignmentX(Component.RIGHT_ALIGNMENT);
        desno.add(oznakaBAM);
        desno.add(Box.createVerticalStrut(2));
        desno.add(iznosBAM);

        iznosi.add(lijevo, BorderLayout.WEST);
        iznosi.add(desno, BorderLayout.EAST);
        panel.add(iznosi);

        panel.add(linija());

        JPanel akcije = new JPanel(new GridBagLayout());
        akcije.setBackground(new Color(250, 251, 252));
        akcije.setBorder(new EmptyBorder(14, 22, 16, 22));

        JLabel razlogLabela = new JLabel("RAZLOG ODBIJANJA");
        razlogLabela.setFont(F_LABELA);
        razlogLabela.setForeground(TEKST_SLAB);

        razlogPolje.setFont(F_VRIJEDNOST);
        razlogPolje.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(IVICA),
                new EmptyBorder(7, 10, 7, 10)));

        stilizujDugme(odobriDugme, ZELENA);
        stilizujDugme(odbijDugme, CRVENA);

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.WEST;
        g.insets = new Insets(0, 0, 4, 0);
        akcije.add(razlogLabela, g);

        g.gridy = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(0, 0, 0, 12);
        akcije.add(razlogPolje, g);

        g.gridx = 1;
        g.weightx = 0;
        g.fill = GridBagConstraints.NONE;
        g.insets = new Insets(0, 0, 0, 8);
        akcije.add(odobriDugme, g);

        g.gridx = 2;
        g.insets = new Insets(0, 0, 0, 0);
        akcije.add(odbijDugme, g);

        panel.add(akcije);
        return panel;
    }

    private void stilizujDugme(JButton dugme, Color boja) {
        dugme.setFont(F_BOLD);
        dugme.setForeground(Color.WHITE);
        dugme.setBackground(boja);
        dugme.setFocusPainted(false);
        dugme.setBorderPainted(false);
        dugme.setOpaque(true);
        dugme.setPreferredSize(new Dimension(110, 38));
        dugme.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }

    private JLabel zaglavljeSekcije(String tekst) {
        JLabel labela = new JLabel(tekst);
        labela.setFont(F_LABELA);
        labela.setForeground(TEKST_SLAB);
        labela.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, IVICA),
                new EmptyBorder(12, 16, 10, 16)));
        labela.setOpaque(true);
        labela.setBackground(KARTICA);
        return labela;
    }

    private JPanel linija() {
        JPanel p = new JPanel();
        p.setBackground(IVICA);
        p.setPreferredSize(new Dimension(0, 1));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return p;
    }

    private void postaviPonasanje() {
        lista.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                prikaziDetalje(lista.getSelectedValue());
            }
        });

        odobriDugme.addActionListener(e -> odluci(true));
        odbijDugme.addActionListener(e -> odluci(false));
    }

    public void dodajFakturu(Invoice invoice) {
        SwingUtilities.invokeLater(() -> {
            model.addElement(invoice);
            osvjeziBrojac();
            statusTraka.setText("Novi zahtjev: " + invoice.getId());

            if (model.size() == 1) {
                lista.setSelectedIndex(0);
            }
            toFront();
        });
    }

    private void osvjeziBrojac() {
        brojac.setText(String.valueOf(model.size()));
        brojac.setBackground(model.isEmpty() ? new Color(150, 158, 170) : AKCENAT);
    }

    private void prikaziDetalje(Invoice invoice) {

        boolean imaIzbor = invoice != null;
        odobriDugme.setEnabled(imaIzbor);
        odbijDugme.setEnabled(imaIzbor);
        razlogPolje.setEnabled(imaIzbor);

        sadrzajDesno.removeAll();
        sadrzajDesno.add(imaIzbor ? detaljiPanel : prazanPanel, BorderLayout.CENTER);
        sadrzajDesno.revalidate();
        sadrzajDesno.repaint();

        if (!imaIzbor) {
            return;
        }

        brojFakture.setText(invoice.getId());
        datum.setText(String.valueOf(invoice.getDate()));
        izvorniFajl.setText(invoice.getSourceFile() == null ? "—" : invoice.getSourceFile());

        if (invoice.getClient() != null) {
            nazivKlijenta.setText(invoice.getClient().getName());
            jib.setText(invoice.getClient().getJib());
            email.setText(invoice.getClient().getEmail());
        } else {
            nazivKlijenta.setText("—");
            jib.setText("—");
            email.setText("—");
        }

        stavkeModel.setRowCount(0);
        for (InvoiceItem item : invoice.getItems()) {
            stavkeModel.addRow(new Object[] {
                    item.getDescription(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.lineTotal()
            });
        }

        iznosOriginal.setText(invoice.calculateTotal() + " " + invoice.getCurrency());
        iznosBAM.setText(invoice.getTotalBAM() + " BAM");
        izvorKursa.setText("Izvor kursa: " + invoice.getRateSource());
    }

    private void odluci(boolean odobreno) {

        Invoice invoice = lista.getSelectedValue();
        if (invoice == null) {
            return;
        }

        String razlog = razlogPolje.getText().trim();

        if (!odobreno && razlog.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Za odbijanje fakture obavezno unesite razlog.",
                    "Razlog je obavezan",
                    JOptionPane.WARNING_MESSAGE);
            razlogPolje.requestFocus();
            return;
        }

        odobriDugme.setEnabled(false);
        odbijDugme.setEnabled(false);

        if (odobreno) {
            invoice.setStatus(Status.APPROVED);
            invoice.setRejectionReason(null);
        } else {
            invoice.setStatus(Status.REJECTED);
            invoice.setRejectionReason(razlog);
        }

        String topic = odobreno ? Topics.APPROVED : Topics.REJECTED;

        new Thread(() -> {
            InvoicePublisher.posalji(topic, invoice);

            SwingUtilities.invokeLater(() -> {
                model.removeElement(invoice);
                razlogPolje.setText("");
                osvjeziBrojac();

                statusTraka.setText("Faktura " + invoice.getId()
                        + (odobreno ? " je odobrena" : " je odbijena")
                        + "  •  na čekanju: " + model.size());

                if (!model.isEmpty()) {
                    lista.setSelectedIndex(0);
                } else {
                    prikaziDetalje(null);
                }
            });

            log.info("Faktura {} {}", invoice.getId(), odobreno ? "odobrena" : "odbijena");

        }, "odluka-sender").start();
    }

    private static class InvoiceCellRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel labela = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            if (value instanceof Invoice) {
                Invoice inv = (Invoice) value;
                String naziv = inv.getClient() != null ? inv.getClient().getName() : "—";

                labela.setText("<html><div style='padding:2px 0'>"
                        + "<span style='font-size:12px;font-weight:bold'>" + inv.getId() + "</span><br>"
                        + "<span style='font-size:11px;color:#78808C'>" + naziv + "</span><br>"
                        + "<span style='font-size:11px;color:#2563EB'>"
                        + inv.getTotalBAM() + " BAM</span>"
                        + "</div></html>");
            }

            labela.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, isSelected ? 3 : 0, 1, 0,
                            isSelected ? AKCENAT : IVICA),
                    new EmptyBorder(8, isSelected ? 13 : 16, 8, 16)));

            labela.setBackground(isSelected ? IZBOR : KARTICA);
            labela.setForeground(TEKST);
            labela.setOpaque(true);

            return labela;
        }
    }
}