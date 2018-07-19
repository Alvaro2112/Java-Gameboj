
package ch.epfl.gameboj.component.cpu;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.Bit;
import ch.epfl.gameboj.bits.Bits;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Class qui représente la partie d'un
 *         processeur chargée d'effectuer des calculs sur des « entiers » de
 *         taille fixe.
 */
public final class Alu {

    public enum Flag implements Bit {
        UNUSED_0, UNUSED_1, UNUSED_2, UNUSED_3, C, H, N, Z
    };

    public enum RotDir {
        LEFT, RIGHT
    };

    private Alu() {
    }

    /**
     * crée une valeur dont les bits correspondant aux différents fanions valent
     * 1 ssi l'argument correspondant est vrai,
     * 
     * @param z
     *            si z est égal à true alors le fanion z vaut 1
     * @param n
     *            n est égal à true alors le fanion n vaut 1
     * @param h
     *            h est égal à true alors le fanion h vaut 1
     * @param c
     *            c est égal à true alors le fanion c vaut 1
     * @return la valeur crée
     */

    public static int maskZNHC(boolean z, boolean n, boolean h, boolean c) {
        int maskZ = 0b00000000_00000000_00000000_00000000;
        int maskN = 0b00000000_00000000_00000000_00000000;
        int maskH = 0b00000000_00000000_00000000_00000000;
        int maskC = 0b00000000_00000000_00000000_00000000;
        if (z) {

            maskZ = Bits.mask(7);
        }

        if (n) {

            maskN = Bits.mask(6);
        }

        if (h) {

            maskH = Bits.mask(5);
        }
        if (c) {

            maskC = Bits.mask(4);
        }

        return (maskZ | maskN | maskH | maskC);
    }

    /**
     * extrait les 16 bits de poids forts de la valeur contenue dans le paquet
     * valeur/fanion donné,
     * 
     * @param valueFlags
     *            valeur dont on a enlever les fanions, c'est à dire enlever les
     *            8 bis de poids faibles
     * @return la valeur extraite
     */

    public static int unpackValue(int valueFlags) {

        return (Bits.extract(valueFlags, 8, 16));
    }

    /**
     * extrait les fanions contenus dans le paquet valeur/fanion donné
     * 
     * @param valueFlags
     *            valeur dont on va enlever les 8 bits de poids forts, pour
     *            garder que les fanions
     * @return la valeur extraite
     */
    public static int unpackFlags(int valueFlags) {

        return Bits.clip(8, valueFlags);
    }

    /**
     * additionne les deux valeurs 8 bits données et le bit initial de retenue
     * si l'argument correspondant est true
     * 
     * @param l
     *            valeur à additioner
     * @param r
     *            valeur à additioner
     * @param c0
     *            bit de retenue initial, si true alors le bit de retenue vaut 1
     * @return la somme et les fanions ZOHC
     * @throws IllegalArgumentException
     *             si une des deux valeurs ne fait pas 8 bits
     */
    public static int add(int l, int r, boolean c0) {

        int carry = 0;

        if (c0) {

            carry = 1;
        }

        int sum = Preconditions.checkBits8(l) + Preconditions.checkBits8(r)
                + carry;
        int clip = Bits.clip(8, sum);
        boolean H = Bits.clip(4, l) + Bits.clip(4, r) + carry > 0xF;

        return packValueZNHC(clip, clip == 0, false, H, sum > 0xFF);

    }

    /**
     * additionne les deux valeurs 8 bits données et le bit initial de retenue
     * initial vaut 0.
     * 
     * @param l
     *            valeur à additioner
     * @param r
     *            valeur à additioner
     * @return la somme de l et r et les fanions Z0HC.
     * @throws IllegalArgumentException
     *             si une des deux valeurs ne fait pas 8 bits
     */
    public static int add(int l, int r) {

        return add(l, r, false);

    }

    /**
     * additionne des deux valeurs 16 bits données, les fanions H et C
     * correspondent à l'addition des 8 bits de poids forts.
     * 
     * @param l
     *            valeur à additioner
     * @param r
     *            valeur à additioner
     * @return la somme des deux arguments et les fanions 00HC.
     * 
     * @throws IllegalArgumentException
     *             si un des arguments n'est pas une valeur de 16 bits
     */

