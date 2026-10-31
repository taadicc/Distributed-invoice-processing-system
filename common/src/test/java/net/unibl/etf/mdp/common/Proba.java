package net.unibl.etf.mdp.common;

import net.unibl.etf.mdp.common.config.AppConfig;

public class Proba {
    public static void main(String[] args) {

        System.out.println("inbox: " + AppConfig.get("inbox.dir", "NEMA"));
        System.out.println("parser.host: " + AppConfig.get("parser.host", "NEMA"));
        System.out.println("parser.port: " + AppConfig.getInt("parser.port", -1));
        System.out.println("mq.url: " + AppConfig.get("mq.url", "NEMA"));
        System.out.println("multicast.port: " + AppConfig.getInt("multicast.port", -1));

        System.out.println("nepostojeci: " + AppConfig.get("nema.me", "podrazumijevano"));
    }
}