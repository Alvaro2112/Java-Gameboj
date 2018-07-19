package ch.epfl.gameboj.component.lcd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ch.epfl.gameboj.Preconditions;
import ch.epfl.gameboj.bits.BitVector;

/**
 * 
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Représente une image affichée à l'écran,
 */

public final class LcdImage {
    private final int width;
    private final int height;
    private final List<LcdImageLine> lineList;
    private final static int MAX_WIDTH = 256;
    private static final int MAX_HEIGHT = 256;
    private final int MIN_WIDTH = 0, MIN_HEIGHT = 0;

    /**
     * Construit une image
     * 
     * @param width
     *            la larguer de l'image
     * @param height
     *            la hauteur de l'image
     * @param list
     *            liste toutes les lignes de l'image
     * @throws IllegalArgumentException
     *             si la hauteur ou la largeur sont négatives ou supérieures à
     *             256
     */
    public LcdImage(int width, int height, List<LcdImageLine> list) {

        Preconditions
                .checkArgument(height == Objects.requireNonNull(list).size());
        Preconditions.checkArgument(width > MIN_WIDTH && width <= MAX_WIDTH
                && height > MIN_HEIGHT && height <= MAX_HEIGHT);
        this.width = width;
        this.height = height;
        this.lineList = list;
    }

    /**
     * retourne la largeur
     * 
     * @return la largeur
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * retourne la hauteur
     * 
     * @return la hauteur
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * Retourne la couleur à un point(x,y) de l'image, la couleur est une valeur
     * allant de 0 à 3 (inclu)
     * 
     * @param x
     *            donne le x du point
     * @param y
     *            donne le y du point
     * @return 0 si le point est blanc, 1 si le point est gris clair 2 si gris
     *         foncé et 3 si noir
     * @throws IndexOUtOfBondsException
     *             si x ou y sont négatifs ou si x est supérieur à la largeur ou
     *             si y est supérieur à la hauteur
     */
    public int getColor(int x, int y) {

        Objects.checkIndex(x, width);
        Objects.checkIndex(y, height);

        LcdImageLine line = lineList.get(y);

        int a = line.msb().testBit(x) ? 0b010 : 0;
        int b = line.lsb().testBit(x) ? 0b001 : 0;

        return a | b;
    }

    /**
     * Retourne le hachage de l'image (fonction générée par eclipse)
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + height;
        result = prime * result
                + ((lineList == null) ? 0 : lineList.hashCode());
        result = prime * result + width;
        return result;
    }

    /**
     * Détermine si l'image est égal à l'objet passé en argument
     * 
     * @param obj
     *            objet dont on teste l'égalité avec l'image
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LcdImage other = (LcdImage) obj;
        if (height != other.height)
            return false;
        if (lineList == null) {
            if (other.lineList != null)
                return false;
        } else if (!lineList.equals(other.lineList))
            return false;
        if (width != other.width)
            return false;
        return true;
    }

    public static final class Builder {

        /**
         * @author Alvaro Cauderan ( 282186)
         * @author Gauthier Boeshertz (283192)
         * 
         *         Builder d'une image
         */

        private List<LcdImageLine> lineList;
        private final int height;
        private final int width;
        private boolean canBuild;

        /**
         * construit un builder d'une image
         * 
         * @param width
         *            donne la largeur du builder, donc celle de l'image qu'on
         *            va construire
         * @param heigh
         *            tdonne la hauteur du builder, donc celle de l'image qu'on
         *            va construire
         * @throws IllegalArgumentException
         *             si la width passé en argument nes pas divisible par 32
         * @throws IndexOutOfBounds
         *             si la width est plus petite que 0 ou plus grande que 256
         * @throws IndexOutOfBounds
         *             si la height est plus petite que 0 ou plus grande que 256
         */
        public Builder(int width, int height) {

            Preconditions.checkArgument(width % 32 == 0);
            Objects.checkIndex(width, MAX_WIDTH);
            Objects.checkIndex(height, MAX_HEIGHT);

            this.height = height;
            this.width = width;
            BitVector a = new BitVector(width);
            LcdImageLine b = new LcdImageLine(a, a, a);
            lineList = new ArrayList<>(height);
            for (int c = 0; c < height; ++c) {
                lineList.add(b);
            }
            canBuild = true;
        }

        /**
         * Donne à une ligne les vecteurs de couleur et d'opacité
         * 
         * @param index
         *            index de la ligne à changer
         * @param line
         *            contient les vecteurs
         * @return this pour pouvoir cumuler les fonctions
         * 
         * @throws IllegalStateException
         *             si un build a deja été fait sur ce builder
         * 
         * @throws NullPointerException
         *             si lineList est nul
         * @throws IllegalArgumentException
         *             si la taille de la ligne passé en argument nest pas egale
         *             a la taille de la ligne de la classe ou si lindex passé
         *             en argument est plus petit que zero ou plus grand que la
         *             hauteur de la ligne de la classe
         * 
         */
        public Builder setLine(int index, LcdImageLine line) {
            if (!canBuild)
                throw new IllegalStateException();

            Preconditions.checkArgument(line.size() == width);
            Preconditions.checkArgument(index >= 0 && index < height);
            Objects.requireNonNull(lineList).set(index, line);
            return this;
        }

        /**
         * construit l'image
         * 
         * @return l'image faite avec le builder
         * 
         * @throws IllegalStateException
         *             si un build a deja été fait sur ce builder
         * 
         * @throws NullPointerException
         *             si lineList est nul
         */
        public LcdImage build() {
            if (!canBuild)
                throw new IllegalStateException();

            canBuild = false;
            return new LcdImage(width, height, lineList);

        }

    }

}