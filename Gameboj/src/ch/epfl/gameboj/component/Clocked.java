package ch.epfl.gameboj.component;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*représente un composant piloté par l'horloge du système
*/

public interface Clocked {
   /**
    *  demande au composant d'évoluer en exécutant toutes les opérations qu'il doit exécuter 
    *  durant le cycle  donné.
    *  
    * @param cycle donne le cycle 
    */
    void cycle(long cycle);
}
