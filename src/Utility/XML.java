package Utility;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

// Tuto
// https://openclassrooms.com/fr/courses/1766341-structurez-vos-donnees-avec-xml/1768662-dom-exemple-dutilisation-en-java

public class XML {
    // Permet de créer notre un builder
    private final DocumentBuilderFactory factory;
    // Permet de créer notre parser
    private final DocumentBuilder builder;
    // Document issue de fichier dom ou de flux .xml
    private final Document document;
    // Racine du document
    private final Element racine;
    // Noeuds fils de la racine
    private final NodeList noeudsFils;


    public XML (File file) throws ParserConfigurationException, IOException, SAXException {
        this.factory        = DocumentBuilderFactory.newInstance();
        this.builder        = this.factory.newDocumentBuilder();
        this.document       = this.builder.parse(file);
        this.racine         = this.document.getDocumentElement();
        this.noeudsFils     = this.racine.getChildNodes();
    }

    public String test (){
        this.visite(this.noeudsFils);
        return "";
    }

    // Parcourt récursif de l'arbre
    private void visite (NodeList listeNoeudFils){
        // Vérifie que la liste des noeuds ne soit pas vide
        for(int i=0; listeNoeudFils != null && i<listeNoeudFils.getLength(); i++){
            // Récupération noeud fils
            Node noeudFils = listeNoeudFils.item(i);
            // On ne s'intéresse qu'aux noeuds étant des éléments sinon passe au suivant
            if(noeudFils.getNodeType() != Node.ELEMENT_NODE) continue;

            System.out.println("\n" + noeudFils.getNodeName() + " = " + noeudFils.getNodeValue());

            // Parcours des attributs
            NamedNodeMap namedNodeMap = noeudFils.getAttributes();
            for(int j=0; namedNodeMap != null && j<namedNodeMap.getLength(); j++){
                Node noeudAttribut = namedNodeMap.item(j);
                System.out.println("\t" + noeudAttribut.getNodeName() + " = " + noeudAttribut.getNodeValue());
            }

            this.visite(noeudFils.getChildNodes());
        }
    }


}
