
package ch.epfl.gameboj.component.memory;

import ch.epfl.gameboj.AddressMap;
import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.component.Component;
import ch.epfl.gameboj.component.cartridge.Cartridge;


/**
*@author Alvaro Cauderan ( 282186)
*@author Gauthier Boeshertz (283192)
*
*représente un controleur de la mémoire mortue du démarrage
*/

public class BootRomController implements Component {

    Cartridge cartridge;
    BootRom bootRom;
    boolean active = true;
    /**
     * construit un controleur de la mémoire morte du démarrage auquel la cartouche donnée est attachée
     * @param cartridge cartouche à attacher au controleur
     * @throws NullPointerException si l'argument est nul
     */
    public BootRomController(Cartridge cartridge) {

     Preconditions.checkNull(cartridge);

        this.cartridge = cartridge;

    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#read(int)
     */
    @Override
    public int read(int address) {
        int a = 0;
        if (address >= 0 && address <= 255 && active) {
            a = Byte.toUnsignedInt(BootRom.DATA[address]);

        }

        else {
            a = cartridge.read(address);
        }
        return a;
    }

    /* (non-Javadoc)
     * @see ch.epfl.gameboj.component.Component#write(int, int)
     */
    @Override
    public void write(int address, int data) {
        if (address == AddressMap.REG_BOOT_ROM_DISABLE) {

            active = false;

        }

        else {
            cartridge.write(address, data);
        }

    }
    
    public void setCartridgeRam(byte[] byteArray) {
        cartridge.setMBCRam(byteArray);
    }

}