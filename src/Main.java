import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main (String[] args) throws ParserConfigurationException, IOException, SAXException, TransformerException, XPathExpressionException {
        // Path va gérer lui même quel type de slash il faut mettre
        Path cheminRacineProjet = Paths.get(System.getProperty("user.dir"));
        Path cheminDossierBPMN = cheminRacineProjet.resolve(Paths.get("BPMN/"));
        Path cheminDossierRPA = cheminRacineProjet.resolve(Paths.get("RPA/"));
        Path cheminFichierBPMN = cheminDossierBPMN.resolve(Paths.get("diagram.bpmn"));

        // Concaténation de tout pour aboutir au chemin du fichier
        Path cheminFichier = cheminRacineProjet.resolve(cheminFichierBPMN);
        // Création du fichier BPMN
        File fichierBPMN = new File(cheminFichier.toString());

        // Moteur de génération RPA
        GenerateRPA generateRPA = new GenerateRPA();

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
        // Element racine = documentBPMN.getDocumentElement();

        // Utilisation de XPath
        XPathFactory xPathfactory = XPathFactory.newInstance();
        XPath xpath = xPathfactory.newXPath();
        String expression;
        XPathExpression xPathExpression;
        NodeList nodeList;

        // Récupération de la liste des participants
        expression = "/definitions/collaboration/participant";
        xPathExpression = xpath.compile(expression);
        nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);

        // Parcourt la liste des participant et ajoute pour chacun une racine au générateur
        for(int i=0; nodeList!=null && i<nodeList.getLength(); i++){
            Node node = nodeList.item(i);

            if(node.getNodeType() == Node.ELEMENT_NODE){
                Element e = (Element) node;
                String nomRacine = e.getAttribute("name");
                generateRPA.setRacine(nomRacine);

                // Récupération du processus assigné à l'ID du participant
                // Récupération des éléments d'un processus d'après son ID
                String id = e.getAttribute("processRef");
                expression = String.format("/definitions/process[@id=\"%s\"]", id);
                xPathExpression = xpath.compile(expression);
                nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);
                // Parcourt récursif du processus et de ses lanes
                Main.visite(nodeList, generateRPA, nomRacine);
            }
        }

        // Récupération de toutes les lanes ayant été taggé par "Objet: ..."
        expression = "/definitions/process/laneSet/lane/childLaneSet/lane[contains(@name, 'Objet')]";
        xPathExpression = xpath.compile(expression);
        nodeList = (NodeList) xPathExpression.evaluate(documentBPMN, XPathConstants.NODESET);

        // Parcourt la liste des lanes commencant par "Objet: ..."
        for(int i=0; nodeList!=null && i<nodeList.getLength(); i++){
            Node node = nodeList.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                String nomRacine = e.getAttribute("name");
                generateRPA.setRacine(nomRacine);

                // Récupération des tâches au sein de la lane
                NodeList tacheLane = e.getChildNodes();
                for(int j=0; tacheLane!=null && j<tacheLane.getLength(); j++){
                    node = tacheLane.item(j);
                    if(node.getNodeType() == Node.ELEMENT_NODE){
                        e = (Element) node;
                        // Récupération de l'ID de la tâche
                        String id = e.getTextContent().trim();
                        // Faire un Xpath pour retrouver la tâche associé à l'ID
                        expression = String.format("/definitions/process/task[@id=\"%s\"]", id);
                        xPathExpression = xpath.compile(expression);
                        node = (Node) xPathExpression.evaluate(documentBPMN, XPathConstants.NODE);
                        // Vérifie qu'on a bien un résultat
                        if(node != null){
                            // Récupération du nom de la tâche
                            e = (Element) node;
                            String nom = e.getAttribute("name");
                            // Récupérere l'attribut name pour add la page
                            generateRPA.addSubsheet(id, nom, nomRacine);
                        }
                    }
                }
            }
        }
        generateRPA.createXML(cheminDossierRPA);




        // Inversion de génération RPA -> BPMN
        GenerateBPMN generateBPMN = new GenerateBPMN();


        generateBPMN.createXML(cheminDossierBPMN, "POC");

    }

    // Parcourt récursif de l'arbre
    public static void visite (NodeList listeNoeudFils, GenerateRPA generateRPA, String nomRacine){
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
                // Page Processus
                case "bpmn:lane":
                    String id = eNoeudFils.getAttribute("id");
                    String nom = eNoeudFils.getAttribute("name");
                    generateRPA.addSubsheet(id, nom, nomRacine);
                    break;
            }

            visite(noeudFils.getChildNodes(), generateRPA, nomRacine);
        }
    }
}
