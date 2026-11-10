**DG J-Code** 

**📌 Tentang Proyek**

**_DG J-Code_** adalah IDE yang sederhana untuk pengembangan perangkat lunak, pemodelan, serta *reverse* dan *forward engineering*. Namun, ukurannya yang besar, proses *setup* yang rumit, dan ketergantungan pada instalasi Java (JRE/JDK) terpisah sering kali menjadi hambatan bagi mahasiswa yang sedang praktikum atau pemula yang ingin cepat ngoding tanpa ribet konfigurasi environment.

**_DG J-Code_** hadir sebagai solusi: versi ringkas yang dioptimalkan untuk kebutuhan esensial **reverse engineering** (diagram UML, analisis struktur kode) dan *forward engineering* (generasi kode otomatis), dikemas dalam bentuk *standalone executable* yang bisa langsung dijalankan (**Zero-Config, No JRE Required**).

---

✨ Fitur Utama

**⚡ Zero-Config & No JRE Installation:** Tidak perlu repot mengunduh atau mengatur variabel lingkungan Java (JAVA_HOME / JRE). Cukup klik dua kali dan langsung jalankan!

**🔄 Reverse Engineering:** Mampu memindai struktur basis data, skrip, atau proyek kode yang ada dan mengubahnya kembali menjadi diagram atau model konseptual dengan cepat.

**🛠️ Forward Engineering:** Menghasilkan draf kerangka kode, skrip basis data, atau struktur kelas secara otomatis dari model rancangan Anda.

**🪶 Ultra-Lightweight:** Memangkas modul-modul berat yang tidak perlu, menghasilkan penggunaan RAM yang jauh lebih hemat dibandingkan versi standar NetBeans.

**🎓 Student & Beginner Friendly:** Antarmuka disederhanakan agar mudah dipahami, sangat cocok untuk kebutuhan praktikum kuliah, tugas perancangan sistem, dan pembelajaran cepat.

---

💻 Sistem yang Didukung

- OS: Windows (10 / 11), Linux, macOS
- Arsitektur: 64-bit
- Ketergantungan: Tidak ada (dipermudah, JRE sudah dibundel di dalam aplikasi / dikompilasi secara mandiri).

---

🚀 Cara Penggunaan

1. Masuk ke halaman **[Releases](../../releases)** repository ini.
2. Unduh file installer msi ini sesuai sistem operasi Anda ("DG-J-Code-Setup.msi").
3. Buka file installer msi dan ikuti instruksinya, dan aplikasi akan ada di Desktop anda.
4. Jalankan file eksekusi utama ("DG J-Code.exe").
5. Aplikasi siap digunakan untuk praktikum tanpa konfigurasi tambahan!

---

📂 Struktur Proyek

Folder
├── bin/          # Berkas eksekusi utama
├── config/       # Pengaturan konfigurasi ringan
├── modules/      # Modul esensial (Reverse/Forward Engineering)
└── README.md     # Dokumentasi proyek
