import org.w3c.dom.*;
import org.xml.sax.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import java.io.*;
import javax.xml.transform.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class Main {
    public static void main (String[] args) throws ParserConfigurationException, IOException, SAXException, TransformerException, XPathExpressionException, XPathExpressionException {
        // Path va gérer lui même quel type de slash il faut mettre
        Path cheminRacineProjet = Paths.get(System.getProperty("user.dir"));
        Path cheminFichierBpmn = Paths.get("BPMN/POC.bpmn");
        Path cheminDossierRPA = cheminRacineProjet.resolve(Paths.get("RPA/"));

        // Concaténation de tout pour aboutir au chemin du fichier
        Path cheminFichier = cheminRacineProjet.resolve(cheminFichierBpmn);
        // Création du fichier BPMN
        File fichierBPMN = new File(cheminFichier.toString());

        // Permet de créer notre un builder
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        // Permet de créer notre parser
        final DocumentBuilder builder = factory.newDocumentBuilder();
        // Document issue de fichier dom ou de flux .xml
        Document documentBPMN = builder.parse(fichierBPMN);
        //optional, but recommended
        //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
        documentBPMN.getDocumentElement().normalize();
        // Racine du document
        Element racine = documentBPMN.getDocumentElement();

        // Utilisation de XPath
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        String expression = null;
        XPathExpression xPathExpression = null;
        NodeList nodeList = null;

        // Récupération de la liste des participants
        expression = "/definitions/collaboration/participant";
        xPathExpression = xpath.compile(expression);
        nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);

        // Parcourt la liste des participant et ajoute pour chacun une racine au générateur
        for(int i=0; nodeList!=null && i<nodeList.getLength(); i++){
            // Moteur de génération RPA
            GenerateRPA generateRPA = new GenerateRPA();
            Node node = nodeList.item(i);

            if(node.getNodeType() == Node.ELEMENT_NODE){
                Element e = (Element) node;
                generateRPA.setRacine(e.getAttribute("name"));

                // Récupération du processus assigné à l'ID du participant
                // Récupération des éléments d'un processus d'après son ID
                String id = e.getAttribute("processRef");
                expression = String.format("/definitions/process[@id=\"%s\"]", id);
                xPathExpression = xpath.compile(expression);
                nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);
                // Parcourt récursif du processus et de ses lanes
                Main.visite(nodeList, generateRPA);
            }

            generateRPA.createXML(cheminDossierRPA);
        }

        // Récupération de toutes les lanes ayant été taggé par "Objet: ..."
        // Set n'autorise pas les doublons
        Set<String> uniqueObjet = new HashSet<String>();
        expression = "/definitions/process/laneSet/lane/childLaneSet/lane[contains(@name, 'Objet')]";
        xPathExpression = xpath.compile(expression);
        nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);

        // Parcourt la liste des lanes commencant par "Objet: ..."
        for(int i=0; nodeList!=null && i<nodeList.getLength(); i++){
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String name = e.getAttribute("name");
                if (uniqueObjet.add(name)) {
                    GenerateRPA generateRPA = new GenerateRPA();
                    generateRPA.setRacine(name);
                    generateRPA.createXML(cheminDossierRPA);
                }
            }
        }


    }

    // Parcourt récursif de l'arbre
    public static void visite (NodeList listeNoeudFils, GenerateRPA generateRPA){
        // Vérifie que la liste des noeuds ne soit pas vide
        for(int i=0; listeNoeudFils != null && i<listeNoeudFils.getLength(); i++){
            // Récupération noeud fils
            Node noeudFils = listeNoeudFils.item(i);
            // On ne s'intéresse qu'aux noeuds étant des éléments sinon passe au suivant
            if(noeudFils.getNodeType() != Node.ELEMENT_NODE) continue;
            // Conversion en objet Element pour avoir plus de possibilité
            Element eNoeudFils = (Element) noeudFils;

            System.out.println(noeudFils.getNodeName() + " = " + noeudFils.getTextContent());

            // Parcours des attributs
            NamedNodeMap namedNodeMap = noeudFils.getAttributes();
            for(int j=0; namedNodeMap != null && j<namedNodeMap.getLength(); j++){
                Node noeudAttribut = namedNodeMap.item(j);
                System.out.println("\t" + noeudAttribut.getNodeName() + " = " + noeudAttribut.getNodeValue());
            }

            switch(eNoeudFils.getNodeName().toLowerCase()){
                // Page
                case "bpmn:lane":
                    String id = eNoeudFils.getAttribute("id");
                    String nom = eNoeudFils.getAttribute("name");
                    generateRPA.addSubsheet(id, nom);
                    break;
            }

            visite(noeudFils.getChildNodes(), generateRPA);
        }
    }
}
