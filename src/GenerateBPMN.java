import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
    private Element process;
    private Element diagram;

    public GenerateBPMN() throws ParserConfigurationException, TransformerConfigurationException {
        // Permet de créer notre un builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        // Permet de créer notre document
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();

        // Création de la base du document
        racine = document.createElement("bpmn:definitions");

        collaboration = document.createElement("bpmn:collaboration");
        collaboration.setAttribute("id", String.valueOf(UUID.randomUUID()));

        process = document.createElement("bpmn:process");
        process.setAttribute("id", String.valueOf(UUID.randomUUID()));

        diagram = document.createElement("bpmn:BPMNDiagram");

        // Lien avec la racine
        racine.appendChild(collaboration);
        racine.appendChild(process);
        racine.appendChild(diagram);


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

    public Element getProcess() {
        return process;
    }

    public Element getDiagram() {
        return diagram;
    }

    public void addLane(String id, String name){

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
