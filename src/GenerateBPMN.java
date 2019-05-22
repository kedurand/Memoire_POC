import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GenerateBPMN {
    private final Document document;
    private final Transformer transformer;
    private Element racine;
    private Element collaboration;
    private Element plane;

    private final String X = "220";
    private final String Y = "80";
    private final String LARGEUR = "1000";
    private final String HAUTEUR = "150";

    public GenerateBPMN() throws ParserConfigurationException, TransformerConfigurationException {
        // Permet de créer notre un builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        // Permet de créer notre document
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

        // Création de la base du document BPMN !
        racine = document.createElement("bpmn:definitions");

        collaboration = document.createElement("bpmn:collaboration");
        collaboration.setAttribute("id", String.valueOf(UUID.randomUUID()));

        Element tmp = document.createElement("bpmndi:BPMNDiagram");
        plane = document.createElement("bpmndi:BPMNPlane");
        plane.setAttribute("bpmnElement", collaboration.getAttribute("id"));
        tmp.appendChild(plane);

        // Lien avec la racine
        racine.appendChild(collaboration);
        racine.appendChild(tmp);


        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
    }

    // Récupére la racine d'après son nom
    public Element getRacine (){
        return this.racine;
    }

    public Element getCollaboration() {
        return collaboration;
    }

    public Element getPlane() {
        return plane;
    }

    // Participant doit avoir un id retrouvable d'après son nom, peu servir j'imagine
    // Ajouter un participant revient à ajouter un process !
    // Ajouter un paricipant revient également à ajouter une shape
    public void addParticipant(String name, int nbLane){
        Element e = this.document.createElement("bpmn:participant");
        e.setAttribute("name", name);
        String idFromName = UUID.nameUUIDFromBytes(name.getBytes()).toString();
        e.setAttribute("id", idFromName);
        e.setAttribute("processRef" , "ref_" + idFromName);
        // Devient fils de collaboration
        this.collaboration.appendChild(e);

        this.addProcess(e.getAttribute("processRef"));

        // Lorsqu'il s'agit d'un processus, le X sera un peu plus à gauche
        // il faut donc aussi ajuster sa largeur !
        // il faut aussi avoir une hauteur importante !
        String x = String.valueOf(Integer.valueOf(this.X) - 40);
        String largeur = String.valueOf(Integer.valueOf(this.LARGEUR) + 40);
        String hauteur = String.valueOf(Integer.valueOf(this.HAUTEUR) * nbLane);
        this.addShape(idFromName, x, this.Y, largeur, hauteur);
    }

    private void addProcess(String id){
        Element e = document.createElement("bpmn:process");
        e.setAttribute("id", id);
        // On lui crée directement un fils
        Element tmp = document.createElement("bpmn:laneSet");
        e.appendChild(tmp);
        // Liaison directement avec la racine
        this.racine.appendChild(e);
    }

    private void addShape(String id, String x, String y, String largeur, String hauteur){
        Element e = this.document.createElement("bpmndi:BPMNShape");
        e.setAttribute("id", id + "_di");
        e.setAttribute("bpmnElement", id);

        // Création des dismensions
        Element tmp = this.document.createElement("dc:Bounds");
        tmp.setAttribute("x", x);
        tmp.setAttribute("y", y);
        tmp.setAttribute("width", largeur);
        tmp.setAttribute("height", hauteur);
        // Liason avec la shape
        e.appendChild(tmp);

        //Liaison au plan BPMN
        this.plane.appendChild(e);
    }

    // Ajoute une lane dans le processus
    // Ajouter une lane revient à ajouter une shape
    public void addLane(String processName, String id, String name, int laneNumber){
        Element laneSet = this.getLaneSetProcess(processName);

        Element e = this.document.createElement("bpmn:lane");
        // Création d'un ID unique pour le nom de la lane
        e.setAttribute("id", id);
        e.setAttribute("name", name);

        // Liason de la lane avec la laneSet du process
        laneSet.appendChild(e);

        // On va mettre à un Y en bas de la dernière lane mise !
        int y = Integer.valueOf(this.Y);
        int hauteur = Integer.valueOf(this.HAUTEUR);
        String finalY = String.valueOf( y + (hauteur * laneNumber) );
        this.addShape(id, this.X, finalY, this.LARGEUR, this.HAUTEUR);
    }

    // Récupération du laneSet du process où insérer les lane d'après son nom
    Element getLaneSetProcess(String nom){
        String idReconstitue = UUID.nameUUIDFromBytes(nom.getBytes()).toString();
        // Un prefix est ajouté à cet id pour la ref process
        idReconstitue = "ref_" + idReconstitue;

        NodeList ls = this.racine.getElementsByTagName("bpmn:process");
        for(int i=0; ls != null && i < ls.getLength(); i++){
            Node node = ls.item(i);
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;
                if(e.getAttribute("id").equals(idReconstitue)){
                    // Récupére l'élément laneSet qui est sencé être un laneSet unique
                    return (Element) e.getFirstChild();
                }
            }
        }
        return null;
    }

    public void createXML(Path cheminDossier, String nomFichier) throws TransformerException {
        if(!nomFichier.endsWith(".bpmn")) {
            nomFichier = nomFichier + ".bpmn";
        }

        Path cheminFichier = cheminDossier.resolve(nomFichier);
        File fichier = new File(cheminFichier.toString());

        DOMSource domSource = new DOMSource(this.racine);
        StreamResult streamResult = new StreamResult(fichier);
        this.transformer.transform(domSource, streamResult);
        System.out.println(nomFichier + " a été crée !");
    }

    // Format les string en entrée pour respecter norme XML
    // On enleve les espace ou on les remplace par "_"
    private String formatStringXML(String string){
        return string.trim()
                .replace(" ", "_");
    }

}
