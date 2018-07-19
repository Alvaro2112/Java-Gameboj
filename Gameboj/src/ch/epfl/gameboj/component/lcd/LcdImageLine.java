package ch.epfl.gameboj.component.lcd;

import java.util.Objects;

import ch.epfl.gameboj.Preconditions;

import ch.epfl.gameboj.bits.BitVector;

/**
 * 
 * @author Alvaro Cauderan ( 282186)
 * @author Gauthier Boeshertz (283192) Représente une ligne affichée dans une
 *         image,
 */

public final class LcdImageLine {

    private final BitVector msb;
    private final BitVector lsb;
    private final BitVector opacity;
    private final static int sizeOfInt = 32;

    /**
     * Construit une ligne
     * 
     * @param msb
     *            Un BitVector qui represente les bits les plus significant de
     *            la ligne.
     * @param lsb
     *            Un BitVector qui represente les bits les moins significant de
     *            la ligne.
     * @param opacity
     *            Un BitVector qui represente lopacite de la ligne, 1 -> Opaque,
     *            0 -> Transparent
     * @throws IllegalArgumentException
     *             si msb , lsb et opacity non pas la meme taille
     * @throws NullPointerException
     *             si msb, lsb ou opacity est nul
     */
    public LcdImageLine(BitVector msb, BitVector lsb, BitVector opacity) {
        Objects.requireNonNull(lsb);
        Objects.requireNonNull(msb);
        Objects.requireNonNull(opacity);
        Preconditions.checkArgument(
                !(msb.size() != lsb.size() || msb.size() != opacity.size()
                        || lsb.size() != opacity.size()));

        this.msb = msb;
        this.lsb = lsb;
        this.opacity = opacity;
    }

    /**
     * donne la taille de la ligne
     * 
     * @return la taille du vecteur de msb puisque tous les vecteurs ont la meme
     *         taille
     */
    public int size() {
        return this.msb.size();
    }

    /**
     * retourne le msb de la ligne
     * 
     * @return le msb
     */
    public BitVector msb() {

        return this.msb;

    }

    /**
     * retourne le lsb de la ligne
     * 
     * @return le lsb
     */
    public BitVector lsb() {

        return this.lsb;

    }

    /**
     * retourne la opacity de la ligne
     * 
     * @return la opacity
     */
    public BitVector opacity() {

        return this.opacity;

    }

    /**
     * Shift la ligne d'une valeur donnee en mettant des zero la oui il laisse
     * des espaces vides.
     * 
     * @param shift,
     *            definie la valeur du decalage, si il est positif le decalage
     *            seras vers la droite,sinon le decalage se fera vers la gauche
     * 
     * @return la ligne shifter
     */
    public LcdImageLine shift(int shift) {

        BitVector msbb = this.msb.shift(shift);
        BitVector lsbb = this.lsb.shift(shift);
        BitVector opacityy = this.opacity.shift(shift);

        return new LcdImageLine(msbb, lsbb, opacityy);

    }

    /**
     * extrait une ligne d'une taille donnée et depuis un index donné depuis
     * l'ancienne par enroulement
     * 
     * @param x
     *            donne l'index de dpépart de l'extraction
     * @param size
     *            la taille de la nouvelle ligne
     * @return la ligne extraite
     * 
     * @throws IllegalArgumentException
     *             si la taille n'est pas un multiple de 32 ou si la taile est
     *             négative
     */
    public LcdImageLine extract(int x, int size) {

        Preconditions.checkArgument(size % sizeOfInt == 0 && size > 0);

        BitVector msbb = this.msb.extractWrapped(x, size);
        BitVector lsbb = this.lsb.extractWrapped(x, size);
        BitVector opacityy = this.opacity.extractWrapped(x, size);

        return new LcdImageLine(msbb, lsbb, opacityy);

    }

