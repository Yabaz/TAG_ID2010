// BailiffInterface.java
// Fredrik Kilander, DSV
// 18-nov-2004/FK Adapted for PIS course.
// 2000-12-13/FK Adapted from earlier version.

package dsv.pis.gotag.bailiff;

import dsv.pis.gotag.exceptions.NoSuchAgentException;
import dsv.pis.gotag.player.TagPlayer;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * This interface is for the Bailiff's clients. This is mobile code which
 * move into the Bailiff's JVM for execution.
 */
public interface BailiffInterface
        extends
        java.rmi.Remote {
    /**
     * Returns a string which confirms communication with the Bailiff
     * service instance.
     */
    public String ping()
            throws
            java.rmi.RemoteException;

    /**
     * Returns a property of the Bailiff.
     *
     * @param key The case-insensitive property key to retrieve.
     * @return The property string or null.
     */
    public String getProperty(String key)
            throws
            java.rmi.RemoteException;

    /**
     * The entry point for mobile code.
     * The client sends and object (itself perhaps), a string
     * naming the callback method and an array of arguments which must
     * map against the parameters of the callback method.
     *
     * @param obj  The object (to execute).
     * @param cb   The name of the method to call as the program of obj.
     * @param args The parameters for the callback method. Note that if
     *             the method has a signature without arguments the value of args
     *             should be an empty array. Setting args to null will not work.
     * @throws java.rmi.RemoteException        Thrown if there is an RMI problem.
     * @throws java.lang.NoSuchMethodException Thrown if the proposed
     *                                         callback is not found (which happen if the name is spelled wrong,
     *                                         the number of arguments is wrong or are of the wrong type).
     */
    public void migrate(TagPlayer obj, String cb, Object[] args)
            throws
            java.rmi.RemoteException,
            java.lang.NoSuchMethodException;


    /**
     * To retrieve the names of agents in the requested Bailiff
     *
     * @return An list containing the UUID of each agent in the local Bailiff
     * @throws RemoteException
     */
    public ArrayList<UUID> getAgentsNames()
            throws java.rmi.RemoteException;


    /**
     * For asking to an agent if he is 'it'.
     * If the agent has migrated and is not in the Bailiff anymore, a {@link java.util.NoSuchElementException}
     * is thrown. Otherwise, the remote agent return true if he is the 'it' agent or false if he is not.
     *
     * @param name The UUID of the remote agent
     * @return true if the agent with the UUID name is 'it', otherwise false
     * @throws java.rmi.RemoteException
     * @throws NoSuchAgentException
     */
    public boolean isIt(UUID name)
            throws java.rmi.RemoteException, NoSuchAgentException;

    /**
     * Try to 'it' a remote agent.
     * If the remote agent has migrated and is not in the Bailiff anymore,
     * a {@link java.util.NoSuchElementException} is thrown.
     * If the remote agent is migrating, the 'it' will fail and the method will return false.
     * Otherwise, the 'it' succeeds and the method returns true.
     *
     * @param name The UUID of the remote agent
     * @return true if the 'it' succeeds, otherwise false
     * @throws java.rmi.RemoteException
     * @throws NoSuchAgentException
     */
    public boolean itAgent(UUID name)
            throws java.rmi.RemoteException, NoSuchAgentException;

}
