package id.ac.kampus.rmi.web;

import id.ac.kampus.rmi.model.Mahasiswa;
import id.ac.kampus.rmi.service.MahasiswaService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;

@WebServlet(urlPatterns = {"/", "/login", "/logout", "/mahasiswa"})
public final class MahasiswaWebServlet extends HttpServlet {
    private static final String SERVICE_NAME = "MahasiswaService";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if ("/login".equals(path) || "/".equals(path) && !isLoggedIn(request)) {
            renderLogin(request, response, null);
            return;
        }
        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if ("/".equals(path) || "/mahasiswa".equals(path)) {
            renderDashboard(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String action = request.getParameter("action");
        if ("login".equals(action)) {
            if (!validCsrf(request)) {
                renderLogin(request, response, "Sesi formulir tidak valid. Muat ulang halaman dan coba lagi.");
                return;
            }
            if (!credentialsMatch(request.getParameter("username"), request.getParameter("password"))) {
                renderLogin(request, response, "Nama pengguna atau kata sandi salah.");
                return;
            }
            request.changeSessionId();
            request.getSession().setAttribute("loggedIn", Boolean.TRUE);
            response.sendRedirect(request.getContextPath() + "/mahasiswa");
            return;
        }

        if (!isLoggedIn(request)) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }
        if (!validCsrf(request)) {
            renderError(response, HttpServletResponse.SC_BAD_REQUEST,
                    "Permintaan tidak valid. Muat ulang halaman dan coba lagi.");
            return;
        }

        if ("logout".equals(action)) {
            request.getSession().invalidate();
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        try {
            MahasiswaService service = lookupService();
            if ("delete".equals(action)) {
                if (!service.deleteMahasiswa(request.getParameter("nim"))) {
                    throw new IllegalArgumentException("Data mahasiswa tidak ditemukan.");
                }
            } else {
                Mahasiswa mahasiswa = mahasiswaFrom(request);
                if ("create".equals(action)) {
                    service.addMahasiswa(mahasiswa);
                } else if ("update".equals(action)) {
                    service.updateMahasiswa(mahasiswa);
                } else {
                    response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Aksi tidak dikenal.");
                    return;
                }
            }
            response.sendRedirect(request.getContextPath() + "/mahasiswa");
        } catch (IllegalArgumentException exception) {
            renderError(response, HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            getServletContext().log("Gagal memproses data mahasiswa melalui RMI.", exception);
            renderError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Layanan RMI tidak tersedia. Periksa server dan konfigurasi koneksi.");
        }
    }

    private void renderLogin(HttpServletRequest request, HttpServletResponse response, String error)
            throws IOException {
        boolean configured = configured("APP_ADMIN_USER") && configured("APP_ADMIN_PASSWORD");
        StringBuilder body = new StringBuilder();
        if (error != null) {
            body.append("<p class=\"error\">").append(escape(error)).append("</p>");
        }
        if (!configured) {
            body.append("<p class=\"error\">Login belum dikonfigurasi. Atur APP_ADMIN_USER dan ")
                    .append("APP_ADMIN_PASSWORD pada environment Tomcat.</p>");
        }
        body.append("<form method=\"post\" action=\"").append(escape(request.getContextPath()))
                .append("/login\"><input type=\"hidden\" name=\"action\" value=\"login\">")
                .append(csrfInput(request.getSession(true)))
                .append("<label>Nama pengguna<input name=\"username\" autocomplete=\"username\" required></label>")
                .append("<label>Kata sandi<input type=\"password\" name=\"password\" ")
                .append("autocomplete=\"current-password\" required></label>")
                .append("<button type=\"submit\" ").append(configured ? "" : "disabled")
                .append(">Masuk</button></form>");
        writePage(response, "Login Administrator", body.toString());
    }

    private void renderDashboard(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            MahasiswaService service = lookupService();
            String query = request.getParameter("q");
            List<Mahasiswa> mahasiswa = query == null || query.isBlank()
                    ? service.getAllMahasiswa() : service.searchByName(query);
            StringBuilder body = new StringBuilder();
            body.append("<div class=\"toolbar\"><div><h1>Data Mahasiswa</h1><p class=\"muted\">")
                    .append(service.getJumlahMahasiswa()).append(" mahasiswa terdaftar</p></div>")
                    .append("<a class=\"button\" href=\"").append(escape(request.getContextPath()))
                    .append("/mahasiswa?form=create\">Tambah mahasiswa</a></div>")
                    .append("<form class=\"search\" method=\"get\"><label class=\"sr-only\" for=\"q\">")
                    .append("Cari nama mahasiswa</label><input id=\"q\" name=\"q\" placeholder=\"Cari nama...\" value=\"")
                    .append(escape(query == null ? "" : query)).append("\"><button>Cari</button>")
                    .append("<a href=\"").append(escape(request.getContextPath()))
                    .append("/mahasiswa\">Reset</a></form>");

            String form = request.getParameter("form");
            String nim = request.getParameter("nim");
            if ("create".equals(form)) {
                appendMahasiswaForm(body, request, null);
            } else if ("edit".equals(form)) {
                Mahasiswa existing = service.findByNim(nim);
                if (existing == null) {
                    body.append("<p class=\"error\">Data mahasiswa tidak ditemukan.</p>");
                } else {
                    appendMahasiswaForm(body, request, existing);
                }
            } else if (nim != null && !nim.isBlank()) {
                appendDetail(body, request, service.findByNim(nim));
            }

            body.append("<div class=\"table-wrap\"><table><thead><tr><th>NIM</th><th>Nama</th>")
                    .append("<th>Program Studi</th><th>Semester</th><th>Email</th><th>Aksi</th>")
                    .append("</tr></thead><tbody>");
            if (mahasiswa.isEmpty()) {
                body.append("<tr><td colspan=\"6\" class=\"empty\">Tidak ada data mahasiswa.</td></tr>");
            }
            for (Mahasiswa item : mahasiswa) {
                String encodedNim = escape(item.getNim());
                body.append("<tr><td>").append(encodedNim).append("</td><td><a href=\"")
                        .append(escape(request.getContextPath())).append("/mahasiswa?nim=")
                        .append(encodedNim).append("\">").append(escape(item.getNama()))
                        .append("</a></td><td>").append(escape(item.getProgramStudi()))
                        .append("</td><td>").append(item.getSemester()).append("</td><td>")
                        .append(escape(item.getEmail())).append("</td><td class=\"actions\"><a href=\"")
                        .append(escape(request.getContextPath())).append("/mahasiswa?form=edit&amp;nim=")
                        .append(encodedNim).append("\">Ubah</a>")
                        .append("<form method=\"post\" onsubmit=\"return confirm('Hapus data ini?')\">")
                        .append(csrfInput(request.getSession()))
                        .append("<input type=\"hidden\" name=\"action\" value=\"delete\">")
                        .append("<input type=\"hidden\" name=\"nim\" value=\"").append(encodedNim)
                        .append("\"><button class=\"link danger\" type=\"submit\">Hapus</button></form></td></tr>");
            }
            body.append("</tbody></table></div>");
            body.append("<form class=\"logout\" method=\"post\"><input type=\"hidden\" name=\"action\" value=\"logout\">")
                    .append(csrfInput(request.getSession())).append("<button class=\"link\">Keluar</button></form>");
            writePage(response, "Monitoring Mahasiswa", body.toString());
        } catch (Exception exception) {
            getServletContext().log("Tidak dapat mengambil data mahasiswa dari RMI.", exception);
            renderError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Layanan RMI tidak tersedia. Pastikan server RMI aktif dan alamatnya benar.");
        }
    }

    private void appendMahasiswaForm(StringBuilder body, HttpServletRequest request, Mahasiswa item) {
        boolean editing = item != null;
        body.append("<section class=\"card\"><h2>").append(editing ? "Ubah" : "Tambah")
                .append(" mahasiswa</h2><form method=\"post\"><input type=\"hidden\" name=\"action\" value=\"")
                .append(editing ? "update" : "create").append("\">")
                .append(csrfInput(request.getSession()));
        appendInput(body, "NIM", "nim", editing ? item.getNim() : "", !editing);
        appendInput(body, "Nama", "nama", editing ? item.getNama() : "", true);
        appendInput(body, "Program studi", "programStudi", editing ? item.getProgramStudi() : "", true);
        appendInput(body, "Semester", "semester", editing ? Integer.toString(item.getSemester()) : "1", true);
        appendInput(body, "Email", "email", editing ? item.getEmail() : "", true);
        body.append("<button type=\"submit\">Simpan</button> <a href=\"")
                .append(escape(request.getContextPath())).append("/mahasiswa\">Batal</a></form></section>");
    }

    private void appendDetail(StringBuilder body, HttpServletRequest request, Mahasiswa item) {
        if (item == null) {
            body.append("<p class=\"error\">Data mahasiswa tidak ditemukan.</p>");
            return;
        }
        body.append("<section class=\"card\"><h2>Detail mahasiswa</h2><dl>")
                .append("<dt>NIM</dt><dd>").append(escape(item.getNim()))
                .append("</dd><dt>Nama</dt><dd>").append(escape(item.getNama()))
                .append("</dd><dt>Program Studi</dt><dd>").append(escape(item.getProgramStudi()))
                .append("</dd><dt>Semester</dt><dd>").append(item.getSemester())
                .append("</dd><dt>Email</dt><dd>").append(escape(item.getEmail()))
                .append("</dd></dl><a href=\"").append(escape(request.getContextPath()))
                .append("/mahasiswa\">Kembali ke daftar</a></section>");
    }

    private static void appendInput(StringBuilder body, String label, String name, String value, boolean required) {
        body.append("<label>").append(escape(label)).append("<input name=\"").append(name)
                .append("\" value=\"").append(escape(value)).append("\" ")
                .append(required ? "required" : "readonly").append(" maxlength=\"150\"></label>");
    }

    private static Mahasiswa mahasiswaFrom(HttpServletRequest request) {
        String nim = request.getParameter("nim");
        String nama = request.getParameter("nama");
        String programStudi = request.getParameter("programStudi");
        String email = request.getParameter("email");
        String semesterValue = request.getParameter("semester");
        try {
            int semester = Integer.parseInt(semesterValue);
            return new Mahasiswa(nim, nama, programStudi, semester, email);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Semester harus berupa angka.");
        }
    }

    private MahasiswaService lookupService() throws Exception {
        String host = System.getenv().getOrDefault("RMI_HOST", "127.0.0.1");
        int port = Integer.parseInt(System.getenv().getOrDefault("RMI_REGISTRY_PORT", "1099"));
        Registry registry = LocateRegistry.getRegistry(host, port);
        return (MahasiswaService) registry.lookup(SERVICE_NAME);
    }

    private static boolean credentialsMatch(String username, String password) {
        String expectedUser = System.getenv("APP_ADMIN_USER");
        String expectedPassword = System.getenv("APP_ADMIN_PASSWORD");
        return expectedUser != null && expectedPassword != null && username != null && password != null
                && MessageDigest.isEqual(expectedUser.getBytes(StandardCharsets.UTF_8),
                username.getBytes(StandardCharsets.UTF_8))
                && MessageDigest.isEqual(expectedPassword.getBytes(StandardCharsets.UTF_8),
                password.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean configured(String key) {
        String value = System.getenv(key);
        return value != null && !value.isBlank();
    }

    private static boolean isLoggedIn(HttpServletRequest request) {
        return request.getSession(false) != null
                && Boolean.TRUE.equals(request.getSession(false).getAttribute("loggedIn"));
    }

    private static String csrfInput(HttpSession session) {
        return "<input type=\"hidden\" name=\"csrf\" value=\"" + csrfToken(session) + "\">";
    }

    private static String csrfToken(HttpSession session) {
        String token = (String) session.getAttribute("csrf");
        if (token == null) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            session.setAttribute("csrf", token);
        }
        return token;
    }

    private static boolean validCsrf(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return false;
        }
        String expected = (String) session.getAttribute("csrf");
        String supplied = request.getParameter("csrf");
        return expected != null && supplied != null
                && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }

    private static void renderError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        writePage(response, "Terjadi Kesalahan",
                "<p class=\"error\">" + escape(message) + "</p><a href=\"javascript:history.back()\">Kembali</a>");
    }

