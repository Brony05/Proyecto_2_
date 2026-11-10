// Sirve la carpeta docs/ para abrir la pizarra Scrum con el historial de Git.
// La pizarra necesita leer docs/historial-git.js, y eso solo funciona desde
// http:// o abriendo el archivo; este servidor es el camino corto:
//
//   node tools/servir-pizarra.mjs        (y abre http://127.0.0.1:4173/pizarra-scrum.html)
import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, join, normalize, extname } from 'node:path';

const docs = join(dirname(dirname(fileURLToPath(import.meta.url))), 'docs');
const puerto = Number(process.env.PUERTO || 4173);

const tipos = {
    '.html': 'text/html; charset=utf-8',
    '.js': 'text/javascript; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon'
};

const servidor = createServer(async (req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1');
    let ruta = decodeURIComponent(url.pathname);
    if (ruta === '/' || ruta === '') ruta = '/pizarra-scrum.html';

    const destino = normalize(join(docs, ruta));
    if (!destino.startsWith(docs)) {
        res.writeHead(403, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('Fuera de la carpeta docs/');
        return;
    }

    try {
        const cuerpo = await readFile(destino);
        res.writeHead(200, {
            'content-type': tipos[extname(destino).toLowerCase()] || 'application/octet-stream',
            'cache-control': 'no-store'
        });
        res.end(cuerpo);
    } catch (error) {
        res.writeHead(404, { 'content-type': 'text/plain; charset=utf-8' });
        res.end('No encontrado: ' + ruta);
    }
});

servidor.listen(puerto, '127.0.0.1', () => {
    console.log('Pizarra en http://127.0.0.1:' + puerto + '/pizarra-scrum.html');
    console.log('Ctrl+C para detener el servidor.');
});