    public static int add16L(int l, int r) {

        int firstClip = Bits.clip(8, Preconditions.checkBits16(l));
        int secondClip = Bits.clip(8, Preconditions.checkBits16(r));

        int adition = add(firstClip, secondClip);

        return packValueZNHC(Bits.clip(16, l + r), false, false,
                Bits.test(adition, 5), Bits.test(adition, 4));

    }

    /**
     * additionne des deux valeurs 16 bits données, les fanions H et C
     * correspondent à l'addition des 8 bits de poids forts.
     * 
     * @param l
     *            valeur à additioner
     * @param r
     *            valeur à additioner
     * @return la somme des deux arguments et les fanions 00HC
     * @throws IllegalArgumentException
     *             si un des arguments n'est pas une valeur de 16 bits
     */

    public static int add16H(int l, int r) {

        int sumWithL = add(Bits.clip(8, Preconditions.checkBits16(l)),
                Bits.clip(8, Preconditions.checkBits16(r)));

        int sumWithH = add(Bits.extract(l, 8, 8), Bits.extract(r, 8, 8),
                Bits.test(sumWithL, Flag.C));

        return packValueZNHC(Bits.clip(16, l + r), false, false,
                Bits.test(sumWithH, 5), Bits.test(sumWithH, 4));
    }

    /**
     * soustrait les deux valeurs 8 bits données et le bit initial d'emprunt
     * initial vaut 1si l'argument est true.
     * 
     * @param l
     *            argument à soustraire par r
     * @param r
     *            argument qui va soustraire l
     * @param b0
     *            emprunt initial, s'il est vrai alors l'emprunt vaut 1
     * @return la différence et les fanions Z1HC.
     * 
     * @throws IllegalArgumentException
     *             si un des arguments n'est pas une valeur de 8 bits
     */
    public static int sub(int l, int r, boolean b0) {

        int carry = 0;

        if (b0) {

            carry = 1;
        }

        int substraction = Preconditions.checkBits8(l)
                - Preconditions.checkBits8(r) - carry;

        int clip = Bits.clip(8, substraction);

        boolean firstClip = Bits.clip(4, l) < Bits.clip(4, r) + carry;
        boolean seconfClip = l < r + carry;

        return packValueZNHC(clip, clip == 0, true, firstClip, seconfClip);

    }

    /**
     * soustrait les deux valeurs 8 bits données et le bit initial d'emprunt
     * initial vaut 0.
     * 
     * @param l
     *            argument à soustraire par r
     * @param r
     *            argument qui va soustraire l
     * 
     * @return la différence et les fanions Z1HC
     * @throws IllegalArgumentException
     *             si un des arguments n'est pas une valeur de 8 bits
     */

    public static int sub(int l, int r) {

        return sub(l, r, false);
    }

    /**
     * ajuste la valeur 8 bits donnée en argument afin qu'elle soit au format
     * DCB
     * 
     * @param v
     *            valeur à ajuster
     * @param n
     *            si n est true le fanion n vaut 1, sinon 0.
     * @param h
     *            sert à calcuer la valeur ajustée
     * @param c
     *            sert à calculer la valeur ajustée
     * @return la valeur ajustée et les fanions ZN0C
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur 8 bits
     */
    public static int bcdAdjust(int v, boolean n, boolean h, boolean c) {

        int calcul = 0;
        int finall = 0;

        int clip = Bits.clip(4, Preconditions.checkBits8(v));

        Boolean a = (!n && clip > 9);
        Boolean b = (!n && v > 0x99);

        boolean d = h || a;
        boolean e = c || b;

        if (e & d) {
            calcul = 0x60 + 0x06;
        }

        else if (e) {
            calcul = 0x60;
        }

        else if (d) {
            calcul = 0x06;
        }

        else {
            calcul = 0;
        }

        if (n) {
            finall = v - calcul;
        } else {
            finall = v + calcul;
        }
        int result = Bits.clip(8, finall);

        return packValueZNHC(result, result == 0, n, false, e);

    }

