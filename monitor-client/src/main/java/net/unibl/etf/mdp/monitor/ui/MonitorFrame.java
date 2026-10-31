package net.unibl.etf.mdp.monitor.ui;

import net.unibl.etf.mdp.monitor.model.Notification;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class MonitorFrame extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final Color POZADINA = new Color(246, 247, 249);
    private static final Color KARTICA = Color.WHITE;
    private static final Color IVICA = new Color(223, 226, 231);
    private static final Color TEKST = new Color(32, 36, 44);
    private static final Color TEKST_SLAB = new Color(120, 128, 140);
    private static final Color AKCENAT = new Color(37, 99, 235);
    private static final Color ZELENA = new Color(22, 143, 90);
    private static final Color NOVO = new Color(240, 249, 244);

    private static final Font F_NASLOV = new Font("Segoe UI", Font.BOLD, 17);
    private static final Font F_LABELA = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font F_TABELA = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font F_BROJ = new Font("Segoe UI", Font.BOLD, 20);

    private static final DateTimeFormatter VRIJEME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Vrijeme", "Faktura", "Klijent", "Iznos (BAM)", "Status" }, 0) {
        private static final long serialVersionUID = 1L;

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabela = new JTable(model);

    private final JLabel brojNotifikacija = new JLabel("0");
    private final JLabel ukupnoBAM = new JLabel("0.00");
    private final JLabel statusTraka = new JLabel("Sluša multicast grupu…");

    private final String imeInstance;

    private BigDecimal zbir = BigDecimal.ZERO;
    private int posljednjiNovi = -1;

    public MonitorFrame(String imeInstance, String grupa, int port) {
        super("Monitor — " + imeInstance);
        this.imeInstance = imeInstance;

        napraviKomponente(grupa, port);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(940, 560);
        setMinimumSize(new Dimension(760, 420));
        setLocationByPlatform(true);
    }

    private void napraviKomponente(String grupa, int port) {

        getContentPane().setBackground(POZADINA);
        setLayout(new BorderLayout());

        add(napraviZaglavlje(grupa, port), BorderLayout.NORTH);
        add(napraviTabelu(), BorderLayout.CENTER);

        statusTraka.setFont(F_LABELA);
        statusTraka.setForeground(TEKST_SLAB);
        statusTraka.setBorder(new EmptyBorder(8, 18, 10, 18));

        JPanel dno = new JPanel(new BorderLayout());
        dno.setBackground(POZADINA);
        dno.add(statusTraka, BorderLayout.WEST);
        add(dno, BorderLayout.SOUTH);
    }

    private JPanel napraviZaglavlje(String grupa, int port) {

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(KARTICA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, IVICA),
                new EmptyBorder(16, 22, 16, 22)));

        JLabel naslov = new JLabel("Odobrene fakture — " + imeInstance);
        naslov.setFont(F_NASLOV);
        naslov.setForeground(TEKST);

        JLabel podnaslov = new JLabel("UDP multicast " + grupa + ":" + port);
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

        JPanel desno = new JPanel();
        desno.setLayout(new BoxLayout(desno, BoxLayout.X_AXIS));
        desno.setOpaque(false);
        desno.add(metrika("NOTIFIKACIJA", brojNotifikacija, AKCENAT));
        desno.add(Box.createHorizontalStrut(28));
        desno.add(metrika("UKUPNO BAM", ukupnoBAM, ZELENA));

        panel.add(lijevo, BorderLayout.WEST);
        panel.add(desno, BorderLayout.EAST);
        return panel;
    }

    private JPanel metrika(String naslov, JLabel vrijednost, Color boja) {
        JLabel labela = new JLabel(naslov);
        labela.setFont(F_LABELA);
        labela.setForeground(TEKST_SLAB);

        vrijednost.setFont(F_BROJ);
        vrijednost.setForeground(boja);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        labela.setAlignmentX(Component.RIGHT_ALIGNMENT);
        vrijednost.setAlignmentX(Component.RIGHT_ALIGNMENT);
        panel.add(labela);
        panel.add(Box.createVerticalStrut(2));
        panel.add(vrijednost);
        return panel;
    }

    private JPanel napraviTabelu() {

        tabela.setFont(F_TABELA);
        tabela.setRowHeight(34);
        tabela.setShowVerticalLines(false);
        tabela.setGridColor(new Color(238, 240, 243));
        tabela.setBackground(KARTICA);
        tabela.setIntercellSpacing(new Dimension(0, 0));
        tabela.setSelectionBackground(new Color(232, 240, 254));
        tabela.setSelectionForeground(TEKST);

        JTableHeader header = tabela.getTableHeader();
        header.setFont(F_LABELA);
        header.setForeground(TEKST_SLAB);
        header.setBackground(KARTICA);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, IVICA));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 34));

        TableCellRenderer renderer = new RedRenderer();
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        tabela.getColumnModel().getColumn(0).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(240);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(140);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(120);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(KARTICA);

        JButton ocisti = new JButton("Očisti prikaz");
        ocisti.setFont(F_LABELA);
        ocisti.setFocusPainted(false);
        ocisti.addActionListener(e -> {
            model.setRowCount(0);
            zbir = BigDecimal.ZERO;
            posljednjiNovi = -1;
            osvjeziMetrike();
            statusTraka.setText("Prikaz očišćen");
        });

        JPanel alatna = new JPanel(new BorderLayout());
        alatna.setBackground(KARTICA);
        alatna.setBorder(new EmptyBorder(8, 16, 8, 16));
        JLabel oznaka = new JLabel("PRIMLJENE NOTIFIKACIJE");
        oznaka.setFont(F_LABELA);
        oznaka.setForeground(TEKST_SLAB);
        alatna.add(oznaka, BorderLayout.WEST);
        alatna.add(ocisti, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(KARTICA);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(12, 14, 8, 14),
                BorderFactory.createLineBorder(IVICA)));
        panel.add(alatna, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    public void dodajNotifikaciju(Notification n) {
        SwingUtilities.invokeLater(() -> {

            model.insertRow(0, new Object[] {
                    LocalTime.now().format(VRIJEME),
                    n.getInvoiceId(),
                    n.getClientName(),
                    n.getTotalBAM() == null ? "—" : n.getTotalBAM().toPlainString(),
                    n.getStatus()
            });

            if (n.getTotalBAM() != null) {
                zbir = zbir.add(n.getTotalBAM());
            }

            posljednjiNovi = 0;
            osvjeziMetrike();

            statusTraka.setText("Posljednja notifikacija: " + n.getInvoiceId()
                    + "  •  " + LocalTime.now().format(VRIJEME));

            tabela.repaint();
        });
    }

    private void osvjeziMetrike() {
        brojNotifikacija.setText(String.valueOf(model.getRowCount()));
        ukupnoBAM.setText(zbir.toPlainString());
    }

    private class RedRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setBorder(new EmptyBorder(0, 12, 0, 12));
            setHorizontalAlignment(column == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);

            if (!isSelected) {
                setBackground(row == posljednjiNovi ? NOVO
                        : (row % 2 == 0 ? KARTICA : new Color(250, 251, 252)));
                setForeground(column == 3 ? ZELENA : TEKST);
            }

            if (column == 3) {
                setFont(F_TABELA.deriveFont(Font.BOLD));
            } else {
                setFont(F_TABELA);
            }

            return this;
        }
    }
}