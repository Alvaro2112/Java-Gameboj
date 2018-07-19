package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.memory.Ram;

/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
* représente un composant contrôlant l'accès à une mémoire vive.
*/

public final class RamController implements Component {


    private Ram ram;
    private int startAddress;
    private int endAddress;

    /**
     * qui construit un contrôleur pour la mémoire vive donnée
     *  @param ram pour laquelle on construit un controleur
     *  @param startAddress donne l'addresse de départ
     *  @param endAddress donne l'addresse de la fin du controleur
     *  @throws si l'une des deux adresses n'est pas une valeur 16 bits ou i l'intervalle qu'elles décrivent a une taille 
     *  négative ou supérieure à celle de la mémoire.  
     *  @throws NullPointerException si l'argument ram  donné est nul
     */
    public RamController(Ram ram, int startAddress, int endAddress) {
      Preconditions.checkNull(ram);
        Preconditions.checkBits16(startAddress);
        Preconditions.checkBits16(endAddress);
        Preconditions.checkArgument(endAddress - startAddress >= 0
                && endAddress - startAddress <= ram.size());

        this.ram = ram;
        this.startAddress = startAddress;
        this.endAddress = endAddress;

    }

  public  RamController(Ram ram, int startAddress) {
        this(ram, startAddress, startAddress + ram.size());
    }
  
/** qui retourne l'octet stocké à l'adresse donnée par le controleur , 
     * ou NO_DATA si le composant ne possède aucune valeur à cette adresse
     * @param donne l'addresse du parametre du tableau à retourner 
     * @return la valeur stockée dans le tableau à l'addresse donnée
     */
 
    @Override
    public int read(int address) {
        if (Preconditions.checkBits16(address) < startAddress
                || Preconditions.checkBits16(address) >= endAddress) {
            return NO_DATA;
        } else {
            return ram.read(address - startAddress);
        }
    }
    
    /**
     * modifie le contenu du composant  à l'index donné pour qu'il soit égal à
     * la valeur donnée 
     * 
     * @param address donne l'index de la valeur a changer
     * 
     * @param data donne la valeur qu'on met dans le tableau à l'index donné
     * 
     * @throws IndexOutOfBondsException si l'index donné n'est pas une valeur
     * comprise entre startAddress  et endAddress 
     */

    @Override
    public void write(int address, int data) {
        Preconditions.checkBits8(data);
        if (Preconditions.checkBits16(address) >= startAddress
                && Preconditions.checkBits16(address) < endAddress) {

            ram.write(address - startAddress, data);
        }

    }

}