    /**
     * retourne le « et » bit à bit des deux valeurs 8 bits données
     * 
     * @param l
     *            valeur qu'on utilise pour le "et"
     * @param r
     *            valeur qu'on utilise pour le "et"
     * @return le « et » bit à bit des deux valeurs 8 bits données et les
     *         fanions Z010,
     * @throws IllegalArgumentException
     *             si un des deux arguments n'est pas une valeur 8 bits
     */
    public static int and(int l, int r) {

        int and = Preconditions.checkBits8(l) & Preconditions.checkBits8(r);
        boolean z = and == 0;

        return (packValueZNHC(and, z, false, true, false));
    }

    /**
     * retourne le « et » bit à bit des deux valeurs 8 bits données
     * 
     * @param l
     *            valeur qu'on utilise pour le "ou"
     * @param r
     *            valeur qu'on utilise pour le "ou"
     * @return le « et » bit à bit des deux valeurs 8 bits données et les
     *         fanions Z000,
     * @throws IllegalArgumentException
     *             si un des deux arguments n'est pas une valeur 8 bits
     */
    public static int or(int l, int r) {

        int or = Preconditions.checkBits8(l) | Preconditions.checkBits8(r);
        boolean z = or == 0;

        return (packValueZNHC(or, z, false, false, false));
    }

    /**
     * retourne le « ou » bit à bit des deux valeurs 8 bits données
     * 
     * @param l
     *            valeur qu'on utilise pour le "ou"
     * @param r
     *            valeur qu'on utilise pour le "ou"
     * @return le « ou » bit à bit des deux valeurs 8 bits données et les
     *         fanions Z000,
     * @throws IllegalArgumentException
     *             si un des deux arguments n'est pas une valeur 8 bits
     */

    public static int xor(int l, int r) {

        int xor = Preconditions.checkBits8(l) ^ Preconditions.checkBits8(r);
        boolean z = xor == 0;

        return (packValueZNHC(xor, z, false, false, false));
    }

    /**
     * ajoute à la valeur de 16 bits données les fanions en décalant la valeur
     * de 8bits vers la gauche, et en faisant un "ou" avec le masque créé avec
     * les arguments qui correspondent aux fanions
     * 
     * @param v
     *            l'entier auquelle on ajoute les fanions
     * @param z
     *            si z est égal à true alors le fanion z vaut 1
     * @param n
     *            n est égal à true alors le fanion n vaut 1
     * @param h
     *            h est égal à true alors le fanion h vaut 1
     * @param c
     *            c est égal à true alors le fanion c vaut 1
     * @return une valeur dont les 8bits de poids faibles représente les fanions
     *         et dont les bits de poids forts représente un entier
     * @throws IllegalArgumentException
     *             v n'est pas une valeur de 16 bits
     */
    public static int packValueZNHC(int v, boolean z, boolean n, boolean h,
            boolean c) {

        int mask = maskZNHC(z, n, h, c);

        v = Preconditions.checkBits16(v) << 8;

        return (v | mask);

    }

    /**
     * décale à gauche la valeur donnée d'un bit
     * 
     * @param v
     *            valeur qu'on décale à gauche d'un bit
     * 
     * @return la valeur faite par le décalage , et les fanions Z00C où le
     *         fanion C contient le bit éjecté par le décalage
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur de 8 bits
     */

    public static int shiftLeft(int v) {

        int valueChecked = Preconditions.checkBits8(v) << 1;
        int clip = Bits.clip(8, valueChecked);

        boolean a = clip == 0;
        boolean test = Bits.test(v, 7);

        return (packValueZNHC(clip, a, false, false, test));
    }

    /**
     * 
     * décale la valeur 8 bits données à droite d'un bit, de manière
     * arithmétique
     * 
     * @param v
     *            valeur qu'on décale à gauche d'un bit
     * @return la valeur décalée les fanions Z00C où C contient le bit éjecté
     *         par le décalage,
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur de 8 bits
     */
    public static int shiftRightA(int v) {

        int valueCheckedAndExtended = (Bits
                .signExtend8(Preconditions.checkBits8(v)) >> 1);
        int clip = Bits.clip(8, valueCheckedAndExtended);

        boolean a = clip == 0;
        boolean test = Bits.test(v, 0);

        return (packValueZNHC(clip, a, false, false, test));
    }

