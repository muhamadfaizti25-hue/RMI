package id.ac.kampus.rmi.model;

import java.io.Serial;
import java.io.Serializable;

public final class Mahasiswa implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final String nim;
    private final String nama;
    private final String programStudi;
    private final int semester;
    private final String email;

    public Mahasiswa(String nim, String nama, String programStudi, int semester, String email) {
        this.nim = nim;
        this.nama = nama;
        this.programStudi = programStudi;
        this.semester = semester;
        this.email = email;
    }

    public String getNim() {
        return nim;
    }

    public String getNama() {
        return nama;
    }

    public String getProgramStudi() {
        return programStudi;
    }

    public int getSemester() {
        return semester;
    }

    public String getEmail() {
        return email;
    }
}
