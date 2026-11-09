package id.ac.kampus.rmi.server;

import id.ac.kampus.rmi.service.MahasiswaService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class ServerMain {
    private static final int REGISTRY_PORT = 1099;
    private static final int SERVICE_PORT = 1100;
    private static final String SERVICE_NAME = "MahasiswaService";

    private ServerMain() {
    }

    public static void main(String[] args) throws Exception {
        Registry registry = LocateRegistry.createRegistry(REGISTRY_PORT);
        MahasiswaServiceImpl service = new MahasiswaServiceImpl();
        MahasiswaService stub = (MahasiswaService) UnicastRemoteObject.exportObject(service, SERVICE_PORT);
        registry.rebind(SERVICE_NAME, stub);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(SERVICE_NAME);
            } catch (Exception ignored) {
            }
            try {
                UnicastRemoteObject.unexportObject(service, true);
                UnicastRemoteObject.unexportObject(registry, true);
            } catch (Exception ignored) {
            }
        }));

        System.out.printf("RMI Server aktif. Registry: %d, service: %d, nama: %s%n",
                REGISTRY_PORT, SERVICE_PORT, SERVICE_NAME);
    }
}
