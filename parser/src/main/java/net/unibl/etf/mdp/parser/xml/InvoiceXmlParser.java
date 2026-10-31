package net.unibl.etf.mdp.parser.xml;

import net.unibl.etf.mdp.common.model.Client;
import net.unibl.etf.mdp.common.model.Invoice;
import net.unibl.etf.mdp.common.model.InvoiceItem;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class InvoiceXmlParser {

    private InvoiceXmlParser() {
    }

    public static Invoice parse(byte[] xmlBajtovi) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xmlBajtovi));
        doc.getDocumentElement().normalize();

        Element root = doc.getDocumentElement();

        Invoice invoice = new Invoice();
        invoice.setId(tekst(root, "id"));
        invoice.setCurrency(tekst(root, "currency"));

        String datum = tekst(root, "date");
        if (datum != null && !datum.isBlank()) {
            invoice.setDate(LocalDate.parse(datum.trim()));
        }

        Element clientEl = prviElement(root, "client");
        if (clientEl != null) {
            Client client = new Client();
            client.setName(tekst(clientEl, "name"));
            client.setJib(tekst(clientEl, "jib"));
            client.setEmail(tekst(clientEl, "email"));
            invoice.setClient(client);
        }

        NodeList stavke = root.getElementsByTagName("item");
        for (int i = 0; i < stavke.getLength(); i++) {
            Node node = stavke.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element itemEl = (Element) node;

            InvoiceItem item = new InvoiceItem();
            item.setDescription(tekst(itemEl, "description"));

            String kolicina = tekst(itemEl, "quantity");
            if (kolicina != null && !kolicina.isBlank()) {
                item.setQuantity(Integer.parseInt(kolicina.trim()));
            }

            String cijena = tekst(itemEl, "unitPrice");
            if (cijena != null && !cijena.isBlank()) {
                item.setUnitPrice(new BigDecimal(cijena.trim()));
            }

            invoice.getItems().add(item);
        }

        return invoice;
    }

    private static Element prviElement(Element roditelj, String naziv) {
        NodeList lista = roditelj.getElementsByTagName(naziv);
        if (lista.getLength() == 0) {
            return null;
        }
        Node node = lista.item(0);
        return node.getNodeType() == Node.ELEMENT_NODE ? (Element) node : null;
    }

    private static String tekst(Element roditelj, String naziv) {
        Element el = prviElement(roditelj, naziv);
        return el == null ? null : el.getTextContent().trim();
    }
}