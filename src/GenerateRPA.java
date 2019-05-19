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

public class GenerateRPA {
    // Permet de créer notre un builder
    private final DocumentBuilderFactory factory;
    // Permet de créer notre document
    private final DocumentBuilder builder;
    private final Document document;
    private final TransformerFactory transformerFactory;
    private final Transformer transformer;
//    private List<Element> racines;
    private final Element racine;


    public GenerateRPA() throws ParserConfigurationException, TransformerConfigurationException {
        factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        builder = factory.newDocumentBuilder();
        document = builder.newDocument();
        transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        racine = document.createElement("process");
//        racines = new ArrayList<>();
    }

//    public List<Element> getRacines() {
//        return racines;
//    }

//    // Récupére la racine d'après son nom
//    public Element getRacine (String nom){
//        nom = this.formatStringXML(nom);
//        for (Element e : this.racines) {
//            String lowNomRacine = e.getAttribute("name").toLowerCase();
//            if( lowNomRacine.equals(nom.toLowerCase()) ){
//                return e;
//            }
//        }
//        return null;
//    }

    // Ne change pas la racine car toujours la même mais change crée ses attributs différenciatif
    public void setRacine(String name) {
//        // Vérifie qu'il n'y a pas d'autre racine de ce nom
//        if(this.getRacine(finalName)== null){
        String finalName = null;
        String[] split = name.split(":");

        if(split.length==2){
            finalName = this.formatStringXML(split[1]);
            // Si labelé comme objet on ajoute un attribut type="object"
            if(split[0].toLowerCase().contains("objet")){
                this.racine.setAttribute("type", "object");
            }
        }
        else if(split.length==1){
            finalName = this.formatStringXML(split[0]);
        }
        this.racine.setAttribute("name", finalName);

        // Ajout des éléments élémentaire d'une page
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(),
                        "Stage1", "ProcessInfo", null));
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(),
                        "Start", "Start", null));
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(),
                        "End", "End", null));
    }

    public void addSubsheet(String id, String name){
        String finalName = null;
        String[] split = name.split(":");

        // On prévoit un comportement différent pour les lanes "Objet:..."
        if(split.length==2){
            finalName = this.formatStringXML(split[1]);
            // Si labelé comme objet on ajoute un attribut type="object"
            if(split[0].toLowerCase().contains("objet")){
                // TODO: Do nothing for now
                return;
            }
        }
        else if(split.length==1){
            finalName = this.formatStringXML(split[0]);
        }

        // Crée un élément pour la page
        Element subsheet = this.document.createElement("subsheet");
        String idSheet = UUID.nameUUIDFromBytes(id.getBytes()).toString();
        subsheet.setAttribute("subsheetid", idSheet);
        // Lie la page à la racine
        this.racine.appendChild(subsheet);

        // Crée un sous élément nom de la page
        Element eName = this.document.createElement("name");
        eName.setTextContent(finalName);
        // Lie le nom de la page à l'élément page
        subsheet.appendChild(eName);

        // Lie les stages (info, start, end) à la racine
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(), finalName, "SubSheetInfo", idSheet));
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(), "Start", "Start", idSheet));
        this.racine.appendChild(this.createStage(UUID.randomUUID().toString(), "End", "End", idSheet));
    }

    public void createXML(Path cheminDossier) throws TransformerException {
        // Création d'un fichier par racine
        String nomFichier = this.racine.getAttribute("name") + ".xml";
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

    // Création d'élément de type Stage
    private Element createStage(String id, String nom, String type, String subsheetID){
        Element stage = this.document.createElement("stage");
        // Création des attributs primaires d'une page
        stage.setAttribute("stageid", id);
        stage.setAttribute("name", nom);
        stage.setAttribute("type", type);

        // Pas d'id de page si c'est pour la Main Page
        if(subsheetID != null){
            stage.appendChild(this.createSubElementNoAttribute("subsheetid", subsheetID));
        }

        // Disposition et dimension par défault différentes selon le type d'élément
        switch(type){
            case "ProcessInfo":
            case "SubSheetInfo":
                stage.appendChild(this.createSubElementNoAttribute("displayx", "-195"));
                stage.appendChild(this.createSubElementNoAttribute("displayy", "-105"));
                stage.appendChild(this.createSubElementNoAttribute("displaywidth", "150"));
                stage.appendChild(this.createSubElementNoAttribute("displayheight", "90"));
                break;
            case "Start":
                stage.appendChild(this.createSubElementNoAttribute("displayx", "15"));
                stage.appendChild(this.createSubElementNoAttribute("displayy", "-105"));
                stage.appendChild(this.createSubElementNoAttribute("displaywidth", "60"));
                stage.appendChild(this.createSubElementNoAttribute("displayheight", "30"));
                break;
            case "End":
                stage.appendChild(this.createSubElementNoAttribute("displayx", "15"));
                stage.appendChild(this.createSubElementNoAttribute("displayy", "90"));
                stage.appendChild(this.createSubElementNoAttribute("displaywidth", "60"));
                stage.appendChild(this.createSubElementNoAttribute("displayheight", "30"));
                break;
        }

        return stage;
    }

    private Element createSubElementNoAttribute(String name, String textContent){
        Element subElement = this.document.createElement(name);
        subElement.setTextContent(textContent);
        return subElement;
    }

}