    /**
     * décale la valeur 8 bits données à droite d'un bit, de manière logique
     * 
     * 
     * @param v
     *            valeur qu'on décale à gauche d'un bit
     * @return la valeur décalée les fanions Z00C où C contient le bit éjecté
     *         par le décalage,
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur de 8 bits
     */

    public static int shiftRightL(int v) {

        int valueChecked = Preconditions.checkBits8(v) >>> 1;

        boolean a = valueChecked == 0;
        boolean test = Bits.test(v, 0);

        return (packValueZNHC(valueChecked, a, false, false, test));
    }

    /**
     * fait une rotation de la valeur 8 bits donnée, d'une distance de un bit
     * dans la direction donnée,
     * 
     * @param d
     *            direction de la rotation.
     * @param v
     *            valeur à décaler
     * @returnla valeur créée après la rotation et les fanions Z00C où C
     *           contient le bit qui est passé d'une extrémité à l'autre lors de
     *           la rotation.
     * 
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur de 8bits
     */
    public static int rotate(RotDir d, int v) {

        int a = 0;
        int valueChecked = Preconditions.checkBits8(v);
       

        if (d == RotDir.LEFT) {
            a = Bits.rotate(8, valueChecked, 1);
            return packValueZNHC(a, a==0, false, false, Bits.test(v, 7));

        } else {
            a = Bits.rotate(8, valueChecked, -1);

            return packValueZNHC(a, a==0, false, false, Bits.test(v, 0));
        }
        
    }
    
 
    /**
     * effectue une rotation à travers la retenue, dans la direction donnée, de
     * la combinaison de la valeur 8 bits
     * 
     * @param d
     *            direction de la rotation
     * @param v
     *            valeur a décaler
     * @param c
     *            retenue, s'il vaut true alors la retenue vaut 1, sinon 0.
     * @return la valeur créée apès la rotation et les fanions Z00C
     * 
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur de 8bits
     */
    public static int rotate(RotDir d, int v, boolean c) {
        int a = 0;

        if (d == RotDir.LEFT) {
            a = 1;
        } else {
            a = -1;
        }

        int set = Bits.set(Preconditions.checkBits8(v), 8, c);

        int rotatedInt = Bits.set(Bits.rotate(9, set, a), a == 1 ? 0 : 8,
                a == 1 ? Bits.test(set, 8) : Bits.test(set, 0));

        return packValueZNHC(Bits.clip(8, rotatedInt),
                Bits.clip(8, rotatedInt) == 0, false, false,
                a == 1 ? Bits.test(v, 7) : Bits.test(v, 0));
    }

    /**
     * échange les 4bits de poids forts avec les 4 bits de poids faibles de la
     * valeur 8 bits donnée
     * 
     * @param v
     *            valeur dont on échangera les bits
     * @return la valeur faite par l'échange et les fanions Z000
     * @throws IllegalArgumentException
     *             si v n'est pas une valeur 8 bits
     */
    public static int swap(int v) {

        int d = Bits.rotate(8, v, 4);
        boolean e = d == 0;

        return packValueZNHC(d, e, false, false, false);

    }

    /**
     * teste le bit d'index donné, retourne une valeur qui vaut 0 et des fanions
     * où Z dépend du bit d'index donné
     * 
     * @param v
     *            valeur dont on teste la valeur à l'index donné
     * @param bitIndex
     * @return la valeur 0 et les fanions Z010 où Z est vrai ssi le bit d'index
     *         donné de la valeur 8 bits donnée vaut 0
     * @throws IndexOutOfBondsException
     *             si l'index n'est pas compris entre 0 et 7
     */
    public static int testBit(int v, int bitIndex) {

        Objects.checkIndex(bitIndex, 8);

        boolean a = !(Bits.test(Preconditions.checkBits8(v), bitIndex));

        return packValueZNHC(0, a, false, true, false);

    }

}
