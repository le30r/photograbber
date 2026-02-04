package io.r03el.photograbber.server

import io.r03el.photograbber.model.Type
import io.r03el.photograbber.service.GalleryService

private val TEMPLATE: String = """
    <!DOCTYPE html>
    <html lang="ru">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
      <title>Свадебная галерея 💍</title>

      <style>
        :root {
          --gap: 12px;
          --radius: 10px;
          --bg: #fafafa;
        }

        body {
          margin: 0;
          padding: 16px;
          background: var(--bg);
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          color: #222;
        }

        header {
          margin-bottom: 16px;
        }

        header h1 {
          font-size: 22px;
          margin: 0;
        }

        header p {
          margin: 4px 0 0;
          color: #666;
          font-size: 14px;
        }

        /* ====== GALLERY ====== */

        .gallery {
          column-count: 4;
          column-gap: var(--gap);
        }

        @media (max-width: 1200px) {
          .gallery { column-count: 3; }
        }

        @media (max-width: 800px) {
          .gallery { column-count: 2; }
        }

        @media (max-width: 480px) {
          .gallery { column-count: 1; }
        }

        .gallery-item {
          break-inside: avoid;
          margin-bottom: var(--gap);
          cursor: zoom-in;
        }

        .gallery-item img {
          width: 100%;
          display: block;
          border-radius: var(--radius);
          background: #eaeaea;
          transition: transform 0.2s ease, box-shadow 0.2s ease;
        }
        
        .gallery-item video {
          width: 100%;
          display: block;
          border-radius: var(--radius);
          background: #eaeaea;
          transition: transform 0.2s ease, box-shadow 0.2s ease;
        }

        .gallery-item img:hover {
          transform: scale(1.015);
          box-shadow: 0 6px 20px rgba(0,0,0,0.12);
        }
        
        .gallery-item video:hover {
          transform: scale(1.015);
          box-shadow: 0 6px 20px rgba(0,0,0,0.12);
        }

        /* ====== FULLSCREEN VIEW ====== */

        .overlay {
          position: fixed;
          inset: 0;
          background: rgba(0,0,0,0.85);
          display: none;
          align-items: center;
          justify-content: center;
          z-index: 1000;
          padding: 16px;
        }

        .overlay img {
          max-width: 100%;
          max-height: 100%;
          border-radius: var(--radius);
          box-shadow: 0 0 40px rgba(0,0,0,0.4);
        }

        .overlay.visible {
          display: flex;
        }

        .overlay::after {
          content: "✕";
          position: absolute;
          top: 16px;
          right: 16px;
          font-size: 28px;
          color: white;
          cursor: pointer;
          opacity: 0.8;
        }

        .overlay::after:hover {
          opacity: 1;
        }
      </style>
    </head>

    <body>

    <header>
      <h1>📸 Свадебная галерея</h1>
      <p>Все фотографии, загруженные гостями</p>
    </header>

    <div class="gallery">
        {{GALLERY}}
    </div>

    <div class="overlay" id="overlay">
      <img id="overlayImage" src="">
    </div>

    <script>
      const overlay = document.getElementById("overlay");
      const overlayImage = document.getElementById("overlayImage");

      document.addEventListener("click", (e) => {
        const img = e.target.closest(".gallery-item img");
        if (!img) return;

        overlayImage.src = img.src;
        overlay.classList.add("visible");
      });

      overlay.addEventListener("click", () => {
        overlay.classList.remove("visible");
        overlayImage.src = "";
      });
    </script>

    </body>
    </html>

""".trimIndent()

class GalleryController(
    val galleryService: GalleryService
) {

    fun loadGallery(): String {
        return TEMPLATE.replace("{{GALLERY}}", formatGallery(galleryService.getImages()))
    }

    fun formatGallery(medias: List<String>): String {
        val sb: StringBuilder = StringBuilder()
        medias.forEach { media ->
            if (media.endsWith(".jpg")) {
                sb.append(
                    """
                       <div class="gallery-item">
                           <img src="$media" loading="lazy" onerror="this.style.display='none'">
                       </div>
                    """.trimIndent()
                )
            }
            if (media.endsWith(".mp4")) {
                sb.append(
                    """
                        <video class="gallery-item" controls preload="metadata">
                            <source src="$media" type="video/mp4">
                        </video>
                    """.trimIndent()
                )
            }
        }
        return sb.toString()
    }
}