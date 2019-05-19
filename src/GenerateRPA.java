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
    private final Document document;
    private final Transformer transformer;
    private List<Element> racines;

    public GenerateRPA() throws ParserConfigurationException, TransformerConfigurationException {
        // Permet de créer notre un builder
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringElementContentWhitespace(true);
        // Permet de créer notre document
        DocumentBuilder builder = factory.newDocumentBuilder();
        document = builder.newDocument();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        racines = new ArrayList<>();
    }

    public List<Element> getRacines() {
        return racines;
    }

    // Récupére la racine d'après son nom
    public Element getRacine (String nom){
        nom = this.formatStringXML(nom);
        for (Element e : this.racines) {
            String lowNomRacine = e.getAttribute("name").toLowerCase();
            if( lowNomRacine.equals(nom.toLowerCase()) ){
                return e;
            }
        }
        return null;
    }

    // Créer et ajoute la racine si pas déjà présente !
    public void setRacine(String name) {
        Element nouvRacine = this.document.createElement("process");
        String finalName = this.getFinalName(name);

        // Vérifie qu'il n'y a pas d'autre racine de ce nom
        if(this.getRacine(finalName)== null){
            nouvRacine.setAttribute("name", finalName);
            // Si labelé comme objet on ajoute un attribut type="object"
            if(name.toLowerCase().contains("objet")){
                nouvRacine.setAttribute("type", "object");
            }

            // Ajout des éléments élémentaire d'une page
            nouvRacine.appendChild(this.createStage(UUID.randomUUID().toString(),
                    "Stage1", "ProcessInfo", null));
            nouvRacine.appendChild(this.createStage(UUID.randomUUID().toString(),
                    "Start", "Start", null));
            nouvRacine.appendChild(this.createStage(UUID.randomUUID().toString(),
                    "End", "End", null));

            // Ajoute si aucune autre racine déjà de ce nom !
            this.racines.add(nouvRacine);
        }
    }

    public void addSubsheet(String id, String name, String nomRacine){
        // Si label contient "objet", on ajoute pas la page
        if(name.toLowerCase().contains("objet")) return;

        String finalName = this.getFinalName(name);
        String finalRacine = this.getFinalName(nomRacine);

        // Crée un élément pour la page
        Element subsheet = this.document.createElement("subsheet");
        String idSheet = UUID.nameUUIDFromBytes(id.getBytes()).toString();
        subsheet.setAttribute("subsheetid", idSheet);
        // Lie la page à la racine
        this.getRacine(finalRacine).appendChild(subsheet);

        // Crée un sous élément nom de la page
        Element eName = this.document.createElement("name");
        eName.setTextContent(finalName);
        // Lie le nom de la page à l'élément page
        subsheet.appendChild(eName);

        // Lie les stages (info, start, end) à la racine
        this.getRacine(finalRacine).appendChild(this.createStage(UUID.randomUUID().toString(), finalName, "SubSheetInfo", idSheet));
        this.getRacine(finalRacine).appendChild(this.createStage(UUID.randomUUID().toString(), "Start", "Start", idSheet));
        this.getRacine(finalRacine).appendChild(this.createStage(UUID.randomUUID().toString(), "End", "End", idSheet));
    }

    public void createXML(Path cheminDossier) throws TransformerException {
        for(Element e : this.racines) {
            // Création d'un fichier par racine
            String nomFichier = e.getAttribute("name") + ".xml";
            Path cheminFichier = cheminDossier.resolve(nomFichier);
            File fichier = new File(cheminFichier.toString());

            DOMSource domSource = new DOMSource(e);
            StreamResult streamResult = new StreamResult(fichier);
            this.transformer.transform(domSource, streamResult);
            System.out.println(nomFichier + " a été crée !");
        }
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

    // Noms peuvent être splité, on va donc chercher le nom final
    private String getFinalName(String name){
        String[] split = name.split(":");

        // Si "Processus : Nom"
        if(split.length==2){
            return this.formatStringXML(split[1]);
        }
        // Si "Nom"
        else if(split.length==1){
            return this.formatStringXML(split[0]);
        }

        return null;
    }
}
