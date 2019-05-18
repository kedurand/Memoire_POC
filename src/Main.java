import Utility.XML;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main (String[] args){
        // Path va gérer lui même quel type de slash il faut mettre
        Path cheminRacineProjet = Paths.get(System.getProperty("user.dir"));
        Path cheminFichierBpmn = Paths.get("BPMN/POC.bpmn");

        // Concaténation de tout pour aboutir au chemin du fichier
        Path cheminFichier = cheminRacineProjet.resolve(cheminFichierBpmn);
        // Création du fichier BPMN
        File fichier = new File(cheminFichier.toString());


        try {
            XML xml = new XML(fichier);
            System.out.println(xml.test());
        }
        catch (ParserConfigurationException
                | IOException
                | SAXException e) {
            e.printStackTrace();
        }


    }
}
