package ch.epfl.gameboj.component;

import ch.epfl.gameboj.Bus;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*
*représente un composant du Game Boy connecté aux bus d'adresses et de données (processeur, clavier, etc.).
* Elle a donc pour but d'être implémentée par toutes les classes représentant un tel composant.
* 
*/
public interface Component {
    
    int NO_DATA = 256;
  
    /**
     * qui retourne l'octet stocké à l'adresse donnée par le composant, 
     * ou NO_DATA si le composant ne possède aucune valeur à cette adresse
     * @param address donne l'addresse du parametre du tableau à retourner 
     * @return la valeur stockée dans le tableau à l'addresse donnée
     */
    
    int read(int address);
    
    /**
     * modifie le contenu du composant  à l'index donné pour qu'il soit égal à
     * la valeur donnée 
     * 
     * @param address donne l'index de la valeur a changer
     * 
     * @param data donne la valeur qu'on met dans le tableau à l'index donné
     * 
     * @throws IndexOutOfBondsException si l'index donné n'est pas une valeur
     * comprise entre 0 et la taille du tableau
     */
    void write(int address, int data);

    /**
     * attache le composant au bus donné, en appelant simplement la méthode attach de celui-ci
     * @param le bus auquel   le composé sera attaché
     * 
     */
    default void attachTo(Bus bus) {
        
        bus.attach(this);
    }

}