    /**
     * Fonction qui change les couleurs dune ligne en fonction dune palette
     * passer en argument. cette palette est une valeurs de 8 bits qui contien
     * au deux premiers bits la couleur a metre la ou la couleur vaux
     * actuelement 0, dans les deux bits suivant la couleur a metre la ou la
     * couleur vaux actuelemnt 1 et ainsi de suite
     *
     * 
     * @param colors
     *            , la palette a apliquer a this.
     * @return Ligne avec les couleurs changer en fonction de la palette.
     * @throws IllegalArgumentException
     *             si la valeur passé en argument n'est pas une valeur de 8 bits
     */
    public LcdImageLine mapColors(int colors) {

        Preconditions.checkBits8(colors);

        if (colors == 0b11100100)
            return this;

        BitVector msbb = this.msb;
        BitVector lsbb = this.lsb;
        BitVector mask = null;

        for (int i = 0; i < 4; i++) {

            int color = colors >> 2 * i & 0b0000000000000000000000000000_0011;
            switch (i) {

            case 0:
                mask = msb().or(lsb()).not();
                break;

            case 1:
                mask = lsb().and(msb().not());
                break;

            case 2:
                mask = lsb().not().and(msb());
                break;

            case 3:
                mask = msb().and(lsb());
                break;
            }

            lsbb = (color % 2 == 0) ? lsbb.and(mask.not()) : lsbb.or(mask);
            msbb = (color / 2 < 1) ? msbb.and(mask.not()) : msbb.or(mask);

        }

        return new LcdImageLine(msbb, lsbb, opacity());

    }

    /**
     * créee une ligne en en mettant une en dessous d'une autre
     * 
     * @param lineUp
     *            ligne a mettre sur la ligne actuelle
     * @return la ligne créee
     * 
     * @throws IllegalArgumentException
     *             si les deux lignes n'ont pas la même taille
     * @throws NullPointerException
     *             si la ligne en argument est nulle
     */
    public LcdImageLine below(LcdImageLine lineUp) {

        Preconditions.checkArgument(size() == lineUp.size());
        Preconditions.checkArgument(!(lineUp == null));

        BitVector msbb = (this.msb.and(lineUp.opacity.not()))
                .or(lineUp.msb.and(lineUp.opacity));

        BitVector lsbb = (this.lsb.and(lineUp.opacity.not()))
                .or(lineUp.lsb.and(lineUp.opacity));

        BitVector opacityy = this.opacity.or(lineUp.opacity);

        return new LcdImageLine(msbb, lsbb, opacityy);

    }

    /**
     * créee une ligne en en mettant une en dessous d'une autre mais aveec un
     * vecteur d'opacié passé en argument
     * 
     * @param lineUp
     *            ligne a mettre sur la ligne actuelle
     * @return la ligne créee
     * 
     * @throws IllegalArgumentException
     *             si les deux lignes et le vecteur d'opacité n'ont pas la même
     *             taille
     * @throws NullPointerException
     *             si la ligne en argument est nulle ou si le vecteur d'opacité
     *             est nul.
     */
    public LcdImageLine below(LcdImageLine line, BitVector newOp) {

        Preconditions.checkArgument(!(line == null) || !(newOp == null));
        Preconditions.checkArgument(size() == line.size());
        Preconditions.checkArgument(size() == newOp.size());

        BitVector msbb = (line.msb.and(newOp)).or(msb.and(newOp.not()));
        BitVector lsbb = (line.lsb.and(newOp)).or(lsb.and(newOp.not()));
        BitVector opacityy = this.opacity.or(newOp);

        return new LcdImageLine(msbb, lsbb, opacityy);

    }

