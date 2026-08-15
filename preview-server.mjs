import { createReadStream, existsSync, statSync } from "node:fs";
import { createServer } from "node:http";
import { extname, normalize, resolve } from "node:path";

const root = resolve(process.cwd());
const mime = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".md": "text/markdown; charset=utf-8",
  ".png": "image/png",
};

createServer((request, response) => {
  const pathname = decodeURIComponent((request.url ?? "/").split("?")[0]);
  const previewRootAssets = new Set(["/preview.css", "/preview.js"]);
  const relative = pathname === "/"
    ? "docs/index.html"
    : previewRootAssets.has(pathname)
      ? `docs${pathname}`
      : pathname.replace(/^\/+/, "");
  const target = resolve(root, normalize(relative));
  if (!target.startsWith(root) || !existsSync(target) || statSync(target).isDirectory()) {
    response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
    response.end("Sayfa bulunamadı.");
    return;
  }
  response.writeHead(200, { "Content-Type": mime[extname(target)] ?? "application/octet-stream", "Cache-Control": "no-store" });
  createReadStream(target).pipe(response);
}).listen(4173, "0.0.0.0", () => console.log("APK Cleaner tarayıcı önizlemesi 4173 bağlantı noktasında hazır."));
