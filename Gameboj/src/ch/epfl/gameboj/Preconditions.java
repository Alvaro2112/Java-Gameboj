package ch.epfl.gameboj;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Interface qui recueille des tests.
 *
 */

public interface Preconditions {

    /**
     * lève l'exception IllegalArgumentException si son argument est faux, et ne
     * fait rien sinon.
     * 
     * @param r
     *            boolean dont on teste la valeur
     * 
     * @throws l'exception
     *             IllegalArgumentException l'argument est faux
     * 
     */
    public static void checkArgument(boolean r) {

        if (r == false) {

            throw new IllegalArgumentException();
        }
    }

    /**
     * Retourne l'argument si celui ci est une valeur infèrieure à huit bit
     * sinon lève l'exception IllegalArgumentException
     * 
     * @return l'argument
     * 
     * @param v
     *            entier dont on teste la valeur
     * 
     * @throws l'exception
     *             IllegalArgumentException l'argument est un valeur supérieure
     *             ou égale à 255 ou si elle est négative
     */
    public static int checkBits8(int v) {

        if (v < 0 || v > 255) {

            throw new IllegalArgumentException();

        } else

            return v;
    }

    /**
     * Retourne l'argument si celui ci est une valeur infèrieure à 16 bit sinon
     * lève l'exception IllegalArgumentException
     * 
     * @retourne l'argument
     * 
     * @param v
     *            entier dont on teste la valeur
     * 
     * @throws l'exception
     *             IllegalArgumentException l'argument n'est pas une valeur de
     *             16 bits
     */
    public static int checkBits16(int v) {

        if (v < 0 || v > (65535)) {

            throw new IllegalArgumentException();

        } else

            return v;
    }

    /**
     * Teste l'argument, si il est nul envoie une exception sinnon ne fait rien
     * 
     * @param b
     *            object qu'on teste
     * @throws NullPointerException
     *             si l'argument est nul
     */
    public static void checkNull(Object b) {
        
        if (b == null) {
            
            throw new NullPointerException();
        }
    }

}
