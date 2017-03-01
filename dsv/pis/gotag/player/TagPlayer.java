package dsv.pis.gotag.player;

/**
 * Created by YannL on 28/02/2017.
 */
public interface TagPlayer {

    /**
     * Return true if the TagPLayer is 'it', false otherwise.
     *
     * @return  true if the TagPLayer is 'it', false otherwise.
     */
    public boolean isIt();

    /**
     * Tries to 'it' the TagPlayer.
     *
     * If the  TagPLayer is migrating, the 'it' will fail and the method will return false.
     * Otherwise, the 'it' succeeds and the TagPLayer become the 'it' agent and the method returns true.
     *
     * @return True if the TagPlayer has been 'it', false otherwise.
     */
    public boolean itAgent();
}
