package id.ac.kampus.rmi.service;

import id.ac.kampus.rmi.model.Mahasiswa;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface MahasiswaService extends Remote {
    List<Mahasiswa> getAllMahasiswa() throws RemoteException;

    Mahasiswa findByNim(String nim) throws RemoteException;

    List<Mahasiswa> searchByName(String keyword) throws RemoteException;

    void addMahasiswa(Mahasiswa mahasiswa) throws RemoteException;

    void updateMahasiswa(Mahasiswa mahasiswa) throws RemoteException;

    boolean deleteMahasiswa(String nim) throws RemoteException;

    int getJumlahMahasiswa() throws RemoteException;
}