    private static void writePage(HttpServletResponse response, String title, String body) throws IOException {
        response.setContentType("text/html; charset=UTF-8");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'unsafe-inline'; "
                + "script-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'");
        response.getWriter().append("<!doctype html><html lang=\"id\"><head><meta charset=\"utf-8\">")
                .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
                .append("<title>").append(escape(title)).append("</title><style>")
                .append("body{font:16px system-ui,sans-serif;background:#f3f6fb;color:#17233b;margin:0}")
                .append("main{max-width:1050px;margin:48px auto;padding:0 20px}h1{margin:0}")
                .append(".card,table,.search{background:#fff;border:1px solid #dfe5ef;border-radius:12px}")
                .append(".card{padding:24px;margin:24px 0}.login{max-width:420px;margin:10vh auto}")
                .append("label{display:block;margin:14px 0;font-weight:600}input{display:block;box-sizing:border-box;")
                .append("width:100%;padding:11px;margin-top:6px;border:1px solid #bac5d6;border-radius:7px}")
                .append("button,.button{display:inline-block;background:#2458c6;color:white;border:0;border-radius:7px;")
                .append("padding:10px 15px;text-decoration:none;cursor:pointer}button:disabled{opacity:.5}")
                .append("table{width:100%;border-collapse:collapse;overflow:hidden}th,td{text-align:left;")
                .append("padding:12px;border-bottom:1px solid #e8edf4}th{background:#edf2fa}")
                .append(".table-wrap{overflow-x:auto}.toolbar,.search,.actions{display:flex;align-items:center;")
                .append("justify-content:space-between;gap:14px}.search{padding:14px;margin:20px 0}.search input{")
                .append("max-width:400px;margin:0}.actions form{display:inline}.link{background:none;color:#2458c6;")
                .append("padding:4px}.danger,.error{color:#b42318}.error{background:#fff0ee;padding:12px;border-radius:8px}")
                .append(".muted,.empty{color:#65738b}.logout{text-align:right;margin:16px 0}.sr-only{position:absolute;")
                .append("width:1px;height:1px;overflow:hidden;clip:rect(0,0,0,0)}dl{display:grid;grid-template-columns:150px 1fr}")
                .append("dt,dd{padding:8px;margin:0;border-bottom:1px solid #eee}@media(max-width:600px){")
                .append("main{margin:24px auto}.toolbar{align-items:flex-start;flex-direction:column}.search{flex-wrap:wrap}}")
                .append("</style></head><body><main>")
                .append("<header><a href=\"").append(escape(response.encodeURL("mahasiswa")))
                .append("\" class=\"brand\">Praktikum RMI · Monitoring Mahasiswa</a></header>")
                .append("<h1 class=\"sr-only\">").append(escape(title)).append("</h1>")
                .append(body).append("</main></body></html>");
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
