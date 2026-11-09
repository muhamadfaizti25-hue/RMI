package id.ac.kampus.rmi.client;

import id.ac.kampus.rmi.model.Mahasiswa;
import id.ac.kampus.rmi.service.MahasiswaService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public final class ClientMain {
    private ClientMain() {
    }

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        try {
            Registry registry = LocateRegistry.getRegistry(host, 1099);
            MahasiswaService service = (MahasiswaService) registry.lookup("MahasiswaService");
            System.out.printf("Terhubung ke RMI Server di %s%n", host);
            System.out.printf("Jumlah mahasiswa: %d%n%n", service.getJumlahMahasiswa());
            for (Mahasiswa mahasiswa : service.getAllMahasiswa()) {
                System.out.printf("%s | %s | %s | Semester %d | %s%n",
                        mahasiswa.getNim(), mahasiswa.getNama(), mahasiswa.getProgramStudi(),
                        mahasiswa.getSemester(), mahasiswa.getEmail());
            }
        } catch (Exception exception) {
            System.err.printf("Koneksi ke RMI Server %s gagal: %s%n", host, exception.getMessage());
            System.err.println("Pastikan server aktif dan port 1099 serta 1100 dapat diakses.");
            System.exit(1);
        }
    }
}
