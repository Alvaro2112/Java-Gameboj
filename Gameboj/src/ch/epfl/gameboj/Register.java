package ch.epfl.gameboj;

/**
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Interface implémentée par les registres
 */
public interface Register {

    public int ordinal();

    /**
     * donne la valeur retournée par ordinal
     * 
     * @return la valeur donnée par ordinal
     */
    public default int index() {

        return ordinal();
    }
}
