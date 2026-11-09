# Monitoring Data Mahasiswa dengan Java RMI

Implementasi tugas praktikum RMI: program server dan client Java, serta aplikasi web Servlet sebagai perantara ke layanan RMI. Fitur pengembangan mencakup login administrator, CRUD, pencarian nama, detail berdasarkan NIM, dan jumlah mahasiswa otomatis.

## Teknologi

- Java 17 atau lebih baru
- Maven
- Apache Tomcat 10.1 (Jakarta Servlet 6)
- Java RMI: registry `1099`, service `1100`
- Data contoh disimpan di memori; MySQL tidak diperlukan

## Menjalankan

Jalankan semua perintah Maven dari folder proyek:

```powershell
mvn clean package
```

### 1. Jalankan RMI Server

Atur alamat IP LAN server agar stub RMI dapat diakses komputer lain. Ganti alamat berikut dengan IP server:

```powershell
$env:JAVA_TOOL_OPTIONS="-Djava.rmi.server.hostname=192.168.1.10"
java -cp "rmi-common\target\rmi-common-1.0.0.jar;rmi-server\target\rmi-server-1.0.0.jar" id.ac.kampus.rmi.server.ServerMain
```

Buka port TCP `1099` dan `1100` pada firewall server. Biarkan terminal server tetap berjalan.

### 2. Uji RMI Client

Di terminal lain, gunakan `127.0.0.1` untuk uji lokal atau IP LAN server:

```powershell
java -cp "rmi-common\target\rmi-common-1.0.0.jar;rmi-client\target\rmi-client-1.0.0.jar" id.ac.kampus.rmi.client.ClientMain 127.0.0.1
```

### 3. Jalankan aplikasi web

Deploy `web-app\target\web-app-1.0.0.war` ke Apache Tomcat 10.1. Atur environment berikut pada proses Tomcat sebelum dijalankan:

```powershell
$env:RMI_HOST="127.0.0.1"
$env:RMI_REGISTRY_PORT="1099"
$env:APP_ADMIN_USER="admin"
$env:APP_ADMIN_PASSWORD="ganti-dengan-kata-sandi-kuat"
```

Jika Tomcat berjalan di komputer lain, set `RMI_HOST` ke IP LAN server RMI. Buka `http://localhost:8080/web-app/` lalu masuk dengan kredensial administrator tersebut. Jangan gunakan HTTP untuk kredensial di jaringan yang tidak tepercaya.

## Skenario uji

| Skenario | Hasil yang diharapkan |
|---|---|
| Menjalankan RMI Server | Registry dan service aktif pada port 1099 dan 1100 |
| Client terhubung | Data mahasiswa dari server tampil |
| Membuka web saat server aktif | Daftar dan jumlah mahasiswa tampil |
| Membuka detail berdasarkan NIM | Detail mahasiswa yang sesuai tampil |
| Mencari berdasarkan nama | Hanya data yang cocok ditampilkan |
| Menambahkan/mengubah/menghapus data | Perubahan berhasil dan jumlah diperbarui |
| Mengirim data tidak valid atau NIM duplikat | Pesan kesalahan ditampilkan |
| Mematikan server RMI | Web menampilkan pesan layanan tidak tersedia |
| Mencoba aksi web tanpa login atau token formulir valid | Aksi ditolak |

Data berada di memori server dan akan kembali ke data contoh saat server dimulai ulang. Untuk penggunaan nyata, gunakan database dan HTTPS.