    /**
     * créee une ligne en joignant la ligne actuelle avec une ligne passée en
     * argument à partir d'un index donné
     * 
     * @param line
     *            la ligne a joindre
     * @param index
     *            l'index à partir duquel la jointure se fait
     * @return la ligne créee
     * @throws IllegalArgumentException
     *             si les deux lignes n'ont pas la même taille ou si l'index est
     *             inférieur à 0 ou si l'index est supérieur à la taille de la
     *             ligne
     * 
     * @throws NullPointerException
     *             si la ligne est nulle
     */
    public LcdImageLine join(LcdImageLine line, int index) {

        Preconditions.checkArgument(size() == line.size());
        Preconditions.checkArgument(!(line == null));
        Preconditions.checkArgument(index >= 0 && index < size());

        BitVector msb1 = this.msb.shift(-index + size()).shift(-size() + index);
        BitVector msb2 = line.msb.shift(-index).shift(index);
        BitVector lsb1 = this.lsb.shift(-index + size()).shift(-size() + index);
        BitVector lsb2 = line.lsb.shift(-index).shift(+index);
        BitVector opacity1 = this.opacity.shift(-index + size())
                .shift(-size() + index);
        BitVector opacity2 = line.opacity.shift(-index).shift(+index);

        return new LcdImageLine(msb1.or(msb2), lsb1.or(lsb2),
                opacity1.or(opacity2));

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {

        final int prime = 31;
        int result = 1;

        result = prime * result + ((lsb == null) ? 0 : lsb.hashCode());
        result = prime * result + ((msb == null) ? 0 : msb.hashCode());
        result = prime * result + ((opacity == null) ? 0 : opacity.hashCode());

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj)
            return true;

        if (obj == null)
            return false;

        if (getClass() != obj.getClass())
            return false;

        LcdImageLine other = (LcdImageLine) obj;

        if (lsb == null) {
            if (other.lsb != null)
                return false;

        } else if (!lsb.equals(other.lsb))
            return false;

        if (msb == null) {
            if (other.msb != null)
                return false;

        } else if (!msb.equals(other.msb))
            return false;

        if (opacity == null) {
            if (other.opacity != null)
                return false;

        } else if (!opacity.equals(other.opacity))
            return false;

        return true;

    }

    /**
     * @author Alvaro Cauderan ( 282186)
     * @author Gauthier Boeshertz (283192) Fait le builder d'une ligne
     */
    public static final class Builder {

        private final BitVector.Builder msbBuilder;
        private final BitVector.Builder lsbBuilder;
        private final int size;
        private boolean canBuild;

        /**
         * Construit un builder d'une taille donéne
         * 
         * @param size
         *            donne la taille de la ligne à construire
         * 
         * @throws Si
         *             la taille est négative ou si elle n'est pas multiple de
         *             32
         */
        public Builder(int size) {

            Preconditions.checkArgument(size % sizeOfInt == 0 && size > 0);
            msbBuilder = new BitVector.Builder(size);
            lsbBuilder = new BitVector.Builder(size);
            this.size = size;
            canBuild = true;
        }

        /**
         * Donne une valeur à l'index donnée au vecteur de lsb et de msb de la
         * ligne
         * 
         * @param index
         *            l'index ou on met les valeurs
         * @param octetmsb
         *            la valeur à mettre sur le vecteur de msb
         * @param octetlsb
         *            la valeur à mettre sur le vecteur de lsb
         * @return this pour pouvoir enchainer les actions sur le builder
         * 
         * @throws IllegalStateException
         *             si un build a deja été fait sur ce builder
         * @throws IllegarArgumentException
         *             si octetmsb ou octetlsb sont pas des valeurs de 8 bits
         * 
         * @throws IndexOutOfBounds
         *             si l'index passé en argument est plus petit que zero ou
         *             plus grand que la size
         */

        public Builder setBytes(int index, int octetmsb, int octetlsb) {
            if (!canBuild)
                throw new IllegalStateException();

            Preconditions.checkBits8(octetmsb);
            Preconditions.checkBits8(octetlsb);
            Objects.checkIndex(index, size);

            msbBuilder.setByte(index, octetmsb);
            lsbBuilder.setByte(index, octetlsb);

            return this;

        }

        /**
         * construit la ligne @return la ligne construite à partir du builder
         * 
         * @throws IllegalStateException
         *             si un build a deja été fait sur ce builder
         */
        public LcdImageLine build() {
            if (!canBuild)
                throw new IllegalStateException();

            BitVector msb = msbBuilder.build();
            BitVector lsb = lsbBuilder.build();
            BitVector opacity = msb.or(lsb);
            canBuild = false;

            return new LcdImageLine(msb, lsb, opacity);

        }

    }

}
