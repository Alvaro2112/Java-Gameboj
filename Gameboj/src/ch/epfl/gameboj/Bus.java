package ch.epfl.gameboj;

import java.util.ArrayList;
import java.util.Objects;
import ch.epfl.gameboj.component.Component;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*représente — de manière très abstraite — les bus d'adresses et
* de données connectant les composants du Game Boy entre eux.
*
*/
public final class Bus {
    private ArrayList<Component> attachedto = new ArrayList<>();
    
    
/**
 *  attache le composant donné au bus
 *  @throws NullPointerException si le composant vaut null
 *  @param donne le composant a attacher
 */
    public void attach(Component component) {

        attachedto.add(
                Objects.requireNonNull(component, "The component is null"));

    }
    
/**
 *  retourne la valeur stockée à l'adresse donnée si au moins un des composants attaché au bus 
 *  possède une valeur à cette adresse, ou FF16 sinon 
 *  @param address  l'addresse où il y a les valeurs a retourner
 *  @throws IllegalArgumentException si l'adresse n'est pas une valeur 16 bits
 *  
 */
    public int read(int address) 
    {
        
        int ad = Preconditions.checkBits16(address);
        for (Component c : attachedto) {
            if (c.read(ad) != Component.NO_DATA ) {

                return c.read(ad);
            }
        }

        return 255;

    }
    
    /**
     *  qui écrit la valeur à l'adresse donnée dans tous les composants connectés au bus
     *  
     *  @param address addresse à laquelle la nouvelle valeur sera écrite
     *  @param data valeur a inscrire à l'addresse
     *  @throws IllegalArgumentException si l'adresse n'est pas une valeur 
     *  16 bits ou si la donnée n'est pas une valeur 8 bits.
     *  
     */

    public void write(int address, int data) {

        int a = Preconditions.checkBits16(address);
        int d = Preconditions.checkBits8(data);

        for (Component c : attachedto) {
            c.write(a, d);
        }
    }
}
