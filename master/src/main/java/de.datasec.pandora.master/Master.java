package de.datasec.pandora.master;

import com.datastax.driver.core.policies.DefaultRetryPolicy;
import de.datasec.pandora.master.bot.MasterBot;
import de.datasec.pandora.master.config.MasterServerConfig;
import de.datasec.pandora.master.roundrobinlist.LinkedRoundRobinList;
import de.datasec.pandora.master.roundrobinlist.RoundRobinList;
import de.datasec.pandora.shared.PandoraProtocol;
import de.datasec.pandora.shared.database.CassandraManager;
import de.jackwhite20.cascade.server.Server;
import de.jackwhite20.cascade.server.ServerFactory;
import de.jackwhite20.cascade.shared.protocol.listener.PacketListener;
import de.jackwhite20.cascade.shared.session.Session;
import de.jackwhite20.cascade.shared.session.SessionListener;

/**
 * Created by DataSec on 27.11.2016.
 */
public class Master implements PacketListener {

    private static CassandraManager cassandraManager;

    private String startUrl;

    private int urlsPerPacket;

    private RoundRobinList<Session> sessions = new LinkedRoundRobinList<>();

    public Master(String startUrl, int urlsPerPacket) {
        this.startUrl = startUrl;
        this.urlsPerPacket = urlsPerPacket;

        // Cassandra
        cassandraManager = new CassandraManager("188.68.54.85", "pandora");
        cassandraManager.connect(DefaultRetryPolicy.INSTANCE);
    }

    public void start() {
        Server server = ServerFactory.create(new MasterServerConfig(new PandoraProtocol(this)));

        server.addSessionListener(new SessionListener() {
            @Override
            public void onConnected(Session session) {
                sessions.add(session);

                System.out.println("Slave connected! Amount of connected slaves: " + sessions.size());
            }

            @Override
            public void onDisconnected(Session session) {
                sessions.remove(session);

                System.out.println("Slave disconnected! Amount of connected slaves: " + sessions.size());
            }

            @Override
            public void onStarted() {
                System.out.println("Server started!");
            }

            @Override
            public void onStopped() {
                System.out.println("Server stopped!");
            }
        });

        server.start();
        new MasterBot(sessions, startUrl, urlsPerPacket, 3);
        server.stop();
    }

    public static CassandraManager getCassandraManager() {
        return cassandraManager;
    }
}
