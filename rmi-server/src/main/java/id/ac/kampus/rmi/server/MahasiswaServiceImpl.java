package id.ac.kampus.rmi.server;

import id.ac.kampus.rmi.model.Mahasiswa;
import id.ac.kampus.rmi.service.MahasiswaService;

import java.rmi.RemoteException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class MahasiswaServiceImpl implements MahasiswaService {
    private final ConcurrentMap<String, Mahasiswa> data = new ConcurrentHashMap<>();

    public MahasiswaServiceImpl() {
        addSeed("2023001", "Alya Putri", "Informatika", 5, "alya@example.test");
        addSeed("2023002", "Bima Pratama", "Sistem Informasi", 5, "bima@example.test");
        addSeed("2023003", "Citra Lestari", "Informatika", 3, "citra@example.test");
    }

    @Override
    public List<Mahasiswa> getAllMahasiswa() {
        return data.values().stream()
                .sorted(Comparator.comparing(Mahasiswa::getNim))
                .toList();
    }

    @Override
    public Mahasiswa findByNim(String nim) {
        return data.get(normalizeNim(nim));
    }

    @Override
    public List<Mahasiswa> searchByName(String keyword) {
        String query = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return getAllMahasiswa();
        }
        return getAllMahasiswa().stream()
                .filter(mahasiswa -> mahasiswa.getNama().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    @Override
    public void addMahasiswa(Mahasiswa mahasiswa) throws RemoteException {
        validate(mahasiswa);
        String nim = normalizeNim(mahasiswa.getNim());
        if (data.putIfAbsent(nim, mahasiswa) != null) {
            throw new IllegalArgumentException("NIM tersebut sudah terdaftar.");
        }
    }

    @Override
    public void updateMahasiswa(Mahasiswa mahasiswa) throws RemoteException {
        validate(mahasiswa);
        String nim = normalizeNim(mahasiswa.getNim());
        if (data.replace(nim, mahasiswa) == null) {
            throw new IllegalArgumentException("Data mahasiswa tidak ditemukan.");
        }
    }

    @Override
    public boolean deleteMahasiswa(String nim) {
        return data.remove(normalizeNim(nim)) != null;
    }

    @Override
    public int getJumlahMahasiswa() {
        return data.size();
    }

    private void addSeed(String nim, String nama, String programStudi, int semester, String email) {
        data.put(nim, new Mahasiswa(nim, nama, programStudi, semester, email));
    }

    private static String normalizeNim(String nim) {
        if (nim == null || nim.isBlank()) {
            throw new IllegalArgumentException("NIM wajib diisi.");
        }
        return nim.trim();
    }

    private static void validate(Mahasiswa mahasiswa) {
        if (mahasiswa == null) {
            throw new IllegalArgumentException("Data mahasiswa wajib diisi.");
        }
        String nim = normalizeNim(mahasiswa.getNim());
        if (nim.length() > 20) {
            throw new IllegalArgumentException("NIM maksimal 20 karakter.");
        }
        requireText(mahasiswa.getNama(), "Nama", 100);
        requireText(mahasiswa.getProgramStudi(), "Program studi", 100);
        if (mahasiswa.getSemester() < 1 || mahasiswa.getSemester() > 14) {
            throw new IllegalArgumentException("Semester harus antara 1 dan 14.");
        }
        requireText(mahasiswa.getEmail(), "Email", 150);
    }

    private static void requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " wajib diisi.");
        }
        if (value.trim().length() > maxLength) {
            throw new IllegalArgumentException(label + " maksimal " + maxLength + " karakter.");
        }
    }
}
