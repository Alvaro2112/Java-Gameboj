package ch.epfl.gameboj.bits;

import java.util.Objects;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*a pour but d'être implémentée par les type énumérés représentant un ensemble de bits
*/


public interface Bit {
    
    /**
     * 
     */
    int ordinal();

   /**
    * donne la valeur retournée par ordinal 
    * @return la valeur donnée par ordinal
    */
    default int index() {
        
        return ordinal();
    }
/**
 * retourne une valeur dont seul le bit de même index que celui du récepteur vaut 1.
 * @return le masque correspondant au bit
 */
   default int mask() {
       return 1 << index();
   }
}
