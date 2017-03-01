package dsv.pis.gotag.player;

import dsv.pis.gotag.bailiff.BailiffInterface;
import dsv.pis.gotag.exceptions.NoSuchAgentException;
import dsv.pis.gotag.util.CmdlnOption;
import dsv.pis.gotag.util.Commandline;
import net.jini.core.lookup.ServiceItem;
import net.jini.core.lookup.ServiceTemplate;
import net.jini.lookup.ServiceDiscoveryManager;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class PlayerAgent implements Serializable, TagPlayer {

    /**
     * Unique identifier of the agent
     */
    protected UUID id;

    AtomicBoolean isIt = new AtomicBoolean(false);

    AtomicBoolean isMigrating = new AtomicBoolean(false);

    private Random rnd = new Random(System.currentTimeMillis());

    /**
     * The string name of the Bailiff service interface, used when
     * querying the Jini lookup server.
     */
    protected static final String bfi =
            "dsv.pis.gotag.bailiff.BailiffInterface";

    /**
     * The debug flag controls the amount of diagnostic info we put out.
     */
    protected boolean debug = false;


    /**
     * Dexter uses a ServiceDiscoveryManager to find Bailiffs.
     * The SDM is not serializable so it must recreated on each new Bailiff.
     * That is why it is marked as transient.
     */
    protected transient ServiceDiscoveryManager SDM;

    /**
     * This service template is created in Dexter's constructor and used
     * in the topLevel method to find Bailiffs. The service
     * template IS serializable so Dexter only needs to instantiate it once.
     */
    protected ServiceTemplate bailiffTemplate;

    /**
     * Outputs a diagnostic message on standard output. This will be on
     * the host of the launching JVM before Dexter moves. Once he has migrated
     * to another Bailiff, the text will appear on the console of that Bailiff.
     *
     * @param msg The message to print.
     */
    protected void debugMsg(String msg) {
        if (debug) System.out.println(msg + "\t| {" + this + "}");
    }


    /**
     * Returns a string representation of this service instance.
     *
     * @returns the UUID as a string.
     */
    public String toString() {
        return id.toString();
    }


    public PlayerAgent(boolean debug)
            throws ClassNotFoundException {
        if (this.debug == false) this.debug = debug;

        // Generate a random UUID for the agent instance
        this.id = UUID.randomUUID();

        // This service template is used to query the Jini lookup server
        // for services which implement the BailiffInterface. The string
        // name of that interface is passed in the bfi argument. At this
        // point we only create and configure the service template, no
        // query has yet been issued.

        bailiffTemplate =
                new ServiceTemplate
                        (null,
                                new Class[]{java.lang.Class.forName(bfi)},
                                null);
    }

    /**
     * Getter for the UUID of the agent
     *
     * @return The UUID of the agent
     */
    public UUID getUUID() {
        return id;
    }


    /**
     * Sleep snugly and safely not bothered by interrupts.
     *
     * @param ms The number of milliseconds to sleep.
     */
    protected void snooze(long ms) {
        try {
            Thread.currentThread().sleep(ms);
        } catch (java.lang.InterruptedException e) {
        }
    }


    public void topLevel(Boolean isIt)
            throws
            java.io.IOException {

        this.isMigrating.compareAndSet(true, false);

        // Set the it property
        this.isIt.set(isIt);

        // Local Bailiff
        BailiffInterface localBailiff = null;

        debugMsg("\n[Start Toplevel] isIt = " + (this.isIt.get() ? "YES" : "NO"));
        if (debug)
            System.out.println();

        // Create a Jini service discovery manager to help us interact with
        // the Jini lookup service.
        SDM = new ServiceDiscoveryManager(null, null);

        for (; ; ) {

            ServiceItem[] svcItems;

            long retryInterval = 0;

            // The restraint sleep is just there so we don't get hyperactive
            // and confuse the slow human beings.

            debugMsg("Entering restraint sleep");

            snooze(5000);

            debugMsg("Leaving restraint sleep");

            // Enter a loop in which Dexter tries to find some Bailiffs.

            do {

                if (0 < retryInterval) {
                    debugMsg("[No bailiff detected] Sleeping");
                    snooze(retryInterval);
                    debugMsg("[No Bailiff detected] Waking up");
                }

                // Put our query, expressed as a service template, to the Jini
                // service discovery manager.

                svcItems = SDM.lookup(bailiffTemplate, 8, null);
                retryInterval = 20 * 1000;

                // If no lookup servers are found, go back up to the beginning
                // of the loop, sleep a bit and then try again.
            } while (svcItems.length == 0);

            // We have the Bailiffs.

            debugMsg("Found " + svcItems.length + " Bailiffs.");

            // Now enter a loop in which we try to ping and migrate to them.

            int nofItems = svcItems.length;

            // 1) Find our local bailiff
            try {
                int idx = 0;
                while (localBailiff == null && idx < nofItems) {
                    Object obj = svcItems[idx].service; // Get the service object
                    idx++;

                    // Try to ping the bailiff
                    BailiffInterface bfi = pingBailiff(obj);
                    if (bfi == null) {
                        // Ping failed
                        continue;
                    }

                    // Ping successful
                    ArrayList<UUID> agentsList = bfi.getAgentsNames();
                    if (agentsList.contains(id)) {
                        localBailiff = bfi;
                    }
                }
            } catch (java.rmi.RemoteException e) { // FAILURE
                if (debug) {
                    e.printStackTrace();
                }
                localBailiff = null;
            }

            // 2) If not in a bailiff => migrate in one chosen randomly
            if (localBailiff == null) {
                debugMsg("Not in a bailiff");
                if (migrate(svcItems, nofItems))
                    return; // Migrate = SUCCESS
                else
                    continue;
            }


            // If it => try to it a player agent
            if (this.isIt.get()) {
                try {
                    debugMsg("[IT Agent] In action");

                    // 3) Try to it one agent in the local bailiff
                    ArrayList<UUID> agentsList = localBailiff.getAgentsNames();
                    debugMsg("Nb agent in local bailiff = " + agentsList.size());

                    int nbAgents = agentsList.size();
                    int nbAttempts = 0;
                    while (this.isIt.get() && (nbAttempts < nbAgents - 1)) { // -1 => do not count the it agent
                        nbAttempts++;

                        UUID agent = agentsList.get(rnd.nextInt(nbAgents));

                        // Avoid to it ourselves
                        if (agent.equals(id))
                            continue;

                        // Try to it the agent
                        try {
                            if (localBailiff.itAgent(agent)) {
                                // It successfull
                                this.isIt.compareAndSet(true, false);
                                debugMsg("[IT SUCCEEDED] Agent succeeded to it agent " + agent + " !");
                                if (debug)
                                    System.out.println();
                                continue;
                            }
                        } catch (NoSuchAgentException e) {
                            if (debug) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // If still it agent => migrate in another bailiff
                    if (migrate(svcItems, nofItems))
                        return; // Migrate = SUCCESS
                    else
                        continue;

                } catch (java.rmi.RemoteException e) { // FAILURE
                    if (debug) {
                        e.printStackTrace();
                    }
                    localBailiff = null;
                }

            } else {
                debugMsg("[Simple Agent] In action ");

                snooze(2000); // We put a snooze to avoid agent to always migrating and therefore to never be 'itable'

                // Not a it agent
                if (migrate(svcItems, nofItems))
                    return; // Migrate = SUCCESS
                else
                    continue;
            }
        } // for ever // go back up and try to find more Bailiffs
    }


    private boolean migrate(ServiceItem[] svcItems, int nbItems) {
        // While we still have at least one Bailiff service to try...
        int nofItems = nbItems;

        while (0 < nofItems) {

            // Select one Bailiff randomly.
            int idx = 0;
            if (1 < nofItems) {
                idx = rnd.nextInt(nofItems);
            }


            Object obj = svcItems[idx].service; // Get the service object

            // Try to ping the selected Bailiff.
            BailiffInterface bfi = pingBailiff(obj);

            debugMsg("[Ping Result] " + (bfi != null ? "Accepted." : "Not accepted."));

            // If the ping failed, delete that Bailiff from the array and
            // try another. The current (idx) entry in the list of service items
            // is replaced by the last item in the list, and the list length
            // is decremented by one.
            if (bfi == null) {
                svcItems[idx] = svcItems[nofItems - 1];
                nofItems -= 1;
                continue;        // Back to top of while-loop.
            } else {


                // This is the spot where PlayerAgent tries to migrate
                try {
                    // TODO : Remove debugging
                    ArrayList<UUID> agentsList = bfi.getAgentsNames();
                    debugMsg("List of agents | Size = " + agentsList.size());
                    for (int i = 0; i < agentsList.size(); ++i) {
                        try {
                            debugMsg("Agent " + i + " : " + agentsList.get(i)
                                    + " | isIt = " + (bfi.isIt(agentsList.get(i)) ? "YES" : "NO"));
                        } catch (NoSuchAgentException noSuchAgentInCurrentBailiff) {
                            debugMsg("Agent " + i + " : " + agentsList.get(i));
                        }
                    }

                    debugMsg("[Trying to migrate] isIT = " + (this.isIt.get() ? "YES" : "NO"));

                    this.isMigrating.set(true);
                    bfi.migrate(this, "topLevel", new Object[]{isIt.get()});
                    //this.isMigrating.compareAndSet(true, false);

                    debugMsg("[Migrating Succeeded]");
                    SDM.terminate();    // SUCCESS
                    return true;        // SUCCESS
                } catch (java.rmi.RemoteException | java.lang.NoSuchMethodException e) { // FAILURE
                    if (debug) {
                        e.printStackTrace();
                    }
                    //this.isMigrating.compareAndSet(true, false);
                }

                debugMsg("[Migrating failed]");
            }
        }    // while there are candidates left

        return false;
    }

    /**
     * Try to ping the bailiff corresponding to the object passed as parameter.
     * If the ping is successful, it returns the reference on the instance of the Bailiff.
     * Otherwise, if it fails, it returns null.
     *
     * @param objToPing
     * @return
     */
    private BailiffInterface pingBailiff(Object objToPing) {
        boolean accepted = false;        // Assume it will fail

        BailiffInterface bfi = null;

        // Try to ping the selected Bailiff.
        debugMsg("[Trying to ping]");
        try {
            if (objToPing instanceof BailiffInterface) {
                bfi = (BailiffInterface) objToPing;
                String response = bfi.ping(); // Ping it
                debugMsg(response);
                accepted = true;    // Oh, it worked!
            }
        } catch (java.rmi.RemoteException e) { // Ping failed
            if (debug) {
                e.printStackTrace();
            }
        } finally {
            return (accepted ? bfi : null);
        }
    }


    /**
     * The main program of Player Agent. It is only used when a tag player agent is launched.
     */
    public static void main(String[] argv)
            throws
            java.lang.ClassNotFoundException,
            java.io.IOException {
        CmdlnOption helpOption = new CmdlnOption("-help");
        CmdlnOption debugOption = new CmdlnOption("-debug");
        CmdlnOption isItByDefault = new CmdlnOption("-it");

        CmdlnOption[] opts =
                new CmdlnOption[]{helpOption, debugOption, isItByDefault};

        String[] restArgs = Commandline.parseArgs(System.out, argv, opts);

        if (restArgs == null) {
            System.exit(1);
        }

        if (helpOption.getIsSet() == true) {
            System.out.println("Usage: [-help]|[-debug][-it]");
            System.out.println("where -help shows this message");
            System.out.println("      -debug turns on debugging.");
            System.out.println("      -it player agent 'it' by default.");
            System.exit(0);
        }

        boolean debug = debugOption.getIsSet();
        boolean it = isItByDefault.getIsSet();

        // We will try without it first
        // System.setSecurityManager (new RMISecurityManager ());
        PlayerAgent pa = new PlayerAgent(debug);
        pa.topLevel(it);
        System.exit(0);
    }

    @Override
    public boolean isIt() {
        return isIt.get();
    }

    @Override
    public boolean itAgent() {
        debugMsg("\n[TRY TO IT] Someone try to it me...!");

        // If is migrating, cannot be it
        if (isMigrating.get()) {
            debugMsg("[IT FAILED] We are not it because I am migrating :)");
            if (debug)
                System.out.println();
            return false;
        }

        // Otherwise, become the it agent
        debugMsg("[IT SUCCESS] I have been it :( !");
        return isIt.compareAndSet(false, true);
    }
}
